package tip.client;

import static org.junit.Assert.*;
import static tip.testutil.TestData.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.Test;
import tip.client.TipConnectCheck.Status;
import tip.testutil.FakeServer;
import tip.xm.XmCrypt;
import tip.xm.XmService;

public class TipConnectCheckTest {
    private static final byte[] PRE_SHARED = hex("0f1e2d3c4b5a6978a1b2c3d4e5f60718");
    private static final String PWD = "S3cr3t-Pa55word";

    private static String[] args(String s) { return s.trim().split("\\s+"); }

    private static TipConnectCheck.Options opts(String cmd) { return TipConnectCheck.parse(args(cmd)); }

    private static void rejects(String cmd, String part) {
        try {
            TipConnectCheck.parse(args(cmd));
            fail("should reject: " + cmd);
        } catch (IllegalArgumentException e) {
            assertTrue("'" + e.getMessage() + "' should mention '" + part + "'", e.getMessage().contains(part));
        }
    }

    private static XmService.Address addr(int port) { return new XmService.Address("127.0.0.1", port); }

    private static String report(TipConnectCheck.Options o, int[] exit) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        exit[0] = TipConnectCheck.run(o, new PrintStream(bytes, true, StandardCharsets.UTF_8));
        return bytes.toString(StandardCharsets.UTF_8);
    }

    /**
     * The check opens a plain TCP connection first (the "tcp" step) and only then the real session, so the server sees
     * two connections: the first is just waited out, the second runs {@code script}.
     */
    private static FakeServer probed(FakeServer.Script script) throws Exception {
        return new FakeServer(2, c -> {
            if (c.index == 0) c.awaitClientClose();
            else script.run(c);
        });
    }

    /** Server that accepts a logon and an orderly logoff. */
    private static FakeServer goodServer() throws Exception {
        return probed(c -> {
            assertEquals("LGn;UIdalice;PWd" + PWD + ";\n", c.readText());
            c.send("GEn;Rc0;Rxwelcome;\n");
            assertEquals("LGf;\n", c.readText());
            c.send("GEn;Rc0;Rxbye;\n");
        });
    }

    // ------------------------------------------------------------------ parse

    @Test public void defaults() {
        TipConnectCheck.Options o = opts("-a host:4000");
        assertEquals(List.of(new XmService.Address("host", 4000)), o.addresses());
        assertNull(o.uid());
        assertNull(o.pwd());
        assertNull(o.crypt());
        assertFalse(o.compression());
        assertEquals(5, o.connectSecs());
        assertEquals(10, o.replySecs());
        assertEquals(0, o.waitSecs());
        assertEquals(0, o.verbosity());
    }

    @Test public void allOptions() {
        TipConnectCheck.Options o = opts("-a h1:1 -a h2:2 -u me -p pw -x 7 -z -E des -K 0102030405060708 -I 9 -T 3 -t 4 -w 6 -vv");
        assertEquals(2, o.addresses().size());
        assertEquals("me", o.uid());
        assertEquals("pw", o.pwd());
        assertEquals("7", o.version());
        assertTrue(o.compression());
        assertEquals(XmCrypt.Algorithm.DES, o.crypt());
        assertArrayEquals(hex("0102030405060708"), o.key());
        assertEquals(9, o.keyId());
        assertEquals(3, o.connectSecs());
        assertEquals(4, o.replySecs());
        assertEquals(6, o.waitSecs());
        assertEquals(2, o.verbosity());
    }

    @Test public void badCommandLines() {
        try {
            TipConnectCheck.parse(new String[0]);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("-a"));
        }
        rejects("-a hostonly", "host:port");
        rejects("-a h:99999", "port");
        rejects("-a h:x", "number");
        rejects("-a h:1 -u me", "together");
        rejects("-a h:1 -p pw", "together");
        rejects("-a h:1 -E RC4", "together");
        rejects("-a h:1 -K 00", "together");
        rejects("-a h:1 -E AES -K 00", "RC4 or DES");
        rejects("-a h:1 -E RC4 -K xyz", "hex");
        rejects("-a h:1 -E RC4 -K abc", "hex");
        rejects("-a h:1 -T 0", "-T");
        rejects("-a h:1 -t 0", "-t");
        rejects("-a h:1 -w -1", "-w");
        rejects("-a h:1 -w 5", "-w needs -u");
        rejects("-a h:1 -q", "unknown option");
        rejects("-a h:1 -u", "needs a value");
    }

    @Test public void optionsToStringDoesNotShowThePassword() {
        assertFalse(opts("-a h:1 -u me -p " + PWD).toString().contains(PWD));
    }

    @Test public void usageDocumentsEveryOption() {
        for (String o : new String[] {"-a", "-u", "-p", "-x", "-z", "-E", "-K", "-I", "-T", "-t", "-w", "-v"})
            assertTrue(o, TipConnectCheck.USAGE.contains(o + " ") || TipConnectCheck.USAGE.contains(o + "\n"));
        assertTrue(TipConnectCheck.USAGE.contains("exit code"));
    }

    // ------------------------------------------------------------------ transport only

    @Test public void transportOnlyCheckPasses() throws Exception {
        try (FakeServer srv = probed(c -> c.awaitClientClose())) {
            TipConnectCheck.Result r = TipConnectCheck.check(addr(srv.port()), opts("-a 127.0.0.1:" + srv.port()));
            assertTrue(r.ok());
            assertEquals(List.of("dns", "tcp", "session"), r.stepNames());
            for (TipConnectCheck.Step s : r.steps()) assertEquals(s.name(), Status.OK, s.status());
            assertTrue(r.step("dns").get().detail().contains("127.0.0.1"));
            assertTrue(r.step("session").get().detail().contains("encryption=off"));
        }
    }

    @Test public void nothingListeningFailsAtTcpAndStopsThere() throws Exception {
        TipConnectCheck.Result r = TipConnectCheck.check(addr(FakeServer.deadPort()), opts("-a 127.0.0.1:1 -T 2"));
        assertFalse(r.ok());
        assertEquals(List.of("dns", "tcp"), r.stepNames());
        assertEquals(Status.FAIL, r.step("tcp").get().status());
        assertTrue(r.step("tcp").get().detail(), r.step("tcp").get().detail().toLowerCase().contains("refused"));
    }

    @Test public void unknownHostFailsAtDns() {
        TipConnectCheck.Result r = TipConnectCheck.check(new XmService.Address("no-such-host.invalid", 4000), opts("-a x:1"));
        assertFalse(r.ok());
        assertEquals(List.of("dns"), r.stepNames());
        assertTrue(r.step("dns").get().detail().contains("no-such-host.invalid"));
    }

    // ------------------------------------------------------------------ logon

    @Test public void fullCheckWithLogonAndLogoff() throws Exception {
        try (FakeServer srv = goodServer()) {
            TipConnectCheck.Result r = TipConnectCheck.check(addr(srv.port()), opts("-a 127.0.0.1:1 -u alice -p " + PWD));
            assertTrue(r.steps().toString(), r.ok());
            assertEquals(List.of("dns", "tcp", "session", "logon", "logoff"), r.stepNames());
            assertTrue(r.step("logon").get().detail(), r.step("logon").get().detail().contains("Rc=0 Rx=welcome"));
            assertTrue(r.step("logoff").get().detail(), r.step("logoff").get().detail().contains("reply GEn"));
        }
    }

    @Test public void refusedLogonFailsWithTheServerReason() throws Exception {
        try (FakeServer srv = probed(c -> { c.readText(); c.send("GEn;Rc12;Rxinvalid password;\n"); c.awaitClientClose(); })) {
            TipConnectCheck.Result r = TipConnectCheck.check(addr(srv.port()), opts("-a 127.0.0.1:1 -u alice -p wrong"));
            assertFalse(r.ok());
            assertEquals(List.of("dns", "tcp", "session", "logon"), r.stepNames());
            String d = r.step("logon").get().detail();
            assertTrue(d, d.contains("Rc=12") && d.contains("invalid password"));
        }
    }

    @Test public void serverThatHangsUpDuringLogonFails() throws Exception {
        try (FakeServer srv = probed(c -> { c.readText(); c.hangUp(); })) {
            TipConnectCheck.Result r = TipConnectCheck.check(addr(srv.port()), opts("-a 127.0.0.1:1 -u alice -p pw"));
            assertFalse(r.ok());
            assertEquals(Status.FAIL, r.step("logon").get().status());
        }
    }

    @Test public void silentServerTimesOutAtLogon() throws Exception {
        try (FakeServer srv = probed(c -> { c.readText(); c.awaitClientClose(); })) {
            long t0 = System.nanoTime();
            TipConnectCheck.Result r = TipConnectCheck.check(addr(srv.port()), opts("-a 127.0.0.1:1 -u alice -p pw -t 1"));
            long ms = (System.nanoTime() - t0) / 1_000_000;
            assertFalse(r.ok());
            assertEquals(Status.FAIL, r.step("logon").get().status());
            assertTrue("took " + ms, ms >= 900 && ms < 5000);
            assertTrue(r.step("logon").get().millis() >= 900);
        }
    }

    @Test public void garbageLogonReplyFails() throws Exception {
        try (FakeServer srv = probed(c -> { c.readText(); c.send("not tip\n"); c.awaitClientClose(); })) {
            TipConnectCheck.Result r = TipConnectCheck.check(addr(srv.port()), opts("-a 127.0.0.1:1 -u alice -p pw"));
            assertFalse(r.ok());
            assertEquals(Status.FAIL, r.step("logon").get().status());
        }
    }

    @Test public void logoffWithoutReplyIsStillOk() throws Exception {
        try (FakeServer srv = probed(c -> { c.readText(); c.send("GEn;Rc0;\n"); c.readText(); c.hangUp(); })) {
            TipConnectCheck.Result r = TipConnectCheck.check(addr(srv.port()), opts("-a 127.0.0.1:1 -u alice -p pw"));
            assertTrue(r.steps().toString(), r.ok());
            assertEquals(Status.OK, r.step("logoff").get().status());
            assertEquals("sent", r.step("logoff").get().detail());
        }
    }

    // ------------------------------------------------------------------ first message

    @Test public void firstMessageIsReported() throws Exception {
        try (FakeServer srv = probed(c -> { c.readText(); c.send("GEn;Rc0;\n"); c.send("HBt;\n"); c.readText(); c.hangUp(); })) {
            TipConnectCheck.Result r = TipConnectCheck.check(addr(srv.port()), opts("-a 127.0.0.1:1 -u alice -p pw -w 3"));
            assertTrue(r.steps().toString(), r.ok());
            assertEquals(List.of("dns", "tcp", "session", "logon", "first-message", "logoff"), r.stepNames());
            assertEquals("received HBt", r.step("first-message").get().detail());
        }
    }

    @Test public void quietServerIsOnlyAWarning() throws Exception {
        try (FakeServer srv = probed(c -> { c.readText(); c.send("GEn;Rc0;\n"); c.awaitClientClose(); })) {
            TipConnectCheck.Result r = TipConnectCheck.check(addr(srv.port()), opts("-a 127.0.0.1:1 -u alice -p pw -w 1"));
            assertTrue("a warning does not fail the check", r.ok());
            assertEquals(Status.WARN, r.step("first-message").get().status());
            assertEquals(List.of("dns", "tcp", "session", "logon", "first-message"), r.stepNames());
        }
    }

    @Test public void connectionLostWhileWaitingFails() throws Exception {
        try (FakeServer srv = probed(c -> { c.readText(); c.send("GEn;Rc0;\n"); c.hangUp(); })) {
            TipConnectCheck.Result r = TipConnectCheck.check(addr(srv.port()), opts("-a 127.0.0.1:1 -u alice -p pw -w 3"));
            assertFalse(r.ok());
            assertEquals(Status.FAIL, r.step("first-message").get().status());
        }
    }

    // ------------------------------------------------------------------ encryption

    @Test public void encryptedSessionCheck() throws Exception {
        try (FakeServer srv = probed(c -> {
            c.keyExchange(XmCrypt.create(XmCrypt.Algorithm.RC4, PRE_SHARED, 42));
            c.readText(); c.send("GEn;Rc0;\n");
            c.readText(); c.send("GEn;Rc0;\n");
        })) {
            TipConnectCheck.Result r = TipConnectCheck.check(addr(srv.port()),
                    opts("-a 127.0.0.1:1 -u alice -p pw -E RC4 -I 42 -K 0f1e2d3c4b5a6978a1b2c3d4e5f60718"));
            assertTrue(r.steps().toString(), r.ok());
            assertTrue(r.step("session").get().detail().contains("RC4 (key exchange done)"));
        }
    }

    @Test public void wrongKeyIdFailsTheSessionStep() throws Exception {
        try (FakeServer srv = probed(c -> {
            c.keyExchange(XmCrypt.create(XmCrypt.Algorithm.RC4, PRE_SHARED, 42), FakeServer.KxFault.WRONG_KEY_ID_BODY);
            c.awaitClientClose();
        })) {
            TipConnectCheck.Result r = TipConnectCheck.check(addr(srv.port()),
                    opts("-a 127.0.0.1:1 -E RC4 -I 42 -K 0f1e2d3c4b5a6978a1b2c3d4e5f60718"));
            assertFalse(r.ok());
            assertEquals(List.of("dns", "tcp", "session"), r.stepNames());
            assertEquals(Status.FAIL, r.step("session").get().status());
        }
    }

    @Test public void weakDesKeyIsReportedAsInvalidKey() throws Exception {
        try (FakeServer srv = new FakeServer(c -> c.awaitClientClose())) {            // only the TCP probe connects
            TipConnectCheck.Result r = TipConnectCheck.check(addr(srv.port()), opts("-a 127.0.0.1:1 -E DES -K 0101010101010101"));
            assertFalse(r.ok());
            assertTrue(r.step("session").get().detail(), r.step("session").get().detail().contains("invalid encryption key"));
        }
    }

    // ------------------------------------------------------------------ report and exit code

    @Test public void reportForASuccessfulRun() throws Exception {
        int[] exit = new int[1];
        try (FakeServer srv = goodServer()) {
            String out = report(opts("-a 127.0.0.1:" + srv.port() + " -u alice -p " + PWD), exit);
            assertEquals(0, exit[0]);
            assertTrue(out, out.contains("1 address(es), logon as alice"));
            assertTrue(out, out.contains("127.0.0.1:" + srv.port()));
            for (String step : new String[] {"dns", "tcp", "session", "logon", "logoff"}) assertTrue(step + "\n" + out, out.contains(step));
            assertTrue(out, out.contains("[  OK  ]"));
            assertTrue(out, out.contains("=> OK"));
            assertTrue(out, out.contains("1 of 1 address(es) OK"));
            assertFalse("the password must never be printed", out.contains(PWD));
        }
    }

    @Test public void reportForAFailedRun() throws Exception {
        int[] exit = new int[1];
        String out = report(opts("-a 127.0.0.1:" + FakeServer.deadPort() + " -T 2"), exit);
        assertEquals(1, exit[0]);
        assertTrue(out, out.contains("[ FAIL ]"));
        assertTrue(out, out.contains("=> FAILED"));
        assertTrue(out, out.contains("0 of 1 address(es) OK"));
        assertTrue(out, out.contains("transport only"));
    }

    @Test public void eachAddressIsCheckedOnItsOwn() throws Exception {
        int[] exit = new int[1];
        try (FakeServer live = probed(c -> c.awaitClientClose())) {
            String out = report(opts("-a 127.0.0.1:" + FakeServer.deadPort() + " -a 127.0.0.1:" + live.port() + " -T 2"), exit);
            assertEquals("one dead address fails the run even though the other works", 1, exit[0]);
            assertTrue(out, out.contains("1 of 2 address(es) OK"));
            assertTrue(out, out.contains("=> FAILED"));
            assertTrue(out, out.contains("=> OK"));
        }
    }

    @Test public void quietWarningsKeepExitCodeZero() throws Exception {
        int[] exit = new int[1];
        try (FakeServer srv = probed(c -> { c.readText(); c.send("GEn;Rc0;\n"); c.awaitClientClose(); })) {
            String out = report(opts("-a 127.0.0.1:" + srv.port() + " -u a -p b -w 1"), exit);
            assertEquals(out, 0, exit[0]);
            assertTrue(out, out.contains("[ WARN ]"));
        }
    }
}
