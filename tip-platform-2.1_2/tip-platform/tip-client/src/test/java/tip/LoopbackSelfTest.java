package tip;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import tip.client.TipClient;
import tip.client.TipMessage;
import tip.xm.ExtHeader;
import tip.xm.XmCrypt;
import tip.xm.XmError;
import tip.xm.XmException;
import tip.xm.XmService;
import tip.xm.Xmp;
import tip.xm.XmpFrame;

/**
 * Self-contained check of the client against an in-process fake server (no JUnit needed):
 * {@code java -cp target/classes:target/test-classes tip.LoopbackSelfTest}.
 *
 * <p>Message field tags used here (Sy, Pr, ...) are made up; only the framing and the TIP
 * tag/value syntax are real. The compressed block was produced by an independent encoder and
 * cross-checked against the original C decompressor.
 */
public final class LoopbackSelfTest {
    /** Two XMP frames (RTr n101 / n102) compressed into one LZ1 block. */
    private static final String COMPRESSED_BLOCK = "4150040118549545c93b5b9314c1314ed535e5314c1314c13b541724c9354b9354ed0a4150040118549545c93b5b9314c1324ed535e5314c1314c13b541724c9354b9364ed0afe0100";

    private static int failures;

    public static void main(String[] args) throws Exception {
        plainSession();
        compressedBlock();
        encryptedSession(XmCrypt.Algorithm.RC4);
        encryptedSession(XmCrypt.Algorithm.DES);
        failOver();
        receiveTimeout();
        messageSyntax();
        System.out.println(failures == 0 ? "ALL PASSED" : failures + " FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    // ------------------------------------------------------------------ scenarios

    static void plainSession() throws Exception {
        List<String> events = new CopyOnWriteArrayList<>();
        List<XmpFrame> seenByServer = new CopyOnWriteArrayList<>();
        try (FakeServer srv = new FakeServer(null, c -> {
            seenByServer.add(c.read());                                                   // LGn
            c.send(Xmp.Type.BINARY_RECORD, ExtHeader.EMPTY, "GEn;Rc0;Rxlogged on;\n");
            seenByServer.add(c.read());                                                   // RTr
            c.send(Xmp.Type.NONE, ExtHeader.EMPTY.withByte(Xmp.Tag.SESSION_STATE, 2).withInt(Xmp.Tag.QUEUE_ALERT, 700), "");
            c.send(Xmp.Type.BINARY_RECORD, ExtHeader.EMPTY.withByte(Xmp.Tag.SESSION_STATE, 3), "RTr;n1;Sy1010;\n");
            c.send(Xmp.Type.BINARY_RECORD, ExtHeader.EMPTY, "HBt;\n");
            seenByServer.add(c.read());                                                   // LGf
        });
             TipClient tip = TipClient.builder().compression(true).queueAlertMillis(250)
                     .listener(new TipClient.ExtensionListener() {
                         @Override public void onQueueAlert(int ms) { events.add("alert " + ms); }
                         @Override public void onSessionState(Xmp.SessionState s, int raw) { events.add("state " + s); }
                     }).build()) {
            tip.connect("127.0.0.1", srv.port());
            TipMessage reply = tip.logon("user;1", "pa\\ss", "1");
            check("logon reply type", "GEn".equals(reply.type()));
            check("logon reply Rc", "0".equals(reply.get("Rc").orElse(null)));
            tip.requestRealtime(5, 0, 20261031, TipClient.Scope.ALL);
            TipMessage m1 = tip.next();
            TipMessage m2 = tip.next();
            check("message 1", "RTr".equals(m1.type()) && "1".equals(m1.get("n").orElse("")) && "1010".equals(m1.get("Sy").orElse("")));
            check("heartbeat", "HBt".equals(m2.type()));
            check("extension events", events.equals(List.of("state CATCHUP", "alert 700", "state ONLINE")));
            tip.logoff();
            Thread.sleep(200);
        }
        check("logon text escaped", seenByServer.get(0).body().length > 0
                && new String(seenByServer.get(0).body(), StandardCharsets.UTF_8).equals("LGn;UIduser\\;1;PWdpa\\\\ss;VEr1;\n"));
        check("request text", new String(seenByServer.get(1).body(), StandardCharsets.UTF_8).equals("RTr;Sn5;QDAy20261031;QSCo3;\n"));
        ExtHeader ext = seenByServer.get(1).ext();
        check("compression ext header", ext.find(Xmp.Tag.COMPRESSION_ALGORITHM).map(e -> e.data()[0]).orElse((byte) -1) == 1);
        check("queue alert ext header", ext.find(Xmp.Tag.QUEUE_ALERT).map(ExtHeader.Entry::intValue).orElse(-1) == 250);
        check("logoff sent", new String(seenByServer.get(2).body(), StandardCharsets.UTF_8).equals("LGf;\n"));
    }

    static void compressedBlock() throws Exception {
        byte[] block = HexFormat.of().parseHex(COMPRESSED_BLOCK);
        try (FakeServer srv = new FakeServer(null, c -> {
            c.read();
            c.sendRaw(new XmpFrame(Xmp.Type.COMPRESSED, block));
            c.sendRaw(new XmpFrame(Xmp.Type.COMPRESSED, block));   // second use of the same history window
        }); TipClient tip = TipClient.builder().compression(true).build()) {
            tip.connect("127.0.0.1", srv.port());
            tip.requestBasicData(0, TipClient.Scope.UPDATES);
            String[] got = new String[4];
            for (int i = 0; i < 4; i++) got[i] = tip.nextRaw();
            check("compressed frames unpacked in order",
                    got[0].equals("RTr;n101;Sy1010;Pr25.5;\n") && got[1].equals("RTr;n102;Sy1010;Pr25.6;\n")
                            && got[2].equals(got[0]) && got[3].equals(got[1]));
        }
    }

    static void encryptedSession(XmCrypt.Algorithm alg) throws Exception {
        byte[] preShared = HexFormat.of().parseHex("0f1e2d3c4b5a6978a1b2c3d4e5f60718");
        try (FakeServer srv = new FakeServer(XmCrypt.create(alg, preShared, 42), c -> {
            XmpFrame req = c.read();                       // logon, encrypted with the session key
            check(alg + ": server read decrypted logon", new String(req.body(), StandardCharsets.UTF_8).startsWith("LGn;UIdu;"));
            c.send(Xmp.Type.BINARY_RECORD, ExtHeader.EMPTY, "GEn;Rc0;\n");
            c.send(Xmp.Type.BINARY_RECORD, ExtHeader.EMPTY, "RTr;n7;Sy1;\n");
        }); TipClient tip = TipClient.builder().initialCrypt(XmCrypt.create(alg, preShared, 42)).build()) {
            tip.connect("127.0.0.1", srv.port());
            check(alg + ": logon over encrypted session", "GEn".equals(tip.logon("u", "p", null).type()));
            check(alg + ": data over encrypted session", "7".equals(tip.next().get("n").orElse("")));
        }
    }

    static void failOver() throws Exception {
        int dead;
        try (ServerSocket s = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) { dead = s.getLocalPort(); }
        try (FakeServer srv = new FakeServer(null, c -> c.send(Xmp.Type.BINARY_RECORD, ExtHeader.EMPTY, "HBt;\n"));
             TipClient tip = TipClient.builder().connectTimeout(Duration.ofSeconds(2)).build()) {
            tip.connect(List.of(new XmService.Address("127.0.0.1", dead), new XmService.Address("127.0.0.1", srv.port())));
            check("fail-over reaches second address", "HBt".equals(tip.next().type()));
        }
        try (TipClient tip = TipClient.builder().connectTimeout(Duration.ofSeconds(2)).build()) {
            try {
                tip.connect("127.0.0.1", dead);
                check("connect to closed port fails", false);
            } catch (XmException e) {
                check("connect to closed port fails", e.error() == XmError.NETWORK_ERROR);
            }
        }
    }

    static void receiveTimeout() throws Exception {
        try (FakeServer srv = new FakeServer(null, c -> Thread.sleep(1500));
             TipClient tip = TipClient.builder().recvTimeout(Duration.ofMillis(300)).build()) {
            tip.connect("127.0.0.1", srv.port());
            long t0 = System.nanoTime();
            try {
                tip.nextRaw();
                check("receive timeout", false);
            } catch (XmException e) {
                check("receive timeout", e.error() == XmError.TIMEOUT && (System.nanoTime() - t0) < 1_000_000_000L);
            }
        }
    }

    static void messageSyntax() throws Exception {
        TipMessage m = TipMessage.parse("RTr;n12;Rxa\\;b\\\\c;Qt;\n");
        check("parse escapes", "a;b\\c".equals(m.get("Rx").orElse(null)) && "".equals(m.get("Qt").orElse(null)) && m.fields().size() == 3);
        check("round trip", TipMessage.parse(m.toWire()).toWire().equals(m.toWire()));
        for (String bad : List.of("RTr", "RTr;n12", "RTr;1x;", "RTr;R x;", "RTr;Rxabc\\")) {
            try {
                TipMessage.parse(bad);
                check("rejects '" + bad + "'", false);
            } catch (IOException e) {
                check("rejects '" + bad + "'", true);
            }
        }
    }

    // ------------------------------------------------------------------ fake server

    interface Script { void run(Conn c) throws Exception; }

    /** Server side of one connection. */
    static final class Conn {
        private final DataInputStream in;
        private final OutputStream out;
        private XmCrypt toClient;     // server -> client
        private XmCrypt fromClient;   // client -> server

        Conn(Socket s) throws IOException {
            in = new DataInputStream(s.getInputStream());
            out = s.getOutputStream();
        }

        XmpFrame readRaw() throws IOException {
            byte[] h = new byte[4];
            in.readFully(h);
            byte[] all = new byte[XmpFrame.frameLength(h, 0)];
            System.arraycopy(h, 0, all, 0, 4);
            in.readFully(all, 4, all.length - 4);
            return XmpFrame.decode(all, 0, all.length);
        }

        XmpFrame read() throws IOException {
            XmpFrame f = readRaw();
            if (f.type() == Xmp.Type.ENCRYPTED && fromClient != null) {
                byte[] p = fromClient.decrypt(f.body(), 0, f.body().length);
                return XmpFrame.decode(p, 0, p.length);
            }
            return f;
        }

        void sendRaw(XmpFrame f) throws IOException {
            out.write(f.encode());
            out.flush();
        }

        void send(int type, ExtHeader ext, String text) throws IOException {
            XmpFrame f = new XmpFrame(type, ext, text.getBytes(StandardCharsets.UTF_8));
            if (toClient != null) {
                byte[] bytes = f.encode();
                f = new XmpFrame(Xmp.Type.ENCRYPTED, toClient.encrypt(bytes, 0, bytes.length));
            }
            sendRaw(f);
        }

        /** Server half of the key exchange; {@code initial} is used for both directions, like the C client. */
        void keyExchange(XmCrypt initial) throws IOException {
            XmpFrame tsReq = readRaw();
            check("timestamp request", tsReq.ext().find(Xmp.Tag.TIMESTAMP).isPresent());
            sendRaw(new XmpFrame(Xmp.Type.NONE, ExtHeader.EMPTY.with(Xmp.Tag.TIMESTAMP, new byte[] {0x65, 0x00, 0x00, 0x00, 0, 0, 0, 0}), null));

            XmpFrame req = readRaw();
            check("key exchange request ext", req.ext().find(Xmp.Tag.KEY_ID).get().intValue() == initial.keyId());
            byte[] plain = initial.decrypt(req.body(), 0, req.body().length);
            check("key exchange request type", (plain[0] & 0xFF) == Xmp.Type.KEY_EXCHANGE && plain[4] == 1);

            byte[] sessionKey = HexFormat.of().parseHex("a5a5a5a55a5a5a5a0102030405060708");
            byte[] kx = new byte[36];
            kx[0] = Xmp.Type.KEY_EXCHANGE;
            kx[3] = 32;
            kx[4] = 2;                                   // reply
            System.arraycopy(plain, 5, kx, 5, 7);        // echo nonce
            System.arraycopy(plain, 12, kx, 12, 8);      // key id + time
            System.arraycopy(sessionKey, 0, kx, 20, 16);
            ExtHeader ext = ExtHeader.EMPTY.withInt(Xmp.Tag.KEY_ID, initial.keyId()).withByte(Xmp.Tag.ENCRYPTION_ALGORITHM, initial.algorithm().code());
            sendRaw(new XmpFrame(Xmp.Type.ENCRYPTED, ext, initial.encrypt(kx, 0, kx.length)));

            fromClient = XmCrypt.create(initial.algorithm(), sessionKey, XmCrypt.NO_KEY_ID);
            toClient = XmCrypt.create(initial.algorithm(), sessionKey, XmCrypt.NO_KEY_ID);
        }
    }

    static final class FakeServer implements AutoCloseable {
        private final ServerSocket server;
        private final Thread thread;

        FakeServer(XmCrypt initialKey, Script script) throws IOException {
            server = new ServerSocket(0, 5, InetAddress.getLoopbackAddress());
            thread = Thread.ofVirtual().start(() -> {
                try (Socket s = server.accept()) {
                    Conn c = new Conn(s);
                    if (initialKey != null) c.keyExchange(initialKey);
                    script.run(c);
                    Thread.sleep(300);
                } catch (Exception e) {
                    if (!server.isClosed()) { System.out.println("  fake server: " + e); failures++; }
                }
            });
        }

        int port() { return server.getLocalPort(); }

        @Override public void close() throws IOException { server.close(); thread.interrupt(); }
    }

    static void check(String what, boolean ok) {
        System.out.println((ok ? "  ok    " : "  FAIL  ") + what);
        if (!ok) failures++;
    }
}
