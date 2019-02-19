package org.apache.hadoop.fs.s3a.multipart;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.thirdparty.apache.http.conn.socket.ConnectionSocketFactory;
import com.amazonaws.thirdparty.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.junit.Test;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MultipartDownloaderTest {

    @Test
    public void testDownload() throws NoSuchAlgorithmException, IOException {
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
        }, 256000, 16);

        InputStream inputStream = multipartDownloader.download("multiparttesting", "fairscheduler.xml", 0, 101);
        Files.copy(inputStream, Paths.get("/Users/juang/Desktop/fairscheduler.xml"), StandardCopyOption.REPLACE_EXISTING);
        downloadExecutorService.shutdown();
        writingExecutorService.shutdown();
    }
}