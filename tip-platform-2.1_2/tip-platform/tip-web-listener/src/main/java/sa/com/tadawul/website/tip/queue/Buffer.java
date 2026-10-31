package sa.com.tadawul.website.tip.queue;

/**
 * Queue between the parser / DBQueueUpdater threads (producers) and exactly one DB writer or
 * updater thread (consumer).
 * <p>
 * Keeps the legacy "peek, process, release" contract: {@link #getFrame()} returns the head
 * without removing it and {@link #releaseFrame()} removes it once it has been fully handled.
 * Because the head stays in the queue until released, {@link #size()} counts it - exactly as
 * the legacy implementation did - without the race the old "popped-but-not-released" field had
 * when another thread read the size.
 */
public final class Buffer {

    private final MessageChannel<Object> channel;
    /** Head handed out by getFrame() and not yet released. Consumer-confined. */
    private Object current;

    public Buffer() {
        this("Buffer");
    }

    public Buffer(String name) {
        this.channel = new MessageChannel<>(name);
    }

    /** Producer side: enqueue a value object. */
    public void setFrame(Object frame) {
        channel.push(frame);
    }

    /**
     * Consumer side: returns the head frame without removing it (repeated calls return the same
     * frame until {@link #releaseFrame()}), or {@code null} if the buffer is empty.
     */
    public Object getFrame() {
        if (current == null) {
            current = channel.peek();
        }
        return current;
    }

    /** Consumer side: marks the frame returned by {@link #getFrame()} as consumed. */
    public void releaseFrame() {
        if (current != null) {
            channel.pop();
            current = null;
        }
    }

    /**
     * Consumer side: block until a frame is available or {@code maxWaitMillis} elapses.
     * Replaces the legacy {@code Thread.sleep(100)} idle polling.
     */
    public boolean awaitFrame(long maxWaitMillis) {
        return channel.awaitData(maxWaitMillis);
    }

    /** Number of frames waiting, including one handed out but not yet released. */
    public int size() {
        return channel.size();
    }

    @Override
    public String toString() {
        return channel.toString();
    }
}
