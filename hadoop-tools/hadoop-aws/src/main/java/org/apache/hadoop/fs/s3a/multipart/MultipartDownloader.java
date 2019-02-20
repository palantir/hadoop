package org.apache.hadoop.fs.s3a.multipart;

import com.amazonaws.services.s3.model.S3Object;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public final class MultipartDownloader {
    private static final Log LOG = LogFactory.getLog(MultipartDownloader.class);

    private final long partSize;
    private final ListeningExecutorService downloadExecutorService;
    private final PartDownloader partDownloader;
    private final long chunkSize;
    private final long bufferSize;

    public MultipartDownloader(long partSize, ListeningExecutorService downloadExecutorService, PartDownloader partDownloader, long chunkSize, long bufferSize) {
        this.partSize = partSize;
        this.downloadExecutorService = downloadExecutorService;
        this.partDownloader = partDownloader;
        this.chunkSize = chunkSize;
        this.bufferSize = bufferSize;
    }

    public InputStream download(final String bucket, final String key, long rangeStart, long rangeEnd) {
        final long size = rangeEnd - rangeStart;
        int numParts = (int) Math.ceil((double) size / partSize);

        final OrderingQueue orderingQueue = new OrderingQueue(rangeStart, size, bufferSize);

        final List<ListenableFuture<?>> partFutures = Lists.newArrayList();

        for (long i = 0; i < numParts; i++) {
            final long partRangeStart = rangeStart + i * partSize;
            final long partRangeEnd = i == numParts - 1 ? rangeEnd : partRangeStart + partSize;

            ListenableFuture<?> partFuture = downloadExecutorService.submit(new Runnable() {
                @Override
                public void run() {
                    LOG.info(String.format("Downloading part %d - %d", partRangeStart, partRangeEnd));
                    try (S3Object s3Object = partDownloader.downloadPart(bucket, key, partRangeStart, partRangeEnd);
                         DataInputStream inputStream = new DataInputStream(s3Object.getObjectContent())) {
                        long currentOffset = partRangeStart;
                        while (currentOffset < partRangeEnd) {
                            long bytesLeft = partRangeEnd - currentOffset;
                            long bytesToRead = bytesLeft > chunkSize ? chunkSize : bytesLeft;

                            byte[] chunk = new byte[(int) bytesToRead];
                            inputStream.readFully(chunk);

                            LOG.debug("Pushing offset: " + currentOffset);
                            orderingQueue.push(currentOffset, chunk);
                            currentOffset += chunk.length;
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
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
                    orderingQueue.propagateException(new RuntimeException("Exception caught while downloading part", throwable));
                    cancelAllDownloads(partFutures);
                }
            });
        }

        return new OrderingQueueInputStream(orderingQueue, new Runnable() {
            @Override
            public void run() {
                cancelAllDownloads(partFutures);
            }
        });
    }

    private void cancelAllDownloads(List<ListenableFuture<?>> partFutures) {
        for (ListenableFuture<?> partFuture : partFutures) {
            partFuture.cancel(false);
        }
    }
}
