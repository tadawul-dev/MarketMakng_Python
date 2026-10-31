package tip.xm;

import java.security.SecureRandom;
import java.util.Optional;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Session key exchange (port of {@code xmkeyexch.c}).
 *
 * <p>The client proves it knows a pre-shared key (identified by a key id) by encrypting a
 * request with it; the server answers with a fresh 16-byte session key encrypted the same way.
 * Two new ciphers are then created from that key, one per direction.
 *
 * <p>Unlike the C code, nonce and padding bytes come from {@link SecureRandom}
 * (the C code used a seeded drand48 generator).
 */
final class KeyExchange {
    private KeyExchange() {}

    record Keys(XmCrypt decrypt, XmCrypt encrypt) {}

    private static final int KX_MESSAGE_SIZE = 32;
    private static final int KX_NONCE_SIZE = 7;
    private static final int OP_REQUEST = 1;
    private static final int OP_REPLY = 2;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Logger LOG = LogManager.getLogger("tip.xm.keyexchange");

    static Keys perform(XmSession session, XmCrypt initial) throws XmException {
        XmCrypt.Algorithm ea = initial.algorithm();
        if (initial.keyId() == XmCrypt.NO_KEY_ID) throw new XmException(XmError.BAD_PARAM, "initial key has no key id");

        LOG.log(Level.DEBUG, "starting key exchange ({}, key id {})", ea, initial.keyId());
        requestKey(session, initial, ea);
        LOG.log(Level.DEBUG, "key request sent, waiting for reply");
        byte[] sessionKey = receiveReply(session, initial, ea);
        LOG.log(Level.DEBUG, "session key received");   // the key itself is never logged
        return new Keys(XmCrypt.create(ea, sessionKey, XmCrypt.NO_KEY_ID),
                        XmCrypt.create(ea, sessionKey, XmCrypt.NO_KEY_ID));
    }

    private static void requestKey(XmSession session, XmCrypt initial, XmCrypt.Algorithm ea) throws XmException {
        int unixTime = requestTime(session);
        LOG.log(Level.DEBUG, "server time {}", unixTime);

        ExtHeader ext = ExtHeader.EMPTY
                .withInt(Xmp.Tag.KEY_ID, initial.keyId())
                .withByte(Xmp.Tag.ENCRYPTION_ALGORITHM, ea.code());

        byte[] kx = new byte[Xmp.HEADER_SIZE + KX_MESSAGE_SIZE];
        kx[0] = Xmp.Type.KEY_EXCHANGE;
        kx[1] = 0;
        kx[2] = 0;
        kx[3] = KX_MESSAGE_SIZE;
        kx[4] = OP_REQUEST;
        byte[] nonce = new byte[KX_NONCE_SIZE];
        RANDOM.nextBytes(nonce);
        System.arraycopy(nonce, 0, kx, 5, KX_NONCE_SIZE);
        putInt(kx, 12, initial.keyId());
        putInt(kx, 16, unixTime);
        byte[] randomKey = new byte[Xmp.MAX_CRYPT_KEY_SIZE];
        RANDOM.nextBytes(randomKey);
        System.arraycopy(randomKey, 0, kx, 20, randomKey.length);

        byte[] encrypted = initial.encrypt(kx, 0, kx.length);
        session.send(new XmpFrame(Xmp.Type.ENCRYPTED, ext, encrypted));
    }

    private static byte[] receiveReply(XmSession session, XmCrypt initial, XmCrypt.Algorithm ea) throws XmException {
        XmpFrame reply = session.recv();
        if (reply.type() != Xmp.Type.ENCRYPTED) throw fail("key exchange reply is not encrypted");

        Optional<ExtHeader.Entry> id = reply.ext().find(Xmp.Tag.KEY_ID);
        Optional<ExtHeader.Entry> alg = reply.ext().find(Xmp.Tag.ENCRYPTION_ALGORITHM);
        if (id.isEmpty() || id.get().data().length != 4 || id.get().intValue() != initial.keyId()
                || alg.isEmpty() || alg.get().data().length != 1 || (alg.get().data()[0] & 0xFF) != ea.code()) {
            throw fail("key exchange reply has wrong key id or algorithm");
        }

        byte[] body = reply.body();
        if (body.length != initial.alignedLength(Xmp.HEADER_SIZE + KX_MESSAGE_SIZE)) {
            throw fail("key exchange reply has unexpected length " + body.length);
        }
        byte[] plain = initial.decrypt(body, 0, body.length);

        if ((plain[0] & 0xFF) != Xmp.Type.KEY_EXCHANGE
                || XmpFrame.frameLength(plain, 0) != Xmp.HEADER_SIZE + KX_MESSAGE_SIZE) {
            throw fail("malformed key exchange message");
        }
        int opcode = plain[4] & 0xFF;
        if (opcode != OP_REPLY && opcode != OP_REQUEST) throw fail("bad key exchange opcode " + opcode);
        if (getInt(plain, 12) != initial.keyId()) throw fail("key id mismatch");

        byte[] key = new byte[Xmp.MAX_CRYPT_KEY_SIZE];
        System.arraycopy(plain, 20, key, 0, key.length);
        return key;
    }

    /** Asks the server for its clock; the key exchange message carries that time. */
    private static int requestTime(XmSession session) throws XmException {
        session.send(new XmpFrame(Xmp.Type.NONE, ExtHeader.EMPTY.with(Xmp.Tag.TIMESTAMP, new byte[0]), null));
        XmpFrame reply = session.recv();
        Optional<ExtHeader.Entry> ts = reply.ext().find(Xmp.Tag.TIMESTAMP);
        if (ts.isEmpty() || ts.get().data().length < 4) {
            throw fail("no timestamp in reply to timestamp request");
        }
        return ts.get().intValue();
    }

    private static XmException fail(String why) {
        LOG.log(Level.WARN, "key exchange rejected: {}", why);
        return new XmException(XmError.INTERNAL_ERROR, why);
    }

    private static void putInt(byte[] b, int off, int v) {
        b[off] = (byte) (v >>> 24);
        b[off + 1] = (byte) (v >>> 16);
        b[off + 2] = (byte) (v >>> 8);
        b[off + 3] = (byte) v;
    }

    private static int getInt(byte[] b, int off) {
        return ((b[off] & 0xFF) << 24) | ((b[off + 1] & 0xFF) << 16) | ((b[off + 2] & 0xFF) << 8) | (b[off + 3] & 0xFF);
    }

}
