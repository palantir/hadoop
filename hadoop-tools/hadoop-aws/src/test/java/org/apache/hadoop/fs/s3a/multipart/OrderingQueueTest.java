package org.apache.hadoop.fs.s3a.multipart;

import org.junit.AfterClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class OrderingQueueTest {

    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    @AfterClass
    public static void after() {
        executorService.shutdownNow();
    }

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
    public void testCloseDeliversExceptionToBlockedPush() throws InterruptedException {
        testCloseDeliversExceptionToBlockedCall(new Consumer<OrderingQueue>() {
            @Override
            public void consume(OrderingQueue orderingQueue) {
                try {
                    orderingQueue.push(5, new byte[6]);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    @Test
    public void testCloseDeliversExceptionToBlockedPop() throws InterruptedException {
        testCloseDeliversExceptionToBlockedCall(new Consumer<OrderingQueue>() {
            @Override
            public void consume(OrderingQueue orderingQueue) {
                try {
                    orderingQueue.popInOrder();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private void testCloseDeliversExceptionToBlockedCall(final Consumer<OrderingQueue> consumer) throws InterruptedException {
        final OrderingQueue orderingQueue = new OrderingQueue(0, 20, 10);
        final CountDownLatch deliveredException = new CountDownLatch(1);
        final RuntimeException myException = new RuntimeException("My Exception");

        executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    consumer.consume(orderingQueue);
                } catch (RuntimeException e) {
                    if (e.equals(myException)) {
                        deliveredException.countDown();
                    }
                }
            }
        });

        assertFalse(deliveredException.await(1, TimeUnit.SECONDS));
        orderingQueue.closeWithException(myException);
        assertTrue(deliveredException.await(1, TimeUnit.SECONDS));

        try {
            consumer.consume(orderingQueue);
            fail();
        } catch (RuntimeException e) {
            assertEquals(myException, e);
        }
    }

    interface Consumer<T> {

        void consume(T t);

    }
}
