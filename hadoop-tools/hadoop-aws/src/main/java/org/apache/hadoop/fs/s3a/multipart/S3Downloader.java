package org.apache.hadoop.fs.s3a.multipart;

public interface S3Downloader {
    AbortableInputStream download(String bucket, String key, long rangeStart, long rangeEnd);
}
