package org.apache.hadoop.fs.s3a.multipart;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

public final class MultipartDownloader implements S3Downloader {
    private static final Logger LOG = LoggerFactory.getLogger(MultipartDownloader.class);

    private final long partSize;
    private final ListeningExecutorService downloadExecutorService;
    private final S3Downloader partDownloader;
    private final long chunkSize;
    private final long bufferSize;

    public MultipartDownloader(long partSize, ListeningExecutorService downloadExecutorService, S3Downloader partDownloader, long chunkSize, long bufferSize) {
        Preconditions.checkArgument(partSize > 0);
        Preconditions.checkArgument(chunkSize > 0);
        Preconditions.checkArgument(bufferSize > 0);
        Preconditions.checkArgument(chunkSize <= bufferSize);
        Preconditions.checkArgument(chunkSize <= partSize);

        this.partSize = partSize;
        this.downloadExecutorService = downloadExecutorService;
        this.partDownloader = partDownloader;
        this.chunkSize = chunkSize;
        this.bufferSize = bufferSize;
    }

    @Override
    public AbortableInputStream download(final String bucket, final String key, long rangeStart, long rangeEnd) {
        Preconditions.checkArgument(rangeEnd > rangeStart, "Range end must be larger than range start");
        Preconditions.checkArgument(rangeStart >= 0, "Range end must be non-negative");

        final long size = rangeEnd - rangeStart;
        int numParts = (int) Math.ceil((double) size / partSize);

        final OrderingQueue orderingQueue = new OrderingQueue(rangeStart, size, bufferSize);

        final List<ListenableFuture<?>> partFutures = new ArrayList<>(numParts);
        final AtomicBoolean isAbort = new AtomicBoolean();

        for (long i = 0; i < numParts; i++) {
            final long partRangeStart = rangeStart + i * partSize;
            final long partRangeEnd = i == numParts - 1 ? rangeEnd : partRangeStart + partSize;

            ListenableFuture<?> partFuture = downloadExecutorService.submit(new Runnable() {
                @Override
                public void run() {
                    LOG.info("Downloading part {} - {}", partRangeStart, partRangeEnd);
                    // Since the parts should be small, we should be able to just close the streams instead of abort.
                    // try-with-resources will call close when we get interrupted
                    AbortableInputStream abortableInputStream = partDownloader.download(bucket, key, partRangeStart, partRangeEnd);
                    try {
                        DataInputStream dataInputStream = new DataInputStream(abortableInputStream);
                        long currentOffset = partRangeStart;
                        while (currentOffset < partRangeEnd) {
                            long bytesLeft = partRangeEnd - currentOffset;
                            int bytesToRead = toIntExact(bytesLeft > chunkSize ? chunkSize : bytesLeft);

                            byte[] chunk = new byte[bytesToRead];
                            dataInputStream.readFully(chunk);

                            LOG.debug("Pushing offset: {}", currentOffset);
                            orderingQueue.push(currentOffset, chunk);
                            currentOffset += chunk.length;
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    } finally {
                        if (abortableInputStream != null) {
                            try {
                                if (isAbort.get()) {
                                    abortableInputStream.abort();
                                } else {
                                    abortableInputStream.close();
                                }
                            } catch (IOException e) {
                                // TODO use try-with-resources
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
            });

            partFutures.add(partFuture);
        }

        for (ListenableFuture<?> partFuture : partFutures) {
            Futures.addCallback(partFuture, new FutureCallback<Object>() {
                @Override
                public void onSuccess(Object o) {

                }

                @Override
                public void onFailure(Throwable throwable) {
                    orderingQueue.closeWithException(new RuntimeException("Exception caught while downloading part", throwable));
                    cancelAllDownloads(partFutures);
                }
            }, DirectExecutor.INSTANCE);
        }

        return new OrderingQueueInputStream(orderingQueue, new Runnable() {
            @Override
            public void run() {
                isAbort.set(false);
                cancelAllDownloads(partFutures);
            }
        }, new Runnable() {
            @Override
            public void run() {
                isAbort.set(true);
                cancelAllDownloads(partFutures);
            }
        });
    }

    private void cancelAllDownloads(List<ListenableFuture<?>> partFutures) {
        for (ListenableFuture<?> partFuture : partFutures) {
            partFuture.cancel(true);
        }
    }

    /**
     * Copied from Guava's MoreExecutors#directExecutor.
     * <p>
     * Had to copy this because directExecutor doesn't exist in Guava 11.0, and sameThreadExecutor
     * from Guava 11.0 doesn't exist in newer versions of Guava.
     */
    private enum DirectExecutor implements Executor {
        INSTANCE;

        @Override
        public void execute(Runnable command) {
            command.run();
        }

        @Override
        public String toString() {
            return "MultipartDownloader.directExecutor()";
        }
    }

    /**
     * Copied from Math#toIntExact from Java 7.
     * @param value
     * @return
     */
    private static int toIntExact(long value) {
        if ((int)value != value) {
            throw new ArithmeticException("integer overflow");
        }
        return (int)value;
    }
}
