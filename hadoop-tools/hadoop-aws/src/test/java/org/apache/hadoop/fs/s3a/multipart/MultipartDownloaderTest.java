package org.apache.hadoop.fs.s3a.multipart;

import com.google.common.base.Charsets;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MultipartDownloaderTest {

    private final byte[] bytes = generateBytes(0, 1000);

    private final MultipartDownloader multipartDownloader = new MultipartDownloader(
            100,
            MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(2)),
            new S3Downloader() {
                @Override
                public AbortableInputStream download(String bucket, String key, long rangeStart, long rangeEnd) {
                    final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(Arrays.copyOfRange(bytes, (int) rangeStart, (int) rangeEnd));
                    return new AbortableInputStream() {
                        @Override
                        public void abort() throws IOException {
                            close();
                        }

                        @Override
                        public int read() {
                            return byteArrayInputStream.read();
                        }
                    };
                }
            },
            10,
            1000);

    @Test
    public void testDownloadAll() throws IOException {
        assertDownloadRange(0, bytes.length);
    }

    @Test
    public void testDownloadRange() throws IOException {
        assertDownloadRange(10, 20);
    }

    @Test
    public void testDownloadTail() throws IOException {
        assertDownloadRange(bytes.length - 10, bytes.length);
    }

    @Test
    public void testCancelPartsIfFailure() throws InterruptedException {
        S3Downloader partDownloader = new S3Downloader() {
            @Override
            public AbortableInputStream download(String bucket, String key, long rangeStart, long rangeEnd) {
                if (rangeStart == 0) {
                    throw new RuntimeException();
                } else {
                    return new AbortableInputStream() {
                        @Override
                        public void abort() throws IOException {
                            close();
                        }

                        @Override
                        public int read() {
                            try {
                                Thread.sleep(1000000000);
                                return -1;
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    };
                }
            }
        };
        ListeningExecutorService executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(2));
        MultipartDownloader multipartDownloader = new MultipartDownloader(100, executorService, partDownloader, 10, 1000);
        multipartDownloader.download("bucket", "key", 0, 1000);

        executorService.shutdown();
        assertTrue(executorService.awaitTermination(1, TimeUnit.SECONDS));
    }

    @Test
    public void testCancelPartsIfClosed() throws InterruptedException, IOException {
        testCompletion(false);
    }

    @Test
    public void testPropagateAbortIfAborted() throws InterruptedException, IOException {
        testCompletion(true);
    }

    private void testCompletion(boolean checkAborted) throws IOException, InterruptedException {
        final AtomicBoolean closed = new AtomicBoolean();
        final AtomicBoolean aborted = new AtomicBoolean();

        S3Downloader partDownloader = new S3Downloader() {
            @Override
            public AbortableInputStream download(String bucket, String key, long rangeStart, long rangeEnd) {
                return new AbortableInputStream() {
                    @Override
                    public void abort() {
                        aborted.set(true);
                    }

                    @Override
                    public int read() {
                        try {
                            Thread.sleep(1000000000);
                            return -1;
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public void close() {
                        closed.set(true);
                    }
                };
            }
        };
        ListeningExecutorService executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(2));
        MultipartDownloader multipartDownloader = new MultipartDownloader(100, executorService, partDownloader, 10, 1000);
        AbortableInputStream download = multipartDownloader.download("bucket", "key", 0, 1000);
        if (checkAborted) {
            download.abort();
        } else {
            download.close();
        }

        executorService.shutdown();
        assertTrue(executorService.awaitTermination(1, TimeUnit.SECONDS));
        if (checkAborted) {
            assertFalse(closed.get());
            assertTrue(aborted.get());
        } else {
            assertFalse(aborted.get());
            assertTrue(closed.get());
        }
    }


    private void assertDownloadRange(int from, int to) throws IOException {
        Assert.assertArrayEquals(
                Arrays.copyOfRange(bytes, from, to),
                IOUtils.toByteArray(multipartDownloader.download("bucket", "key",  from, to)));
    }

    private byte[] generateBytes(int from, int to) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = from; i < to; i++) {
            stringBuilder.append(i);
            stringBuilder.append("\n");
        }
        return stringBuilder.toString().getBytes(Charsets.UTF_8);
    }
}