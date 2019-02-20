package org.apache.hadoop.fs.s3a.multipart;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.tuple.Pair;

import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class OrderingQueue {

    private final PriorityQueue<Pair<Long, byte[]>> pendingWrites = new PriorityQueue<>();
    private final LinkedList<Pair<Long, byte[]>> availableWrites = new LinkedList<>();

    private final long totalSize;
    private final long bufferSize;

    private final Lock lock = new ReentrantLock();
    private final Condition writeAhead = lock.newCondition();
    private final Condition availableNotEmpty = lock.newCondition();

    private long nextOffset;
    private long readOffset;
    private RuntimeException exception;

    public OrderingQueue(long startingOffset, long totalSize, long bufferSize) {
        this.nextOffset = startingOffset;
        this.readOffset = startingOffset;
        this.totalSize = totalSize;
        this.bufferSize = bufferSize;
    }

    public void push(long offset, byte[] data) throws InterruptedException {
        Preconditions.checkArgument(offset >= 0);
        lock.lock();
        try {
            maybeThrowAlreadyClosed();
            while (offset >= readOffset + bufferSize) {
                writeAhead.await();
                maybeThrowAlreadyClosed();
            }

            pendingWrites.add(Pair.of(offset, data));

            Pair<Long, byte[]> lowestWrite = pendingWrites.peek();
            while (lowestWrite != null && lowestWrite.getLeft().longValue() == nextOffset) {
                availableWrites.add(pendingWrites.poll());
                availableNotEmpty.signalAll();

                nextOffset += lowestWrite.getRight().length;
                lowestWrite = pendingWrites.peek();
            }

        } finally {
            lock.unlock();
        }
    }

    public byte[] popInOrder() throws InterruptedException {
        lock.lock();
        try {
            maybeThrow();

            if (readOffset == totalSize) {
                return null;
            }

            while (availableWrites.size() == 0) {
                availableNotEmpty.await();
                maybeThrow();
            }

            Pair<Long, byte[]> availableWrite = availableWrites.remove();
            byte[] bytes = availableWrite.getRight();
            readOffset = availableWrite.getLeft() + bytes.length;
            writeAhead.signalAll();
            return bytes;
        } finally {
            lock.unlock();
        }
    }

    private void maybeThrow() {
        if (exception != null) {
            throw exception;
        }
    }

    private void maybeThrowAlreadyClosed() {
        if (exception != null) {
            throw new RuntimeException("Already closed");
        }
    }

    public void propagateException(RuntimeException exception) {
        lock.lock();
        try {
            if (this.exception == null) {
                this.exception = exception;
                availableNotEmpty.signalAll();
                writeAhead.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }
}
