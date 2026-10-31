package sa.com.tadawul.applications.tip.isi.efp.analyser;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.stream.IntStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import sa.com.tadawul.website.tip.queue.FrameHolder;
import sa.com.tadawul.website.tip.shared.TIPDataObjects;
import sa.com.tadawul.website.tip.shared.TipConstants;
import tip.client.TipClient;
import tip.client.TipMessage;
import tip.client.TipTags;
import tip.xm.XmException;
import tip.xm.XmService;

/**
 * Reads the TIP feed with the pure-Java {@link TipClient} and hands every message to the
 * dispatcher. Replaces the JNI calls into {@code libtipclient.so}
 * ({@code CNativeConnect/Logon/StreamingRequest/GetNextMessage/Logoff/Disconnect}).
 *
 * <h2>Flow (same as the JNI version)</h2>
 * <ol>
 *   <li>connect: every attempt tries the primary, then the secondary address
 *       (TARGIN_CONNECT_MAX_RETRY_COUNT attempts); no connection = shutdown;</li>
 *   <li>logon, retried on the same connection; refused = shutdown;</li>
 *   <li>file-based recovery: replay today's feed log, then request the stream from the last
 *       sequence number;</li>
 *   <li>read until the connection ends or a stop is requested; on a lost connection reconnect
 *       (unless the global shutdown flag is set); on stop send LGf and wait for its reply.</li>
 * </ol>
 *
 * <h2>Differences from the JNI version</h2>
 * <ul>
 *   <li>The feed log is replayed only once per process. On an in-process reconnect the JNI version
 *       replayed the whole file into the queues again (re-processing the day); now the stream is
 *       simply resumed from the last sequence number received.</li>
 *   <li>The resume sequence number is taken from the messages themselves (tag {@code n}) instead of
 *       {@code TIPDataObjects.last_Sequencer_Analyzer}, which the parser threads update
 *       asynchronously and was often behind right after the replay.</li>
 *   <li>Signals: the C library trapped SIGINT/SIGHUP/SIGTSTP. Now a JVM shutdown hook (SIGINT,
 *       SIGTERM, SIGHUP) calls {@link #requestStop()}.</li>
 * </ul>
 */
public final class TipFeedReader {

    private static final Logger logger = LogManager.getLogger(TipFeedReader.class);

    /** Everything the reader needs; built from efp.ini by {@link AnalyserReader}. */
    public record Config(
            String user,
            String password,
            String protocolVersion,
            List<XmService.Address> addresses,
            int requestDate,
            int maxRetries,
            boolean compression,
            Duration connectTimeout,
            Duration recvTimeout,
            int queueAlertMillis,
            FeedTextDecoder decoder,
            boolean logFeed,
            boolean fileBasedRecovery,
            String feedLogPath,
            long feedLogFlushMillis,
            long displayDelayMillis,
            Duration reconnectPause) {

        public Config {
            Objects.requireNonNull(user, "user");
            Objects.requireNonNull(password, "password");
            Objects.requireNonNull(decoder, "decoder");
            addresses = List.copyOf(addresses);
            if (addresses.isEmpty()) {
                throw new IllegalArgumentException("no TIP address configured");
            }
            maxRetries = Math.max(1, maxRetries);
        }

        @Override
        public String toString() { // never print the password
            return "Config[user=" + user + ", addresses=" + addresses + ", date=" + requestDate + ", retries=" + maxRetries
                    + ", compression=" + compression + ", decoder=" + decoder.name() + ", feedLog=" + (logFeed ? feedLogPath : "off") + "]";
        }
    }

    private static final String LINE_SEP = TipConstants.TIP_LINE_SEPARATOR;
    private static final boolean MULTI_CHAR_LINE_SEP = LINE_SEP.length() > 1;
    private static final Duration LOGOFF_REPLY_TIMEOUT = Duration.ofSeconds(10);

    private final Config cfg;
    private final Consumer<FrameHolder> sink;
    private final LongSupplier parserSequence;

    private volatile boolean stopRequested;
    private volatile TipClient current;
    private volatile Thread readerThread;
    private boolean logoffSent;              // guarded by this
    private volatile boolean logoffAcknowledged;
    private boolean loggedOn;

    private FeedLogWriter feedLog;
    private boolean replayDone;
    private long lastSequence;               // highest TIP sequence number seen (replay + live)
    private int messageCount;

    /**
     * @param sink           receives every message (normally {@code MessageQueue.g_oBuffer::setFrame})
     * @param parserSequence last sequence number processed by the parsers (resume fallback)
     */
    public TipFeedReader(Config cfg, Consumer<FrameHolder> sink, LongSupplier parserSequence) {
        this.cfg = Objects.requireNonNull(cfg);
        this.sink = Objects.requireNonNull(sink);
        this.parserSequence = Objects.requireNonNull(parserSequence);
    }

    /** Runs the connect / read / reconnect loop on the calling thread until stopped or shut down. */
    public void run() {
        readerThread = Thread.currentThread();
        logger.info("TIP feed reader starting: {}", cfg);
        try {
            while (!stopRequested && !TipConstants.shutdownFlag) {
                TipClient client = newClient();
                current = client;
                synchronized (this) {
                    logoffSent = false;
                }
                logoffAcknowledged = false;
                loggedOn = false;
                try {
                    if (!connect(client)) {
                        logger.error("Unable to connect to any TIP address {} ... shutting down", cfg.addresses());
                        TipConstants.shutdownFlag = true;
                        break;
                    }
                    if (!logon(client)) {
                        logger.error("Unable to Logon... shutting down");
                        TipConstants.shutdownFlag = true;
                        break;
                    }
                    loggedOn = true;
                    if (!sendStreamingRequest(client)) {
                        logger.error("Unable to Send Streaming Request...");
                        logoffAndDrain(client);
                        continue;
                    }
                    readUntilClosed(client);
                } catch (IOException e) {
                    if (!stopRequested) {
                        logger.error("TIP connection lost: {}", e.getMessage());
                    }
                } finally {
                    if (stopRequested || TipConstants.shutdownFlag) {
                        logger.info("Stop requested={} shutdownFlag={}", stopRequested, TipConstants.shutdownFlag);
                        TipConstants.shutdownFlag = true;
                        if (loggedOn) {
                            logoffAndDrain(client);   // like the JNI reader: no logoff if never logged on
                        }
                    }
                    client.close();
                    current = null;
                }
                if (!stopRequested && !TipConstants.shutdownFlag) {
                    logger.warn("Reconnecting to TIP in {} ms (resume from sequence {})", cfg.reconnectPause().toMillis(), resumeSequence());
                    sleepQuietly(cfg.reconnectPause().toMillis());
                }
            }
        } finally {
            closeFeedLog();
            logger.info("TIP feed reader stopped after {} messages, last sequence {}", messageCount, lastSequence);
        }
    }

    /**
     * Asks the reader to log off and stop. Safe to call from any thread (e.g. a shutdown hook).
     * If the reader is blocked waiting for data, LGf is sent from here so the server's reply
     * unblocks it; as a last resort the connection is closed.
     */
    public void requestStop(Duration maxWait) {
        stopRequested = true;
        Thread t = readerThread;
        if (t == null || t == Thread.currentThread()) {
            return;
        }
        long deadline = System.nanoTime() + maxWait.toNanos();
        joinQuietly(t, Math.min(2_000, maxWait.toMillis()));
        TipClient c = current;
        if (t.isAlive() && c != null) {
            sendLogoffOnce(c);                         // the reply wakes up the blocked reader
            joinQuietly(t, Math.max(0, (deadline - System.nanoTime()) / 1_000_000));
        }
        c = current;
        if (t.isAlive() && c != null) {
            logger.warn("TIP reader did not stop in time, closing the connection");
            c.close();
            joinQuietly(t, 2_000);
        }
    }

    public boolean isStopRequested() {
        return stopRequested;
    }

    public long lastSequence() {
        return lastSequence;
    }

    public int messageCount() {
        return messageCount;
    }

    // ------------------------------------------------------------------ steps

    private TipClient newClient() {
        return TipClient.builder()
                .compression(cfg.compression())
                .connectTimeout(cfg.connectTimeout())
                .recvTimeout(cfg.recvTimeout())
                .queueAlertMillis(cfg.queueAlertMillis())
                .build();
    }

    /** Each attempt tries the addresses in order (primary first), like the JNI reader did. */
    private boolean connect(TipClient client) {
        return IntStream.range(0, cfg.maxRetries())
                .takeWhile(attempt -> !stopRequested)
                .anyMatch(attempt -> tryConnect(client, attempt));
    }

    private boolean tryConnect(TipClient client, int attempt) {
        logger.info("Connect Attempt {} to {}", attempt, cfg.addresses());
        try {
            client.connect(cfg.addresses());
            return true;
        } catch (IOException e) {
            logger.error("Connect attempt {} failed: {}", attempt, e.getMessage());
            return false;
        }
    }

    /** Like CNativeLogon: retried on the same connection; a failure after the retries means shutdown. */
    private boolean logon(TipClient client) {
        return IntStream.range(0, cfg.maxRetries())
                .takeWhile(attempt -> !stopRequested && client.isConnected())
                .anyMatch(attempt -> tryLogon(client, attempt));
    }

    private boolean tryLogon(TipClient client, int attempt) {
        logger.info("Logon Attempt {}", attempt);
        try {
            TipMessage reply = client.logon(cfg.user(), cfg.password(), cfg.protocolVersion());
            if (isGeneralReplyOk(reply, TipTags.LOGON)) {
                return true;
            }
            logger.error("Logon refused: {}", reply);
        } catch (IOException | IllegalStateException e) {
            logger.error("Logon failed: {}", e.getMessage());
        }
        return false;
    }

    /** True for a General Reply ({@code GEn}) to {@code requestType} with reply code 0. */
    static boolean isGeneralReplyOk(TipMessage reply, String requestType) {
        if (!TipTags.GENERAL_REPLY.equals(reply.type())) {
            return false;
        }
        String rc = reply.get(TipTags.REPLY_CODE).orElse(null);
        if (rc == null) {
            logger.error("Invalid General reply: no reply code found");
            return false;
        }
        String qt = reply.get(TipTags.REQUEST_TYPE).orElse(null);
        logger.info("General reply: {}:{}:{}", rc, qt, reply.get(TipTags.REPLY_TEXT).orElse(""));
        try {
            return requestType.equals(qt) && Integer.parseInt(rc.trim()) == 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean sendStreamingRequest(TipClient client) throws IOException {
        if (!replayDone) {
            replayDone = true;
            if (cfg.logFeed() && cfg.fileBasedRecovery() && !replay()) {
                return false;
            }
        }
        long start = resumeSequence();
        logger.info("Before calling StreamingRequest: reqType {} scope {} Date {} startSeq {} endSeq 0",
                TipConstants.TIP_REAL_TIME_REQUEST, TipConstants.TIP_SCOPE_ALL, cfg.requestDate(), start);
        client.streamingRequest(TipClient.RequestType.REALTIME, TipClient.Scope.ALL, cfg.requestDate(), start, 0);
        if (cfg.logFeed() && feedLog == null) {
            try {
                feedLog = new FeedLogWriter(cfg.feedLogPath(), cfg.feedLogFlushMillis());
            } catch (IOException e) {
                logger.error("File Output Stream Error: {}", e.toString());
            }
        }
        return true;
    }

    /** Same convention as the JNI reader: resume AT the last sequence seen (it is re-sent once). */
    private long resumeSequence() {
        return Math.max(1L, Math.max(lastSequence, parserSequence.getAsLong()));
    }

    private void readUntilClosed(TipClient client) throws IOException {
        long readerStart = System.currentTimeMillis();
        while (!stopRequested) {
            byte[] body = client.nextBody();
            long now = System.currentTimeMillis();
            String msg = cfg.decoder().decode(body);
            if (isLogoffReply(msg)) {
                return;                                  // our LGf was answered: stop reading
            }
            if (msg.isEmpty()) {
                logger.error("Empty message received from TIP - ignored");
                continue;
            }
            if (msg.charAt(msg.length() - 1) != TipConstants.TIP_ENDOFMSG
                    || (MULTI_CHAR_LINE_SEP && !msg.contains(LINE_SEP))) {
                msg = msg.concat(LINE_SEP);
            }
            if (feedLog != null) {
                try {
                    feedLog.write(body);
                } catch (IOException e) {
                    logger.error("File Write Error = {}", e.toString());
                }
            }
            trackSequence(msg);
            sink.accept(new FrameHolder(now + cfg.displayDelayMillis(), msg, ++messageCount, readerStart, now));
            readerStart = now;
        }
    }

    /** Replays today's feed log (raw TIP bytes, one message per line). */
    private boolean replay() {
        String path = cfg.feedLogPath();
        long t0 = System.currentTimeMillis();
        int replayed = 0;
        try (InputStream in = new FileInputStream(path)) {
            logger.info("Replay mode started from {}", path);
            TIPDataObjects.replayMode = true;
            byte[] chunk = new byte[1 << 16];
            byte[] line = new byte[4096];
            int len = 0;
            int n;
            while ((n = in.read(chunk)) > 0) {
                int from = 0;
                for (int i = 0; i < n; i++) {
                    if (chunk[i] == '\n') {
                        int part = i - from;
                        if (len + part > line.length) {
                            line = Arrays.copyOf(line, Math.max(line.length * 2, len + part));
                        }
                        System.arraycopy(chunk, from, line, len, part);
                        len += part;
                        replayed += replayLine(line, len);
                        len = 0;
                        from = i + 1;
                    }
                }
                int rest = n - from;
                if (rest > 0) {
                    if (len + rest > line.length) {
                        line = Arrays.copyOf(line, Math.max(line.length * 2, len + rest));
                    }
                    System.arraycopy(chunk, from, line, len, rest);
                    len += rest;
                }
            }
            if (len > 0) {
                replayed += replayLine(line, len);
            }
        } catch (FileNotFoundException e) {
            logger.info("No file found for replay...Sending streaming request from seq 1");
            return true;
        } catch (IOException e) {
            logger.error("IOException occured while reading log file for reloading {}", e.toString());
            return false;
        } finally {
            TIPDataObjects.replayMode = false;
        }
        logger.info("Replay mode ended: {} messages in {} ms, last sequence {}", replayed, System.currentTimeMillis() - t0, lastSequence);
        return true;
    }

    private int replayLine(byte[] line, int len) {
        if (len > 0 && line[len - 1] == '\r') {
            len--;
        }
        String msg = cfg.decoder().decode(line, 0, len).concat(LINE_SEP);
        trackSequence(msg);
        long now = System.currentTimeMillis();
        sink.accept(new FrameHolder(now, msg, ++messageCount, now, now));
        return 1;
    }

    /** Picks up the value of the {@code n} (sequence number) field, if any. */
    void trackSequence(String msg) {
        int i = msg.indexOf(";n");
        while (i >= 0) {
            int p = i + 2;
            long v = 0;
            int digits = 0;
            while (p < msg.length()) {
                char c = msg.charAt(p);
                if (c < '0' || c > '9') {
                    break;
                }
                v = v * 10 + (c - '0');
                p++;
                digits++;
            }
            if (digits > 0 && digits < 19 && (p == msg.length() || msg.charAt(p) == ';' || msg.charAt(p) == '\n' || msg.charAt(p) == '\r')) {
                if (v > lastSequence) {
                    lastSequence = v;
                }
                return;
            }
            i = msg.indexOf(";n", i + 2);
        }
    }

    // ------------------------------------------------------------------ logoff

    private synchronized boolean sendLogoffOnce(TipClient client) {
        if (logoffSent || !client.isConnected()) {
            return false;
        }
        logoffSent = true;
        try {
            client.logoff();
            return true;
        } catch (IOException | IllegalStateException e) {
            logger.error("Call to Logoff failed: {}", e.getMessage());
            return false;
        }
    }

    /** True (and logged) if {@code msg} is the server's General Reply to the LGf we sent. */
    private boolean isLogoffReply(String msg) {
        boolean sent;
        synchronized (this) {
            sent = logoffSent;
        }
        if (!sent || !msg.startsWith(TipTags.GENERAL_REPLY)) {
            return false;
        }
        try {
            if (isGeneralReplyOk(TipMessage.parse(msg), TipTags.LOGOFF)) {
                logoffAcknowledged = true;
                logger.info("Logoff Acknowledged by TIP Server..");
                return true;
            }
        } catch (IOException e) {
            logger.warn("Unparsable reply during logoff: {}", e.getMessage());
        }
        return false;
    }

    /** Sends LGf (once) and reads until the server's General Reply to it, the timeout, or close. */
    private void logoffAndDrain(TipClient client) {
        if (logoffAcknowledged || !client.isConnected()) {
            return;
        }
        sendLogoffOnce(client);
        try {
            client.setRecvTimeout(LOGOFF_REPLY_TIMEOUT);
            for (int i = 0; i < 100_000 && client.isConnected(); i++) {
                if (isLogoffReply(cfg.decoder().decode(client.nextBody()))) {
                    return;
                }
            }
        } catch (XmException | IllegalStateException e) {
            // closed by the server after LGf, or timed out
        } catch (IOException e) {
            logger.warn("Error while waiting for logoff reply: {}", e.getMessage());
        }
        logger.info("Logoff Not Acknowledged by TIP Server..");
    }

    // ------------------------------------------------------------------ helpers

    private void closeFeedLog() {
        if (feedLog != null) {
            try {
                feedLog.close();
            } catch (IOException e) {
                logger.error("Log File Close Error: {}", e.toString());
            }
            feedLog = null;
        }
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void joinQuietly(Thread t, long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            t.join(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
