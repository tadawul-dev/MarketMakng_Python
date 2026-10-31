package tip.client;

import static org.junit.Assert.*;
import static tip.testutil.TestData.*;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import org.apache.logging.log4j.Level;
import org.junit.Test;
import tip.testutil.FakeServer;
import tip.testutil.TestData.LogCapture;
import tip.testutil.Lz1Writer;
import tip.xm.XmCrypt;
import tip.xm.XmService;
import tip.xm.Xmp;
import tip.xm.XmpFrame;

/** What gets logged, at which level, and (just as important) what never does. */
public class LoggingTest {
    private static final byte[] PRE_SHARED = hex("0f1e2d3c4b5a6978a1b2c3d4e5f60718");
    private static final String PASSWORD = "S3cr3t-Pa55word";

    private static TipClient.Builder client() {
        return TipClient.builder().connectTimeout(Duration.ofSeconds(2)).recvTimeout(Duration.ofSeconds(3));
    }

    private static void session(FakeServer srv, TipClient tip) throws Exception {
        tip.connect("127.0.0.1", srv.port());
        tip.logon("alice", PASSWORD, null);
        tip.requestRealtime(1, 0, 20261031, TipClient.Scope.ALL);
        tip.nextRaw();
        tip.logoff();
    }

    private static FakeServer happyServer() throws IOException {
        return new FakeServer(c -> {
            c.readText(); c.send("GEn;Rc0;Rxok;\n");
            c.readText(); c.send("RTr;n1;\n");
            c.readText();
        });
    }

    // ------------------------------------------------------------------ lifecycle at INFO

    @Test public void lifecycleIsVisibleAtInfo() throws Exception {
        try (LogCapture log = new LogCapture(Level.INFO); FakeServer srv = happyServer(); TipClient tip = client().build()) {
            session(srv, tip);
            tip.close();
            assertTrue(log.text(), log.has(Level.INFO, "tip.client ", "connecting"));
            assertTrue(log.text(), log.has(Level.INFO, "tip.xm.session", "connected to"));
            assertTrue(log.text(), log.has(Level.INFO, "tip.client ", "logging on as alice"));
            assertTrue(log.text(), log.has(Level.INFO, "tip.client ", "logon reply"));
            assertTrue(log.text(), log.has(Level.INFO, "tip.client ", "streaming request"));
            assertTrue(log.text(), log.has(Level.INFO, "tip.client ", "logging off"));
            assertTrue(log.text(), log.has(Level.INFO, "tip.client ", "client closed"));
        }
    }

    @Test public void infoLevelHidesDebugAndTrace() throws Exception {
        try (LogCapture log = new LogCapture(Level.INFO); FakeServer srv = happyServer(); TipClient tip = client().build()) {
            session(srv, tip);
            for (LogCapture.Entry r : log.records) assertTrue(r.toString(), r.level().isMoreSpecificThan(Level.INFO));
        }
    }

    @Test public void happyPathProducesNoWarningsOrErrors() throws Exception {
        try (LogCapture log = new LogCapture(Level.ALL); FakeServer srv = happyServer(); TipClient tip = client().build()) {
            session(srv, tip);
            tip.close();
            for (LogCapture.Entry r : log.records) assertFalse(log.text(), r.level().isMoreSpecificThan(Level.WARN));
        }
    }

    // ------------------------------------------------------------------ level mapping

    @Test public void debugAndTraceAreSeparateLevels() throws Exception {
        try (LogCapture log = new LogCapture(Level.DEBUG); FakeServer srv = happyServer(); TipClient tip = client().build()) {
            session(srv, tip);
            assertTrue("DEBUG appears at DEBUG", log.records.stream().anyMatch(r -> r.level() == Level.DEBUG));
            assertTrue("TRACE does not", log.records.stream().noneMatch(r -> r.level() == Level.TRACE));
        }
        try (LogCapture log = new LogCapture(Level.TRACE); FakeServer srv = happyServer(); TipClient tip = client().build()) {
            session(srv, tip);
            assertTrue("TRACE appears at TRACE", log.records.stream().anyMatch(r -> r.level() == Level.TRACE));
        }
    }

    @Test public void wireRecordsAppearOnlyWhenTraceIsOn() throws Exception {
        try (LogCapture log = new LogCapture(Level.DEBUG); FakeServer srv = happyServer(); TipClient tip = client().build()) {
            session(srv, tip);
            assertTrue(log.text(), log.records.stream().noneMatch(r -> "tip.client.wire".equals(r.logger())));
        }
        try (LogCapture log = new LogCapture(Level.TRACE); FakeServer srv = happyServer(); TipClient tip = client().build()) {
            session(srv, tip);
            assertTrue(log.text(), log.has(Level.TRACE, "tip.client.wire", ">> RTr;Sn1;"));
            assertTrue(log.text(), log.has(Level.TRACE, "tip.client.wire", "<< RTr;n1;"));
            assertTrue(log.text(), log.has(Level.TRACE, "tip.xm.session", "send type=0x5"));
            assertTrue(log.text(), log.has(Level.TRACE, "tip.xm.session", "recv type=0x5"));
        }
    }

    // ------------------------------------------------------------------ secrets

    @Test public void passwordNeverAppearsAnywhere() throws Exception {
        try (LogCapture log = new LogCapture(Level.ALL); FakeServer srv = happyServer(); TipClient tip = client().build()) {
            session(srv, tip);
            tip.close();
            assertFalse(log.text(), log.text().contains(PASSWORD));
            assertTrue("the wire trace shows the logon with the password masked", log.has(Level.TRACE, "tip.client.wire", "PWd***"));
        }
    }

    @Test public void passwordIsNotLoggedWhenLogonIsRejectedEither() throws Exception {
        try (LogCapture log = new LogCapture(Level.ALL);
             FakeServer srv = new FakeServer(c -> { c.readText(); c.send("GEn;Rc12;Rxinvalid password;\n"); });
             TipClient tip = client().build()) {
            tip.connect("127.0.0.1", srv.port());
            tip.logon("alice", PASSWORD, null);
            assertFalse(log.text().contains(PASSWORD));
            assertTrue(log.text(), log.has(Level.INFO, "tip.client ", "Rc=12"));
        }
    }

    @Test public void keyMaterialNeverAppears() throws Exception {
        try (LogCapture log = new LogCapture(Level.ALL);
             FakeServer srv = new FakeServer(c -> {
                 c.keyExchange(XmCrypt.create(XmCrypt.Algorithm.RC4, PRE_SHARED, 42));
                 c.readText(); c.send("GEn;Rc0;\n");
                 c.readText();
             });
             TipClient tip = client().initialCrypt(XmCrypt.create(XmCrypt.Algorithm.RC4, PRE_SHARED, 42)).build()) {
            tip.connect("127.0.0.1", srv.port());
            tip.logon("alice", PASSWORD, null);
            tip.logoff();
            String text = log.text().toLowerCase();
            assertFalse("pre-shared key as hex", text.contains("0f1e2d3c"));
            assertFalse("pre-shared key as a byte list", text.contains("[15, 30, 45"));
            assertFalse("no long hex strings of any kind", text.matches("(?s).*[0-9a-f]{24}.*"));
            assertTrue(log.text(), log.has(Level.INFO, "tip.xm.session", "session encryption active (RC4)"));
            assertTrue(log.text(), log.has(Level.DEBUG, "tip.xm.keyexchange", "session key received"));
            assertTrue(log.text(), log.has(Level.DEBUG, "tip.xm.crypt", "created RC4 cipher (key id 42)"));
        }
    }

    // ------------------------------------------------------------------ problems are WARNING / ERROR

    @Test public void peerClosingIsAWarning() throws Exception {
        try (LogCapture log = new LogCapture(Level.INFO); FakeServer srv = new FakeServer(c -> c.hangUp()); TipClient tip = client().build()) {
            tip.connect("127.0.0.1", srv.port());
            try { tip.nextRaw(); fail(); } catch (IOException expected) { /* ok */ }
            assertTrue(log.text(), log.has(Level.WARN, "tip.xm.session", "closed by peer"));
            assertTrue(log.text(), log.has(Level.WARN, "tip.client ", "read failed, closing client"));
        }
    }

    @Test public void timeoutIsDebugNotWarning() throws Exception {
        try (LogCapture log = new LogCapture(Level.ALL); FakeServer srv = new FakeServer(c -> c.awaitClientClose());
             TipClient tip = client().recvTimeout(Duration.ofMillis(200)).build()) {
            tip.connect("127.0.0.1", srv.port());
            try { tip.nextRaw(); fail(); } catch (IOException expected) { /* ok */ }
            assertTrue(log.text(), log.has(Level.DEBUG, "tip.xm.session", "receive timed out"));
        }
    }

    @Test public void failoverLogsAWarningPerFailedAddress() throws Exception {
        try (LogCapture log = new LogCapture(Level.INFO); FakeServer srv = new FakeServer(c -> c.awaitClientClose()); TipClient tip = client().build()) {
            tip.connect(List.of(new XmService.Address("127.0.0.1", FakeServer.deadPort()), new XmService.Address("127.0.0.1", srv.port())));
            assertTrue(log.text(), log.has(Level.WARN, "tip.xm.session", "connect to 127.0.0.1"));
            assertTrue(log.text(), log.has(Level.INFO, "tip.xm.session", "connected to"));
        }
    }

    @Test public void allAddressesDownIsAnError() throws Exception {
        try (LogCapture log = new LogCapture(Level.INFO); TipClient tip = client().build()) {
            try { tip.connect("127.0.0.1", FakeServer.deadPort()); fail(); } catch (IOException expected) { /* ok */ }
            assertTrue(log.text(), log.has(Level.ERROR, "tip.xm.session", "could not connect to any address"));
        }
    }

    @Test public void corruptCompressedBlockIsAWarning() throws Exception {
        try (LogCapture log = new LogCapture(Level.INFO);
             FakeServer srv = new FakeServer(c -> {
                 c.send("HBt;\n");
                 c.sendRaw(new XmpFrame(Xmp.Type.COMPRESSED, new byte[] {(byte) 0xff, (byte) 0xff, 1, 2, 3}));
             });
             TipClient tip = client().compression(true).build()) {
            tip.connect("127.0.0.1", srv.port());
            tip.nextRaw();
            try { tip.nextRaw(); fail(); } catch (IOException expected) { /* ok */ }
            assertTrue(log.text(), log.has(Level.WARN, "tip.xm.lz1", "corrupt compressed block"));
        }
    }

    @Test public void weakDesKeyIsAWarning() throws Exception {
        try (LogCapture log = new LogCapture(Level.INFO)) {
            try {
                XmCrypt.create(XmCrypt.Algorithm.DES, hex("0101010101010101"), 1);
                fail();
            } catch (IOException expected) {
                // ok
            }
            assertTrue(log.text(), log.has(Level.WARN, "tip.xm.crypt", "cannot create DES cipher"));
        }
    }

    @Test public void rejectedKeyExchangeIsLoggedAtWarningAndError() throws Exception {
        try (LogCapture log = new LogCapture(Level.INFO);
             FakeServer srv = new FakeServer(c -> {
                 c.keyExchange(XmCrypt.create(XmCrypt.Algorithm.RC4, PRE_SHARED, 42), FakeServer.KxFault.WRONG_KEY_ID_BODY);
                 c.awaitClientClose();
             });
             TipClient tip = client().initialCrypt(XmCrypt.create(XmCrypt.Algorithm.RC4, PRE_SHARED, 42)).build()) {
            try { tip.connect("127.0.0.1", srv.port()); fail(); } catch (IOException expected) { /* ok */ }
            assertTrue(log.text(), log.has(Level.WARN, "tip.xm.keyexchange", "key exchange rejected"));
            assertTrue(log.text(), log.has(Level.ERROR, "tip.xm.session", "key exchange with"));
        }
    }

    @Test public void queueAlertAndSessionStateFromTheServerAreLogged() throws Exception {
        try (LogCapture log = new LogCapture(Level.INFO);
             FakeServer srv = new FakeServer(c -> {
                 c.send(Xmp.Type.NONE, tip.xm.ExtHeader.EMPTY.withByte(Xmp.Tag.SESSION_STATE, 3).withInt(Xmp.Tag.QUEUE_ALERT, 800), "");
                 c.send("HBt;\n");
             });
             TipClient tip = client().build()) {
            tip.connect("127.0.0.1", srv.port());
            tip.nextRaw();
            assertTrue(log.text(), log.has(Level.WARN, "tip.client ", "queue alert: 800 ms"));
            assertTrue(log.text(), log.has(Level.INFO, "tip.client ", "session state: ONLINE"));
        }
    }

    // ------------------------------------------------------------------ message hygiene

    @Test public void noMessageHasUnreplacedPlaceholdersOrBrokenFormatting() throws Exception {
        try (LogCapture log = new LogCapture(Level.ALL);
             FakeServer srv = new FakeServer(c -> {
                 c.readText(); c.send("GEn;Rc0;\n");
                 c.read();
                 c.sendRaw(new XmpFrame(Xmp.Type.COMPRESSED, Lz1Writer.literalBlock(frameBytes("RTr;n1;\n"))));
                 c.hangUp();
             });
             TipClient tip = client().compression(true).build()) {
            tip.connect(List.of(new XmService.Address("127.0.0.1", FakeServer.deadPort()), new XmService.Address("127.0.0.1", srv.port())));
            tip.logon("alice", PASSWORD, "1");
            tip.requestRealtime(1, 0, 0, TipClient.Scope.ALL);
            tip.nextRaw();
            try { tip.nextRaw(); } catch (IOException expected) { /* ok */ }
            tip.close();
            assertFalse(log.records.isEmpty());
            for (String line : log.text().split("\n")) {
                assertFalse("unreplaced placeholder: " + line, line.contains("{}"));
                assertFalse("null in message: " + line, line.contains(" null"));
            }
            // formatting problems would surface as exceptions from text(); reaching here proves there were none
        }
    }

    @Test public void loggerNamesStayUnderTheTipHierarchy() throws Exception {
        try (LogCapture log = new LogCapture(Level.ALL); FakeServer srv = happyServer(); TipClient tip = client().build()) {
            session(srv, tip);
            for (LogCapture.Entry r : log.records) assertTrue(r.logger(), r.logger().equals("tip") || r.logger().startsWith("tip."));
        }
    }
}
