package tip.client;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tip.xm.ExtHeader;
import tip.xm.XmCrypt;
import tip.xm.XmError;
import tip.xm.XmException;
import tip.xm.XmService;
import tip.xm.XmSession;
import tip.xm.Xmp;
import tip.xm.XmpFrame;

/**
 * TIP client: connect, log on, send a streaming request, read messages
 * (pure Java replacement for the JNI {@code AnalyserReader.CNative*} functions).
 *
 * <pre>{@code
 * try (TipClient tip = TipClient.builder().compression(true).build()) {
 *     tip.connect("10.0.0.5", 4000);
 *     TipMessage reply = tip.logon("user", "secret", null);
 *     tip.requestRealtime(1, 0, TipClient.currentDate(), TipClient.Scope.ALL);
 *     while (true) System.out.print(tip.nextRaw());
 * }
 * }</pre>
 *
 * Reading ({@code next*}) is meant for one thread; {@link #logoff()} and {@link #close()}
 * may be called from another (closing makes a blocked read fail).
 */
public final class TipClient implements AutoCloseable {
    private static final Logger LOG = LogManager.getLogger("tip.client");
    /** TRACE-level copy of every TIP text message sent and received (passwords masked). */
    private static final Logger WIRE = LogManager.getLogger("tip.client.wire");

    public enum RequestType {
        REALTIME(1), BASIC_DATA(2);

        private final int code;
        RequestType(int code) { this.code = code; }
        public int code() { return code; }

        public static RequestType of(int code) {
            for (RequestType t : values()) if (t.code == code) return t;
            throw new IllegalArgumentException("unknown request type " + code);
        }
    }

    public enum Scope {
        NONE(1), UPDATES(2), ALL(3);

        private final int code;
        Scope(int code) { this.code = code; }
        public int code() { return code; }

        public static Scope of(int code) {
            for (Scope s : values()) if (s.code == code) return s;
            throw new IllegalArgumentException("unknown scope " + code);
        }
    }

    /** Callbacks for the extension headers the server attaches to frames. All optional. */
    public interface ExtensionListener {
        /** The server's send queue for this session crossed the alert threshold. */
        default void onQueueAlert(int millis) {
            LOG.log(Level.WARN, "queue alert: {} ms", millis);
        }

        default void onSessionState(Xmp.SessionState state, int rawCode) {
            LOG.log(Level.INFO, "session state: {}", state == Xmp.SessionState.UNKNOWN ? "0x" + Integer.toHexString(rawCode) : state);
        }

        /** Per-frame timestamp (only sent if the server is configured to). {@code fraction} is the raw second int32. */
        default void onTimestamp(Instant seconds, int fraction) {}
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private int queueAlertMillis = 500;
        private boolean compression;
        private Duration connectTimeout = Duration.ofSeconds(30);
        private Duration recvTimeout;                  // none: like the C client, which never applied its 35 s default
        private Charset charset = StandardCharsets.UTF_8;
        private ExtensionListener listener = new ExtensionListener() {};
        private XmCrypt initialCrypt;

        /** Server-side queue alert threshold, milliseconds (sent with every request). Default 500. */
        public Builder queueAlertMillis(int ms) { this.queueAlertMillis = ms; return this; }
        /** Ask the server to compress what it sends (LZ1). */
        public Builder compression(boolean on) { this.compression = on; return this; }
        public Builder connectTimeout(Duration d) { this.connectTimeout = d; return this; }
        /** Fail a read with {@link XmError#TIMEOUT} if nothing arrives for this long (null = wait forever). */
        public Builder recvTimeout(Duration d) { this.recvTimeout = d; return this; }
        /** Charset for text messages. UTF-8 matches what the JNI wrapper's NewStringUTF produced. */
        public Builder charset(Charset c) { this.charset = c; return this; }
        public Builder listener(ExtensionListener l) { this.listener = l; return this; }
        /** Pre-shared key for the optional XMP encryption handshake. */
        public Builder initialCrypt(XmCrypt c) { this.initialCrypt = c; return this; }

        public TipClient build() { return new TipClient(this); }
    }

    private final int queueAlertMillis;
    private final boolean compression;
    private final Duration connectTimeout;
    private final Duration recvTimeout;
    private final Charset charset;
    private final ExtensionListener listener;
    private final XmCrypt initialCrypt;

    private volatile XmSession session;
    /** Sent with every request, as the C client did: compression algorithm, then queue alert. */
    private ExtHeader sessionExt = ExtHeader.EMPTY;

    private TipClient(Builder b) {
        this.queueAlertMillis = b.queueAlertMillis;
        this.compression = b.compression;
        this.connectTimeout = b.connectTimeout;
        this.recvTimeout = b.recvTimeout;
        this.charset = b.charset;
        this.listener = b.listener;
        this.initialCrypt = b.initialCrypt;
    }

    /** Today's date as YYYYMMDD (the default request date of the C client). */
    public static int currentDate() {
        LocalDate d = LocalDate.now();
        return d.getYear() * 10000 + d.getMonthValue() * 100 + d.getDayOfMonth();
    }

    // ------------------------------------------------------------------ connect

    public void connect(String host, int port) throws IOException {
        connect(new XmService().add(host, port));
    }

    /** Several addresses give fail-over: each connect starts at the next one and falls through on error. */
    public void connect(List<XmService.Address> addresses) throws IOException {
        XmService svc = new XmService();
        for (XmService.Address a : addresses) svc.add(a.host(), a.port());
        connect(svc);
    }

    public synchronized void connect(XmService service) throws IOException {
        if (session != null) throw new IllegalStateException("already connected");
        LOG.log(Level.INFO, "connecting (compression={}, queueAlert={} ms, encrypted={})", compression, queueAlertMillis, initialCrypt != null);
        XmSession.Options opts = XmSession.Options.defaults()
                .withConnectTimeout(connectTimeout)
                .withRecvTimeout(recvTimeout)
                .withInitialCrypt(initialCrypt);
        session = XmSession.connect(service, opts);
        sessionExt = ExtHeader.EMPTY
                .withByte(Xmp.Tag.COMPRESSION_ALGORITHM, compression ? Xmp.Compression.LZ1 : Xmp.Compression.NONE)
                .withInt(Xmp.Tag.QUEUE_ALERT, queueAlertMillis);
    }

    /** Changes the receive timeout of the open connection ({@code null} or zero: wait forever). */
    public void setRecvTimeout(Duration timeout) throws IOException {
        requireSession().setRecvTimeout(timeout);
    }

    public boolean isConnected() {
        XmSession s = session;
        return s != null && !s.isClosed();
    }

    // ------------------------------------------------------------------ TIP operations

    /**
     * Logs on and returns the server's reply (normally a {@code GEn} general reply; inspect
     * {@code Rc}/{@code Rx} with {@link TipMessage#get}). The meaning of the reply codes is defined
     * by the exchange's TIP specification, not by this library.
     *
     * @param protocolVersion optional; sent as {@code VEr} when non-null
     */
    public TipMessage logon(String user, String password, String protocolVersion) throws IOException {
        if (user == null) throw new IllegalArgumentException("user is null");
        if (password == null) throw new IllegalArgumentException("password is null");
        TipMessage.Builder m = TipMessage.builder(TipTags.LOGON)
                .add(TipTags.USER_ID, user)
                .add(TipTags.PASSWORD, password);
        if (protocolVersion != null) m.add(TipTags.PROTOCOL_VERSION, protocolVersion);
        LOG.log(Level.INFO, "logging on as {}", user);       // the password is never logged
        send(m.build());
        TipMessage reply = TipMessage.parse(nextRaw());
        LOG.log(Level.INFO, "logon reply: type={} Rc={} Rx={}", reply.type(),
                reply.get(TipTags.REPLY_CODE).orElse("-"), reply.get(TipTags.REPLY_TEXT).orElse("-"));
        return reply;
    }

    /**
     * Same checks as the JNI {@code StreamingRequest}.
     *
     * @param date     YYYYMMDD, 0 = omit
     * @param startSeq first sequence number (> 0); only used for realtime requests
     * @param endSeq   last sequence number, 0 = open ended; only used for realtime requests
     */
    public void streamingRequest(RequestType type, Scope scope, int date, long startSeq, long endSeq) throws IOException {
        TipMessage.Builder m;
        if (type == RequestType.REALTIME) {
            if (startSeq <= 0) throw new IllegalArgumentException("startSeq must be > 0, got " + startSeq);
            if (endSeq != 0 && endSeq < startSeq) throw new IllegalArgumentException("endSeq " + endSeq + " < startSeq " + startSeq);
            m = TipMessage.builder(TipTags.REALTIME).add(TipTags.START_SEQ_NO, startSeq);
            if (endSeq != 0) m.add(TipTags.END_SEQ_NO, endSeq);
        } else {
            m = TipMessage.builder(TipTags.BASIC_DATA);
        }
        if (date != 0) m.add(TipTags.REQUEST_DATE, date);
        m.add(TipTags.SCOPE, scope.code());
        LOG.log(Level.INFO, "streaming request {}, scope={}, date={}, seq {}..{}",
                type, scope, date == 0 ? "-" : date, startSeq, endSeq == 0 ? "open" : endSeq);
        send(m.build());
    }

    public void requestRealtime(long startSeq, long endSeq, int date, Scope scope) throws IOException {
        streamingRequest(RequestType.REALTIME, scope, date, startSeq, endSeq);
    }

    public void requestBasicData(int date, Scope scope) throws IOException {
        streamingRequest(RequestType.BASIC_DATA, scope, date, 0, 0);
    }

    /** Sends {@code LGf}. Like the C client, it does not wait for the reply: keep calling {@code next*}. */
    public void logoff() throws IOException {
        LOG.log(Level.INFO, "logging off");
        send(TipMessage.builder(TipTags.LOGOFF).build());
    }

    // ------------------------------------------------------------------ reading

    /**
     * Next message body as bytes. Frames that carry only extension headers (no body) are
     * consumed here and reported through the {@link ExtensionListener}.
     */
    public byte[] nextBody() throws IOException {
        XmSession s = requireSession();
        try {
            for (;;) {
                XmpFrame f = s.recv();
                dispatchExtensions(f.ext());
                if (f.body().length > 0) {
                    if (WIRE.isTraceEnabled()) WIRE.log(Level.TRACE, "<< {}", new String(f.body(), charset).stripTrailing());
                    return f.body();
                }
                LOG.log(Level.DEBUG, "extension-only frame ({} entries)", f.ext().entries().size());
            }
        } catch (XmException e) {
            LOG.log(Level.WARN, "read failed, closing client: {}", e.getMessage());
            close();
            throw e;
        }
    }

    /** Next message as text, exactly as sent (including the trailing newline). */
    public String nextRaw() throws IOException {
        return new String(nextBody(), charset);
    }

    public TipMessage next() throws IOException {
        return TipMessage.parse(nextRaw());
    }

    // ------------------------------------------------------------------ internals

    private void send(TipMessage message) throws IOException {
        XmSession s = requireSession();
        if (WIRE.isTraceEnabled()) WIRE.log(Level.TRACE, ">> {}", message);   // toString masks the password
        try {
            s.send(new XmpFrame(Xmp.Type.BINARY_RECORD, sessionExt, message.toWire().getBytes(charset)));
        } catch (XmException e) {
            LOG.log(Level.WARN, "send failed, closing client: {}", e.getMessage());
            close();
            throw e;
        }
    }

    private XmSession requireSession() {
        XmSession s = session;
        if (s == null || s.isClosed()) throw new IllegalStateException("not connected");
        return s;
    }

    private void dispatchExtensions(ExtHeader ext) {
        for (ExtHeader.Entry e : ext.entries()) {
            byte[] d = e.data();
            LOG.log(Level.TRACE, "extension header tag {}, {} byte(s)", e.tag(), d.length);
            switch (e.tag()) {
                case Xmp.Tag.TIMESTAMP -> {
                    if (d.length >= 8) {
                        listener.onTimestamp(Instant.ofEpochSecond(e.intValue() & 0xFFFFFFFFL), readInt(d, 4));
                    }
                }
                case Xmp.Tag.QUEUE_ALERT -> {
                    if (d.length >= 4) listener.onQueueAlert(e.intValue());
                }
                case Xmp.Tag.SESSION_STATE -> {
                    if (d.length >= 1) listener.onSessionState(Xmp.SessionState.of(d[0]), d[0] & 0xFF);
                }
                default -> { /* not interesting */ }
            }
        }
    }

    private static int readInt(byte[] b, int off) {
        return ((b[off] & 0xFF) << 24) | ((b[off + 1] & 0xFF) << 16) | ((b[off + 2] & 0xFF) << 8) | (b[off + 3] & 0xFF);
    }

    @Override
    public synchronized void close() {
        XmSession s = session;
        session = null;
        if (s != null) {
            LOG.log(Level.INFO, "client closed");
            s.close();
        }
    }
}
