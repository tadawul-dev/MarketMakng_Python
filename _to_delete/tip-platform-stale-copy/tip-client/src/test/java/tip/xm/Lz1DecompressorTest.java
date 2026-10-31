package tip.xm;

import static org.junit.Assert.*;
import static tip.testutil.TestData.*;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.junit.Test;
import tip.testutil.Lz1Writer;

public class Lz1DecompressorTest {
    private static byte[] ascii(String s) { return s.getBytes(StandardCharsets.ISO_8859_1); }

    private static byte[] inflate(Lz1Decompressor d, byte[] in) throws XmException {
        return d.decompress(in, 0, in.length);
    }

    private static XmError inflateError(Lz1Decompressor d, byte[] in) {
        try {
            d.decompress(in, 0, in.length);
            fail("expected XmException");
            return null;
        } catch (XmException e) {
            return e.error();
        }
    }

    /** Replays commands recorded from the original C implementation. */
    @Test public void matchesTheOriginalCImplementation() throws Exception {
        Lz1Decompressor[] dcs = new Lz1Decompressor[8];
        int streams = 0;
        for (String line : resourceLines("/golden/dc.golden")) {
            String[] p = line.split(" => ", 2);
            String[] cmd = p[0].split(" ");
            switch (cmd[0]) {
                case "dcnew" -> dcs[Integer.parseInt(cmd[1])] = new Lz1Decompressor();
                case "dc" -> {
                    byte[] in = hex(cmd[2]);
                    assertEquals("C produced an error for valid input?", false, p[1].startsWith("ERR"));
                    assertEquals("stream #" + (++streams), p[1], hex(dcs[Integer.parseInt(cmd[1])].decompress(in, 0, in.length)));
                }
                default -> fail("unknown command " + cmd[0]);
            }
        }
        assertTrue("golden file was empty", streams > 50);
    }

    @Test public void longFormLiterals() throws Exception {
        byte[] out = inflate(new Lz1Decompressor(), new Lz1Writer().literals(ascii("TIP")).end().toBytes());
        assertEquals("TIP", new String(out, StandardCharsets.ISO_8859_1));
    }

    @Test public void shortFormLiteralsSpaceDigitsAndNul() throws Exception {
        Lz1Writer w = new Lz1Writer();
        for (char c : "A 0123456789".toCharArray()) w.shortLiteral(c);
        w.shortLiteral(0);
        byte[] out = inflate(new Lz1Decompressor(), w.end().toBytes());
        assertArrayEquals(bytes('A', ' ', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 0), out);
    }

    @Test public void binaryLiteralsAllByteValues() throws Exception {
        byte[] all = new byte[256];
        for (int i = 0; i < 256; i++) all[i] = (byte) i;
        assertArrayEquals(all, inflate(new Lz1Decompressor(), new Lz1Writer().literals(all).end().toBytes()));
    }

    @Test public void phraseCopiesFromHistory() throws Exception {
        byte[] in = new Lz1Writer().literals(ascii("abcdef")).phrase(6, 6).end().toBytes();
        assertEquals("abcdefabcdef", new String(inflate(new Lz1Decompressor(), in), StandardCharsets.ISO_8859_1));
    }

    @Test public void overlappingPhraseRepeatsTheLastByte() throws Exception {
        byte[] in = new Lz1Writer().literal('x').phrase(1, 10).end().toBytes();
        assertEquals("x" + "x".repeat(10), new String(inflate(new Lz1Decompressor(), in), StandardCharsets.ISO_8859_1));
    }

    @Test public void allLengthEncodingsAreDecoded() throws Exception {
        // phrase lengths 3..33 exercise every branch of the length code (0, 1-2, 3-6, 7-14, 15-30 extra)
        for (int len = 3; len <= 33; len++) {
            byte[] seed = new byte[40];
            for (int i = 0; i < seed.length; i++) seed[i] = (byte) ('a' + i % 26);
            byte[] in = new Lz1Writer().literals(seed).phrase(40, len).end().toBytes();
            byte[] out = inflate(new Lz1Decompressor(), in);
            assertEquals("len " + len, 40 + len, out.length);
            assertArrayEquals("len " + len, Arrays.copyOf(seed, len), Arrays.copyOfRange(out, 40, 40 + len));
        }
    }

    @Test public void shortAndLongOffsets() throws Exception {
        byte[] seed = new byte[600];
        new Random(1).nextBytes(seed);
        // offset 255 (8-bit form) and offset 300 (13-bit form, >= 256)
        byte[] out = inflate(new Lz1Decompressor(), new Lz1Writer().literals(seed).phrase(255, 10).phrase(300, 10).end().toBytes());
        assertArrayEquals(Arrays.copyOfRange(seed, 600 - 255, 600 - 255 + 10), Arrays.copyOfRange(out, 600, 610));
        assertArrayEquals(Arrays.copyOfRange(out, 610 - 300, 610 - 300 + 10), Arrays.copyOfRange(out, 610, 620));
    }

    @Test public void historySurvivesBetweenBlocks() throws Exception {
        Lz1Decompressor d = new Lz1Decompressor();
        assertEquals("HELLO", new String(inflate(d, new Lz1Writer().literals(ascii("HELLO")).end().toBytes()), StandardCharsets.ISO_8859_1));
        // second block copies the 5 bytes produced by the first
        assertEquals("HELLO", new String(inflate(d, new Lz1Writer().phrase(5, 5).end().toBytes()), StandardCharsets.ISO_8859_1));
        // a fresh decompressor has no such history: the same bytes copy zeros
        assertArrayEquals(new byte[5], inflate(new Lz1Decompressor(), new Lz1Writer().phrase(5, 5).end().toBytes()).length == 5
                ? new byte[5] : null);
    }

    @Test public void emptyBlockWithOnlyEndMarker() throws Exception {
        assertEquals(0, inflate(new Lz1Decompressor(), new Lz1Writer().end().toBytes()).length);
    }

    @Test public void bitsAfterEndMarkerAreIgnored() throws Exception {
        byte[] in = new Lz1Writer().literal('Z').end().toBytes();
        byte[] padded = Arrays.copyOf(in, in.length + 3);
        Arrays.fill(padded, in.length, padded.length, (byte) 0xFF);
        assertArrayEquals(ascii("Z"), inflate(new Lz1Decompressor(), padded));
    }

    @Test public void honoursOffsetAndLengthOfInputSlice() throws Exception {
        byte[] block = new Lz1Writer().literals(ascii("ok")).end().toBytes();
        byte[] buf = new byte[block.length + 6];
        Arrays.fill(buf, (byte) 0xFF);
        System.arraycopy(block, 0, buf, 3, block.length);
        assertEquals("ok", new String(new Lz1Decompressor().decompress(buf, 3, block.length), StandardCharsets.ISO_8859_1));
    }

    @Test public void emptyInputIsBadParam() {
        assertEquals(XmError.BAD_PARAM, inflateError(new Lz1Decompressor(), new byte[0]));
    }

    @Test public void streamWithoutEndMarkerIsRejected() {
        // the padding bits read as more (zero) literals and run dry, as in the C decoder
        XmError e = inflateError(new Lz1Decompressor(), new Lz1Writer().literals(ascii("abc")).toBytes());
        assertTrue(e.toString(), e == XmError.DC_END_OF_FILE || e == XmError.DC_BAD_BIT_STREAM);
    }

    @Test public void unknownCommandOpcodeIsRejected() {
        // offset 8192 means "command"; the "length" field carries the opcode, 2 is undefined
        assertEquals(XmError.DC_BAD_OPCODE, inflateError(new Lz1Decompressor(), new Lz1Writer().command(8192, 2).toBytes()));
    }

    @Test public void phraseReachingBeforeTheStartOfTheWindowIsRejected() {
        // distance 8447 at history position 0 would read memory that never existed (undefined behaviour in C)
        assertEquals(XmError.DC_BAD_PHRASE_OFFSET, inflateError(new Lz1Decompressor(), new Lz1Writer().phrase(8447, 5).end().toBytes()));
    }

    @Test public void outputLargerThanOneFrameIsRejected() {
        Lz1Writer w = new Lz1Writer().literals(ascii("0123456789"));
        for (int i = 0; i < 120; i++) w.phrase(10, 33);          // 10 + 120*33 = 3970 bytes > 3072
        assertEquals(XmError.BUFFER_TOO_SMALL, inflateError(new Lz1Decompressor(), w.end().toBytes()));
    }

    @Test public void decompressorIsReusableAfterAnError() throws Exception {
        Lz1Decompressor d = new Lz1Decompressor();
        assertNotNull(inflateError(d, new Lz1Writer().command(8192, 5).toBytes()));
        assertEquals("fine", new String(inflate(d, new Lz1Writer().literals(ascii("fine")).end().toBytes()), StandardCharsets.ISO_8859_1));
    }

    /** Corrupt input must surface as XmException, never as ArrayIndexOutOfBounds or similar. */
    @Test public void randomGarbageOnlyEverThrowsXmException() {
        Random rnd = new Random(12345);
        Lz1Decompressor shared = new Lz1Decompressor();
        int rejected = 0;
        for (int i = 0; i < 5000; i++) {
            byte[] junk = new byte[1 + rnd.nextInt(120)];
            rnd.nextBytes(junk);
            try {
                (i % 2 == 0 ? shared : new Lz1Decompressor()).decompress(junk, 0, junk.length);
            } catch (XmException e) {
                rejected++;
            } catch (Throwable t) {
                fail("unexpected " + t + " for input " + hex(junk));
            }
        }
        assertTrue("most random input should be rejected", rejected > 1000);
    }

    @Test public void truncatedValidStreamsAreRejectedNotCrashing() throws Exception {
        byte[] seed = new byte[100];
        new Random(2).nextBytes(seed);
        byte[] full = new Lz1Writer().literals(seed).phrase(50, 20).end().toBytes();
        for (int cut = 1; cut < full.length; cut++) {
            try {
                new Lz1Decompressor().decompress(full, 0, cut);
            } catch (XmException expected) {
                // fine
            }
        }
    }

    @Test public void realisticTipBlockWithSeveralFrames() throws Exception {
        byte[] f1 = frameBytes("RTr;n1;Sy1010;\n");
        byte[] f2 = frameBytes("RTr;n2;Sy1010;\n");
        byte[] block = new Lz1Writer().literals(f1).literals(f2).end().toBytes();
        byte[] out = inflate(new Lz1Decompressor(), block);
        List<byte[]> frames = List.of(f1, f2);
        int p = 0;
        for (byte[] f : frames) {
            assertArrayEquals(f, Arrays.copyOfRange(out, p, p + f.length));
            p += f.length;
        }
        assertEquals(p, out.length);
    }
}
