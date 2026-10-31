package sa.com.tadawul.applications.tip.isi.efp.analyser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import sa.com.tadawul.website.tip.queue.FrameHolder;
import sa.com.tadawul.website.tip.shared.TipConstants;
import tip.testutil.FakeServer;
import tip.xm.XmService;

/** End-to-end: TipFeedReader + TipClient against an in-process XMP/TIP server. */
class TipFeedReaderTest {

    private static final String WELCOME = "GEn;QtLGn;Rc0;RxWelcome;\n";

    private static TipFeedReader.Config config(int port, Path feedLog, boolean recovery) {
        return new TipFeedReader.Config("web", "secret", null,
                List.of(new XmService.Address("127.0.0.1", port)), 20261031, 1, false,
                Duration.ofSeconds(2), Duration.ofSeconds(5), 500, FeedTextDecoder.of("JNI"),
                feedLog != null, recovery, feedLog == null ? null : feedLog.toString(), 0L, 0L, Duration.ofMillis(50));
    }

    private static Thread start(TipFeedReader r) {
        Thread t = new Thread(r::run, "test-reader");
        t.start();
        return t;
    }

    @Test
    void logonStreamDeliverAndFeedLog() throws Exception {
        TipConstants.shutdownFlag = false;
        Path log = Files.createTempFile("tipfeed", ".txt");
        Files.delete(log);
        List<String> wire = new CopyOnWriteArrayList<>();
        List<FrameHolder> got = new CopyOnWriteArrayList<>();
        CountDownLatch delivered = new CountDownLatch(3);
        try (FakeServer srv = new FakeServer(1, c -> {
            wire.add(c.readText());
            c.send(WELCOME);
            wire.add(c.readText());
            c.send("TRd;Si1120;Tp32.45;n10;\n");
            c.send("TSt;Si1120;Pd-0.40;n11");                 // no newline: reader adds it, as before
            c.send("INd;Si90001;Vc11234.56;n12;\n");
            wire.add(c.readText());                            // LGf on stop
            c.send("GEn;QtLGf;Rc0;RxBye;\n");
            c.awaitClientClose();
        })) {
            TipFeedReader reader = new TipFeedReader(config(srv.port(), log, true), f -> { got.add(f); delivered.countDown(); }, () -> 0L);
            Thread t = start(reader);
            assertTrue(delivered.await(5, TimeUnit.SECONDS), "messages not delivered");
            reader.requestStop(Duration.ofSeconds(5));
            t.join(5000);
            assertFalse(t.isAlive());
            assertEquals("LGf;\n", wire.get(2));
            assertEquals("LGn;UIdweb;PWdsecret;\n", wire.get(0));
            assertEquals("RTr;Sn1;QDAy20261031;QSCo3;\n", wire.get(1));
            assertEquals("TRd;Si1120;Tp32.45;n10;\n", got.get(0).buffer());
            assertEquals("TSt;Si1120;Pd-0.40;n11" + TipConstants.TIP_LINE_SEPARATOR, got.get(1).buffer());
            assertEquals(3, got.get(2).frameNo());
            assertEquals(12, reader.lastSequence());
            assertEquals("TRd;Si1120;Tp32.45;n10;\nTSt;Si1120;Pd-0.40;n11\nINd;Si90001;Vc11234.56;n12;\n",
                    Files.readString(log, StandardCharsets.ISO_8859_1));
        } finally {
            Files.deleteIfExists(log);
            TipConstants.shutdownFlag = false;
        }
    }

    @Test
    void replaysFeedLogThenResumesFromLastSequence() throws Exception {
        TipConstants.shutdownFlag = false;
        Path log = Files.createTempFile("tipfeed", ".txt");
        Files.writeString(log, "TRd;Si1120;n498;\nTRd;Si1120;n499;\r\nTRd;Si1120;n500;\n", StandardCharsets.ISO_8859_1);
        List<String> wire = new CopyOnWriteArrayList<>();
        List<FrameHolder> got = new CopyOnWriteArrayList<>();
        CountDownLatch delivered = new CountDownLatch(4);
        try (FakeServer srv = new FakeServer(1, c -> {
            wire.add(c.readText());
            c.send(WELCOME);
            wire.add(c.readText());
            c.send("TRd;Si1120;n500;\n");
            c.awaitClientClose();
        })) {
            TipFeedReader reader = new TipFeedReader(config(srv.port(), log, true), f -> { got.add(f); delivered.countDown(); }, () -> 0L);
            Thread t = start(reader);
            assertTrue(delivered.await(5, TimeUnit.SECONDS));
            reader.requestStop(Duration.ofSeconds(5));
            t.join(5000);
            assertEquals("RTr;Sn500;QDAy20261031;QSCo3;\n", wire.get(1));
            assertEquals("TRd;Si1120;n498;" + TipConstants.TIP_LINE_SEPARATOR, got.get(0).buffer());
            assertEquals("TRd;Si1120;n499;" + TipConstants.TIP_LINE_SEPARATOR, got.get(1).buffer());
            assertEquals(4, got.size());
        } finally {
            Files.deleteIfExists(log);
            TipConstants.shutdownFlag = false;
        }
    }

    @Test
    void reconnectResumesWithoutReplayingAgain() throws Exception {
        TipConstants.shutdownFlag = false;
        List<String> requests = new CopyOnWriteArrayList<>();
        List<FrameHolder> got = new CopyOnWriteArrayList<>();
        CountDownLatch delivered = new CountDownLatch(4);
        try (FakeServer srv = new FakeServer(2, c -> {
            c.readText();
            c.send(WELCOME);
            requests.add(c.readText());
            if (c.index == 0) {
                c.send("TRd;Si1;n10;\n");
                c.send("TRd;Si1;n11;\n");
                c.send("TRd;Si1;n12;\n");
                c.hangUp();                                     // connection lost
            } else {
                c.send("TRd;Si1;n13;\n");
                c.awaitClientClose();
            }
        })) {
            TipFeedReader reader = new TipFeedReader(config(srv.port(), null, false), f -> { got.add(f); delivered.countDown(); }, () -> 0L);
            Thread t = start(reader);
            assertTrue(delivered.await(10, TimeUnit.SECONDS), "got " + got.size());
            assertFalse(TipConstants.shutdownFlag, "a lost connection must not shut the listener down");
            reader.requestStop(Duration.ofSeconds(5));
            t.join(5000);
            assertEquals("RTr;Sn1;QDAy20261031;QSCo3;\n", requests.get(0));
            assertEquals("RTr;Sn12;QDAy20261031;QSCo3;\n", requests.get(1));
            assertEquals(4, got.size());
        } finally {
            TipConstants.shutdownFlag = false;
        }
    }

    @Test
    void refusedLogonShutsDown() throws Exception {
        TipConstants.shutdownFlag = false;
        try (FakeServer srv = new FakeServer(1, c -> {
            c.readText();
            c.send("GEn;QtLGn;Rc4;RxInvalid password;\n");
            c.awaitClientClose();
        })) {
            TipFeedReader reader = new TipFeedReader(config(srv.port(), null, false), f -> { }, () -> 0L);
            Thread t = start(reader);
            t.join(10_000);
            assertFalse(t.isAlive(), "reader still running");
            assertTrue(TipConstants.shutdownFlag, "shutdown flag not set");
        } finally {
            TipConstants.shutdownFlag = false;
        }
    }

    @Test
    void unreachableServerShutsDown() throws Exception {
        TipConstants.shutdownFlag = false;
        try {
            TipFeedReader reader = new TipFeedReader(config(FakeServer.deadPort(), null, false), f -> { }, () -> 0L);
            Thread t = start(reader);
            t.join(10_000);
            assertFalse(t.isAlive());
            assertTrue(TipConstants.shutdownFlag);
        } finally {
            TipConstants.shutdownFlag = false;
        }
    }

    @Test
    void stopWhileIdleSendsLogoffAndWaitsForReply() throws Exception {
        TipConstants.shutdownFlag = false;
        List<String> wire = new CopyOnWriteArrayList<>();
        CountDownLatch streaming = new CountDownLatch(1);
        try (FakeServer srv = new FakeServer(1, c -> {
            c.readText();
            c.send(WELCOME);
            c.readText();
            streaming.countDown();
            wire.add(c.readText());                            // LGf
            c.send("GEn;QtLGf;Rc0;RxBye;\n");
            c.awaitClientClose();
        })) {
            TipFeedReader reader = new TipFeedReader(config(srv.port(), null, false), f -> { }, () -> 0L);
            Thread t = start(reader);
            assertTrue(streaming.await(5, TimeUnit.SECONDS));
            Thread.sleep(200);                                 // reader is now blocked waiting for data
            long t0 = System.nanoTime();
            reader.requestStop(Duration.ofSeconds(10));
            t.join(5000);
            assertFalse(t.isAlive(), "reader still running");
            assertEquals("LGf;\n", wire.get(0));
            long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
            assertTrue(ms < 4000, "stop took " + ms + " ms");
            assertTrue(TipConstants.shutdownFlag, "shutdown flag not set");
        } finally {
            TipConstants.shutdownFlag = false;
        }
    }

    @Test
    void trackSequenceFindsOnlyTheNField() {
        TipFeedReader r = new TipFeedReader(config(1, null, false), f -> { }, () -> 0L);
        r.trackSequence("TRd;NAmAbc;n77;\n");
        assertEquals(77, r.lastSequence());
        r.trackSequence("TRd;NAmn9x;Tp1;n78\n");
        assertEquals(78, r.lastSequence());
        r.trackSequence("TRd;Si1;n5;\n");                     // lower: ignored
        assertEquals(78, r.lastSequence());
    }
}
