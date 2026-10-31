package sa.com.tadawul.website.tip.queue;

/**
 * Per-message-type queues between the {@code DataReader} dispatcher and the parser threads.
 * Each queue has exactly one consuming parser thread.
 */
public final class MessageQueue {

    private MessageQueue() {
    }

    public static final MessageChannel<String> turnoverListUpdateQueue = new MessageChannel<>("TLu");
    public static final MessageChannel<String> indexUpdateQueue = new MessageChannel<>("INd");
    public static final MessageChannel<String> tRdAndTStQueue = new MessageChannel<>("TRd/TSt/OBs");
    public static final MessageChannel<String> orderBookQueue = new MessageChannel<>("OBk");
    public static final MessageChannel<String> orderBookStateQueue = new MessageChannel<>("OKs");
    public static final MessageChannel<String> indicativeOpeningQueue = new MessageChannel<>("INo");
    public static final MessageChannel<String> lowVlmMsgQueue = new MessageChannel<>("LowVolume");

    /** Raw frames from the native TIP reader. */
    public static final AnalyserInBuffer g_oBuffer = new AnalyserInBuffer();

    /** Messages waiting in the reader buffer and all parser queues. */
    public static int totalPending() {
        return g_oBuffer.size() + turnoverListUpdateQueue.size() + indexUpdateQueue.size() + tRdAndTStQueue.size()
                + orderBookQueue.size() + orderBookStateQueue.size() + indicativeOpeningQueue.size() + lowVlmMsgQueue.size();
    }
}
