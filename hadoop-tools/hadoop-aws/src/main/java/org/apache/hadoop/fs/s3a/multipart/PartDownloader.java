package org.apache.hadoop.fs.s3a.multipart;

import java.io.InputStream;

public interface PartDownloader {

    InputStream downloadPart(String bucket, String key, long rangeStart, long rangeEnd);

}
