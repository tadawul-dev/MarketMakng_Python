package tip.xm;

import static org.junit.Assert.*;
import static tip.testutil.TestData.*;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import tip.testutil.FakeServer;
import tip.testutil.FakeServer.KxFault;

/** The optional key-exchange handshake, seen from the client. */
public class KeyExchangeTest {
    private static final byte[] PRE_SHARED = hex("0f1e2d3c4b5a6978a1b2c3d4e5f60718");

    private static XmCrypt initial(XmCrypt.Algorithm a, int keyId) throws XmException {
        return XmCrypt.create(a, PRE_SHARED, keyId);
    }

    private static XmSession connect(FakeServer s, XmCrypt c) throws XmException {
        return XmSession.connect("127.0.0.1", s.port(),
                XmSession.Options.defaults().withConnectTimeout(Duration.ofSeconds(3)).withRecvTimeout(Duration.ofSeconds(3)).withInitialCrypt(c));
    }

    private static final XmCrypt.Algorithm[] ALGORITHMS = XmCrypt.Algorithm.values();

    @Test public void requestHasTheDocumentedLayout() throws Exception {
        for (XmCrypt.Algorithm alg : ALGORITHMS) {
            FakeServer srv = new FakeServer(c -> {
                c.serverTime = 0x5e0be100;
                c.keyExchange(initial(alg, 42));
                byte[] p = c.kxRequestPlain;
                assertEquals(alg + ": frame type", Xmp.Type.KEY_EXCHANGE, p[0] & 0xFF);
                assertEquals("ext length", 0, p[1]);
                assertEquals("body length", 32, ((p[2] & 0xFF) << 8) | (p[3] & 0xFF));
                assertEquals("opcode request", 1, p[4]);
                assertEquals("key id", 42, ((p[12] & 0xFF) << 24) | ((p[13] & 0xFF) << 16) | ((p[14] & 0xFF) << 8) | (p[15] & 0xFF));
                assertEquals("server time is echoed", 0x5e0be100, ((p[16] & 0xFF) << 24) | ((p[17] & 0xFF) << 16) | ((p[18] & 0xFF) << 8) | (p[19] & 0xFF));
                assertEquals("encrypted length is aligned", alg == XmCrypt.Algorithm.DES ? 40 : 36, p.length);
                assertEquals(42, c.kxRequestExt.find(Xmp.Tag.KEY_ID).get().intValue());
                assertEquals(alg.code(), c.kxRequestExt.find(Xmp.Tag.ENCRYPTION_ALGORITHM).get().data()[0]);
            });
            try (srv; XmSession s = connect(srv, initial(alg, 42))) {
                assertFalse(s.isClosed());
            }
        }
    }

    @Test public void eachRequestUsesFreshRandomness() throws Exception {
        Set<String> nonces = new HashSet<>();
        Set<String> keys = new HashSet<>();
        for (int i = 0; i < 6; i++) {
            try (FakeServer srv = new FakeServer(c -> {
                c.keyExchange(initial(XmCrypt.Algorithm.RC4, 1));
                nonces.add(hex(java.util.Arrays.copyOfRange(c.kxRequestPlain, 5, 12)));
                keys.add(hex(java.util.Arrays.copyOfRange(c.kxRequestPlain, 20, 36)));
            }); XmSession s = connect(srv, initial(XmCrypt.Algorithm.RC4, 1))) {
                assertNotNull(s);
            }
        }
        assertEquals("nonces must differ between handshakes", 6, nonces.size());
        assertEquals("proposed keys must differ between handshakes", 6, keys.size());
    }

    @Test public void clientAsksForTheServerTimeFirst() throws Exception {
        try (FakeServer srv = new FakeServer(c -> {
            XmpFrame first = c.readRaw();
            assertEquals(Xmp.Type.NONE, first.type());
            assertEquals(0, first.body().length);
            assertEquals(0, first.ext().find(Xmp.Tag.TIMESTAMP).get().data().length);
            c.awaitClientClose();                                           // never answers
        })) {
            try {
                XmSession.connect("127.0.0.1", srv.port(), XmSession.Options.defaults()
                        .withRecvTimeout(Duration.ofMillis(300)).withInitialCrypt(initial(XmCrypt.Algorithm.RC4, 1))).close();
                fail("server never answers, the handshake must fail");
            } catch (XmException e) {
                assertEquals(XmError.TIMEOUT, e.error());
            }
        }
    }

    @Test public void faultyServerRepliesAreRejected() throws Exception {
        KxFault[] faults = {KxFault.WRONG_KEY_ID_EXT, KxFault.WRONG_ALGORITHM_EXT, KxFault.NO_KEY_ID_EXT, KxFault.REPLY_NOT_ENCRYPTED,
                KxFault.SHORT_REPLY, KxFault.BAD_OPCODE, KxFault.WRONG_KEY_ID_BODY, KxFault.NO_TIMESTAMP, KxFault.SHORT_TIMESTAMP,
                KxFault.ENCRYPTED_WITH_OTHER_KEY};
        for (XmCrypt.Algorithm alg : ALGORITHMS) {
            for (KxFault fault : faults) {
                try (FakeServer srv = new FakeServer(c -> {
                    c.keyExchange(initial(alg, 42), fault);
                    c.awaitClientClose();                                   // the client must hang up
                })) {
                    try {
                        connect(srv, initial(alg, 42)).close();
                        fail(alg + "/" + fault + ": handshake should have failed");
                    } catch (XmException e) {
                        assertEquals(alg + "/" + fault, XmError.INTERNAL_ERROR, e.error());
                    }
                }
            }
        }
    }

    @Test public void initialKeyWithoutKeyIdIsRefused() throws Exception {
        try (FakeServer srv = new FakeServer(c -> c.awaitClientClose())) {
            try {
                connect(srv, XmCrypt.create(XmCrypt.Algorithm.RC4, PRE_SHARED, XmCrypt.NO_KEY_ID)).close();
                fail();
            } catch (XmException e) {
                assertEquals(XmError.BAD_PARAM, e.error());
            }
        }
    }

    @Test public void serverThatHangsUpDuringTheHandshake() throws Exception {
        try (FakeServer srv = new FakeServer(c -> { c.readRaw(); c.hangUp(); })) {
            try {
                connect(srv, initial(XmCrypt.Algorithm.DES, 1)).close();
                fail();
            } catch (XmException e) {
                assertEquals(XmError.NETWORK_ERROR, e.error());
            }
        }
    }

    @Test public void serverWithAnotherPreSharedKeyIsNotAccepted() throws Exception {
        XmCrypt serverSide = XmCrypt.create(XmCrypt.Algorithm.DES, hex("1111111122222222"), 42);
        try (FakeServer srv = new FakeServer(c -> {
            try { c.keyExchange(serverSide); } catch (Exception expected) { /* server cannot read our request either */ }
            c.awaitClientClose();
        })) {
            try {
                connect(srv, initial(XmCrypt.Algorithm.DES, 42)).close();
                fail();
            } catch (XmException e) {
                assertNotEquals(XmError.NO_ERROR, e.error());
            }
        }
    }

    @Test public void sessionIsEncryptedAfterTheHandshake() throws Exception {
        for (XmCrypt.Algorithm alg : ALGORITHMS) {
            try (FakeServer srv = new FakeServer(c -> {
                c.keyExchange(initial(alg, 7));
                assertEquals("LGn;UIdu;\n", c.readText());
                c.send("GEn;Rc0;\n");
            }); XmSession s = connect(srv, initial(alg, 7))) {
                s.send(new XmpFrame(Xmp.Type.BINARY_RECORD, "LGn;UIdu;\n".getBytes()));
                assertEquals("GEn;Rc0;\n", new String(s.recv().body()));
            }
        }
    }
}
