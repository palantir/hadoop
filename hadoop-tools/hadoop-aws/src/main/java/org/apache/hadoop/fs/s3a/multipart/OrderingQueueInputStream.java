package org.apache.hadoop.fs.s3a.multipart;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PushbackInputStream;

public final class OrderingQueueInputStream extends AbortableInputStream {

    private final OrderingQueue orderingQueue;
    private final Runnable closeAction;
    private final Runnable abortAction;

    private PushbackInputStream inputStream = new PushbackInputStream(new ByteArrayInputStream(new byte[0]));

    public OrderingQueueInputStream(OrderingQueue orderingQueue, Runnable closeAction, Runnable abortAction) {
        this.orderingQueue = orderingQueue;
        this.closeAction = closeAction;
        this.abortAction = abortAction;
    }

    @Override
    public int read() throws IOException {
        fillByteArrayInputStream();
        return inputStream.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        fillByteArrayInputStream();
        return inputStream.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        fillByteArrayInputStream();
        return inputStream.read(b, off, len);
    }

    @Override
    public int available() throws IOException {
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

    private void fillByteArrayInputStream() throws IOException {
        int read = inputStream.read();
        if (read == -1) {
            try {
                byte[] bytes = orderingQueue.popInOrder();
                if (bytes != null) {
                    inputStream = new PushbackInputStream(new ByteArrayInputStream(bytes));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        } else {
            inputStream.unread(read);
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
