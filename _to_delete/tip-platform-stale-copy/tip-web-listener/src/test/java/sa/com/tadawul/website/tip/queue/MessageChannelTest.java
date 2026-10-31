package sa.com.tadawul.website.tip.queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

class MessageChannelTest {

    @Test
    void fifoAndSize() {
        MessageChannel<String> ch = new MessageChannel<>("t");
        assertTrue(ch.isEmpty());
        assertNull(ch.pop());
        ch.push("a");
        ch.push("b");
        ch.push("c");
        assertEquals(3, ch.size());
        assertEquals("a", ch.peek());
        assertEquals("a", ch.pop());
        assertEquals("b", ch.pop());
        assertEquals(1, ch.size());
        assertEquals("c", ch.pop());
        assertEquals(0, ch.size());
        assertNull(ch.pop());
    }

    @Test
    void awaitDataTimesOutWhenIdle() {
        MessageChannel<String> ch = new MessageChannel<>("t");
        long t0 = System.nanoTime();
        assertFalse(ch.awaitData(50));
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
        assertTrue(elapsedMs >= 45, "returned too early: " + elapsedMs + " ms");
    }

    @Test
    void awaitDataReturnsImmediatelyWhenDataPresent() {
        MessageChannel<String> ch = new MessageChannel<>("t");
        ch.push("x");
        long t0 = System.nanoTime();
        assertTrue(ch.awaitData(5_000));
        assertTrue(System.nanoTime() - t0 < TimeUnit.MILLISECONDS.toNanos(100));
    }

    @Test
    void consumerWakesPromptlyOnPush() throws Exception {
        MessageChannel<String> ch = new MessageChannel<>("t");
        CountDownLatch waiting = new CountDownLatch(1);
        long[] wokeAt = new long[1];
        Thread consumer = new Thread(() -> {
            waiting.countDown();
            ch.awaitData(10_000);
            wokeAt[0] = System.nanoTime();
        });
        consumer.start();
        waiting.await();
        Thread.sleep(50); // let it park
        long pushedAt = System.nanoTime();
        ch.push("hello");
        consumer.join(5_000);
        assertFalse(consumer.isAlive(), "consumer did not wake up");
        long latencyMs = TimeUnit.NANOSECONDS.toMillis(wokeAt[0] - pushedAt);
        assertTrue(latencyMs < 1_000, "wake-up latency " + latencyMs + " ms");
    }

    @Test
    void multiProducerSingleConsumerDeliversEverythingInPerProducerOrder() throws Exception {
        final int producers = 6;
        final int perProducer = 100_000;
        MessageChannel<long[]> ch = new MessageChannel<>("t");
        Thread[] ps = new Thread[producers];
        for (int p = 0; p < producers; p++) {
            final int id = p;
            ps[p] = new Thread(() -> {
                for (int i = 0; i < perProducer; i++) {
                    ch.push(new long[] { id, i });
                }
            });
        }
        for (Thread t : ps) {
            t.start();
        }
        long[] next = new long[producers];
        int received = 0;
        while (received < producers * perProducer) {
            long[] m = ch.poll(2_000);
            if (m == null) {
                throw new AssertionError("timed out after " + received + " messages");
            }
            assertEquals(next[(int) m[0]], m[1], "out of order for producer " + m[0]);
            next[(int) m[0]]++;
            received++;
        }
        for (Thread t : ps) {
            t.join();
        }
        assertEquals(0, ch.size());
        assertNull(ch.pop());
    }

    @Test
    void bufferKeepsHeadUntilReleased() {
        Buffer b = new Buffer("b");
        Object o1 = new Object();
        Object o2 = new Object();
        b.setFrame(o1);
        b.setFrame(o2);
        assertSame(o1, b.getFrame());
        assertSame(o1, b.getFrame(), "getFrame must be idempotent until release");
        assertEquals(2, b.size(), "un-released head is still counted, as in the legacy Buffer");
        b.releaseFrame();
        assertEquals(1, b.size());
        assertSame(o2, b.getFrame());
        b.releaseFrame();
        b.releaseFrame(); // extra release must not drop anything
        assertEquals(0, b.size());
        assertNull(b.getFrame());
    }

    @Test
    void bufferReleaseWithoutGetDoesNotDropFrames() {
        Buffer b = new Buffer("b");
        b.setFrame("x");
        b.releaseFrame();
        assertEquals(1, b.size());
        assertEquals("x", b.getFrame());
    }
}
