package sa.com.tadawul.website.tip.queue;

import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

/**
 * Lock-free, unbounded multi-producer / single-consumer message channel.
 * <p>
 * Replaces the legacy {@code com.objectspace.jgl.Queue}, whose every operation took a monitor
 * lock and which offered no way to wait for data. That forced every consumer thread in the
 * listener into a "drain, then {@code Thread.sleep(1..100ms)}" polling loop (the DataReader
 * thread did not sleep at all and spun at 100% CPU).
 * <p>
 * Design:
 * <ul>
 *   <li>{@link ConcurrentLinkedQueue} (Michael-Scott lock-free queue) holds the data, so
 *       producers never block each other or the consumer.</li>
 *   <li>An {@link AtomicInteger} keeps an O(1) size; {@code ConcurrentLinkedQueue.size()} is O(n).</li>
 *   <li>{@link #awaitData(long)} parks the consumer until a producer publishes, so an idle
 *       consumer costs zero CPU and wakes within microseconds when a message arrives.
 *       Producers only pay for an {@code unpark} when the consumer is actually parked.</li>
 * </ul>
 * The API keeps the method names the existing parser code already used ({@code push},
 * {@code pop}, {@code size}) so the business logic did not have to be rewritten.
 * <p>
 * <b>Threading contract:</b> any number of threads may call {@link #push(Object)} and
 * {@link #size()}; exactly one consumer thread may call {@link #pop()}, {@link #peek()} and
 * {@link #awaitData(long)}. That is how every queue in this application is used.
 *
 * @param <T> element type
 */
public final class MessageChannel<T> {

    private final ConcurrentLinkedQueue<T> queue = new ConcurrentLinkedQueue<>();
    private final AtomicInteger count = new AtomicInteger();
    private final String name;

    /** Consumer currently parked in {@link #awaitData(long)}, or {@code null}. */
    private volatile Thread waiter;

    public MessageChannel(String name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    /** Appends an element. Never blocks. Null elements are rejected. */
    public void push(T item) {
        queue.offer(Objects.requireNonNull(item, "item"));
        count.incrementAndGet();
        Thread w = waiter;
        if (w != null) {
            LockSupport.unpark(w);
        }
    }

    /** Removes and returns the head element, or {@code null} if the channel is empty. */
    public T pop() {
        T item = queue.poll();
        if (item != null) {
            count.decrementAndGet();
        }
        return item;
    }

    /** Returns (without removing) the head element, or {@code null} if empty. */
    public T peek() {
        return queue.peek();
    }

    /** O(1) element count. May be momentarily stale while a push/pop is in flight. */
    public int size() {
        int n = count.get();
        return Math.max(n, 0);
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    /**
     * Blocks the consumer until the channel is non-empty, the timeout elapses, or the thread is
     * interrupted. Returns immediately if data is already available.
     *
     * @param maxWaitMillis upper bound on the wait, in milliseconds
     * @return {@code true} if data is available
     */
    public boolean awaitData(long maxWaitMillis) {
        if (!queue.isEmpty()) {
            return true;
        }
        final long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(Math.max(0L, maxWaitMillis));
        waiter = Thread.currentThread();
        try {
            // Re-check after publishing ourselves as waiter: a producer that offered before it
            // could see 'waiter' has already made the element visible here (no lost wake-up).
            while (queue.isEmpty()) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0L || Thread.currentThread().isInterrupted()) {
                    break;
                }
                LockSupport.parkNanos(this, remaining);
            }
        } finally {
            waiter = null;
        }
        return !queue.isEmpty();
    }

    /**
     * Waits up to {@code maxWaitMillis} for an element and removes it.
     *
     * @return the head element, or {@code null} on timeout
     */
    public T poll(long maxWaitMillis) {
        T item = pop();
        if (item == null && awaitData(maxWaitMillis)) {
            item = pop();
        }
        return item;
    }

    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return name + "[size=" + size() + "]";
    }
}
