package org.apache.hadoop.fs.s3a.multipart;

import com.google.common.io.ByteStreams;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;

public class MultipartDownloader {

    private final int partSize;
    private final ExecutorService downloadExecutorService;
    private final ExecutorService writesExecutorService;
    private final PartDownloader partDownloader;
    private final int chunkSize;

    public MultipartDownloader(int partSize, ExecutorService downloadExecutorService, ExecutorService writesExecutorService, PartDownloader partDownloader, int chunkSize) {
        this.partSize = partSize;
        this.downloadExecutorService = downloadExecutorService;
        this.writesExecutorService = writesExecutorService;
        this.partDownloader = partDownloader;
        this.chunkSize = chunkSize;
    }

    public InputStream download(final String bucket, final String key, long rangeStart, long rangeEnd) {
        final long size = rangeEnd - rangeStart;
        int numParts = (int) Math.ceil((double) size / partSize);

        final DeferQueue deferQueue = new DeferQueue();

        for (int i = 0; i < numParts; i++) {
            final long partRangeStart = rangeStart + i * partSize;
            final long partRangeEnd = i == numParts - 1 ? rangeEnd : partRangeStart + partSize;

            downloadExecutorService.submit(new Runnable() {
                @Override
                public void run() {
                    InputStream partInputStream = partDownloader.downloadPart(bucket, key, partRangeStart, partRangeEnd);

                    long currentOffset = partRangeStart;
                    byte[] chunk;
                    while ((chunk = readBytesGuaranteed(partInputStream, chunkSize)).length > 0) {
                        deferQueue.addWriteAndRequestAvailable(currentOffset, chunk);
                        currentOffset += chunk.length;
                    }
                }
            });
        }

        final PipedOutputStream pipedOutputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream;
        try {
            pipedInputStream = new PipedInputStream(pipedOutputStream, chunkSize);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                long writtenBytes = 0;
                while (writtenBytes < size) {
                    byte[] bytes = deferQueue.popAvailableWrite();
                    try {
                        pipedOutputStream.write(bytes);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    writtenBytes += bytes.length;
                }
            }
        }).start();

        return pipedInputStream;
    }

    private static byte[] readBytesGuaranteed(InputStream is, int numBytes) {
        byte[] bytes = new byte[numBytes];

        try {
            int read = ByteStreams.read(is, bytes, 0, numBytes);

            if (read != numBytes) {
                return Arrays.copyOf(bytes, read);
            }

            return bytes;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {

    }
}
