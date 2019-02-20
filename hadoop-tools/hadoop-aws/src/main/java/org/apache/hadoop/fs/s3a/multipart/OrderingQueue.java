package org.apache.hadoop.fs.s3a.multipart;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public final class OrderingQueue {

    private final PriorityQueue<Entry> pendingWrites = new PriorityQueue<>(10, new Comparator<Entry>() {
        @Override
        public int compare(Entry o1, Entry o2) {
            return (int) (o1.getOffset() - o2.getOffset());
        }
    });

    private final BlockingQueue<Entry> availableWrites = new LinkedBlockingQueue<>();
    private Long nextOffset;

    public OrderingQueue(Long startingOffset) {
        this.nextOffset = startingOffset;
    }

    public synchronized ListenableFuture<?> push(long offset, byte[] data) {
        SettableFuture<Void> future = SettableFuture.create();
        pendingWrites.add(new Entry(offset, data, future));

        Entry peek;
        while (((peek = pendingWrites.peek()) != null) && peek.getOffset() == nextOffset) {
            availableWrites.add(pendingWrites.poll());
            nextOffset += peek.getData().length;
        }

        return future;
    }

    public Pair<byte[], SettableFuture<Void>> popInOrder() {
        try {
            Entry take = availableWrites.take();
            return Pair.of(take.getData(), take.getFuture());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private static final class Entry {
        private final long offset;
        private final byte[] data;
        private final SettableFuture<Void> future;

        private Entry(long offset, byte[] data, SettableFuture<Void> future) {
            this.offset = offset;
            this.data = data;
            this.future = future;
        }

        public long getOffset() {
            return offset;
        }

        public byte[] getData() {
            return data;
        }

        public SettableFuture<Void> getFuture() {
            return future;
        }
    }
}
