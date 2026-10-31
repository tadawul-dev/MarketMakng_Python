package tip.xm;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

public class XmErrorTest {
    @Test public void codesAreUniqueAndNonPositive() {
        Set<Integer> seen = new HashSet<>();
        for (XmError e : XmError.values()) {
            assertTrue(e + " code", e.code() <= 0);
            assertTrue("duplicate code " + e.code(), seen.add(e.code()));
        }
    }

    @Test public void valuesMatchTheCHeader() {
        assertEquals(0, XmError.NO_ERROR.code());
        assertEquals(-1, XmError.NO_MEMORY.code());
        assertEquals(-2, XmError.BAD_PARAM.code());
        assertEquals(-3, XmError.BAD_LENGTH.code());
        assertEquals(-4, XmError.NO_ROOM.code());
        assertEquals(-5, XmError.NO_SERVICE_ENTRIES.code());
        assertEquals(-6, XmError.NOT_SUPPORTED.code());
        assertEquals(-8, XmError.NETWORK_ERROR.code());
        assertEquals(-9, XmError.TIMEOUT.code());
        assertEquals(-10, XmError.BUFFER_TOO_SMALL.code());
        assertEquals(-11, XmError.INTERNAL_ERROR.code());
        assertEquals(-12, XmError.NOT_INITIALIZED.code());
        assertEquals(-13, XmError.NO_MORE_ENTRIES.code());
        assertEquals(-14, XmError.DC_END_OF_FILE.code());
        assertEquals(-15, XmError.DC_BAD_SIZE.code());
        assertEquals(-16, XmError.DC_BAD_BIT_STREAM.code());
        assertEquals(-17, XmError.DC_BAD_OPCODE.code());
        assertEquals(-18, XmError.DC_BAD_HEADER_TYPE.code());
        assertEquals(-19, XmError.DC_BAD_PHRASE_LENGTH.code());
        assertEquals(-20, XmError.DC_BAD_PHRASE_OFFSET.code());
        assertEquals(-21, XmError.BAD_ALGORITHM.code());
        assertEquals(-22, XmError.BAD_KEY_LENGTH.code());
        assertEquals(-23, XmError.WEAK_KEY.code());
    }

    @Test public void messagesAreNeverBlank() {
        for (XmError e : XmError.values()) assertFalse(e.name(), e.message().isBlank());
        assertEquals("Operation timed out", XmError.TIMEOUT.message());
    }

    @Test public void exceptionCarriesCodeAndDetail() {
        XmException plain = new XmException(XmError.TIMEOUT);
        assertEquals(XmError.TIMEOUT, plain.error());
        assertEquals("Operation timed out", plain.getMessage());

        XmException detailed = new XmException(XmError.NETWORK_ERROR, "boom");
        assertEquals("Network error: boom", detailed.getMessage());

        Throwable cause = new RuntimeException("x");
        XmException withCause = new XmException(XmError.INTERNAL_ERROR, "d", cause);
        assertSame(cause, withCause.getCause());
        assertTrue(withCause instanceof java.io.IOException);
    }

    @Test public void sessionStateMapping() {
        assertEquals(Xmp.SessionState.NONE, Xmp.SessionState.of(0));
        assertEquals(Xmp.SessionState.OFFLINE, Xmp.SessionState.of(1));
        assertEquals(Xmp.SessionState.CATCHUP, Xmp.SessionState.of(2));
        assertEquals(Xmp.SessionState.ONLINE, Xmp.SessionState.of(3));
        assertEquals(Xmp.SessionState.UNKNOWN, Xmp.SessionState.of(4));
        assertEquals(Xmp.SessionState.UNKNOWN, Xmp.SessionState.of(-1));
    }

    @Test public void protocolConstantsMatchTheCHeader() {
        assertEquals(4, Xmp.HEADER_SIZE);
        assertEquals(3072, Xmp.MAX_MESSAGE_LENGTH);
        assertEquals(255, Xmp.MAX_EXT_HDR_SIZE);
        assertEquals(16, Xmp.MAX_CRYPT_KEY_SIZE);
        assertEquals(0x05, Xmp.Type.BINARY_RECORD);
        assertEquals(0x07, Xmp.Type.KEY_EXCHANGE);
        assertEquals(0x81, Xmp.Type.ENCRYPTED);
        assertEquals(0x82, Xmp.Type.COMPRESSED);
        assertEquals(4, Xmp.Tag.TIMESTAMP);
        assertEquals(5, Xmp.Tag.KEY_ID);
        assertEquals(6, Xmp.Tag.QUEUE_ALERT);
        assertEquals(7, Xmp.Tag.COMPRESSION_ALGORITHM);
        assertEquals(8, Xmp.Tag.ENCRYPTION_ALGORITHM);
        assertEquals(9, Xmp.Tag.SESSION_STATE);
    }
}
