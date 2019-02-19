package org.apache.hadoop.fs.s3a.multipart;

import com.amazonaws.services.s3.model.S3Object;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;

public final class MultipartDownloader {
    private static final Log LOG = LogFactory.getLog(MultipartDownloader.class);

    private final int partSize;
    private final ExecutorService downloadExecutorService;
    private final ExecutorService writingExecutorService;
    private final PartDownloader partDownloader;
    private final int chunkSize;
    private final Semaphore semaphore;

    public MultipartDownloader(int partSize, ExecutorService downloadExecutorService, ExecutorService writingExecutorService, PartDownloader partDownloader, int chunkSize, int concurrentParts) {
        this.partSize = partSize;
        this.downloadExecutorService = downloadExecutorService;
        this.writingExecutorService = writingExecutorService;
        this.partDownloader = partDownloader;
        this.chunkSize = chunkSize;
        semaphore = new Semaphore(concurrentParts);
    }

    public InputStream download(final String bucket, final String key, long rangeStart, long rangeEnd) {
        final long size = rangeEnd - rangeStart;
        int numParts = (int) Math.ceil((double) size / partSize);

        final DeferQueue deferQueue = new DeferQueue(rangeStart);

        final PipedOutputStream pipedOutputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream;
        try {
            pipedInputStream = new PipedInputStream(pipedOutputStream, chunkSize);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        writingExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    long writtenBytes = 0;
                    while (writtenBytes < size) {
                        LOG.debug("Writing out bytes for offset " + writtenBytes);
                        byte[] bytes = deferQueue.popAvailableWrite();
                        try {
                            pipedOutputStream.write(bytes);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                        writtenBytes += bytes.length;

                        // release every part bytes
                        if (writtenBytes % partSize == 0 || writtenBytes == size) {
                            semaphore.release();
                        }
                    }
                    try {
                        pipedOutputStream.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } catch (Throwable e) {
                    LOG.error("Exception caught while writing part", e);
                }
            }
        });

        for (int i = 0; i < numParts; i++) {
            final long partRangeStart = rangeStart + i * partSize;
            final long partRangeEnd = i == numParts - 1 ? rangeEnd : partRangeStart + partSize;

            try {
                semaphore.acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }

            downloadExecutorService.submit(new Runnable() {
                @Override
                public void run() {
                    LOG.info(String.format("Downloading part %d - %d", partRangeStart, partRangeEnd));
                    try (S3Object s3Object = partDownloader.downloadPart(bucket, key, partRangeStart, partRangeEnd);
                        DataInputStream inputStream = new DataInputStream(s3Object.getObjectContent())) {
                        long currentOffset = partRangeStart;
                        while (currentOffset < partRangeEnd) {
                            int bytesLeft = (int) (partRangeEnd - currentOffset);
                            byte[] chunk = new byte[bytesLeft > chunkSize ? chunkSize : bytesLeft];
                            inputStream.readFully(chunk);
                            deferQueue.addWriteAndRequestAvailable(currentOffset, chunk);
                            currentOffset += chunk.length;
                        }
                    } catch (Throwable e) {
                        LOG.error("Exception caught while downloading part", e);
                    }
                }
            });
        }

        return pipedInputStream;
    }

}
