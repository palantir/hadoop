package org.apache.hadoop.fs.s3a.multipart;

import com.google.common.base.Charsets;
import com.google.common.util.concurrent.MoreExecutors;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.Executors;

public class MultipartDownloaderTest {

    private final byte[] bytes = generateBytes(0, 1000);

    private final MultipartDownloader multipartDownloader = new MultipartDownloader(
            100,
            MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(2)),
            new PartDownloader() {
                @Override
                public InputStream downloadPart(String bucket, String key, long rangeStart, long rangeEnd) {
                    return new ByteArrayInputStream(Arrays.copyOfRange(bytes, (int) rangeStart, (int) rangeEnd));
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