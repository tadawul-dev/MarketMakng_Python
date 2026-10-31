package tip.xm;

import java.security.GeneralSecurityException;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Session cipher (port of {@code xmcrypt.c}, {@code xmrc4.c}, {@code xmdes.c}).
 *
 * <ul>
 *   <li><b>RC4</b>: stream cipher; the key stream continues across messages, and encrypt and
 *       decrypt on the <em>same</em> object share one key stream (as in the C code).</li>
 *   <li><b>DES</b>: CBC, 8-byte blocks, key 8 bytes, IV all zeros <em>for every message</em>
 *       (the C code is built with {@code CBC_ENC_C__DONT_UPDATE_IV}).</li>
 * </ul>
 * Both are weak by modern standards; they exist only because the wire protocol requires them.
 */
public abstract sealed class XmCrypt permits XmCrypt.Rc4, XmCrypt.Des {
    private static final Logger LOG = LogManager.getLogger("tip.xm.crypt");
    public static final int NO_KEY_ID = -1;

    public enum Algorithm {
        RC4(1), DES(2);

        private final int code;

        Algorithm(int code) { this.code = code; }

        public int code() { return code; }

        public static Algorithm ofCode(int code) throws XmException {
            for (Algorithm a : values()) if (a.code == code) return a;
            throw new XmException(XmError.BAD_ALGORITHM, "code " + code);
        }
    }

    private final Algorithm algorithm;
    private final int keyId;

    private XmCrypt(Algorithm algorithm, int keyId) {
        this.algorithm = algorithm;
        this.keyId = keyId;
    }

    /** Port of {@code XM_Crypt_Init}: keys longer than the algorithm maximum are truncated. */
    public static XmCrypt create(Algorithm algorithm, byte[] key, int keyId) throws XmException {
        try {
            XmCrypt c = switch (algorithm) {
                case RC4 -> new Rc4(key, keyId);
                case DES -> new Des(key, keyId);
            };
            LOG.log(Level.DEBUG, "created {} cipher (key id {})", algorithm, keyId);   // key bytes never logged
            return c;
        } catch (XmException e) {
            LOG.log(Level.WARN, "cannot create {} cipher: {}", algorithm, e.getMessage());
            throw e;
        }
    }

    public Algorithm algorithm() { return algorithm; }

    public int keyId() { return keyId; }

    /** Block size the plaintext must be padded to (1 for RC4, 8 for DES). */
    public abstract int alignment();

    public final int alignedLength(int len) {
        int a = alignment();
        return (len + a - 1) & ~(a - 1);
    }

    /** Zero-pads {@code data} to the alignment and encrypts; the result is {@link #alignedLength} long. */
    public abstract byte[] encrypt(byte[] data, int off, int len) throws XmException;

    /** Decrypts {@code len} bytes (must be a multiple of the alignment). */
    public abstract byte[] decrypt(byte[] data, int off, int len) throws XmException;

    // ------------------------------------------------------------------------------------

    static final class Rc4 extends XmCrypt {
        private final int[] s = new int[256];
        private int i;
        private int j;

        Rc4(byte[] key, int keyId) throws XmException {
            super(Algorithm.RC4, keyId);
            if (key.length < 1) throw new XmException(XmError.BAD_KEY_LENGTH);
            int keySize = Math.min(key.length, Xmp.MAX_CRYPT_KEY_SIZE);
            int[] box = new int[256];
            for (int a = 0, b = 0; a < 256; a++, b++) {
                if (b >= keySize) b = 0;
                s[a] = a;
                box[a] = key[b] & 0xFF;
            }
            for (int a = 0, b = 0; a < 256; a++) {
                b = (b + s[a] + box[a]) & 0xFF;
                int t = s[a]; s[a] = s[b]; s[b] = t;
            }
        }

        @Override public int alignment() { return 1; }

        private int nextKeyByte() {
            i = (i + 1) & 0xFF;
            j = (j + s[i]) & 0xFF;
            int t = s[i]; s[i] = s[j]; s[j] = t;
            return s[(s[i] + s[j]) & 0xFF];
        }

        private synchronized byte[] crypt(byte[] data, int off, int len) {
            byte[] out = new byte[len];
            for (int p = 0; p < len; p++) out[p] = (byte) (data[off + p] ^ nextKeyByte());
            return out;
        }

        @Override public byte[] encrypt(byte[] data, int off, int len) { return crypt(data, off, len); }

        @Override public byte[] decrypt(byte[] data, int off, int len) { return crypt(data, off, len); }
    }

    static final class Des extends XmCrypt {
        private static final byte[] ZERO_IV = new byte[8];
        private final SecretKeySpec key;
        private final Cipher cipher;

        Des(byte[] keyBytes, int keyId) throws XmException {
            super(Algorithm.DES, keyId);
            if (keyBytes.length < 8) throw new XmException(XmError.BAD_KEY_LENGTH);
            byte[] k = Arrays.copyOf(keyBytes, 8);       // excess bytes are ignored
            try {
                if (DESKeySpec.isWeak(k, 0)) throw new XmException(XmError.WEAK_KEY);
                key = new SecretKeySpec(k, "DES");
                cipher = Cipher.getInstance("DES/CBC/NoPadding");
            } catch (GeneralSecurityException e) {
                throw new XmException(XmError.NOT_SUPPORTED, "DES unavailable in this JVM", e);
            }
        }

        @Override public int alignment() { return 8; }

        private synchronized byte[] run(int mode, byte[] data, int off, int len) throws XmException {
            if (len % 8 != 0) throw new XmException(XmError.BAD_LENGTH, "DES input not a multiple of 8");
            try {
                cipher.init(mode, key, new IvParameterSpec(ZERO_IV));   // fresh zero IV per message
                return cipher.doFinal(data, off, len);
            } catch (GeneralSecurityException e) {
                throw new XmException(XmError.INTERNAL_ERROR, "DES failure", e);
            }
        }

        @Override public byte[] encrypt(byte[] data, int off, int len) throws XmException {
            int aligned = alignedLength(len);
            byte[] padded = new byte[aligned];                               // zero padding
            System.arraycopy(data, off, padded, 0, len);
            return run(Cipher.ENCRYPT_MODE, padded, 0, aligned);
        }

        @Override public byte[] decrypt(byte[] data, int off, int len) throws XmException {
            return run(Cipher.DECRYPT_MODE, data, off, len);
        }
    }
}
