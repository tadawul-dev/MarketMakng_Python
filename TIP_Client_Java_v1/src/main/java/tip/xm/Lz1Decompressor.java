package tip.xm;

import java.util.Arrays;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Decompressor for the XMP "LZ1" algorithm (port of {@code xmdc.c}).
 *
 * <p>The 8 KiB history window is kept <b>between</b> calls, exactly like the C
 * implementation, so one instance must be used for the whole session and fed the
 * compressed frames in the order they arrived. Not thread-safe.
 */
public final class Lz1Decompressor {
    private static final Logger LOG = LogManager.getLogger("tip.xm.lz1");
    private static final int HISTORY_SIZE = 1 << 13;
    private static final int HISTORY_MASK = HISTORY_SIZE - 1;
    private static final int PHRASE_LENGTH_ADJUST = 3;
    private static final int PHRASE_OFFSET_COMMAND = HISTORY_SIZE;
    private static final int OPCODE_END_STREAM = 1;

    private static final int[] H_BITS = {1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 4, 4};
    private static final int[] D_BITS = {0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 3, 4};
    private static final int[] BASES = {0, 1, 3, 7, 15};
    /** Short literal table; entries 12..15 are 0, as in the C source. */
    private static final int[] LITERALS = new int[16];

    static {
        LITERALS[0] = 0;
        LITERALS[1] = ' ';
        for (int i = 0; i < 10; i++) LITERALS[2 + i] = '0' + i;
    }

    private final byte[] history = new byte[HISTORY_SIZE];
    private int histCurr;

    // per-call state
    private byte[] in;
    private int inPos;
    private int inEnd;
    private int bitData;
    private int numBits;
    private byte[] out;
    private int outPos;

    /** Decompresses one compressed frame body; returns the concatenated XMP frames it contains. */
    public byte[] decompress(byte[] data, int off, int len) throws XmException {
        if (len <= 0) throw new XmException(XmError.BAD_PARAM, "empty compressed body");
        in = data;
        inPos = off;
        inEnd = off + len;
        bitData = 0;
        numBits = 0;
        out = new byte[Xmp.MAX_MESSAGE_LENGTH];
        outPos = 0;
        try {
            inflate();
            return Arrays.copyOf(out, outPos);
        } catch (XmException e) {
            LOG.log(Level.WARN, "corrupt compressed block ({} bytes in, {} bytes out so far): {}", len, outPos, e.getMessage());
            throw e;
        } finally {
            in = null;
            out = null;
        }
    }

    private void inflate() throws XmException {
        for (;;) {
            int hdr;
            try {
                hdr = getBits(1);
            } catch (XmException e) {
                if (e.error() == XmError.DC_END_OF_FILE) throw new XmException(XmError.DC_BAD_BIT_STREAM);
                throw e;
            }
            if (hdr == 0) {
                literal();
            } else {
                int opcode = phrase();
                if (opcode == 0) continue;                      // ordinary phrase
                if (opcode == OPCODE_END_STREAM) return;
                throw new XmException(XmError.DC_BAD_OPCODE);
            }
        }
    }

    private void literal() throws XmException {
        int chr = literalRepr();
        putOctet(chr);
        history[histCurr] = (byte) chr;
        histCurr = (histCurr + 1) & HISTORY_MASK;
    }

    /** Returns 0 for an ordinary phrase, or the opcode of a command. */
    private int phrase() throws XmException {
        int start = histCurr;
        int xOffset = offsetRepr();
        int xLength = lengthRepr();
        if (xOffset == PHRASE_OFFSET_COMMAND) return xLength;

        int src = histCurr - xOffset + HISTORY_SIZE;
        if (src < 0) throw new XmException(XmError.DC_BAD_PHRASE_OFFSET);
        int length = xLength + PHRASE_LENGTH_ADJUST;

        // Forward byte-by-byte copy: an overlapping phrase repeats itself, like the C code.
        for (int i = 0; i < length; i++) {
            history[histCurr] = history[(src + i) & HISTORY_MASK];
            histCurr = (histCurr + 1) & HISTORY_MASK;
        }
        for (int i = 0; i < length; i++) putOctet(history[(start + i) & HISTORY_MASK]);
        return 0;
    }

    private int literalRepr() throws XmException {
        if (getBits(1) == 0) return LITERALS[getBits(4)];
        return getBits(8);
    }

    private int offsetRepr() throws XmException {
        if (getBits(1) == 0) return getBits(8);
        return getBits(13) + 256;
    }

    private int lengthRepr() throws XmException {
        int header = peekBits(4);
        getBits(H_BITS[header]);
        int dataBits = D_BITS[header];
        int data = dataBits == 0 ? 0 : getBits(dataBits);
        return BASES[dataBits] + data;
    }

    private void putOctet(int c) throws XmException {
        if (outPos >= out.length) throw new XmException(XmError.BUFFER_TOO_SMALL);
        out[outPos++] = (byte) c;
    }

    private void fill() {
        while (numBits <= 24 && inPos < inEnd) {
            bitData |= (in[inPos++] & 0xFF) << (24 - numBits);
            numBits += 8;
        }
    }

    /** Looks at the next {@code n} (1..24) bits; zero-padded if fewer remain, EOF only if none do. */
    private int peekBits(int n) throws XmException {
        if (numBits < n) {
            fill();
            if (numBits == 0) throw new XmException(XmError.DC_END_OF_FILE);
        }
        return bitData >>> (32 - n);
    }

    private int getBits(int n) throws XmException {
        if (numBits < n) {
            fill();
            if (numBits < n) throw new XmException(XmError.DC_END_OF_FILE);
        }
        int v = bitData >>> (32 - n);
        bitData <<= n;
        numBits -= n;
        return v;
    }
}
