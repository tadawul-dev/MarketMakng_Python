package tip.xm;

import static org.junit.Assert.*;
import static tip.testutil.TestData.bytes;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.Test;

public class XmpFrameTest {
    private static byte[] ascii(String s) { return s.getBytes(StandardCharsets.US_ASCII); }

    private static XmError encodeError(XmpFrame f) {
        try {
            f.encode();
            fail("expected XmException");
            return null;
        } catch (XmException e) {
            return e.error();
        }
    }

    @Test public void headerLayout() throws Exception {
        XmpFrame f = new XmpFrame(Xmp.Type.BINARY_RECORD, ExtHeader.EMPTY.withByte(1, 1), ascii("AB"));
        assertArrayEquals(bytes(5, 3, 0, 2, 1, 1, 1, 'A', 'B'), f.encode());
        assertEquals(9, f.encodedLength());
    }

    @Test public void bodyLengthIsTwoBytesBigEndian() throws Exception {
        byte[] enc = new XmpFrame(5, new byte[0x1234]).encode();
        assertEquals(0x12, enc[2] & 0xFF);
        assertEquals(0x34, enc[3] & 0xFF);
        assertEquals(4 + 0x1234, enc.length);
        assertEquals(enc.length, XmpFrame.frameLength(enc, 0));
    }

    @Test public void largestBodyIs65535() throws Exception {
        assertEquals(4 + 65535, new XmpFrame(5, new byte[65535]).encode().length);
        assertEquals(XmError.BAD_PARAM, encodeError(new XmpFrame(5, new byte[65536])));
    }

    @Test public void emptyBodyOnlyAllowedForTypeNone() throws Exception {
        byte[] ok = new XmpFrame(Xmp.Type.NONE, ExtHeader.EMPTY.with(4, new byte[0]), null).encode();
        assertArrayEquals(bytes(0, 2, 0, 0, 4, 0), ok);
        assertEquals(XmError.BAD_LENGTH, encodeError(new XmpFrame(Xmp.Type.BINARY_RECORD, new byte[0])));
        assertEquals(XmError.BAD_LENGTH, encodeError(new XmpFrame(Xmp.Type.ENCRYPTED, null)));
    }

    @Test public void typeMustFitInOneByte() {
        assertEquals(XmError.BAD_PARAM, encodeError(new XmpFrame(-1, new byte[1])));
        assertEquals(XmError.BAD_PARAM, encodeError(new XmpFrame(256, new byte[1])));
    }

    @Test public void nullBodyBecomesEmpty() {
        assertEquals(0, new XmpFrame(0, null).body().length);
        assertEquals(0, new XmpFrame(0, ExtHeader.EMPTY, null).body().length);
    }

    @Test(expected = NullPointerException.class)
    public void nullExtensionHeaderIsRejected() {
        new XmpFrame(5, null, new byte[1]);
    }

    @Test public void decodeRoundTrip() throws Exception {
        XmpFrame f = new XmpFrame(0x82, ExtHeader.EMPTY.withInt(6, 9).withByte(9, 2), ascii("hello world"));
        byte[] enc = f.encode();
        XmpFrame d = XmpFrame.decode(enc, 0, enc.length);
        assertEquals(0x82, d.type());
        assertArrayEquals(ascii("hello world"), d.body());
        assertEquals(9, d.ext().find(6).get().intValue());
        assertEquals(2, d.ext().find(9).get().data()[0]);
    }

    @Test public void decodeAtOffsetIgnoresTrailingBytes() throws Exception {
        byte[] one = new XmpFrame(5, ascii("AB")).encode();
        byte[] buf = new byte[3 + one.length + 5];            // junk before and after (cipher padding)
        Arrays.fill(buf, (byte) 0x7E);
        System.arraycopy(one, 0, buf, 3, one.length);
        XmpFrame d = XmpFrame.decode(buf, 3, buf.length - 3);
        assertArrayEquals(ascii("AB"), d.body());
    }

    @Test public void decodeRejectsTruncatedInput() {
        for (byte[] bad : new byte[][] {new byte[0], bytes(5, 0, 0), bytes(5, 0, 0, 5, 'A', 'B'), bytes(5, 3, 0, 0, 1)}) {
            try {
                XmpFrame.decode(bad, 0, bad.length);
                fail("should reject " + Arrays.toString(bad));
            } catch (XmException e) {
                assertEquals(XmError.BAD_LENGTH, e.error());
            }
        }
    }

    @Test public void frameLengthCountsHeaderExtensionAndBody() {
        assertEquals(4, XmpFrame.frameLength(bytes(0, 0, 0, 0), 0));
        assertEquals(4 + 255 + 65535, XmpFrame.frameLength(bytes(5, 255, 255, 255), 0));
        assertEquals(10, XmpFrame.frameLength(bytes(9, 9, 5, 0, 0, 4, 0, 2, 0, 0), 4));   // read at offset 4: ext 4 + body 2 + header 4
    }
}
