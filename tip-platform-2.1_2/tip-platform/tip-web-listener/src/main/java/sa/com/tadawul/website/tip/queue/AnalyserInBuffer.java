package sa.com.tadawul.website.tip.queue;

/**
 * Hand-off buffer between the native TIP reader thread and the {@code DataReader} dispatcher.
 * Backed by a lock-free {@link MessageChannel}; the dispatcher blocks in {@link #poll(long)}
 * instead of busy-spinning.
 */
public final class AnalyserInBuffer {

    private final MessageChannel<FrameHolder> channel = new MessageChannel<>("AnalyserIn");

    /** Producer side: enqueue a frame read from TIP. */
    public void setFrame(FrameHolder frame) {
        channel.push(frame);
    }

    /** Consumer side: remove the next frame, or {@code null} if empty. */
    public FrameHolder getFrame() {
        return channel.pop();
    }

    /** Consumer side: wait up to {@code maxWaitMillis} for the next frame. */
    public FrameHolder poll(long maxWaitMillis) {
        return channel.poll(maxWaitMillis);
    }

    public int size() {
        return channel.size();
    }
}
