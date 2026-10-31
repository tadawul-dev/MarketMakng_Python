package tip.client;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.Test;
import tip.testutil.FakeServer;
import tip.xm.ExtHeader;
import tip.xm.Xmp;

public class TipClientMainTest {
    private static String[] args(String s) { return s.trim().split("\\s+"); }

    private static void rejects(String cmd, String messagePart) {
        try {
            TipClientMain.parse(args(cmd));
            fail("should reject: " + cmd);
        } catch (IllegalArgumentException e) {
            assertTrue("'" + e.getMessage() + "' should mention '" + messagePart + "'", e.getMessage().contains(messagePart));
        }
    }

    // ------------------------------------------------------------------ parse

    @Test public void minimalCommandLineGetsTheDefaultsOfTheCSample() {
        TipClientMain.Options o = TipClientMain.parse(args("-a host:4000 -u me -p pw -R"));
        assertEquals(1, o.addresses().size());
        assertEquals("host", o.addresses().get(0).host());
        assertEquals(4000, o.addresses().get(0).port());
        assertEquals("me", o.uid());
        assertEquals("pw", o.pwd());
        assertEquals(TipClient.RequestType.REALTIME, o.request());
        assertEquals(3, o.scope());
        assertEquals(1, o.from());
        assertEquals(0, o.to());
        assertEquals(500, o.queueAlert());
        assertEquals(0, o.recvSecs());
        assertEquals(0, o.verbosity());
        assertFalse(o.compression());
        assertNull(o.version());
        assertEquals(TipClient.currentDate(), o.date());
    }

    @Test public void everyOption() {
        TipClientMain.Options o = TipClientMain.parse(args(
                "-a h1:1 -a h2:2 -u u -p p -B -s 2 -d 20261031 -x 7 -b 10 -e 20 -z -q 900 -t 30 -vv"));
        assertEquals(2, o.addresses().size());
        assertEquals("h2", o.addresses().get(1).host());
        assertEquals(TipClient.RequestType.BASIC_DATA, o.request());
        assertEquals(2, o.scope());
        assertEquals(20261031, o.date());
        assertEquals("7", o.version());
        assertEquals(10, o.from());
        assertEquals(20, o.to());
        assertTrue(o.compression());
        assertEquals(900, o.queueAlert());
        assertEquals(30, o.recvSecs());
        assertEquals(2, o.verbosity());
    }

    @Test public void verbosityLevels() {
        assertEquals(1, TipClientMain.parse(args("-a h:1 -u u -p p -R -v")).verbosity());
        assertEquals(2, TipClientMain.parse(args("-a h:1 -u u -p p -R -vv")).verbosity());
        assertEquals(3, TipClientMain.parse(args("-a h:1 -u u -p p -R -vvv")).verbosity());
        assertEquals(3, TipClientMain.parse(args("-a h:1 -u u -p p -R -vvv -v")).verbosity());
    }

    @Test public void lastRequestTypeWins() {
        assertEquals(TipClient.RequestType.BASIC_DATA, TipClientMain.parse(args("-a h:1 -u u -p p -R -B")).request());
    }

    @Test public void hostWithColonsUsesTheLastColonAsPortSeparator() {
        assertEquals("::1", TipClientMain.parse(args("-a ::1:4000 -u u -p p -R")).addresses().get(0).host());
    }

    @Test public void valuesMayLookLikeOptions() {
        assertEquals("-R", TipClientMain.parse(new String[] {"-a", "h:1", "-u", "u", "-p", "-R", "-R"}).pwd());
    }

    @Test public void missingRequiredOptions() {
        rejects("-u u -p p -R", "-a");
        rejects("-a h:1 -p p -R", "-u");
        rejects("-a h:1 -u u -R", "-p");
        rejects("-a h:1 -u u -p p", "-R or -B");
        try {
            TipClientMain.parse(new String[0]);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("-a"));
        }
    }

    @Test public void badValues() {
        rejects("-a hostonly -u u -p p -R", "host:port");
        rejects("-a :4000 -u u -p p -R", "host:port");
        rejects("-a h:0 -u u -p p -R", "port");
        rejects("-a h:70000 -u u -p p -R", "port");
        rejects("-a h:abc -u u -p p -R", "number");
        rejects("-a h:1 -u u -p p -R -s 4", "scope");
        rejects("-a h:1 -u u -p p -R -s 0", "scope");
        rejects("-a h:1 -u u -p p -R -s x", "number");
        rejects("-a h:1 -u u -p p -R -b 0", "-b");
        rejects("-a h:1 -u u -p p -R -b 5 -e 4", "-e");
        rejects("-a h:1 -u u -p p -R -q x", "number");
    }

    @Test public void unknownAndIncompleteOptions() {
        rejects("-a h:1 -u u -p p -R -w", "unknown option -w");
        rejects("-a h:1 -u u -p", "needs a value");
        rejects("-a", "needs a value");
        rejects("-a h:1 -u u -p p -R -t", "needs a value");
    }

    @Test public void usageTextDocumentsEveryOption() {
        for (String o : new String[] {"-a", "-u", "-p", "-R", "-B", "-s", "-d", "-x", "-b", "-e", "-z", "-q", "-t", "-v"}) {
            assertTrue(o, TipClientMain.USAGE.contains(o + " ") || TipClientMain.USAGE.contains(o + "\n") || TipClientMain.USAGE.contains(o + "]"));
        }
    }

    // ------------------------------------------------------------------ run

    private static int run(String cmd, ByteArrayOutputStream captured) {
        return TipClientMain.run(TipClientMain.parse(args(cmd)), new PrintStream(captured, true, StandardCharsets.UTF_8));
    }

    @Test public void runsUntilEndOfStream() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int code;
        try (FakeServer srv = new FakeServer(c -> {
            assertEquals("LGn;UIdme;PWdhunter2;\n", c.readText());
            c.send("GEn;Rc0;Rxok;\n");
            assertEquals("RTr;Sn5;En50;QDAy20261031;QSCo3;\n", c.readText());
            c.send(Xmp.Type.NONE, ExtHeader.EMPTY.withByte(Xmp.Tag.SESSION_STATE, 3), "");
            c.send("RTr;n5;\n");
            c.send("RTr;n6;\n");
            c.send("EOs;\n");
            assertEquals("LGf;\n", c.readText());
        })) {
            code = run("-a 127.0.0.1:" + srv.port() + " -u me -p hunter2 -R -b 5 -e 50 -d 20261031", out);
        }
        assertEquals(0, code);
        String text = out.toString(StandardCharsets.UTF_8);
        assertEquals("logon reply: GEn;Rc0;Rxok;\nRTr;n5;\nRTr;n6;\nEOs;\n", text);
        assertFalse("the password must never be printed", text.contains("hunter2"));
    }

    @Test public void basicDataRunWithCompressionFlag() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int code;
        try (FakeServer srv = new FakeServer(c -> {
            c.readText();
            c.send("GEn;Rc0;\n");
            XmpFrameCheck.expectCompression(c.read(), 1);
            c.send("BDa;Sy1;\n");
            c.send("EOs;\n");
            c.readText();
        })) {
            code = run("-a 127.0.0.1:" + srv.port() + " -u u -p p -B -d 20261031 -z", out);
        }
        assertEquals(0, code);
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("BDa;Sy1;\n"));
    }

    @Test public void exitCodeIsOneWhenTheConnectionDropsWithoutEndOfStream() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int code;
        try (FakeServer srv = new FakeServer(c -> {
            c.readText();
            c.send("GEn;Rc0;\n");
            c.readText();
            c.send("RTr;n1;\n");
            c.hangUp();
        })) {
            code = run("-a 127.0.0.1:" + srv.port() + " -u u -p p -R", out);
        }
        assertEquals(1, code);
        assertTrue(out.toString(StandardCharsets.UTF_8).endsWith("RTr;n1;\n"));
    }

    @Test public void exitCodeIsOneWhenNothingListens() throws Exception {
        assertEquals(1, run("-a 127.0.0.1:" + FakeServer.deadPort() + " -u u -p p -R", new ByteArrayOutputStream()));
    }

    @Test public void exitCodeIsOneOnReceiveTimeout() throws Exception {
        int code;
        try (FakeServer srv = new FakeServer(c -> c.awaitClientClose())) {
            long t0 = System.nanoTime();
            code = run("-a 127.0.0.1:" + srv.port() + " -u u -p p -R -t 1", new ByteArrayOutputStream());
            assertTrue("should give up after about a second", (System.nanoTime() - t0) / 1_000_000 < 5000);
        }
        assertEquals(1, code);
    }

    @Test public void failsOverToTheSecondAddress() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int code;
        try (FakeServer srv = new FakeServer(c -> { c.readText(); c.send("GEn;Rc0;\n"); c.readText(); c.send("EOs;\n"); c.readText(); })) {
            code = run("-a 127.0.0.1:" + FakeServer.deadPort() + " -a 127.0.0.1:" + srv.port() + " -u u -p p -R", out);
        }
        assertEquals(0, code);
    }

    // ------------------------------------------------------------------ logging setup

    @Test public void verbosityMapsToLog4jLevels() {
        assertEquals(Level.WARN, TipClientMain.levelFor(0));
        assertEquals(Level.INFO, TipClientMain.levelFor(1));
        assertEquals(Level.DEBUG, TipClientMain.levelFor(2));
        assertEquals(Level.TRACE, TipClientMain.levelFor(3));
        assertEquals(Level.TRACE, TipClientMain.levelFor(9));
    }

    @Test public void configureLoggingSetsTheLevelOfTheTipLoggers() {
        Level before = LogManager.getLogger("tip").getLevel();
        try {
            TipClientMain.configureLogging(1);
            assertEquals(Level.INFO, LogManager.getLogger("tip").getLevel());
            TipClientMain.configureLogging(2);
            assertEquals(Level.DEBUG, LogManager.getLogger("tip").getLevel());
            assertTrue(LogManager.getLogger("tip.xm.session").isDebugEnabled());
            assertFalse(LogManager.getLogger("tip.xm.session").isTraceEnabled());
            TipClientMain.configureLogging(3);
            assertTrue(LogManager.getLogger("tip.client.wire").isTraceEnabled());
            TipClientMain.configureLogging(0);
            assertFalse(LogManager.getLogger("tip.client").isInfoEnabled());
        } finally {
            Configurator.setLevel("tip", before);
        }
    }

    @Test public void packagedDefaultConfigurationExists() throws Exception {
        try (var in = TipClientMain.class.getClassLoader().getResourceAsStream(TipClientMain.DEFAULT_LOG_CONFIG)) {
            assertNotNull("the jar must contain " + TipClientMain.DEFAULT_LOG_CONFIG, in);
            String xml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(xml.contains("SYSTEM_ERR"));
            assertTrue(xml.contains("name=\"tip\""));
        }
    }

    /** Tiny helper kept next to its only user. */
    static final class XmpFrameCheck {
        static void expectCompression(tip.xm.XmpFrame f, int algorithm) {
            assertEquals(algorithm, f.ext().find(Xmp.Tag.COMPRESSION_ALGORITHM).get().data()[0]);
        }
    }
}
