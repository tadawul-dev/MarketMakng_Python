package tip.xm;

import static org.junit.Assert.*;
import static tip.testutil.TestData.*;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import tip.testutil.FakeServer;
import tip.testutil.Lz1Writer;

/** XmSession against the in-process fake server. */
public class XmSessionTest {
    private static final byte[] PRE_SHARED = hex("0f1e2d3c4b5a6978a1b2c3d4e5f60718");

    private static XmSession open(FakeServer s) throws XmException {
        return XmSession.connect("127.0.0.1", s.port(), XmSession.Options.defaults().withConnectTimeout(Duration.ofSeconds(3)));
    }

    private static XmSession open(FakeServer s, Duration recvTimeout) throws XmException {
        return XmSession.connect("127.0.0.1", s.port(),
                XmSession.Options.defaults().withConnectTimeout(Duration.ofSeconds(3)).withRecvTimeout(recvTimeout));
    }

    private static XmpFrame text(String s) {
        return new XmpFrame(Xmp.Type.BINARY_RECORD, s.getBytes(StandardCharsets.UTF_8));
    }

    private static String body(XmpFrame f) { return new String(f.body(), StandardCharsets.UTF_8); }

    private static XmError recvError(XmSession s) {
        try {
            s.recv();
            fail("expected XmException");
            return null;
        } catch (XmException e) {
            return e.error();
        }
    }

    // ------------------------------------------------------------------ basic traffic

    @Test public void sendsAndReceivesFrames() throws Exception {
        try (FakeServer srv = new FakeServer(c -> {
            assertEquals("ping;\n", c.readText());
            c.send("pong;\n");
        }); XmSession s = open(srv)) {
            s.send(text("ping;\n"));
            assertEquals("pong;\n", body(s.recv()));
            assertEquals(new XmService.Address("127.0.0.1", srv.port()), s.address());
        }
    }

    @Test public void extensionHeadersTravelWithTheFrame() throws Exception {
        try (FakeServer srv = new FakeServer(c -> {
            XmpFrame f = c.read();
            assertEquals(1, f.ext().find(Xmp.Tag.COMPRESSION_ALGORITHM).get().data()[0]);
            assertEquals(500, f.ext().find(Xmp.Tag.QUEUE_ALERT).get().intValue());
            c.sendBytes(Xmp.Type.BINARY_RECORD, ExtHeader.EMPTY.withByte(Xmp.Tag.SESSION_STATE, 3), "x".getBytes());
        }); XmSession s = open(srv)) {
            s.send(new XmpFrame(Xmp.Type.BINARY_RECORD, ExtHeader.EMPTY.withByte(Xmp.Tag.COMPRESSION_ALGORITHM, 1).withInt(Xmp.Tag.QUEUE_ALERT, 500), "hi".getBytes()));
            assertEquals(3, s.recv().ext().find(Xmp.Tag.SESSION_STATE).get().data()[0]);
        }
    }

    @Test public void frameDeliveredOneByteAtATime() throws Exception {
        byte[] wire = frameBytes("RTr;n1;Sy1010;\n");
        try (FakeServer srv = new FakeServer(c -> {
            for (byte b : wire) { c.writeBytes(new byte[] {b}); c.pause(2); }
        }); XmSession s = open(srv)) {
            assertEquals("RTr;n1;Sy1010;\n", body(s.recv()));
        }
    }

    @Test public void manyFramesInOneTcpWrite() throws Exception {
        ByteArrayOutputStream all = new ByteArrayOutputStream();
        for (int i = 0; i < 200; i++) all.write(frameBytes("RTr;n" + i + ";\n"));
        try (FakeServer srv = new FakeServer(c -> c.writeBytes(all.toByteArray())); XmSession s = open(srv)) {
            for (int i = 0; i < 200; i++) assertEquals("RTr;n" + i + ";\n", body(s.recv()));
        }
    }

    @Test public void framesStraddlingTheReadBufferBoundary() throws Exception {
        // 150 frames of ~3000 bytes = 450 KB through a 32 KB read buffer
        String filler = "x".repeat(2990);
        try (FakeServer srv = new FakeServer(c -> {
            for (int i = 0; i < 150; i++) c.send("D;n" + i + ";" + filler + ";\n");
        }); XmSession s = open(srv)) {
            for (int i = 0; i < 150; i++) {
                String b = body(s.recv());
                assertTrue("frame " + i, b.startsWith("D;n" + i + ";"));
                assertEquals("frame " + i + " length", ("D;n" + i + ";").length() + 2990 + 2, b.length());
                assertEquals("frame " + i + " intact", 'x', b.charAt(b.length() - 10));
            }
        }
    }

    @Test public void largestAllowedFrameIsSentAndRefusedWhenOneByteLarger() throws Exception {
        try (FakeServer srv = new FakeServer(c -> assertEquals(Xmp.MAX_MESSAGE_LENGTH, c.readRaw().encodedLength()));
             XmSession s = open(srv)) {
            s.send(new XmpFrame(5, new byte[Xmp.MAX_MESSAGE_LENGTH - 4]));
            try {
                s.send(new XmpFrame(5, new byte[Xmp.MAX_MESSAGE_LENGTH - 3]));
                fail();
            } catch (XmException e) {
                assertEquals(XmError.BAD_PARAM, e.error());
            }
        }
    }

    @Test public void emptyBodyOfTheWrongTypeIsRefusedBeforeAnythingIsSent() throws Exception {
        try (FakeServer srv = new FakeServer(c -> c.awaitClientClose()); XmSession s = open(srv)) {
            try {
                s.send(new XmpFrame(5, new byte[0]));
                fail();
            } catch (XmException e) {
                assertEquals(XmError.BAD_LENGTH, e.error());
            }
        }
    }

    @Test public void concurrentSenderAndReceiver() throws Exception {
        int n = 300;
        try (FakeServer srv = new FakeServer(c -> {
            for (int i = 0; i < n; i++) c.send(c.readText().replace("req", "rsp"));
        }); XmSession s = open(srv, Duration.ofSeconds(10))) {
            AtomicReference<Throwable> sendFailure = new AtomicReference<>();
            Thread sender = new Thread(() -> {
                try { for (int i = 0; i < n; i++) s.send(text("req" + i + ";\n")); } catch (Throwable t) { sendFailure.set(t); }
            });
            sender.start();
            for (int i = 0; i < n; i++) assertEquals("rsp" + i + ";\n", body(s.recv()));
            sender.join();
            assertNull(sendFailure.get());
        }
    }

    @Test public void concurrentSendersDoNotInterleaveFrames() throws Exception {
        int perThread = 100;
        List<String> got = new ArrayList<>();
        try (FakeServer srv = new FakeServer(c -> { for (int i = 0; i < 4 * perThread; i++) got.add(c.readText()); });
             XmSession s = open(srv)) {
            Thread[] ts = new Thread[4];
            for (int t = 0; t < 4; t++) {
                final int id = t;
                ts[t] = new Thread(() -> {
                    try { for (int i = 0; i < perThread; i++) s.send(text("T" + id + ";" + "y".repeat(500) + ";" + i + ";\n")); }
                    catch (XmException e) { throw new RuntimeException(e); }
                });
                ts[t].start();
            }
            for (Thread t : ts) t.join();
        }
        assertEquals(4 * perThread, got.size());
        for (String m : got) assertTrue("intact frame", m.length() > 500 && m.startsWith("T") && m.endsWith(";\n"));
    }

    // ------------------------------------------------------------------ failures

    @Test public void peerClosingBetweenFramesIsANetworkError() throws Exception {
        try (FakeServer srv = new FakeServer(c -> { c.send("one;\n"); c.hangUp(); }); XmSession s = open(srv)) {
            assertEquals("one;\n", body(s.recv()));
            assertEquals(XmError.NETWORK_ERROR, recvError(s));
        }
    }

    @Test public void peerClosingInTheMiddleOfAFrameIsANetworkError() throws Exception {
        byte[] wire = frameBytes("RTr;n1;Sy1010;\n");
        try (FakeServer srv = new FakeServer(c -> { c.writeBytes(wire, 0, wire.length - 3); c.hangUp(); }); XmSession s = open(srv)) {
            assertEquals(XmError.NETWORK_ERROR, recvError(s));
        }
    }

    @Test public void connectionResetIsANetworkError() throws Exception {
        try (FakeServer srv = new FakeServer(c -> { c.pause(100); c.reset(); }); XmSession s = open(srv)) {
            assertEquals(XmError.NETWORK_ERROR, recvError(s));
        }
    }

    @Test public void receiveTimeoutLeavesAPartialFrameIntact() throws Exception {
        byte[] wire = frameBytes("RTr;n1;Sy1010;Pr25.5;\n");
        int cut = wire.length / 2;
        try (FakeServer srv = new FakeServer(c -> {
            c.writeBytes(wire, 0, cut);
            c.pause(700);
            c.writeBytes(wire, cut, wire.length - cut);
        }); XmSession s = open(srv, Duration.ofMillis(250))) {
            assertEquals(XmError.TIMEOUT, recvError(s));
            s.setRecvTimeout(Duration.ofSeconds(5));
            assertEquals("RTr;n1;Sy1010;Pr25.5;\n", body(s.recv()));
        }
    }

    @Test public void silentServerTimesOutQuickly() throws Exception {
        try (FakeServer srv = new FakeServer(c -> c.awaitClientClose()); XmSession s = open(srv, Duration.ofMillis(200))) {
            long t0 = System.nanoTime();
            assertEquals(XmError.TIMEOUT, recvError(s));
            assertTrue((System.nanoTime() - t0) / 1_000_000 < 3000);
        }
    }

    @Test public void noTimeoutWhenNoneIsConfigured() throws Exception {
        try (FakeServer srv = new FakeServer(c -> { c.pause(600); c.send("late;\n"); }); XmSession s = open(srv)) {
            assertEquals("late;\n", body(s.recv()));
        }
    }

    @Test public void usingAClosedSessionFails() throws Exception {
        try (FakeServer srv = new FakeServer(c -> c.awaitClientClose())) {
            XmSession s = open(srv);
            assertFalse(s.isClosed());
            s.close();
            s.close();                                   // idempotent
            assertTrue(s.isClosed());
            assertEquals(XmError.NETWORK_ERROR, recvError(s));
            try {
                s.send(text("x;\n"));
                fail();
            } catch (XmException e) {
                assertEquals(XmError.NETWORK_ERROR, e.error());
            }
        }
    }

    @Test public void closingFromAnotherThreadUnblocksARead() throws Exception {
        try (FakeServer srv = new FakeServer(c -> c.awaitClientClose())) {
            XmSession s = open(srv);
            AtomicReference<XmError> result = new AtomicReference<>();
            Thread reader = new Thread(() -> result.set(recvError(s)));
            reader.start();
            Thread.sleep(200);
            s.close();
            reader.join(3000);
            assertFalse("reader still blocked", reader.isAlive());
            assertEquals(XmError.NETWORK_ERROR, result.get());
        }
    }

    @Test public void connectionRefused() {
        try {
            XmSession.connect("127.0.0.1", FakeServerPorts.dead(), XmSession.Options.defaults().withConnectTimeout(Duration.ofSeconds(2)));
            fail();
        } catch (XmException e) {
            assertEquals(XmError.NETWORK_ERROR, e.error());
            assertTrue(e.getMessage(), e.getMessage().contains("connect"));
        }
    }

    @Test public void unknownHost() {
        try {
            XmSession.connect("no-such-host.invalid", 4000, XmSession.Options.defaults().withConnectTimeout(Duration.ofSeconds(2)));
            fail();
        } catch (XmException e) {
            assertEquals(XmError.NETWORK_ERROR, e.error());
        }
    }

    @Test public void connectTimeoutToUnroutableAddress() {
        long t0 = System.nanoTime();
        try {
            XmSession.connect("10.255.255.1", 4000, XmSession.Options.defaults().withConnectTimeout(Duration.ofMillis(300)));
            fail();
        } catch (XmException e) {
            assertEquals(XmError.NETWORK_ERROR, e.error());
        }
        assertTrue("took too long", (System.nanoTime() - t0) / 1_000_000 < 5000);
    }

    // ------------------------------------------------------------------ fail-over and round robin

    @Test public void failsOverToTheNextAddress() throws Exception {
        try (FakeServer live = new FakeServer(c -> c.send("alive;\n"))) {
            XmService svc = new XmService().add("127.0.0.1", FakeServerPorts.dead()).add("127.0.0.1", live.port());
            try (XmSession s = XmSession.connect(svc, XmSession.Options.defaults().withConnectTimeout(Duration.ofSeconds(2)))) {
                assertEquals("alive;\n", body(s.recv()));
                assertEquals(live.port(), s.address().port());
            }
        }
    }

    @Test public void successiveConnectsStartAtDifferentAddresses() throws Exception {
        try (FakeServer a = new FakeServer(c -> c.awaitClientClose()); FakeServer b = new FakeServer(c -> c.awaitClientClose())) {
            XmService svc = new XmService().add("127.0.0.1", a.port()).add("127.0.0.1", b.port());
            XmSession.Options o = XmSession.Options.defaults();
            try (XmSession s1 = XmSession.connect(svc, o); XmSession s2 = XmSession.connect(svc, o)) {
                assertEquals(a.port(), s1.address().port());
                assertEquals(b.port(), s2.address().port());
            }
        }
    }

    @Test public void allAddressesDown() {
        XmService svc;
        try {
            svc = new XmService().add("127.0.0.1", FakeServerPorts.dead()).add("127.0.0.1", FakeServerPorts.dead());
            XmSession.connect(svc, XmSession.Options.defaults().withConnectTimeout(Duration.ofSeconds(2)));
            fail();
        } catch (XmException e) {
            assertEquals(XmError.NETWORK_ERROR, e.error());
        }
    }

    @Test public void emptyServiceCannotConnect() {
        try {
            XmSession.connect(new XmService(), XmSession.Options.defaults());
            fail();
        } catch (XmException e) {
            assertEquals(XmError.NO_SERVICE_ENTRIES, e.error());
        }
    }

    // ------------------------------------------------------------------ encryption

    private static void encryptedRoundTrip(XmCrypt.Algorithm alg) throws Exception {
        try (FakeServer srv = new FakeServer(c -> {
            c.keyExchange(XmCrypt.create(alg, PRE_SHARED, 42));
            for (int i = 0; i < 3; i++) {
                XmpFrame raw = c.readRaw();
                assertEquals("traffic must be wrapped in an encrypted frame", Xmp.Type.ENCRYPTED, raw.type());
                assertFalse("plaintext leaked", new String(raw.body(), StandardCharsets.ISO_8859_1).contains("secret"));
            }
        }); XmSession s = XmSession.connect("127.0.0.1", srv.port(),
                XmSession.Options.defaults().withInitialCrypt(XmCrypt.create(alg, PRE_SHARED, 42)))) {
            for (int i = 0; i < 3; i++) s.send(text("secret message " + i + ";\n"));
        }
    }

    @Test public void rc4SessionEncryptsClientToServer() throws Exception { encryptedRoundTrip(XmCrypt.Algorithm.RC4); }

    @Test public void desSessionEncryptsClientToServer() throws Exception { encryptedRoundTrip(XmCrypt.Algorithm.DES); }

    private static void encryptedBothWays(XmCrypt.Algorithm alg) throws Exception {
        try (FakeServer srv = new FakeServer(c -> {
            c.keyExchange(XmCrypt.create(alg, PRE_SHARED, 42));
            for (int i = 0; i < 5; i++) {
                assertEquals("q" + i + ";\n", c.readText());
                c.send(Xmp.Type.BINARY_RECORD, ExtHeader.EMPTY.withByte(Xmp.Tag.SESSION_STATE, 3), "a" + i + ";\n");
            }
        }); XmSession s = XmSession.connect("127.0.0.1", srv.port(),
                XmSession.Options.defaults().withInitialCrypt(XmCrypt.create(alg, PRE_SHARED, 42)))) {
            for (int i = 0; i < 5; i++) {
                s.send(text("q" + i + ";\n"));
                XmpFrame r = s.recv();
                assertEquals("a" + i + ";\n", body(r));
                assertEquals("extension header inside the encrypted frame is preserved", 3, r.ext().find(Xmp.Tag.SESSION_STATE).get().data()[0]);
            }
        }
    }

    @Test public void rc4SessionBothDirections() throws Exception { encryptedBothWays(XmCrypt.Algorithm.RC4); }

    @Test public void desSessionBothDirections() throws Exception { encryptedBothWays(XmCrypt.Algorithm.DES); }

    @Test public void encryptedFrameWithoutSessionKeyIsPassedThroughUntouched() throws Exception {
        try (FakeServer srv = new FakeServer(c -> c.sendRaw(new XmpFrame(Xmp.Type.ENCRYPTED, new byte[] {1, 2, 3})));
             XmSession s = open(srv)) {
            XmpFrame f = s.recv();
            assertEquals(Xmp.Type.ENCRYPTED, f.type());
            assertArrayEquals(new byte[] {1, 2, 3}, f.body());
        }
    }

    @Test public void emptyEncryptedFrameIsRejected() throws Exception {
        try (FakeServer srv = new FakeServer(c -> {
            c.keyExchange(XmCrypt.create(XmCrypt.Algorithm.RC4, PRE_SHARED, 42));
            c.writeBytes(new byte[] {(byte) 0x81, 0, 0, 0});
        }); XmSession s = XmSession.connect("127.0.0.1", srv.port(),
                XmSession.Options.defaults().withInitialCrypt(XmCrypt.create(XmCrypt.Algorithm.RC4, PRE_SHARED, 42)))) {
            assertEquals(XmError.BAD_LENGTH, recvError(s));
        }
    }

    // ------------------------------------------------------------------ compression

    @Test public void compressedBlockWithSeveralFramesIsUnpackedInOrder() throws Exception {
        byte[] block = Lz1Writer.literalBlock(frameBytes("RTr;n1;\n"), frameBytes("RTr;n2;\n"), frameBytes("HBt;\n"));
        try (FakeServer srv = new FakeServer(c -> c.sendRaw(new XmpFrame(Xmp.Type.COMPRESSED, block))); XmSession s = open(srv)) {
            assertEquals("RTr;n1;\n", body(s.recv()));
            assertEquals("RTr;n2;\n", body(s.recv()));
            assertEquals("HBt;\n", body(s.recv()));
        }
    }

    @Test public void compressionHistoryIsKeptBetweenBlocks() throws Exception {
        byte[] f = frameBytes("RTr;n1;\n");                                // 12 bytes
        byte[] first = Lz1Writer.literalBlock(f);
        byte[] second = new Lz1Writer().phrase(f.length, f.length).end().toBytes();   // "send that frame again"
        try (FakeServer srv = new FakeServer(c -> {
            c.sendRaw(new XmpFrame(Xmp.Type.COMPRESSED, first));
            c.sendRaw(new XmpFrame(Xmp.Type.COMPRESSED, second));
        }); XmSession s = open(srv)) {
            assertEquals("RTr;n1;\n", body(s.recv()));
            assertEquals("RTr;n1;\n", body(s.recv()));
        }
    }

    @Test public void compressedFrameInsideAnEncryptedFrame() throws Exception {
        byte[] block = Lz1Writer.literalBlock(frameBytes("RTr;n9;\n"), frameBytes("RTr;n10;\n"));
        try (FakeServer srv = new FakeServer(c -> {
            c.keyExchange(XmCrypt.create(XmCrypt.Algorithm.DES, PRE_SHARED, 42));
            c.sendFrame(new XmpFrame(Xmp.Type.COMPRESSED, block));
        }); XmSession s = XmSession.connect("127.0.0.1", srv.port(),
                XmSession.Options.defaults().withInitialCrypt(XmCrypt.create(XmCrypt.Algorithm.DES, PRE_SHARED, 42)))) {
            assertEquals("RTr;n9;\n", body(s.recv()));
            assertEquals("RTr;n10;\n", body(s.recv()));
        }
    }

    @Test public void plainFramesAndCompressedBlocksCanBeMixed() throws Exception {
        try (FakeServer srv = new FakeServer(c -> {
            c.send("plain1;\n");
            c.sendRaw(new XmpFrame(Xmp.Type.COMPRESSED, Lz1Writer.literalBlock(frameBytes("packed;\n"))));
            c.send("plain2;\n");
        }); XmSession s = open(srv)) {
            assertEquals("plain1;\n", body(s.recv()));
            assertEquals("packed;\n", body(s.recv()));
            assertEquals("plain2;\n", body(s.recv()));
        }
    }

    @Test public void emptyCompressedFrameIsRejected() throws Exception {
        try (FakeServer srv = new FakeServer(c -> c.writeBytes(new byte[] {(byte) 0x82, 0, 0, 0})); XmSession s = open(srv)) {
            assertEquals(XmError.BAD_LENGTH, recvError(s));
        }
    }

    @Test public void corruptCompressedFrameIsRejected() throws Exception {
        try (FakeServer srv = new FakeServer(c -> c.sendRaw(new XmpFrame(Xmp.Type.COMPRESSED, new Lz1Writer().command(8192, 7).toBytes())));
             XmSession s = open(srv)) {
            assertEquals(XmError.DC_BAD_OPCODE, recvError(s));
        }
    }

    @Test public void compressedBlockWhoseLastFrameIsCutOffIsRejected() throws Exception {
        byte[] whole = frameBytes("RTr;n1;Sy1010;\n");
        byte[] block = Lz1Writer.literalBlock(java.util.Arrays.copyOf(whole, whole.length - 4));
        try (FakeServer srv = new FakeServer(c -> c.sendRaw(new XmpFrame(Xmp.Type.COMPRESSED, block))); XmSession s = open(srv)) {
            assertEquals(XmError.DC_BAD_SIZE, recvError(s));
        }
    }

    @Test public void frameWhoseExtensionHeaderLiesIsRejected() throws Exception {
        // frame header says ext=200 bytes but the block ends: decode must complain, not read past the end
        byte[] liar = {5, (byte) 200, 0, 1, 1, 2, 3};
        byte[] block = Lz1Writer.literalBlock(liar);
        try (FakeServer srv = new FakeServer(c -> c.sendRaw(new XmpFrame(Xmp.Type.COMPRESSED, block))); XmSession s = open(srv)) {
            assertEquals(XmError.DC_BAD_SIZE, recvError(s));
        }
    }

    /** Local helper: a port with nothing listening. */
    static final class FakeServerPorts {
        static int dead() {
            try { return FakeServer.deadPort(); } catch (IOException e) { throw new RuntimeException(e); }
        }
    }
}
