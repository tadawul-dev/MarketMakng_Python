package tip.client;

import static org.junit.Assert.*;
import static tip.testutil.TestData.*;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Assume;
import org.junit.Test;
import tip.testutil.FakeServer;
import tip.testutil.Lz1Writer;
import tip.xm.ExtHeader;
import tip.xm.XmCrypt;
import tip.xm.XmError;
import tip.xm.XmException;
import tip.xm.XmService;
import tip.xm.Xmp;
import tip.xm.XmpFrame;

/** Every client scenario, against the fake server. */
public class TipClientTest {
    private static final byte[] PRE_SHARED = hex("0f1e2d3c4b5a6978a1b2c3d4e5f60718");

    private static TipClient.Builder client() {
        return TipClient.builder().connectTimeout(Duration.ofSeconds(3)).recvTimeout(Duration.ofSeconds(5));
    }

    private static XmError errorOf(IOException e) {
        assertTrue("expected XmException but got " + e, e instanceof XmException);
        return ((XmException) e).error();
    }

    // ------------------------------------------------------------------ logon

    @Test public void logonSendsCredentialsAndReturnsTheReply() throws Exception {
        try (FakeServer srv = new FakeServer(c -> {
            XmpFrame f = c.read();
            assertEquals(Xmp.Type.BINARY_RECORD, f.type());
            assertEquals("LGn;UIdalice;PWdsecret;\n", new String(f.body(), StandardCharsets.UTF_8));
            c.send("GEn;Rc0;Rxlogged on;\n");
        }); TipClient tip = client().build()) {
            tip.connect("127.0.0.1", srv.port());
            assertTrue(tip.isConnected());
            TipMessage reply = tip.logon("alice", "secret", null);
            assertEquals("GEn", reply.type());
            assertEquals("0", reply.get("Rc").get());
            assertEquals("logged on", reply.get("Rx").get());
        }
    }

    @Test public void logonWithProtocolVersion() throws Exception {
        try (FakeServer srv = new FakeServer(c -> {
            assertEquals("LGn;UIdu;PWdp;VEr1;\n", c.readText());
            c.send("GEn;Rc0;\n");
        }); TipClient tip = client().build()) {
            tip.connect("127.0.0.1", srv.port());
            tip.logon("u", "p", "1");
        }
    }

    @Test public void credentialsWithSpecialCharactersAreEscaped() throws Exception {
        try (FakeServer srv = new FakeServer(c -> {
            assertEquals("LGn;UIdu\\;1;PWdp\\\\w\\\nd;\n", c.readText());
            c.send("GEn;Rc0;\n");
        }); TipClient tip = client().build()) {
            tip.connect("127.0.0.1", srv.port());
            tip.logon("u;1", "p\\w\nd", null);
        }
    }

    @Test public void rejectedLogonIsReturnedNotThrown() throws Exception {
        try (FakeServer srv = new FakeServer(c -> { c.readText(); c.send("GEn;Rc12;Rxinvalid password;\n"); });
             TipClient tip = client().build()) {
            tip.connect("127.0.0.1", srv.port());
            TipMessage r = tip.logon("u", "wrong", null);
            assertEquals("12", r.get("Rc").get());
            assertEquals("invalid password", r.get("Rx").get());
            assertTrue("the client leaves the decision to the caller", tip.isConnected());
        }
    }

    @Test public void unparsableLogonReplyIsAParseError() throws Exception {
        try (FakeServer srv = new FakeServer(c -> { c.readText(); c.send("this is not tip\n"); });
             TipClient tip = client().build()) {
            tip.connect("127.0.0.1", srv.port());
            try {
                tip.logon("u", "p", null);
                fail();
            } catch (TipParseException expected) {
                // ok
            }
        }
    }

    @Test public void logonTimesOutWhenTheServerStaysSilent() throws Exception {
        try (FakeServer srv = new FakeServer(c -> c.awaitClientClose());
             TipClient tip = client().recvTimeout(Duration.ofMillis(300)).build()) {
            tip.connect("127.0.0.1", srv.port());
            try {
                tip.logon("u", "p", null);
                fail();
            } catch (IOException e) {
                assertEquals(XmError.TIMEOUT, errorOf(e));
            }
            assertFalse("a failed read closes the client", tip.isConnected());
        }
    }

    @Test public void logonArgumentsAreChecked() throws Exception {
        try (FakeServer srv = new FakeServer(c -> c.awaitClientClose()); TipClient tip = client().build()) {
            tip.connect("127.0.0.1", srv.port());
            for (String[] bad : new String[][] {{null, "p"}, {"u", null}}) {
                try {
                    tip.logon(bad[0], bad[1], null);
                    fail();
                } catch (IllegalArgumentException expected) {
                    // ok
                }
            }
            assertTrue(tip.isConnected());
        }
    }

    // ------------------------------------------------------------------ streaming requests

    private void expectRequest(String expectedText, ThrowingConsumer<TipClient> action) throws Exception {
        try (FakeServer srv = new FakeServer(c -> assertEquals(expectedText, c.readText())); TipClient tip = client().build()) {
            tip.connect("127.0.0.1", srv.port());
            action.accept(tip);
        }
    }

    @FunctionalInterface interface ThrowingConsumer<T> { void accept(T t) throws Exception; }

    @Test public void realtimeRequestWithOpenEnd() throws Exception {
        expectRequest("RTr;Sn5;QDAy20261031;QSCo3;\n", t -> t.requestRealtime(5, 0, 20261031, TipClient.Scope.ALL));
    }

    @Test public void realtimeRequestWithEndSequence() throws Exception {
        expectRequest("RTr;Sn5;En9;QDAy20261031;QSCo2;\n", t -> t.requestRealtime(5, 9, 20261031, TipClient.Scope.UPDATES));
    }

    @Test public void realtimeRequestWithSameStartAndEnd() throws Exception {
        expectRequest("RTr;Sn7;En7;QSCo1;\n", t -> t.requestRealtime(7, 7, 0, TipClient.Scope.NONE));
    }

    @Test public void dateZeroIsOmitted() throws Exception {
        expectRequest("RTr;Sn1;QSCo3;\n", t -> t.requestRealtime(1, 0, 0, TipClient.Scope.ALL));
    }

    @Test public void largeSequenceNumbers() throws Exception {
        expectRequest("RTr;Sn4000000000;En5000000000;QSCo3;\n", t -> t.requestRealtime(4_000_000_000L, 5_000_000_000L, 0, TipClient.Scope.ALL));
    }

    @Test public void basicDataRequest() throws Exception {
        expectRequest("BDa;QDAy20261031;QSCo3;\n", t -> t.requestBasicData(20261031, TipClient.Scope.ALL));
    }

    @Test public void basicDataRequestWithoutDate() throws Exception {
        expectRequest("BDa;QSCo2;\n", t -> t.requestBasicData(0, TipClient.Scope.UPDATES));
    }

    @Test public void basicDataIgnoresSequenceArguments() throws Exception {
        expectRequest("BDa;QSCo3;\n", t -> t.streamingRequest(TipClient.RequestType.BASIC_DATA, TipClient.Scope.ALL, 0, 0, 0));
    }

    @Test public void invalidRealtimeRequestsAreRefusedBeforeAnythingIsSent() throws Exception {
        try (FakeServer srv = new FakeServer(c -> assertEquals("HBt;\n", c.readText())); TipClient tip = client().build()) {
            tip.connect("127.0.0.1", srv.port());
            for (long[] bad : new long[][] {{0, 0}, {-1, 0}, {5, 4}}) {
                try {
                    tip.requestRealtime(bad[0], bad[1], 0, TipClient.Scope.ALL);
                    fail("start=" + bad[0] + " end=" + bad[1]);
                } catch (IllegalArgumentException expected) {
                    // ok
                }
            }
            // the first thing the server sees is the next valid message, so nothing leaked out
            tip.streamingRequest(TipClient.RequestType.BASIC_DATA, TipClient.Scope.ALL, 0, 0, 0);   // would be "BDa;..." -> mismatch
        } catch (AssertionError expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("fake server script failed"));
        }
    }

    @Test public void scopeAndRequestTypeCodes() {
        assertEquals(1, TipClient.Scope.NONE.code());
        assertEquals(2, TipClient.Scope.UPDATES.code());
        assertEquals(3, TipClient.Scope.ALL.code());
        assertEquals(TipClient.Scope.UPDATES, TipClient.Scope.of(2));
        assertEquals(1, TipClient.RequestType.REALTIME.code());
        assertEquals(2, TipClient.RequestType.BASIC_DATA.code());
        assertEquals(TipClient.RequestType.BASIC_DATA, TipClient.RequestType.of(2));
        for (int bad : new int[] {0, 4, -1}) {
            try { TipClient.Scope.of(bad); fail(); } catch (IllegalArgumentException expected) { /* ok */ }
        }
        for (int bad : new int[] {0, 3}) {
            try { TipClient.RequestType.of(bad); fail(); } catch (IllegalArgumentException expected) { /* ok */ }
        }
    }

    @Test public void currentDateIsYyyymmdd() {
        int d = TipClient.currentDate();
        int y = d / 10000, m = d / 100 % 100, day = d % 100;
        assertTrue("year " + y, y >= 2024 && y < 2200);
        assertTrue("month " + m, m >= 1 && m <= 12);
        assertTrue("day " + day, day >= 1 && day <= 31);
        java.time.LocalDate now = java.time.LocalDate.now();
        assertTrue(Math.abs(java.time.temporal.ChronoUnit.DAYS.between(now, java.time.LocalDate.of(y, m, day))) <= 1);
    }

    // ------------------------------------------------------------------ extension headers on requests

    @Test public void everyRequestCarriesCompressionAndQueueAlertHeaders() throws Exception {
        try (FakeServer srv = new FakeServer(c -> {
            for (int i = 0; i < 3; i++) {
                ExtHeader ext = c.read().ext();
                assertEquals(2, ext.entries().size());
                assertEquals("compression first, then queue alert (same order as the C client)", Xmp.Tag.COMPRESSION_ALGORITHM, ext.entries().get(0).tag());
                assertEquals(1, ext.entries().get(0).data()[0]);
                assertEquals(Xmp.Tag.QUEUE_ALERT, ext.entries().get(1).tag());
                assertEquals(250, ext.entries().get(1).intValue());
            }
            c.send("GEn;Rc0;\n");
        }); TipClient tip = client().compression(true).queueAlertMillis(250).build()) {
            tip.connect("127.0.0.1", srv.port());
            tip.requestBasicData(0, TipClient.Scope.ALL);
            tip.requestRealtime(1, 0, 0, TipClient.Scope.ALL);
            tip.logoff();
        }
    }

    @Test public void defaultsAreNoCompressionAndHalfASecondQueueAlert() throws Exception {
        try (FakeServer srv = new FakeServer(c -> {
            ExtHeader ext = c.read().ext();
            assertEquals(0, ext.find(Xmp.Tag.COMPRESSION_ALGORITHM).get().data()[0]);
            assertEquals(500, ext.find(Xmp.Tag.QUEUE_ALERT).get().intValue());
        }); TipClient tip = TipClient.builder().build()) {
            tip.connect("127.0.0.1", srv.port());
            tip.logoff();
        }
    }

    @Test public void logoffMessage() throws Exception {
        expectRequest("LGf;\n", TipClient::logoff);
    }

    // ------------------------------------------------------------------ reading

    @Test public void readsMessagesInOrderWithAllThreeAccessors() throws Exception {
        try (FakeServer srv = new FakeServer(c -> { c.send("RTr;n1;Sy1010;\n"); c.send("RTr;n2;\n"); c.send("RTr;n3;\n"); });
             TipClient tip = client().build()) {
            tip.connect("127.0.0.1", srv.port());
            assertEquals("RTr;n1;Sy1010;\n", tip.nextRaw());
            assertEquals("2", tip.next().get("n").get());
            assertArrayEquals("RTr;n3;\n".getBytes(StandardCharsets.UTF_8), tip.nextBody());
        }
    }

    @Test public void heartbeatsAndEndOfStreamArePassedThrough() throws Exception {
        try (FakeServer srv = new FakeServer(c -> { c.send("HBt;\n"); c.send("EOs;\n"); }); TipClient tip = client().build()) {
            tip.connect("127.0.0.1", srv.port());
            assertEquals("HBt", tip.next().type());
            assertEquals("EOs", tip.next().type());
        }
    }

    @Test public void extensionOnlyFramesAreConsumedAndReportedToTheListener() throws Exception {
        List<String> events = new CopyOnWriteArrayList<>();
        try (FakeServer srv = new FakeServer(c -> {
            c.send(Xmp.Type.NONE, ExtHeader.EMPTY.withByte(Xmp.Tag.SESSION_STATE, 2).withInt(Xmp.Tag.QUEUE_ALERT, 700), "");
            c.send(Xmp.Type.BINARY_RECORD, ExtHeader.EMPTY.withByte(Xmp.Tag.SESSION_STATE, 3), "RTr;n1;\n");
        }); TipClient tip = client().listener(new TipClient.ExtensionListener() {
            @Override public void onQueueAlert(int ms) { events.add("alert " + ms); }
            @Override public void onSessionState(Xmp.SessionState s, int raw) { events.add("state " + s + "/" + raw); }
        }).build()) {
            tip.connect("127.0.0.1", srv.port());
            assertEquals("the caller never sees the extension-only frame", "RTr;n1;\n", tip.nextRaw());
        }
        assertEquals(List.of("state CATCHUP/2", "alert 700", "state ONLINE/3"), events);
    }

    @Test public void timestampExtension() throws Exception {
        List<Instant> seconds = new ArrayList<>();
        List<Integer> fractions = new ArrayList<>();
        byte[] ts = {0x65, 0x53, (byte) 0xf1, 0x00, 0, 1, (byte) 0xe2, 0x40};          // 1700000000, 123456
        try (FakeServer srv = new FakeServer(c -> c.send(Xmp.Type.BINARY_RECORD, ExtHeader.EMPTY.with(Xmp.Tag.TIMESTAMP, ts), "RTr;n1;\n"));
             TipClient tip = client().listener(new TipClient.ExtensionListener() {
                 @Override public void onTimestamp(Instant s, int f) { seconds.add(s); fractions.add(f); }
             }).build()) {
            tip.connect("127.0.0.1", srv.port());
            tip.nextRaw();
        }
        assertEquals(List.of(Instant.ofEpochSecond(1_700_000_000L)), seconds);
        assertEquals(List.of(123456), fractions);
    }

    @Test public void unknownSessionStateCodeIsReportedAsUnknown() throws Exception {
        List<String> events = new CopyOnWriteArrayList<>();
        try (FakeServer srv = new FakeServer(c -> c.send(Xmp.Type.BINARY_RECORD, ExtHeader.EMPTY.withByte(Xmp.Tag.SESSION_STATE, 7), "x;\n"));
             TipClient tip = client().listener(new TipClient.ExtensionListener() {
                 @Override public void onSessionState(Xmp.SessionState s, int raw) { events.add(s + "/" + raw); }
             }).build()) {
            tip.connect("127.0.0.1", srv.port());
            tip.nextRaw();
        }
        assertEquals(List.of("UNKNOWN/7"), events);
    }

    @Test public void shortOrUnknownExtensionEntriesAreIgnoredSafely() throws Exception {
        List<String> events = new CopyOnWriteArrayList<>();
        ExtHeader ext = ExtHeader.EMPTY
                .with(Xmp.Tag.QUEUE_ALERT, new byte[] {1, 2})              // too short for an int
                .with(Xmp.Tag.TIMESTAMP, new byte[] {1, 2, 3, 4})          // needs 8 bytes
                .with(Xmp.Tag.SESSION_STATE, new byte[0])                  // needs 1
                .with(200, new byte[] {1});                                // unknown tag
        try (FakeServer srv = new FakeServer(c -> c.send(Xmp.Type.BINARY_RECORD, ext, "ok;\n"));
             TipClient tip = client().listener(new TipClient.ExtensionListener() {
                 @Override public void onQueueAlert(int ms) { events.add("alert"); }
                 @Override public void onSessionState(Xmp.SessionState s, int raw) { events.add("state"); }
                 @Override public void onTimestamp(Instant s, int f) { events.add("ts"); }
             }).build()) {
            tip.connect("127.0.0.1", srv.port());
            assertEquals("ok;\n", tip.nextRaw());
        }
        assertTrue(events.toString(), events.isEmpty());
    }

    @Test public void defaultListenerNeverThrows() throws Exception {
        try (FakeServer srv = new FakeServer(c -> {
            c.send(Xmp.Type.NONE, ExtHeader.EMPTY.withByte(Xmp.Tag.SESSION_STATE, 3).withInt(Xmp.Tag.QUEUE_ALERT, 1)
                    .with(Xmp.Tag.TIMESTAMP, new byte[8]), "");
            c.send("done;\n");
        }); TipClient tip = TipClient.builder().build()) {
            tip.connect("127.0.0.1", srv.port());
            assertEquals("done;\n", tip.nextRaw());
        }
    }

    @Test public void manyMessages() throws Exception {
        int n = 5000;
        try (FakeServer srv = new FakeServer(c -> { for (int i = 0; i < n; i++) c.send(i % 100 == 0 ? "HBt;\n" : "RTr;n" + i + ";\n"); });
             TipClient tip = client().build()) {
            tip.connect("127.0.0.1", srv.port());
            for (int i = 0; i < n; i++) assertEquals(i % 100 == 0 ? "HBt;\n" : "RTr;n" + i + ";\n", tip.nextRaw());
        }
    }

    // ------------------------------------------------------------------ compression and encryption end to end

    @Test public void compressedStream() throws Exception {
        try (FakeServer srv = new FakeServer(c -> {
            c.read();
            c.sendRaw(new XmpFrame(Xmp.Type.COMPRESSED, Lz1Writer.literalBlock(frameBytes("RTr;n1;\n"), frameBytes("RTr;n2;\n"))));
            c.sendRaw(new XmpFrame(Xmp.Type.COMPRESSED, Lz1Writer.literalBlock(frameBytes("RTr;n3;\n"))));
        }); TipClient tip = client().compression(true).build()) {
            tip.connect("127.0.0.1", srv.port());
            tip.requestRealtime(1, 0, 0, TipClient.Scope.ALL);
            for (int i = 1; i <= 3; i++) assertEquals("RTr;n" + i + ";\n", tip.nextRaw());
        }
    }

    private static void encryptedClient(XmCrypt.Algorithm alg) throws Exception {
        try (FakeServer srv = new FakeServer(c -> {
            c.keyExchange(XmCrypt.create(alg, PRE_SHARED, 42));
            assertEquals("LGn;UIdu;PWdp;\n", c.readText());
            c.send("GEn;Rc0;\n");
            c.read();
            c.send("RTr;n1;\n");
            c.send("RTr;n2;\n");
        }); TipClient tip = client().initialCrypt(XmCrypt.create(alg, PRE_SHARED, 42)).build()) {
            tip.connect("127.0.0.1", srv.port());
            assertEquals("0", tip.logon("u", "p", null).get("Rc").get());
            tip.requestRealtime(1, 0, 0, TipClient.Scope.ALL);
            assertEquals("RTr;n1;\n", tip.nextRaw());
            assertEquals("RTr;n2;\n", tip.nextRaw());
        }
    }

    @Test public void rc4EncryptedClient() throws Exception { encryptedClient(XmCrypt.Algorithm.RC4); }

    @Test public void desEncryptedClient() throws Exception { encryptedClient(XmCrypt.Algorithm.DES); }

    @Test public void failedKeyExchangeLeavesTheClientDisconnected() throws Exception {
        try (FakeServer srv = new FakeServer(c -> {
            c.keyExchange(XmCrypt.create(XmCrypt.Algorithm.RC4, PRE_SHARED, 42), FakeServer.KxFault.WRONG_KEY_ID_BODY);
            c.awaitClientClose();
        }); TipClient tip = client().initialCrypt(XmCrypt.create(XmCrypt.Algorithm.RC4, PRE_SHARED, 42)).build()) {
            try {
                tip.connect("127.0.0.1", srv.port());
                fail();
            } catch (IOException e) {
                assertEquals(XmError.INTERNAL_ERROR, errorOf(e));
            }
            assertFalse(tip.isConnected());
        }
    }

    // ------------------------------------------------------------------ character sets

    @Test public void arabicTextRoundTripsAsUtf8ByDefault() throws Exception {
        String name = "محمد";
        try (FakeServer srv = new FakeServer(c -> {
            assertEquals("LGn;UId" + name + ";PWdp;\n", c.readText());
            c.send("GEn;Rc0;Rx" + name + ";\n");
        }); TipClient tip = client().build()) {
            tip.connect("127.0.0.1", srv.port());
            assertEquals(name, tip.logon(name, "p", null).get("Rx").get());
        }
    }

    @Test public void otherCharsetsCanBeConfigured() throws Exception {
        Assume.assumeTrue(Charset.isSupported("windows-1256"));
        Charset cs = Charset.forName("windows-1256");
        String name = "محمد";
        try (FakeServer srv = new FakeServer(c -> {
            XmpFrame f = c.read();
            assertEquals("LGn;UId" + name + ";PWdp;\n", new String(f.body(), cs));
            c.sendBytes(Xmp.Type.BINARY_RECORD, ExtHeader.EMPTY, ("GEn;Rc0;Rx" + name + ";\n").getBytes(cs));
        }); TipClient tip = client().charset(cs).build()) {
            tip.connect("127.0.0.1", srv.port());
            assertEquals(name, tip.logon(name, "p", null).get("Rx").get());
        }
    }

    @Test public void nextBodyGivesRawBytesUntouchedByCharsets() throws Exception {
        byte[] raw = {'R', 'T', 'r', ';', 'n', '1', ';', (byte) 0xFF, (byte) 0xFE, ';', '\n'};
        try (FakeServer srv = new FakeServer(c -> c.sendBytes(Xmp.Type.BINARY_RECORD, ExtHeader.EMPTY, raw)); TipClient tip = client().build()) {
            tip.connect("127.0.0.1", srv.port());
            assertArrayEquals(raw, tip.nextBody());
        }
    }

    // ------------------------------------------------------------------ connection handling

    @Test public void usingTheClientBeforeConnectingIsAnError() {
        TipClient tip = client().build();
        assertFalse(tip.isConnected());
        for (ThrowingConsumer<TipClient> op : List.<ThrowingConsumer<TipClient>>of(
                t -> t.logon("u", "p", null), t -> t.requestRealtime(1, 0, 0, TipClient.Scope.ALL),
                t -> t.requestBasicData(0, TipClient.Scope.ALL), TipClient::logoff, TipClient::nextRaw, TipClient::next, TipClient::nextBody)) {
            try {
                op.accept(tip);
                fail();
            } catch (IllegalStateException expected) {
                assertTrue(expected.getMessage().contains("not connected"));
            } catch (Exception other) {
                fail("unexpected " + other);
            }
        }
    }

    @Test public void connectingTwiceIsAnError() throws Exception {
        try (FakeServer srv = new FakeServer(c -> c.awaitClientClose()); TipClient tip = client().build()) {
            tip.connect("127.0.0.1", srv.port());
            try {
                tip.connect("127.0.0.1", srv.port());
                fail();
            } catch (IllegalStateException expected) {
                assertTrue(expected.getMessage().contains("already connected"));
            }
        }
    }

    @Test public void failedConnectCanBeRetried() throws Exception {
        try (FakeServer srv = new FakeServer(c -> c.send("HBt;\n")); TipClient tip = client().connectTimeout(Duration.ofSeconds(2)).build()) {
            try {
                tip.connect("127.0.0.1", FakeServer.deadPort());
                fail();
            } catch (IOException e) {
                assertEquals(XmError.NETWORK_ERROR, errorOf(e));
            }
            assertFalse(tip.isConnected());
            tip.connect("127.0.0.1", srv.port());
            assertEquals("HBt", tip.next().type());
        }
    }

    @Test public void failOverBetweenAddresses() throws Exception {
        try (FakeServer live = new FakeServer(c -> c.send("HBt;\n")); TipClient tip = client().connectTimeout(Duration.ofSeconds(2)).build()) {
            tip.connect(List.of(new XmService.Address("127.0.0.1", FakeServer.deadPort()), new XmService.Address("127.0.0.1", live.port())));
            assertEquals("HBt", tip.next().type());
        }
    }

    @Test public void serverClosingTheConnectionEndsTheClient() throws Exception {
        try (FakeServer srv = new FakeServer(c -> { c.send("RTr;n1;\n"); c.hangUp(); }); TipClient tip = client().build()) {
            tip.connect("127.0.0.1", srv.port());
            assertEquals("RTr;n1;\n", tip.nextRaw());
            try {
                tip.nextRaw();
                fail();
            } catch (IOException e) {
                assertEquals(XmError.NETWORK_ERROR, errorOf(e));
            }
            assertFalse(tip.isConnected());
            try {
                tip.nextRaw();
                fail();
            } catch (IllegalStateException expected) {
                // ok: needs a new connect()
            }
        }
    }

    @Test public void writingToAClosedConnectionClosesTheClient() throws Exception {
        try (FakeServer srv = new FakeServer(c -> c.reset()); TipClient tip = client().build()) {
            tip.connect("127.0.0.1", srv.port());
            IOException failure = null;
            for (int i = 0; i < 200 && failure == null; i++) {
                try {
                    tip.requestBasicData(0, TipClient.Scope.ALL);
                    Thread.sleep(10);
                } catch (IOException e) {
                    failure = e;
                }
            }
            assertNotNull("sending to a reset connection must eventually fail", failure);
            assertEquals(XmError.NETWORK_ERROR, errorOf(failure));
            assertFalse(tip.isConnected());
        }
    }

    @Test public void reconnectAfterClose() throws Exception {
        try (FakeServer srv = new FakeServer(2, c -> c.send("conn" + c.index + ";\n")); TipClient tip = client().build()) {
            tip.connect("127.0.0.1", srv.port());
            assertEquals("conn0;\n", tip.nextRaw());
            tip.close();
            assertFalse(tip.isConnected());
            tip.connect("127.0.0.1", srv.port());
            assertEquals("conn1;\n", tip.nextRaw());
            assertEquals(2, srv.connectionCount());
        }
    }

    @Test public void closeIsIdempotentAndSafeBeforeConnect() throws Exception {
        TipClient never = client().build();
        never.close();
        never.close();
        try (FakeServer srv = new FakeServer(c -> c.awaitClientClose())) {
            TipClient tip = client().build();
            tip.connect("127.0.0.1", srv.port());
            tip.close();
            tip.close();
            assertFalse(tip.isConnected());
        }
    }

    @Test public void closingFromAnotherThreadUnblocksTheReader() throws Exception {
        try (FakeServer srv = new FakeServer(c -> c.awaitClientClose()); TipClient tip = client().recvTimeout(null).build()) {
            tip.connect("127.0.0.1", srv.port());
            AtomicReference<Object> outcome = new AtomicReference<>();
            Thread reader = new Thread(() -> {
                try { outcome.set(tip.nextRaw()); } catch (IOException e) { outcome.set(e); }
            });
            reader.start();
            Thread.sleep(200);
            tip.close();
            reader.join(3000);
            assertFalse("reader still blocked", reader.isAlive());
            assertTrue(String.valueOf(outcome.get()), outcome.get() instanceof XmException);
        }
    }

    @Test public void receiveTimeoutClosesTheClient() throws Exception {
        try (FakeServer srv = new FakeServer(c -> c.awaitClientClose()); TipClient tip = client().recvTimeout(Duration.ofMillis(250)).build()) {
            tip.connect("127.0.0.1", srv.port());
            try {
                tip.nextRaw();
                fail();
            } catch (IOException e) {
                assertEquals(XmError.TIMEOUT, errorOf(e));
            }
            assertFalse(tip.isConnected());
        }
    }

    @Test public void fullSessionLikeTheCSample() throws Exception {
        try (FakeServer srv = new FakeServer(c -> {
            assertEquals("LGn;UIdu;PWdp;VEr1;\n", c.readText());
            c.send("GEn;Rc0;Rxok;\n");
            assertEquals("RTr;Sn1;QDAy20261031;QSCo3;\n", c.readText());
            c.send(Xmp.Type.NONE, ExtHeader.EMPTY.withByte(Xmp.Tag.SESSION_STATE, 2), "");
            c.send("RTr;n1;\n");
            c.send("RTr;n2;\n");
            c.send(Xmp.Type.NONE, ExtHeader.EMPTY.withByte(Xmp.Tag.SESSION_STATE, 3), "");
            c.send("HBt;\n");
            assertEquals("LGf;\n", c.readText());
            c.send("GEn;Rc0;Rxbye;\n");
            c.hangUp();
        }); TipClient tip = client().compression(true).build()) {
            tip.connect("127.0.0.1", srv.port());
            assertEquals("0", tip.logon("u", "p", "1").get("Rc").get());
            tip.requestRealtime(1, 0, 20261031, TipClient.Scope.ALL);
            assertEquals("1", tip.next().get("n").get());
            assertEquals("2", tip.next().get("n").get());
            assertEquals("HBt", tip.next().type());
            tip.logoff();
            assertEquals("bye", tip.next().get("Rx").get());
            try {
                tip.next();
                fail();
            } catch (IOException e) {
                assertEquals(XmError.NETWORK_ERROR, errorOf(e));
            }
        }
    }
}
