package sa.com.tadawul.website.tip.queue;

/**
 * Immutable envelope for one raw TIP frame travelling from the reader thread to the
 * {@code DataReader} dispatcher.
 *
 * @param time            earliest wall-clock time (ms) at which the frame may be processed
 *                        (implements WEBSITE_DISPLAY_DELAY_TIME)
 * @param buffer          raw TIP message text, terminated by the line separator
 * @param frameNo         running frame counter
 * @param readerStartTime reader timestamp before the native read
 * @param readerEndTime   reader timestamp after the native read
 */
public record FrameHolder(long time, String buffer, int frameNo, long readerStartTime, long readerEndTime) {

    /** Legacy accessor kept for source compatibility. */
    public String getBuffer() {
        return buffer;
    }

    /** Legacy accessor kept for source compatibility. */
    public long getTime() {
        return time;
    }

    /** Legacy accessor kept for source compatibility. */
    public int getG_frameNo() {
        return frameNo;
    }
}
