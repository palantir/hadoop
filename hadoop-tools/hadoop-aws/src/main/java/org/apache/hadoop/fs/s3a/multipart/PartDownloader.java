package org.apache.hadoop.fs.s3a.multipart;

import com.amazonaws.services.s3.model.S3Object;

public interface PartDownloader {

    S3Object downloadPart(String bucket, String key, long rangeStart, long rangeEnd);

}
