package sa.com.tadawul.applications.tip.isi.efp.analyser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/**
 * Expected values below were produced by the ORIGINAL code path ({@code strncpy} +
 * {@code NewStringUTF} in libtipclient.so, run on HotSpot). Beyond these vectors, the decoder was
 * compared with the real native library on 3 million random byte strings with zero differences.
 */
class FeedTextDecoderTest {

    private static final FeedTextDecoder JNI = FeedTextDecoder.of("JNI");

    private static byte[] hex(String h) {
        String[] p = h.trim().split("\\s+");
        byte[] b = new byte[p.length];
        for (int i = 0; i < p.length; i++) {
            b[i] = (byte) Integer.parseInt(p[i], 16);
        }
        return b;
    }

    private static String chars(int... cs) {
        StringBuilder sb = new StringBuilder();
        for (int c : cs) {
            sb.append((char) c);
        }
        return sb.toString();
    }

    @Test
    void asciiIsUnchanged() {
        String m = "TRd;Si1120;Tx101523;Tp32.45;Tq1500;n101;\n";
        assertEquals(m, JNI.decode(m.getBytes(StandardCharsets.US_ASCII)));
    }

    @Test
    void windows1256ArabicArrivesAsLatin1CharsLikeTheJniLibrary() {
        byte[] b = "NAlشركة;".getBytes(Charset.forName("windows-1256"));
        // each Arabic byte becomes the char with the same value; the DB layer re-encodes it (Cp1252 -> 1256)
        assertEquals("NAl" + chars(0xD4, 0xD1, 0xDF, 0xC9) + ";", JNI.decode(b));
        assertTrue(JNI.needsLegacyArabicReencode());
    }

    @Test
    void realUtf8IsDecodedAsUtf8() {
        assertEquals("NAlالراجحي;", JNI.decode("NAlالراجحي;".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void stopsAtFirstNulLikeStrncpy() {
        assertEquals("OBk;Si1120;", JNI.decode(hex("4F 42 6B 3B 53 69 31 31 32 30 3B 00 41 42")));
    }

    @Test
    void hotspotVectors() {
        assertEquals("", JNI.decode(hex("A7")));
        assertEquals("", JNI.decode(hex("B0 B4 B7")));
        assertEquals(chars(0xB4, 0x3D, 0x5F), JNI.decode(hex("B4 3D 5F B0 5F")));
        assertEquals(chars(0x9A, 0x32, 0xCA), JNI.decode(hex("9A 32 CA 4C")));
        assertEquals(chars(0xFE, 0x83), JNI.decode(hex("FE 83 AD EB")));
        assertEquals(chars(0x44, 0x01F5, 0x7F), JNI.decode(hex("44 C7 B5 7F B2")));
        assertEquals(chars(0x67, 0xD0, 0x0B, 0xB4), JNI.decode(hex("67 D0 0B B4 16")));
        assertEquals(chars(0x87, 0xE7, 0x42, 0x5E, 0x76), JNI.decode(hex("87 E7 42 5E 76 FE 83")));
        assertEquals(chars(0x93, 0xB1, 0xB5), JNI.decode(hex("93 B1 B5 F7 93 CF 5A BF")));
    }

    @Test
    void charsetModeDecodesProperlyAndDisablesLegacyReencode() {
        FeedTextDecoder w = FeedTextDecoder.of("windows-1256");
        assertEquals("NAlشركة;", w.decode("NAlشركة;".getBytes(Charset.forName("windows-1256"))));
        assertFalse(w.needsLegacyArabicReencode());
        assertEquals("windows-1256", w.name());
        assertEquals("JNI", FeedTextDecoder.of(null).name());
        assertEquals("JNI", FeedTextDecoder.of(" jni ").name());
    }
}
