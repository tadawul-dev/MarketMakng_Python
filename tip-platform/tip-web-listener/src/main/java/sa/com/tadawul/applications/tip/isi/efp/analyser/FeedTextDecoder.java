package sa.com.tadawul.applications.tip.isi.efp.analyser;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Turns the bytes of one TIP message (as delivered by {@code TipClient.nextBody()}) into the
 * {@code String} that the parsers receive.
 * <p>
 * <b>{@code JNI} mode (default)</b> reproduces exactly what the old {@code libtipclient.so} did, so
 * every downstream rule - in particular the Arabic re-encoding in the DB layer
 * (Cp1252 -&gt; Windows-1256) - keeps producing the same output:
 * <ol>
 *   <li>{@code strncpy(mybuf, body, bodyLen)}: the text ends at the first NUL byte;</li>
 *   <li>{@code (*env)->NewStringUTF(env, mybuf)}: HotSpot decodes the bytes as (modified)
 *       UTF-8, and any byte that does not start a valid 1-, 2- or 3-byte sequence becomes the
 *       {@code char} with the same value (ISO-8859-1 style). That is how Windows-1256 Arabic
 *       bytes used to reach Java, and why the DB code re-encodes them through Cp1252.</li>
 *   <li>HotSpot sizes the new string by counting the bytes that are <i>not</i> UTF-8 continuation
 *       bytes ({@code 10xxxxxx}) and truncates the decoded text to that length. So a stray
 *       continuation byte (e.g. Windows-1256 0x80-0xBF outside a valid pair) shortens the string
 *       and drops characters from its end ({@code UTF8::unicode_length} / {@code convert_to_unicode},
 *       the same code in JDK 8 and JDK 21).</li>
 * </ol>
 * This emulation was verified against the original libtipclient.so and against
 * {@code NewStringUTF} on millions of random byte strings (see MIGRATION.md).
 * <b>Charset mode</b> (FEED_CHARSET=windows-1256, UTF-8, ...) decodes with a real charset. In that
 * case the legacy Arabic re-encoding is switched off automatically (see
 * {@link sa.com.tadawul.website.tip.shared.TipConstants#ARABIC_REENCODE}).
 * <p>
 * Pure-ASCII messages, which are almost all of them, take a fast path in both modes.
 */
public abstract sealed class FeedTextDecoder permits FeedTextDecoder.Jni, FeedTextDecoder.ForCharset {

    /** Value of FEED_CHARSET that selects the JNI-compatible decoding. */
    public static final String JNI = "JNI";

    public static FeedTextDecoder of(String name) {
        if (name == null || name.isBlank() || JNI.equals(name.trim().toUpperCase(Locale.ROOT))) {
            return new Jni();
        }
        return new ForCharset(Charset.forName(name.trim()));
    }

    /** Decodes {@code len} bytes starting at {@code off}. */
    public abstract String decode(byte[] b, int off, int len);

    public final String decode(byte[] b) {
        return decode(b, 0, b.length);
    }

    /** True when the legacy Cp1252 -&gt; Windows-1256 re-encoding must still be applied downstream. */
    public abstract boolean needsLegacyArabicReencode();

    public abstract String name();

    static boolean isAscii(byte[] b, int off, int end) {
        for (int i = off; i < end; i++) {
            if (b[i] < 0) {
                return false;
            }
        }
        return true;
    }

    /** Bit-for-bit emulation of {@code strncpy} + HotSpot {@code NewStringUTF}. */
    static final class Jni extends FeedTextDecoder {

        @Override
        public String decode(byte[] b, int off, int len) {
            int end = off + len;
            for (int i = off; i < end; i++) {      // strncpy stops at the first NUL
                if (b[i] == 0) {
                    end = i;
                    break;
                }
            }
            if (isAscii(b, off, end)) {
                return new String(b, off, end - off, StandardCharsets.ISO_8859_1);
            }
            int length = 0;                        // HotSpot: number of non-continuation bytes
            for (int i = off; i < end; i++) {
                if ((b[i] & 0xC0) != 0x80) {
                    length++;
                }
            }
            char[] out = new char[length];
            int n = 0;
            int p = off;
            while (p < end && n < length) {
                int ch = b[p] & 0xFF;
                int ch2 = p + 1 < end ? b[p + 1] & 0xFF : 0;
                int ch3 = p + 2 < end ? b[p + 2] & 0xFF : 0;
                switch (ch >> 4) {
                    case 0xC, 0xD -> {                 // 110xxxxx 10xxxxxx
                        if ((ch2 & 0xC0) == 0x80) {
                            out[n++] = (char) (((ch & 0x1F) << 6) | (ch2 & 0x3F));
                            p += 2;
                            continue;
                        }
                    }
                    case 0xE -> {                      // 1110xxxx 10xxxxxx 10xxxxxx
                        if ((ch2 & 0xC0) == 0x80 && (ch3 & 0xC0) == 0x80) {
                            out[n++] = (char) (((ch & 0x0F) << 12) | ((ch2 & 0x3F) << 6) | (ch3 & 0x3F));
                            p += 3;
                            continue;
                        }
                    }
                    case 0x8, 0x9, 0xA, 0xB, 0xF -> {
                        // invalid lead byte: handled below like any other bad sequence
                    }
                    default -> {                       // 0xxxxxxx
                        out[n++] = (char) ch;
                        p++;
                        continue;
                    }
                }
                out[n++] = (char) ch;                  // bad sequence: the byte itself, advance by one
                p++;
            }
            return new String(out, 0, n);
        }

        @Override
        public boolean needsLegacyArabicReencode() {
            return true;
        }

        @Override
        public String name() {
            return JNI;
        }
    }

    static final class ForCharset extends FeedTextDecoder {
        private final Charset charset;

        ForCharset(Charset charset) {
            this.charset = charset;
        }

        @Override
        public String decode(byte[] b, int off, int len) {
            if (isAscii(b, off, off + len)) {
                return new String(b, off, len, StandardCharsets.ISO_8859_1);
            }
            return new String(b, off, len, charset);
        }

        @Override
        public boolean needsLegacyArabicReencode() {
            return false;
        }

        @Override
        public String name() {
            return charset.name();
        }
    }
}
