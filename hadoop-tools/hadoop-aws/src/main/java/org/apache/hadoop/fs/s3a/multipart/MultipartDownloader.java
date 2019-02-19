package org.apache.hadoop.fs.s3a.multipart;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.thirdparty.apache.http.conn.socket.ConnectionSocketFactory;
import com.amazonaws.thirdparty.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.net.ssl.SSLContext;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public final class MultipartDownloader {
    private static final Log LOG = LogFactory.getLog(MultipartDownloader.class);

    private final int partSize;
    private final ExecutorService downloadExecutorService;
    private final ExecutorService writingExecutorService;
    private final PartDownloader partDownloader;
    private final int chunkSize;
    private final Semaphore semaphore;

    public MultipartDownloader(int partSize, ExecutorService downloadExecutorService, ExecutorService writingExecutorService, PartDownloader partDownloader, int chunkSize, int concurrentChunks) {
        this.partSize = partSize;
        this.downloadExecutorService = downloadExecutorService;
        this.writingExecutorService = writingExecutorService;
        this.partDownloader = partDownloader;
        this.chunkSize = chunkSize;
        semaphore = new Semaphore(concurrentChunks);
    }

    public InputStream download(final String bucket, final String key, long rangeStart, long rangeEnd) {
        final long size = rangeEnd - rangeStart;
        int numParts = (int) Math.ceil((double) size / partSize);

        final DeferQueue deferQueue = new DeferQueue(rangeStart);

        for (int i = 0; i < numParts; i++) {
            final long partRangeStart = rangeStart + i * partSize;
            final long partRangeEnd = i == numParts - 1 ? rangeEnd : partRangeStart + partSize;

            downloadExecutorService.submit(new Runnable() {
                @Override
                public void run() {
                    LOG.info(String.format("Downloading part %d - %d", partRangeStart, partRangeEnd));
                    try (S3Object s3Object = partDownloader.downloadPart(bucket, key, partRangeStart, partRangeEnd);
                         DataInputStream inputStream = new DataInputStream(s3Object.getObjectContent())) {
                        long currentOffset = partRangeStart;

                        while (currentOffset < partRangeEnd) {
                            try {
                                semaphore.acquire();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                throw new RuntimeException(e);
                            }

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

                        semaphore.release();
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

        return pipedInputStream;
    }

    public static void main(String[] args) throws NoSuchAlgorithmException, IOException {
        ConnectionSocketFactory connectionSocketFactory = new SSLConnectionSocketFactory(
                SSLContext.getDefault(),
                new String[] {"TLSv1.2"},
                null, null);
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.getApacheHttpClientConfig().setSslSocketFactory(connectionSocketFactory);
        final AmazonS3 amazonS3 = AmazonS3ClientBuilder.standard()
                .withClientConfiguration(clientConfiguration)
                .build();

        ExecutorService downloadExecutorService = Executors.newFixedThreadPool(8);
        ExecutorService writingExecutorService = Executors.newCachedThreadPool();
        MultipartDownloader multipartDownloader = new MultipartDownloader(8000000, downloadExecutorService, writingExecutorService, new PartDownloader() {
            @Override
            public S3Object downloadPart(String bucket, String key, long rangeStart, long rangeEnd) {
                return amazonS3.getObject(new GetObjectRequest(bucket, key).withRange(rangeStart, rangeEnd - 1));
            }
        }, 256000, 400);

        InputStream inputStream = multipartDownloader.download("multiparttesting", "fairscheduler.xml", 0, 101);
        Files.copy(inputStream, Paths.get("/Users/juang/Desktop/fairscheduler.xml"), StandardCopyOption.REPLACE_EXISTING);
        downloadExecutorService.shutdown();
        writingExecutorService.shutdown();;
    }
}
