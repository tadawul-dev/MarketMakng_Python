package tip.xm;

import static org.junit.Assert.*;
import static tip.testutil.TestData.bytes;

import org.junit.Test;

public class ExtHeaderTest {
    @Test public void emptyHeader() {
        assertTrue(ExtHeader.EMPTY.isEmpty());
        assertEquals(0, ExtHeader.EMPTY.encodedLength());
        assertEquals(0, ExtHeader.EMPTY.encode().length);
        assertTrue(ExtHeader.EMPTY.find(1).isEmpty());
    }

    @Test public void entriesEncodeAsTagLengthData() {
        ExtHeader h = ExtHeader.EMPTY.withByte(Xmp.Tag.COMPRESSION_ALGORITHM, 1).withInt(Xmp.Tag.QUEUE_ALERT, 500);
        assertEquals(3 + 6, h.encodedLength());
        assertArrayEquals(bytes(7, 1, 1, 6, 4, 0, 0, 1, 0xF4), h.encode());
    }

    @Test public void withIsImmutable() {
        ExtHeader a = ExtHeader.EMPTY.withByte(1, 1);
        ExtHeader b = a.withByte(2, 2);
        assertEquals(1, a.entries().size());
        assertEquals(2, b.entries().size());
        assertEquals(0, ExtHeader.EMPTY.entries().size());
        try {
            a.entries().add(new ExtHeader.Entry(3, new byte[0]));
            fail("entries() must be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // ok
        }
    }

    @Test public void findReturnsTheFirstEntryWithThatTag() {
        ExtHeader h = ExtHeader.EMPTY.withByte(9, 1).withByte(9, 2).withByte(4, 3);
        assertEquals(1, h.find(9).get().data()[0]);
        assertEquals(3, h.find(4).get().data()[0]);
        assertTrue(h.find(5).isEmpty());
    }

    @Test public void intValueIsBigEndianAndSigned() {
        assertEquals(0x01020304, ExtHeader.EMPTY.withInt(1, 0x01020304).find(1).get().intValue());
        assertEquals(-2, ExtHeader.EMPTY.withInt(1, -2).find(1).get().intValue());
        assertEquals(0x80000000, ExtHeader.EMPTY.withInt(1, 0x80000000).find(1).get().intValue());
    }

    @Test(expected = IllegalStateException.class)
    public void intValueNeedsFourBytes() {
        ExtHeader.EMPTY.withByte(1, 5).find(1).get().intValue();
    }

    @Test public void entryDataIsDefensivelyCopied() {
        byte[] src = {1, 2, 3};
        ExtHeader h = ExtHeader.EMPTY.with(1, src);
        src[0] = 99;
        assertEquals(1, h.find(1).get().data()[0]);
        h.find(1).get().data()[1] = 99;
        assertEquals(2, h.find(1).get().data()[1]);
    }

    @Test public void zeroLengthEntryIsAllowed() {
        ExtHeader h = ExtHeader.EMPTY.with(Xmp.Tag.TIMESTAMP, new byte[0]);
        assertEquals(2, h.encodedLength());
        assertArrayEquals(bytes(4, 0), h.encode());
        assertEquals(0, h.find(4).get().data().length);
    }

    @Test public void limitIs255Bytes() {
        ExtHeader h = ExtHeader.EMPTY.with(1, new byte[253]);          // 2 + 253 = 255, exactly full
        assertEquals(255, h.encodedLength());
        try {
            h.with(2, new byte[0]);
            fail("must not exceed 255 bytes");
        } catch (IllegalArgumentException expected) {
            // ok
        }
        try {
            ExtHeader.EMPTY.with(1, new byte[254]);
            fail("254 data bytes do not fit");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }

    @Test public void decodeRoundTrip() {
        ExtHeader h = ExtHeader.EMPTY.withByte(9, 3).withInt(6, 700).with(4, new byte[0]);
        byte[] enc = h.encode();
        ExtHeader d = ExtHeader.decode(enc, 0, enc.length);
        assertEquals(3, d.entries().size());
        assertEquals(3, d.find(9).get().data()[0]);
        assertEquals(700, d.find(6).get().intValue());
        assertEquals(0, d.find(4).get().data().length);
    }

    @Test public void decodeHonoursOffsetAndLength() {
        byte[] buf = bytes(0xEE, 0xEE, 9, 1, 2, 0xEE);
        ExtHeader d = ExtHeader.decode(buf, 2, 3);
        assertEquals(1, d.entries().size());
        assertEquals(2, d.find(9).get().data()[0]);
    }

    @Test public void incompleteTrailingEntryIsSkippedLikeInC() {
        // second entry claims 4 data bytes but only 2 are present
        byte[] buf = bytes(9, 1, 3, 6, 4, 0, 1);
        ExtHeader d = ExtHeader.decode(buf, 0, buf.length);
        assertEquals(1, d.entries().size());
        assertTrue(d.find(6).isEmpty());
    }

    @Test public void strayTrailingByteIsIgnored() {
        byte[] buf = bytes(9, 1, 3, 7);
        assertEquals(1, ExtHeader.decode(buf, 0, buf.length).entries().size());
    }

    @Test public void decodeOfNothing() {
        assertTrue(ExtHeader.decode(new byte[0], 0, 0).isEmpty());
        assertTrue(ExtHeader.decode(bytes(5), 0, 1).isEmpty());
    }

    @Test public void tagByteIsUnsigned() {
        ExtHeader d = ExtHeader.decode(bytes(0xFF, 1, 7), 0, 3);
        assertEquals(255, d.entries().get(0).tag());
    }
}
