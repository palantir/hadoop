package org.apache.hadoop.fs.s3a.multipart;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.util.concurrent.ExecutorService;

public final class MultipartDownloader {
    private static final Log LOG = LogFactory.getLog(MultipartDownloader.class);

    private final int partSize;
    private final ExecutorService downloadExecutorService;
    private final ExecutorService writingExecutorService;
    private final PartDownloader partDownloader;
    private final int chunkSize;
    private final int bufferSize;

    public MultipartDownloader(int partSize, ExecutorService downloadExecutorService, ExecutorService writingExecutorService, PartDownloader partDownloader, int chunkSize, int bufferSize) {
        this.partSize = partSize;
        this.downloadExecutorService = downloadExecutorService;
        this.writingExecutorService = writingExecutorService;
        this.partDownloader = partDownloader;
        this.chunkSize = chunkSize;
        this.bufferSize = bufferSize;
    }

    public InputStream download(final String bucket, final String key, long rangeStart, long rangeEnd) {
        final long size = rangeEnd - rangeStart;
        int numParts = (int) Math.ceil((double) size / partSize);

        final OrderingQueue orderingQueue = new OrderingQueue(rangeStart, bufferSize);

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
                        byte[] bytes = orderingQueue.popInOrder();
                        try {
                            pipedOutputStream.write(bytes);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                        writtenBytes += bytes.length;
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

            downloadExecutorService.submit(new Runnable() {
                @Override
                public void run() {
                    LOG.info(String.format("Downloading part %d - %d", partRangeStart, partRangeEnd));
                    try (DataInputStream inputStream = new DataInputStream(partDownloader.downloadPart(bucket, key, partRangeStart, partRangeEnd).getObjectContent())) {
                        long currentOffset = partRangeStart;
                        while (currentOffset < partRangeEnd) {
                            int bytesLeft = (int) (partRangeEnd - currentOffset);
                            byte[] chunk = new byte[bytesLeft > chunkSize ? chunkSize : bytesLeft];
                            inputStream.readFully(chunk);

                            LOG.info("Pushing offset: " + currentOffset);
                            orderingQueue.push(currentOffset, chunk);
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
