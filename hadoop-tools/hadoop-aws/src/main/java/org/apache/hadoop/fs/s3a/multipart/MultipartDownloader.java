package org.apache.hadoop.fs.s3a.multipart;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.util.List;
import java.util.concurrent.ExecutorService;

public final class MultipartDownloader {
    private static final Log LOG = LogFactory.getLog(MultipartDownloader.class);

    private final int partSize;
    private final ExecutorService downloadExecutorService;
    private final ExecutorService writingExecutorService;
    private final PartDownloader partDownloader;
    private final int chunkSize;

    public MultipartDownloader(int partSize, ExecutorService downloadExecutorService, ExecutorService writingExecutorService, PartDownloader partDownloader, int chunkSize) {
        this.partSize = partSize;
        this.downloadExecutorService = downloadExecutorService;
        this.writingExecutorService = writingExecutorService;
        this.partDownloader = partDownloader;
        this.chunkSize = chunkSize;
    }

    public InputStream download(final String bucket, final String key, long rangeStart, long rangeEnd) {
        final long size = rangeEnd - rangeStart;
        int numParts = (int) Math.ceil((double) size / partSize);

        final OrderingQueue orderingQueue = new OrderingQueue(rangeStart);

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
                        Pair<byte[], SettableFuture<Void>> pair = orderingQueue.popInOrder();
                        byte[] chunk = pair.getLeft();
                        try {
                            pipedOutputStream.write(chunk);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                        writtenBytes += chunk.length;
                        SettableFuture<Void> settableFuture = pair.getRight();
                        settableFuture.set(null);
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
                        List<ListenableFuture<?>> chunkFutures = Lists.newArrayList();
                        while (currentOffset < partRangeEnd) {
                            int bytesLeft = (int) (partRangeEnd - currentOffset);
                            byte[] chunk = new byte[bytesLeft > chunkSize ? chunkSize : bytesLeft];
                            inputStream.readFully(chunk);

                            chunkFutures.add(orderingQueue.push(currentOffset, chunk));
                            currentOffset += chunk.length;
                        }

                        // Block until all chunks written
                        Futures.allAsList(chunkFutures).get();
                    } catch (Throwable e) {
                        LOG.error("Exception caught while downloading part", e);
                    }
                }
            });
        }

        return pipedInputStream;
    }

}
