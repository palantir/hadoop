package org.apache.hadoop.fs.s3a.multipart;

import java.io.IOException;
import java.io.InputStream;

public abstract class AbortableInputStream extends InputStream {

    public abstract void abort() throws IOException;
}
