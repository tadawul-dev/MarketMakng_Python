package tip.xm;

import static org.junit.Assert.*;
import static tip.testutil.TestData.*;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.Test;

public class XmCryptTest {
    private static final byte[] DES_KEY = hex("133457799bbcdff1");

    private static XmCrypt rc4(String keyHex) throws XmException {
        return XmCrypt.create(XmCrypt.Algorithm.RC4, hex(keyHex), 7);
    }

    private static XmCrypt des(byte[] key) throws XmException {
        return XmCrypt.create(XmCrypt.Algorithm.DES, key, 7);
    }

    private static XmError createError(XmCrypt.Algorithm a, byte[] key) {
        try {
            XmCrypt.create(a, key, 1);
            fail("expected XmException");
            return null;
        } catch (XmException e) {
            return e.error();
        }
    }

    // ------------------------------------------------------------------ golden / known answers

    /** Replays commands recorded from the original C implementation (RC4 and DES). */
    @Test public void matchesTheOriginalCImplementation() throws Exception {
        XmCrypt[] crypts = new XmCrypt[8];
        int checked = 0, rejections = 0;
        for (String line : resourceLines("/golden/crypt.golden")) {
            String[] p = line.split(" => ", 2);
            String[] c = p[0].split(" ");
            String expected = p[1];
            String actual;
            switch (c[0]) {
                case "new" -> {
                    try {
                        crypts[Integer.parseInt(c[2])] = XmCrypt.create(XmCrypt.Algorithm.valueOf(c[1]), hex(c[3]), 1);
                        actual = "OK";
                    } catch (XmException e) {
                        actual = "ERR " + e.error().code();
                        rejections++;
                    }
                }
                case "enc" -> { byte[] d = hex(c[2]); actual = hex(crypts[Integer.parseInt(c[1])].encrypt(d, 0, d.length)); }
                case "dec" -> { byte[] d = hex(c[2]); actual = hex(crypts[Integer.parseInt(c[1])].decrypt(d, 0, d.length)); }
                default -> throw new AssertionError("unknown command " + c[0]);
            }
            assertEquals("line: " + line.substring(0, Math.min(60, line.length())), expected, actual);
            checked++;
        }
        assertTrue(checked > 150);
        assertTrue("golden data should include rejected (weak) keys", rejections >= 3);
    }

    @Test public void rc4PublicTestVectors() throws Exception {
        assertEquals("bbf316e8d940af0ad3", hex(rc4(hex("4b6579")[0] == 0x4b ? "4b6579" : "").encrypt(ascii("Plaintext"), 0, 9)));
        assertEquals("1021bf0420", hex(rc4("57696b69").encrypt(ascii("pedia"), 0, 5)));
        assertEquals("45a01f645fc35b383552544b9bf5", hex(rc4("536563726574").encrypt(ascii("Attack at dawn"), 0, 14)));
    }

    @Test public void desPublicTestVector() throws Exception {
        assertEquals("85e813540f0ab405", hex(des(DES_KEY).encrypt(hex("0123456789abcdef"), 0, 8)));
        assertEquals("0123456789abcdef", hex(des(DES_KEY).decrypt(hex("85e813540f0ab405"), 0, 8)));
    }

    private static byte[] ascii(String s) { return s.getBytes(StandardCharsets.US_ASCII); }

    // ------------------------------------------------------------------ RC4

    @Test public void rc4RoundTripWithTwoInstances() throws Exception {
        XmCrypt enc = rc4("0102030405060708090a0b0c0d0e0f10");
        XmCrypt dec = rc4("0102030405060708090a0b0c0d0e0f10");
        for (String msg : new String[] {"first", "second message", "third"}) {
            byte[] c = enc.encrypt(ascii(msg), 0, msg.length());
            assertEquals(msg.length(), c.length);
            assertEquals(msg, new String(dec.decrypt(c, 0, c.length), StandardCharsets.US_ASCII));
        }
    }

    @Test public void rc4KeyStreamContinuesAcrossMessages() throws Exception {
        XmCrypt c = rc4("aabbccdd");
        byte[] one = c.encrypt(ascii("same"), 0, 4);
        byte[] two = c.encrypt(ascii("same"), 0, 4);
        assertFalse("RC4 must not restart its key stream per message", Arrays.equals(one, two));
    }

    @Test public void rc4EncryptAndDecryptShareOneKeyStreamOnOneObject() throws Exception {
        XmCrypt c = rc4("aabbccdd");
        byte[] cipher = c.encrypt(ascii("hello"), 0, 5);
        byte[] again = c.decrypt(cipher, 0, 5);          // advances the same stream, so this is not the plaintext
        assertNotEquals("hello", new String(again, StandardCharsets.US_ASCII));
    }

    @Test public void rc4UsesAtMostTheFirst16KeyBytes() throws Exception {
        byte[] long20 = hex("000102030405060708090a0b0c0d0e0f" + "ffffffff");
        byte[] first16 = Arrays.copyOf(long20, 16);
        byte[] a = XmCrypt.create(XmCrypt.Algorithm.RC4, long20, 1).encrypt(ascii("abc"), 0, 3);
        byte[] b = XmCrypt.create(XmCrypt.Algorithm.RC4, first16, 1).encrypt(ascii("abc"), 0, 3);
        assertArrayEquals(a, b);
    }

    @Test public void rc4ShortKeysRepeat() throws Exception {
        assertArrayEquals(rc4("0102").encrypt(ascii("data"), 0, 4), rc4("01020102").encrypt(ascii("data"), 0, 4));
    }

    @Test public void rc4HonoursOffsetAndLength() throws Exception {
        byte[] buf = ascii("xxHELLOyy");
        assertArrayEquals(rc4("1234").encrypt(ascii("HELLO"), 0, 5), rc4("1234").encrypt(buf, 2, 5));
    }

    @Test public void rc4EmptyInput() throws Exception {
        assertEquals(0, rc4("1234").encrypt(new byte[0], 0, 0).length);
    }

    @Test public void rc4RejectsEmptyKey() {
        assertEquals(XmError.BAD_KEY_LENGTH, createError(XmCrypt.Algorithm.RC4, new byte[0]));
    }

    // ------------------------------------------------------------------ DES

    @Test public void desZeroPadsToEightBytes() throws Exception {
        XmCrypt c = des(DES_KEY);
        byte[] cipher = c.encrypt(ascii("abc"), 0, 3);
        assertEquals(8, cipher.length);
        byte[] plain = c.decrypt(cipher, 0, 8);
        assertArrayEquals(bytes('a', 'b', 'c', 0, 0, 0, 0, 0), plain);
        assertEquals(0, c.encrypt(new byte[0], 0, 0).length);
        assertEquals(16, c.encrypt(new byte[9], 0, 9).length);
    }

    @Test public void desRestartsWithZeroIvForEveryMessage() throws Exception {
        XmCrypt c = des(DES_KEY);
        assertArrayEquals(c.encrypt(ascii("12345678"), 0, 8), c.encrypt(ascii("12345678"), 0, 8));
    }

    @Test public void desChainsBlocksWithinOneMessage() throws Exception {
        byte[] twoEqualBlocks = hex("0123456789abcdef0123456789abcdef");
        byte[] c = des(DES_KEY).encrypt(twoEqualBlocks, 0, 16);
        assertEquals("85e813540f0ab405", hex(Arrays.copyOf(c, 8)));
        assertNotEquals("CBC: second block must differ", hex(Arrays.copyOf(c, 8)), hex(Arrays.copyOfRange(c, 8, 16)));
        assertArrayEquals(twoEqualBlocks, des(DES_KEY).decrypt(c, 0, 16));
    }

    @Test public void desKeyLongerThanEightBytesIsTruncated() throws Exception {
        byte[] long12 = hex("133457799bbcdff1" + "deadbeef");
        assertArrayEquals(des(DES_KEY).encrypt(ascii("12345678"), 0, 8), des(long12).encrypt(ascii("12345678"), 0, 8));
    }

    @Test public void desRejectsShortKeys() {
        assertEquals(XmError.BAD_KEY_LENGTH, createError(XmCrypt.Algorithm.DES, hex("133457799bbcdf")));
        assertEquals(XmError.BAD_KEY_LENGTH, createError(XmCrypt.Algorithm.DES, new byte[0]));
    }

    @Test public void desRejectsWeakKeys() {
        for (String k : new String[] {"0101010101010101", "fefefefefefefefe", "1f1f1f1f0e0e0e0e", "e0e0e0e0f1f1f1f1"}) {
            assertEquals(k, XmError.WEAK_KEY, createError(XmCrypt.Algorithm.DES, hex(k)));
        }
    }

    @Test public void desDecryptRequiresWholeBlocks() throws Exception {
        try {
            des(DES_KEY).decrypt(new byte[7], 0, 7);
            fail();
        } catch (XmException e) {
            assertEquals(XmError.BAD_LENGTH, e.error());
        }
    }

    @Test public void desWithWrongKeyDoesNotRecoverPlaintext() throws Exception {
        byte[] c = des(DES_KEY).encrypt(ascii("12345678"), 0, 8);
        byte[] other = des(hex("0123456789abcdef")).decrypt(c, 0, 8);
        assertNotEquals("12345678", new String(other, StandardCharsets.US_ASCII));
    }

    // ------------------------------------------------------------------ common

    @Test public void alignment() throws Exception {
        XmCrypt r = rc4("00112233"), d = des(DES_KEY);
        assertEquals(1, r.alignment());
        assertEquals(8, d.alignment());
        for (int n = 0; n <= 17; n++) {
            assertEquals(n, r.alignedLength(n));
            assertEquals((n + 7) / 8 * 8, d.alignedLength(n));
        }
    }

    @Test public void algorithmCodes() throws Exception {
        assertEquals(1, XmCrypt.Algorithm.RC4.code());
        assertEquals(2, XmCrypt.Algorithm.DES.code());
        assertEquals(XmCrypt.Algorithm.RC4, XmCrypt.Algorithm.ofCode(1));
        assertEquals(XmCrypt.Algorithm.DES, XmCrypt.Algorithm.ofCode(2));
        for (int bad : new int[] {0, 3, -1, 255}) {
            try {
                XmCrypt.Algorithm.ofCode(bad);
                fail("code " + bad);
            } catch (XmException e) {
                assertEquals(XmError.BAD_ALGORITHM, e.error());
            }
        }
    }

    @Test public void keyIdAndAlgorithmAreReported() throws Exception {
        XmCrypt c = rc4("0011");
        assertEquals(7, c.keyId());
        assertEquals(XmCrypt.Algorithm.RC4, c.algorithm());
        assertEquals(-1, XmCrypt.NO_KEY_ID);
        assertEquals(XmCrypt.NO_KEY_ID, XmCrypt.create(XmCrypt.Algorithm.DES, DES_KEY, XmCrypt.NO_KEY_ID).keyId());
    }

    @Test public void keyArrayIsNotRetained() throws Exception {
        byte[] key = hex("0102030405060708");
        XmCrypt c = XmCrypt.create(XmCrypt.Algorithm.RC4, key, 1);
        byte[] before = c.encrypt(ascii("x"), 0, 1);
        Arrays.fill(key, (byte) 0);                       // caller wipes its copy
        XmCrypt fresh = XmCrypt.create(XmCrypt.Algorithm.RC4, hex("0102030405060708"), 1);
        assertArrayEquals(fresh.encrypt(ascii("x"), 0, 1), before);
    }
}
