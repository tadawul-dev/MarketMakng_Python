package tip.testutil;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import tip.xm.ExtHeader;
import tip.xm.XmCrypt;
import tip.xm.Xmp;
import tip.xm.XmpFrame;

/**
 * An in-process stand-in for the exchange. Each accepted connection runs {@code script};
 * anything the script throws (including failed assertions) is rethrown by {@link #close()},
 * so a test cannot pass while the server side silently failed.
 */
public final class FakeServer implements AutoCloseable {

    @FunctionalInterface
    public interface Script {
        void run(Conn c) throws Exception;
    }

    /** How the server misbehaves during the key exchange. */
    public enum KxFault {
        NONE, WRONG_KEY_ID_EXT, WRONG_ALGORITHM_EXT, NO_KEY_ID_EXT, REPLY_NOT_ENCRYPTED,
        SHORT_REPLY, BAD_OPCODE, WRONG_KEY_ID_BODY, NO_TIMESTAMP, SHORT_TIMESTAMP, ENCRYPTED_WITH_OTHER_KEY
    }

    private final ServerSocket server;
    private final Thread thread;
    private final List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger connections = new AtomicInteger();

    public FakeServer(Script script) throws IOException {
        this(1, script);
    }

    /** Serves {@code maxConnections} connections one after another, each with {@code script}. */
    public FakeServer(int maxConnections, Script script) throws IOException {
        server = new ServerSocket(0, 5, InetAddress.getLoopbackAddress());
        thread = new Thread(() -> {
            for (int i = 0; i < maxConnections; i++) {
                try (Socket s = server.accept()) {
                    connections.incrementAndGet();
                    script.run(new Conn(s, i));
                } catch (java.net.SocketException e) {
                    if (!server.isClosed()) errors.add(e);
                    return;
                } catch (Throwable t) {
                    errors.add(t);
                }
            }
        }, "fake-server");
        thread.setDaemon(true);
        thread.start();
    }

    public int port() { return server.getLocalPort(); }

    public int connectionCount() { return connections.get(); }

    /** A port on which nothing listens. */
    public static int deadPort() throws IOException {
        try (ServerSocket s = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            return s.getLocalPort();
        }
    }

    @Override
    public void close() throws IOException {
        try {
            thread.join(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        server.close();
        thread.interrupt();
        if (!errors.isEmpty()) {
            AssertionError ae = new AssertionError("fake server script failed: " + errors.get(0), errors.get(0));
            for (int i = 1; i < errors.size(); i++) ae.addSuppressed(errors.get(i));
            throw ae;
        }
    }

    // ------------------------------------------------------------------------------------

    /** Server side of one connection. */
    public static final class Conn {
        public final int index;
        private final Socket socket;
        private final DataInputStream in;
        private final OutputStream out;
        private XmCrypt toClient;
        private XmCrypt fromClient;
        /** Plain text of the key exchange request as the client sent it (set by {@link #keyExchange}). */
        public byte[] kxRequestPlain;
        public ExtHeader kxRequestExt;
        public int serverTime = 0x65000000;

        Conn(Socket s, int index) throws IOException {
            this.socket = s;
            this.index = index;
            in = new DataInputStream(s.getInputStream());
            out = s.getOutputStream();
        }

        /** Reads one frame exactly as it came off the wire (no decryption). */
        public XmpFrame readRaw() throws IOException {
            byte[] h = new byte[4];
            in.readFully(h);
            byte[] all = new byte[XmpFrame.frameLength(h, 0)];
            System.arraycopy(h, 0, all, 0, 4);
            in.readFully(all, 4, all.length - 4);
            try {
                return XmpFrame.decode(all, 0, all.length);
            } catch (tip.xm.XmException e) {
                throw new IOException(e);
            }
        }

        /** Reads one frame, decrypting it if a session key is active. */
        public XmpFrame read() throws IOException {
            XmpFrame f = readRaw();
            if (f.type() == Xmp.Type.ENCRYPTED && fromClient != null) {
                byte[] p = fromClient.decrypt(f.body(), 0, f.body().length);
                return XmpFrame.decode(p, 0, p.length);
            }
            return f;
        }

        public String readText() throws IOException {
            return new String(read().body(), StandardCharsets.UTF_8);
        }

        public void writeBytes(byte[] b) throws IOException {
            out.write(b);
            out.flush();
        }

        public void writeBytes(byte[] b, int off, int len) throws IOException {
            out.write(b, off, len);
            out.flush();
        }

        /** Sends the frame as is (not encrypted even if a session key is active). */
        public void sendRaw(XmpFrame f) throws IOException {
            writeBytes(f.encode());
        }

        /** Sends the frame, encrypted if a session key is active. */
        public void sendFrame(XmpFrame f) throws IOException {
            if (toClient != null) {
                byte[] bytes = f.encode();
                f = new XmpFrame(Xmp.Type.ENCRYPTED, toClient.encrypt(bytes, 0, bytes.length));
            }
            sendRaw(f);
        }

        public void send(String tipText) throws IOException {
            send(Xmp.Type.BINARY_RECORD, ExtHeader.EMPTY, tipText);
        }

        public void send(int type, ExtHeader ext, String text) throws IOException {
            sendFrame(new XmpFrame(type, ext, text.getBytes(StandardCharsets.UTF_8)));
        }

        public void sendBytes(int type, ExtHeader ext, byte[] body) throws IOException {
            sendFrame(new XmpFrame(type, ext, body));
        }

        /** Closes the connection now (the client then sees end-of-stream). */
        public void hangUp() throws IOException {
            socket.close();
        }

        /** Resets the connection (RST) instead of a graceful close. */
        public void reset() throws IOException {
            socket.setSoLinger(true, 0);
            socket.close();
        }

        public void pause(long millis) throws InterruptedException {
            Thread.sleep(millis);
        }

        /** Waits until the client closes its side (returns on EOF or error). */
        public void awaitClientClose() {
            try {
                while (in.read() >= 0) { /* drain */ }
            } catch (IOException ignored) {
                // closed
            }
        }

        public void keyExchange(XmCrypt initial) throws Exception {
            keyExchange(initial, KxFault.NONE);
        }

        /**
         * Server half of the key exchange. The session key is fixed and both directions use it
         * (as the client does), so after this {@link #read}/{@link #sendFrame} are encrypted.
         */
        public void keyExchange(XmCrypt initial, KxFault fault) throws Exception {
            XmpFrame tsReq = readRaw();
            if (tsReq.type() != Xmp.Type.NONE || tsReq.ext().find(Xmp.Tag.TIMESTAMP).isEmpty()) {
                throw new AssertionError("expected a timestamp request, got type " + tsReq.type());
            }
            switch (fault) {
                case NO_TIMESTAMP -> sendRaw(new XmpFrame(Xmp.Type.NONE, ExtHeader.EMPTY.withByte(Xmp.Tag.SESSION_STATE, 1), null));
                case SHORT_TIMESTAMP -> sendRaw(new XmpFrame(Xmp.Type.NONE, ExtHeader.EMPTY.with(Xmp.Tag.TIMESTAMP, new byte[] {1, 2}), null));
                default -> sendRaw(new XmpFrame(Xmp.Type.NONE,
                        ExtHeader.EMPTY.with(Xmp.Tag.TIMESTAMP, new byte[] {(byte) (serverTime >>> 24), (byte) (serverTime >>> 16), (byte) (serverTime >>> 8), (byte) serverTime, 0, 0, 0, 0}), null));
            }
            if (fault == KxFault.NO_TIMESTAMP || fault == KxFault.SHORT_TIMESTAMP) return;

            XmpFrame req = readRaw();
            kxRequestExt = req.ext();
            kxRequestPlain = initial.decrypt(req.body(), 0, req.body().length);

            byte[] sessionKey = {(byte) 0xa5, (byte) 0xa5, (byte) 0xa5, (byte) 0xa5, 0x5a, 0x5a, 0x5a, 0x5a, 1, 2, 3, 4, 5, 6, 7, 8};
            byte[] kx = new byte[36];
            kx[0] = Xmp.Type.KEY_EXCHANGE;
            kx[3] = 32;
            kx[4] = (byte) (fault == KxFault.BAD_OPCODE ? 9 : 2);
            System.arraycopy(kxRequestPlain, 5, kx, 5, 7);
            System.arraycopy(kxRequestPlain, 12, kx, 12, 8);
            if (fault == KxFault.WRONG_KEY_ID_BODY) kx[15] ^= 0x7f;
            System.arraycopy(sessionKey, 0, kx, 20, 16);

            int keyIdInExt = initial.keyId();
            int algInExt = initial.algorithm().code();
            if (fault == KxFault.WRONG_KEY_ID_EXT) keyIdInExt++;
            if (fault == KxFault.WRONG_ALGORITHM_EXT) algInExt = algInExt == 1 ? 2 : 1;
            ExtHeader ext = fault == KxFault.NO_KEY_ID_EXT
                    ? ExtHeader.EMPTY.withByte(Xmp.Tag.ENCRYPTION_ALGORITHM, algInExt)
                    : ExtHeader.EMPTY.withInt(Xmp.Tag.KEY_ID, keyIdInExt).withByte(Xmp.Tag.ENCRYPTION_ALGORITHM, algInExt);

            XmCrypt replyCipher = initial;
            if (fault == KxFault.ENCRYPTED_WITH_OTHER_KEY) {
                replyCipher = XmCrypt.create(initial.algorithm(), new byte[] {9, 8, 7, 6, 5, 4, 3, 2, 1, 9, 8, 7, 6, 5, 4, 3}, initial.keyId());
            }
            if (fault == KxFault.REPLY_NOT_ENCRYPTED) {
                sendRaw(new XmpFrame(Xmp.Type.BINARY_RECORD, ext, new byte[] {1, 2, 3}));
                return;
            }
            byte[] cipher = replyCipher.encrypt(kx, 0, fault == KxFault.SHORT_REPLY ? 8 : kx.length);
            sendRaw(new XmpFrame(Xmp.Type.ENCRYPTED, ext, cipher));

            fromClient = XmCrypt.create(initial.algorithm(), sessionKey, XmCrypt.NO_KEY_ID);
            toClient = XmCrypt.create(initial.algorithm(), sessionKey, XmCrypt.NO_KEY_ID);
        }
    }
}
