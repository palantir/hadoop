package org.apache.hadoop.fs.s3a.multipart;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public final class OrderingQueueInputStream extends AbortableInputStream {

    private final OrderingQueue orderingQueue;
    private final Runnable closeAction;
    private final Runnable abortAction;

    private ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[0]);

    public OrderingQueueInputStream(OrderingQueue orderingQueue, Runnable closeAction, Runnable abortAction) {
        this.orderingQueue = orderingQueue;
        this.closeAction = closeAction;
        this.abortAction = abortAction;
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

    @Override
    /**
     * Do same thing as close since we have small part downloads.
     */
    public void abort() throws IOException {
        inputStream.close();
        abortAction.run();
    }
}
