package tip.testutil;

import java.util.ArrayList;
import java.util.List;

/** Minimal encoder for the XMP "LZ1" bit stream, used to build compressed test input. */
public final class Lz1Writer {
    private final List<Integer> bits = new ArrayList<>();

    private void put(int value, int n) {
        for (int i = n - 1; i >= 0; i--) bits.add((value >>> i) & 1);
    }

    /** A literal byte, always in the 8-bit form. */
    public Lz1Writer literal(int b) {
        put(0, 1);        // header: literal
        put(1, 1);        // long form
        put(b & 0xFF, 8);
        return this;
    }

    /** A literal using the 4-bit short form (NUL, space, '0'..'9'); falls back to long form. */
    public Lz1Writer shortLiteral(int b) {
        int code = b == 0 ? 0 : b == ' ' ? 1 : (b >= '0' && b <= '9') ? 2 + (b - '0') : -1;
        if (code < 0) return literal(b);
        put(0, 1);
        put(0, 1);
        put(code, 4);
        return this;
    }

    public Lz1Writer literals(byte[] data) {
        for (byte b : data) literal(b);
        return this;
    }

    /** Copy {@code length} (3..33) bytes starting {@code distance} bytes back in the history window. */
    public Lz1Writer phrase(int distance, int length) {
        put(1, 1);
        offset(distance);
        len(length - 3);
        return this;
    }

    /** Raw offset/length pair, e.g. a command with offset 8192. */
    public Lz1Writer command(int offsetValue, int lengthValue) {
        put(1, 1);
        offset(offsetValue);
        len(lengthValue);
        return this;
    }

    public Lz1Writer end() {
        return command(8192, 1);
    }

    private void offset(int o) {
        if (o < 256) {
            put(0, 1);
            put(o, 8);
        } else {
            put(1, 1);
            put(o - 256, 13);
        }
    }

    private void len(int l) {
        if (l == 0) put(0, 1);
        else if (l <= 2) { put(0b10, 2); put(l - 1, 1); }
        else if (l <= 6) { put(0b110, 3); put(l - 3, 2); }
        else if (l <= 14) { put(0b1110, 4); put(l - 7, 3); }
        else { put(0b1111, 4); put(l - 15, 4); }
    }

    public byte[] toBytes() {
        int n = (bits.size() + 7) / 8;
        byte[] out = new byte[n];
        for (int i = 0; i < bits.size(); i++) if (bits.get(i) == 1) out[i / 8] |= (byte) (0x80 >>> (i % 8));
        return out;
    }

    /** Compresses (using only literals) a sequence of whole XMP frames into one block, ended properly. */
    public static byte[] literalBlock(byte[]... frames) {
        Lz1Writer w = new Lz1Writer();
        for (byte[] f : frames) w.literals(f);
        return w.end().toBytes();
    }
}
