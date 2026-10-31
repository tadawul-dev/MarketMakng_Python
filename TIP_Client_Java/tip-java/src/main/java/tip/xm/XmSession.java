package tip.xm;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A "cooked" XMP session over TCP (port of {@code xmsession.c} / {@code xmisession.c}).
 *
 * <ul>
 *   <li>{@link #send} encrypts outgoing frames once a session key has been negotiated;</li>
 *   <li>{@link #recv} returns the next frame, transparently decrypting and decompressing,
 *       and unpacking a compressed block that contains several frames.</li>
 * </ul>
 * One thread may send while another receives; concurrent senders (or receivers) are serialized.
 */
public final class XmSession implements Closeable {
    private static final Logger LOG = LogManager.getLogger("tip.xm.session");

    /**
     * @param connectTimeout   per-address TCP connect timeout; {@code null}/zero = OS default (blocking)
     * @param recvTimeout      how long {@link #recv} may block waiting for bytes; {@code null}/zero = forever
     * @param sourcePort       local port to bind, 0 = any
     * @param initialCrypt     pre-shared key; if non-null a key exchange is performed after connecting
     * @param receiveBufferSize SO_RCVBUF in bytes (the C code used 100 KB)
     */
    public record Options(Duration connectTimeout, Duration recvTimeout, int sourcePort,
                          XmCrypt initialCrypt, int receiveBufferSize) {
        public static Options defaults() {
            return new Options(Duration.ofSeconds(30), null, 0, null, 102_400);
        }

        public Options withConnectTimeout(Duration d) { return new Options(d, recvTimeout, sourcePort, initialCrypt, receiveBufferSize); }
        public Options withRecvTimeout(Duration d) { return new Options(connectTimeout, d, sourcePort, initialCrypt, receiveBufferSize); }
        public Options withInitialCrypt(XmCrypt c) { return new Options(connectTimeout, recvTimeout, sourcePort, c, receiveBufferSize); }
    }

    private static final int READ_BUFFER_SIZE = 32_768;

    private final Socket socket;
    private final InputStream in;
    private final OutputStream out;
    private final XmService.Address address;

    private final Lz1Decompressor decompressor = new Lz1Decompressor();
    private XmCrypt decrypt;
    private XmCrypt encrypt;

    private final Object sendLock = new Object();
    private final Object recvLock = new Object();
    private final Deque<XmpFrame> pending = new ArrayDeque<>();
    private byte[] rbuf = new byte[READ_BUFFER_SIZE];
    private int rpos;
    private int rend;

    private XmSession(Socket socket, XmService.Address address) throws IOException {
        this.socket = socket;
        this.address = address;
        this.in = socket.getInputStream();
        this.out = socket.getOutputStream();
    }

    // ------------------------------------------------------------------ connecting

    /** Connects to the first reachable address of {@code service}, in round-robin order. */
    public static XmSession connect(XmService service, Options options) throws XmException {
        XmException last = new XmException(XmError.NO_SERVICE_ENTRIES);
        List<XmService.Address> order = service.nextAttemptOrder();
        LOG.log(Level.DEBUG, "connecting; attempt order {}", order);
        for (XmService.Address a : order) {
            XmSession s;
            try {
                s = open(a, options);
            } catch (XmException e) {
                last = e;
                LOG.log(Level.WARN, "connect to {} failed ({}); trying next address if any", a, e.getMessage());
                continue;
            }
            LOG.log(Level.INFO, "connected to {}", a);
            if (options.initialCrypt() != null) {
                try {
                    KeyExchange.Keys keys = KeyExchange.perform(s, options.initialCrypt());
                    s.decrypt = keys.decrypt();
                    s.encrypt = keys.encrypt();
                    LOG.log(Level.INFO, "session encryption active ({})", keys.encrypt().algorithm());
                } catch (XmException e) {
                    LOG.log(Level.ERROR, "key exchange with {} failed: {}", a, e.getMessage());
                    s.close();
                    throw e;
                }
            }
            return s;
        }
        LOG.log(Level.ERROR, "could not connect to any address: {}", last.getMessage());
        throw last;
    }

    public static XmSession connect(String host, int port, Options options) throws XmException {
        return connect(new XmService().add(host, port), options);
    }

    private static XmSession open(XmService.Address a, Options o) throws XmException {
        Socket sock = new Socket();
        try {
            sock.setReuseAddress(true);
            if (o.receiveBufferSize() > 0) sock.setReceiveBufferSize(o.receiveBufferSize());
            if (o.sourcePort() != 0) sock.bind(new InetSocketAddress(o.sourcePort()));
            int ms = toMillis(o.connectTimeout());
            sock.connect(new InetSocketAddress(a.host(), a.port()), ms);
            sock.setSoTimeout(toMillis(o.recvTimeout()));
            return new XmSession(sock, a);
        } catch (IOException | RuntimeException e) {
            try { sock.close(); } catch (IOException ignored) { /* nothing to do */ }
            throw new XmException(XmError.NETWORK_ERROR, "connect " + a + " failed: " + e, e);
        }
    }

    private static int toMillis(Duration d) {
        if (d == null || d.isZero() || d.isNegative()) return 0;
        return (int) Math.min(d.toMillis(), Integer.MAX_VALUE);
    }

    // ------------------------------------------------------------------ accessors

    public XmService.Address address() { return address; }

    public void setRecvTimeout(Duration timeout) throws XmException {
        try {
            socket.setSoTimeout(toMillis(timeout));
        } catch (IOException e) {
            throw new XmException(XmError.NETWORK_ERROR, e.toString(), e);
        }
    }

    public boolean isClosed() { return socket.isClosed(); }

    // ------------------------------------------------------------------ send

    public void send(XmpFrame frame) throws XmException {
        byte[] bytes = frame.encode();
        if (bytes.length > Xmp.MAX_MESSAGE_LENGTH) {
            throw new XmException(XmError.BAD_PARAM, "frame of " + bytes.length + " bytes exceeds " + Xmp.MAX_MESSAGE_LENGTH);
        }
        if (LOG.isTraceEnabled()) {
            LOG.log(Level.TRACE, "send type=0x{} ext={}B body={}B encrypted={}",
                    Integer.toHexString(frame.type()), frame.ext().encodedLength(), frame.body().length, encrypt != null);
        }
        synchronized (sendLock) {
            if (encrypt != null) {
                byte[] cipher = encrypt.encrypt(bytes, 0, bytes.length);
                bytes = new XmpFrame(Xmp.Type.ENCRYPTED, cipher).encode();
            }
            try {
                out.write(bytes);
                out.flush();
            } catch (IOException e) {
                LOG.log(Level.WARN, "write to {} failed: {}", address, e.toString());
                throw new XmException(XmError.NETWORK_ERROR, "write failed: " + e, e);
            }
        }
    }

    // ------------------------------------------------------------------ receive

    /** Blocks for the next frame (after decryption/decompression). */
    public XmpFrame recv() throws XmException {
        synchronized (recvLock) {
            while (pending.isEmpty()) {
                byte[] raw = readRawFrame();
                uncook(XmpFrame.decode(raw, 0, raw.length), pending);
            }
            XmpFrame f = pending.poll();
            if (LOG.isTraceEnabled()) {
                LOG.log(Level.TRACE, "recv type=0x{} ext={}B body={}B",
                        Integer.toHexString(f.type()), f.ext().encodedLength(), f.body().length);
            }
            return f;
        }
    }

    private byte[] readRawFrame() throws XmException {
        for (;;) {
            int avail = rend - rpos;
            if (avail >= Xmp.HEADER_SIZE) {
                int total = XmpFrame.frameLength(rbuf, rpos);
                if (avail >= total) {
                    byte[] frame = new byte[total];
                    System.arraycopy(rbuf, rpos, frame, 0, total);
                    rpos += total;
                    return frame;
                }
                makeRoom(total);
            }
            fill();
        }
    }

    /** Ensures the buffer can hold {@code needed} bytes from {@code rpos}. */
    private void makeRoom(int needed) {
        if (rpos > 0) {
            System.arraycopy(rbuf, rpos, rbuf, 0, rend - rpos);
            rend -= rpos;
            rpos = 0;
        }
        if (rbuf.length < needed) rbuf = java.util.Arrays.copyOf(rbuf, needed);
    }

    private void fill() throws XmException {
        if (rpos == rend) { rpos = 0; rend = 0; }
        if (rend == rbuf.length) {
            if (rpos > 0) makeRoom(0);
            else rbuf = java.util.Arrays.copyOf(rbuf, rbuf.length * 2);
        }
        try {
            int n = in.read(rbuf, rend, rbuf.length - rend);
            if (n < 0) {
                LOG.log(Level.WARN, "connection to {} closed by peer", address);
                throw new XmException(XmError.NETWORK_ERROR, "connection closed by peer");
            }
            rend += n;
        } catch (SocketTimeoutException e) {
            LOG.log(Level.DEBUG, "receive timed out on {}", address);
            throw new XmException(XmError.TIMEOUT, "no data received", e);
        } catch (XmException e) {
            throw e;
        } catch (IOException e) {
            LOG.log(Level.WARN, "read from {} failed: {}", address, e.toString());
            throw new XmException(XmError.NETWORK_ERROR, "read failed: " + e, e);
        }
    }

    /**
     * Unwraps encryption and compression. Any extension headers that were piggy-backed on the
     * wrapper frame are discarded, as in the C implementation.
     */
    private void uncook(XmpFrame f, Deque<XmpFrame> result) throws XmException {
        if (f.type() == Xmp.Type.ENCRYPTED && decrypt != null) {
            byte[] body = f.body();
            if (body.length == 0) throw new XmException(XmError.BAD_LENGTH, "empty encrypted frame");
            byte[] plain = decrypt.decrypt(body, 0, body.length);
            LOG.log(Level.TRACE, "decrypted {} bytes", body.length);
            uncook(XmpFrame.decode(plain, 0, plain.length), result);
        } else if (f.type() == Xmp.Type.COMPRESSED) {
            byte[] body = f.body();
            if (body.length == 0) throw new XmException(XmError.BAD_LENGTH, "empty compressed frame");
            byte[] block = decompressor.decompress(body, 0, body.length);
            List<XmpFrame> inner = new ArrayList<>();
            int p = 0;
            while (block.length - p >= Xmp.HEADER_SIZE) {
                int len = XmpFrame.frameLength(block, p);
                if (len > block.length - p) throw new XmException(XmError.DC_BAD_SIZE, "truncated frame in compressed block");
                inner.add(XmpFrame.decode(block, p, len));
                p += len;
            }
            LOG.log(Level.TRACE, "decompressed {} -> {} bytes, {} frame(s)", body.length, block.length, inner.size());
            for (XmpFrame x : inner) uncook(x, result);
        } else {
            result.add(f);
        }
    }

    // ------------------------------------------------------------------ close

    @Override
    public void close() {
        if (!socket.isClosed()) LOG.log(Level.INFO, "closing session to {}", address);
        try {
            socket.close();
        } catch (IOException ignored) {
            // closing; nothing useful to do
        }
    }
}
