package org.apache.hadoop.fs.s3a.multipart;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.thirdparty.apache.http.conn.socket.ConnectionSocketFactory;
import com.amazonaws.thirdparty.apache.http.conn.ssl.SSLConnectionSocketFactory;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MultipartDownloader {

    private final int partSize;
    private final ExecutorService downloadExecutorService;
    private final PartDownloader partDownloader;
    private final int chunkSize;

    public MultipartDownloader(int partSize, ExecutorService downloadExecutorService, PartDownloader partDownloader, int chunkSize) {
        this.partSize = partSize;
        this.downloadExecutorService = downloadExecutorService;
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
                    try (S3Object s3Object = partDownloader.downloadPart(bucket, key, partRangeStart, partRangeEnd); InputStream inputStream = s3Object.getObjectContent()) {
                        long currentOffset = partRangeStart;
                        byte[] chunk;

                        while ((chunk = readBytesGuaranteed(inputStream, chunkSize)).length > 0) {
                            deferQueue.addWriteAndRequestAvailable(currentOffset, chunk);
                            currentOffset += chunk.length;
                        }
                    } catch (Throwable e) {
                        e.printStackTrace();
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
                    System.out.println(writtenBytes);
                    byte[] bytes = deferQueue.popAvailableWrite();
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
                    e.printStackTrace();
                }
            }
        }).start();

        return pipedInputStream;
    }

    public static int read(InputStream in, byte[] b, int off, int len) throws IOException {
        if (len < 0) {
            throw new IndexOutOfBoundsException("len is negative");
        }
        int total = 0;
        while (total < len) {
            int result = in.read(b, off + total, len - total);
            if (result == -1) {
                break;
            }
            total += result;
        }
        return total;
    }

    private static byte[] readBytesGuaranteed(InputStream is, int numBytes) {
        byte[] bytes = new byte[numBytes];

        try {
            int read = read(is, bytes, 0, numBytes);

            if (read != numBytes) {
                return Arrays.copyOf(bytes, read);
            }

            return bytes;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
        MultipartDownloader multipartDownloader = new MultipartDownloader(8000000, downloadExecutorService, new PartDownloader() {
            @Override
            public S3Object downloadPart(String bucket, String key, long rangeStart, long rangeEnd) {
                return amazonS3.getObject(new GetObjectRequest(bucket, key).withRange(rangeStart, rangeEnd - 1));
            }
        }, 256000);

        InputStream inputStream = multipartDownloader.download("multiparttesting", "big-file.txt", 0, 438888890);
        Files.copy(inputStream, Paths.get("/Users/juang/Desktop/big-file-downloaded.txt"), StandardCopyOption.REPLACE_EXISTING);
        downloadExecutorService.shutdown();
    }
}
