package org.apache.hadoop.fs.s3a.multipart;

import org.apache.commons.lang3.tuple.Pair;

import java.util.PriorityQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public final class DeferQueue {

    private final PriorityQueue<Pair<Long, byte[]>> pendingWrites = new PriorityQueue<>();
    private final BlockingQueue<byte[]> availableWrites = new LinkedBlockingQueue<>();
    private Long nextOffset = 0L;

    public synchronized void addWriteAndRequestAvailable(long offset, byte[] data) {
        pendingWrites.add(Pair.of(offset, data));

        while (pendingWrites.peek().getLeft().equals(nextOffset)) {
            byte[] bytes = pendingWrites.poll().getRight();
            availableWrites.add(bytes);
            nextOffset += bytes.length;
        }
    }

    public synchronized byte[] popAvailableWrite() {
        try {
            return availableWrites.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
