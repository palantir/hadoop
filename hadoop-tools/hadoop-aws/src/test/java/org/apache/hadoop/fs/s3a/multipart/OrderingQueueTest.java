package org.apache.hadoop.fs.s3a.multipart;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class OrderingQueueTest {

    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    @Test
    public void testPopBlocksForNextOffset() throws InterruptedException {
        final OrderingQueue orderingQueue = new OrderingQueue(0, 100, 10);
        final CountDownLatch popLatch = new CountDownLatch(1);

        executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    orderingQueue.popInOrder();
                    popLatch.countDown();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        orderingQueue.push(5, new byte[5]);
        assertFalse(popLatch.await(1, TimeUnit.SECONDS));
        orderingQueue.push(0, new byte[5]);
        assertTrue(popLatch.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void testPushBlocksWhenFull() throws InterruptedException {
        final OrderingQueue orderingQueue = new OrderingQueue(0, 100, 10);
        final CountDownLatch pushLatch = new CountDownLatch(1);

        byte[] firstBytes = new byte[10];
        orderingQueue.push(0, firstBytes);

        executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    orderingQueue.push(10, new byte[1]);
                    pushLatch.countDown();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        assertFalse(pushLatch.await(1, TimeUnit.SECONDS));
        assertEquals(firstBytes, orderingQueue.popInOrder());
        assertTrue(pushLatch.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void testPopReturnsNullIfFinished() throws InterruptedException {
        final OrderingQueue orderingQueue = new OrderingQueue(0, 20, 10);
        orderingQueue.push(0, new byte[10]);
        assertNotNull(orderingQueue.popInOrder());
        orderingQueue.push(10, new byte[10]);
        assertNotNull(orderingQueue.popInOrder());
        orderingQueue.push(20, new byte[10]);
        assertNull(orderingQueue.popInOrder());
    }

    @Test
    public void testCanCloseWithException() throws InterruptedException {
        final OrderingQueue orderingQueue = new OrderingQueue(0, 20, 10);

        final CountDownLatch blockedWriteCaughtException = new CountDownLatch(1);
        final CountDownLatch blockedReadCaughtException = new CountDownLatch(1);

        final RuntimeException myException = new RuntimeException("My Exception");

        orderingQueue.push(0, new byte[5]);
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    orderingQueue.push(5, new byte[6]);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (RuntimeException e) {
                    if (e.equals(myException)) {
                        blockedWriteCaughtException.countDown();
                    }
                }
            }
        });

        executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    orderingQueue.popInOrder();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (RuntimeException e) {
                    if (e.equals(myException)) {
                        blockedReadCaughtException.countDown();
                    }
                }
            }
        });

        assertFalse(blockedReadCaughtException.await(1, TimeUnit.SECONDS));
        assertFalse(blockedWriteCaughtException.await(1, TimeUnit.SECONDS));

        orderingQueue.closeWithException(myException);

        assertTrue(blockedReadCaughtException.await(1, TimeUnit.SECONDS));
        assertTrue(blockedWriteCaughtException.await(1, TimeUnit.SECONDS));

        try {
            orderingQueue.push(0, new byte[11]);
            fail();
        } catch (RuntimeException e) {
            assertEquals(myException, e);
        }

        try {
            orderingQueue.popInOrder();
            fail();
        } catch (RuntimeException e) {
            assertEquals(myException, e);
        }
    }
}