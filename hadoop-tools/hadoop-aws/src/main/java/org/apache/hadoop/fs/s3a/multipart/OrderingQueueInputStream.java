package org.apache.hadoop.fs.s3a.multipart;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class OrderingQueueInputStream extends InputStream {

    private final OrderingQueue orderingQueue;
    private final Runnable closeAction;

    private ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[0]);

    public OrderingQueueInputStream(OrderingQueue orderingQueue, Runnable closeAction) {
        this.orderingQueue = orderingQueue;
        this.closeAction = closeAction;
    }

    @Override
    public int read() {
        fillByteArrayInputStream();
        return inputStream.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        fillByteArrayInputStream();
        return inputStream.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) {
        fillByteArrayInputStream();
        return inputStream.read(b, off, len);
    }

    @Override
    public long skip(long n) {
        fillByteArrayInputStream();
        return inputStream.skip(n);
    }

    @Override
    public int available() {
        fillByteArrayInputStream();
        return inputStream.available();
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
        closeAction.run();
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    private void fillByteArrayInputStream() {
        if (inputStream.available() == 0) {
            try {
                byte[] bytes = orderingQueue.popInOrder();
                if (bytes != null) {
                    inputStream = new ByteArrayInputStream(bytes);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
    }
}
