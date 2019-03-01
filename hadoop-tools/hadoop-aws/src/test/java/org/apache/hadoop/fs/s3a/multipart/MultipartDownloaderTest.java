package org.apache.hadoop.fs.s3a.multipart;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.fs.contract.ContractTestUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class MultipartDownloaderTest {

    private final byte[] bytes = ContractTestUtils.dataset(1000, 0, 1000);

    private final ListeningExecutorService executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(2));

    private final MultipartDownloader multipartDownloader = new MultipartDownloader(
            100,
            executorService,
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

    @After
    public void after() {
        executorService.shutdownNow();
    }

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
        final CountDownLatch interrupted = new CountDownLatch(1);
        final CountDownLatch blockForever = new CountDownLatch(1);
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
                                blockForever.await();
                            } catch (InterruptedException e) {
                                interrupted.countDown();
                            }
                            return -1;
                        }
                    };
                }
            }
        };
        ListeningExecutorService executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(2));
        MultipartDownloader multipartDownloader = new MultipartDownloader(100, executorService, partDownloader, 10, 1000);
        multipartDownloader.download("bucket", "key", 0, 1000);

        assertTrue(interrupted.await(1, TimeUnit.SECONDS));
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
        final CountDownLatch interrupted = new CountDownLatch(1);
        final CountDownLatch blockForever = new CountDownLatch(1);

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
                            blockForever.await();
                        } catch (InterruptedException e) {
                            interrupted.countDown();
                        }
                        return -1;
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

        assertTrue(interrupted.await(1, TimeUnit.SECONDS));

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
}