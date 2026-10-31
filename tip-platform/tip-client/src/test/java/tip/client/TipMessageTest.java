package tip.client;

import static org.junit.Assert.*;

import java.io.IOException;
import org.junit.Test;

public class TipMessageTest {
    private static void rejects(String text) {
        try {
            TipMessage.parse(text);
            fail("should reject: " + text.replace("\n", "\\n"));
        } catch (TipParseException e) {
            assertNotNull(e.getMessage());
        }
    }

    // ------------------------------------------------------------------ building

    @Test public void buildsTheWireFormat() {
        String w = TipMessage.builder("LGn").add("UId", "alice").add("PWd", "pw").add("VEr", "1").toWire();
        assertEquals("LGn;UIdalice;PWdpw;VEr1;\n", w);
    }

    @Test public void numbersAreWrittenWithoutFormatting() {
        assertEquals("RTr;Sn1234567;En9999999999;QDAy20261031;\n",
                TipMessage.builder("RTr").add("Sn", 1_234_567).add("En", 9_999_999_999L).add("QDAy", 20261031).toWire());
        assertEquals("LGn;n-5;\n", TipMessage.builder("LGn").add("n", -5).toWire());
    }

    @Test public void messageWithoutFields() {
        assertEquals("LGf;\n", TipMessage.builder("LGf").toWire());
    }

    @Test public void nullAndEmptyValuesAreEmptyFields() {
        assertEquals("EOs;Qt;Rx;\n", TipMessage.builder("EOs").add("Qt", (String) null).add("Rx", "").toWire());
    }

    @Test public void delimiterBackslashAndNewlineAreEscaped() {
        assertEquals("LGn;Rxa\\;b\\\\c\\\nd;\n", TipMessage.builder("LGn").add("Rx", "a;b\\c\nd").toWire());
    }

    @Test public void fieldOrderIsPreserved() {
        TipMessage m = TipMessage.builder("RTr").add("n", 1).add("Sy", "a").add("n", 2).build();
        assertEquals("n", m.fields().get(0).tag());
        assertEquals("Sy", m.fields().get(1).tag());
        assertEquals("2", m.fields().get(2).value());
    }

    @Test public void tagRules() {
        // grammar: zero or more upper-case letters, then exactly one lower-case letter
        for (String ok : new String[] {"n", "Rc", "UId", "QDAy", "LGn", "ABCd"}) {
            TipMessage.builder("LGn").add(ok, "v");
        }
        for (String bad : new String[] {"", "A", "AB", "1a", "a1", "Ab1", "A b", "\u00c4", "nn", "aB", "N;", "Rx\n"}) {
            try {
                TipMessage.builder("LGn").add(bad, "v");
                fail("tag should be invalid: '" + bad + "'");
            } catch (IllegalArgumentException expected) {
                // ok
            }
        }
        for (String badType : new String[] {null, "", "LG", "1Gn", "lGn"}) {
            try {
                TipMessage.builder(badType);
                fail("type should be invalid: " + badType);
            } catch (IllegalArgumentException expected) {
                // ok
            }
        }
    }

    // ------------------------------------------------------------------ parsing

    @Test public void parsesTypeAndFields() throws Exception {
        TipMessage m = TipMessage.parse("GEn;Rc0;Rxlogged on;\n");
        assertEquals("GEn", m.type());
        assertEquals(2, m.fields().size());
        assertEquals("0", m.get("Rc").get());
        assertEquals("logged on", m.get("Rx").get());
        assertTrue(m.get("Zz").isEmpty());
    }

    @Test public void trailingNewlineIsOptional() throws Exception {
        assertEquals(TipMessage.parse("HBt;\n").toWire(), TipMessage.parse("HBt;").toWire());
    }

    @Test public void messageWithoutFieldsParses() throws Exception {
        TipMessage m = TipMessage.parse("HBt;\n");
        assertEquals("HBt", m.type());
        assertTrue(m.fields().isEmpty());
    }

    @Test public void emptyValuesAreKept() throws Exception {
        TipMessage m = TipMessage.parse("RTr;Qt;n5;\n");
        assertEquals("", m.get("Qt").get());
        assertEquals("5", m.get("n").get());
    }

    @Test public void escapesAreUndone() throws Exception {
        TipMessage m = TipMessage.parse("RTr;Rxa\\;b\\\\c;\n");
        assertEquals("a;b\\c", m.get("Rx").get());
    }

    @Test public void escapedNewlineInsideAValue() throws Exception {
        assertEquals("x\ny", TipMessage.parse("RTr;Rxx\\\ny;\n").get("Rx").get());
    }

    @Test public void duplicateTagsReturnTheFirst() throws Exception {
        TipMessage m = TipMessage.parse("RTr;n1;n2;\n");
        assertEquals("1", m.get("n").get());
        assertEquals(2, m.fields().size());
    }

    @Test public void unicodeValues() throws Exception {
        TipMessage m = TipMessage.parse("RTr;Nmمحمد €;\n");
        assertEquals("محمد €", m.get("Nm").get());
    }

    @Test public void longValue() throws Exception {
        String v = "x".repeat(10_000);
        assertEquals(v, TipMessage.parse("RTr;Rx" + v + ";\n").get("Rx").get());
    }

    @Test public void roundTripOfAwkwardValues() throws Exception {
        String[] values = {"", ";", "\\", "\n", ";;", "a\\;b", "\\\\", "tab\there", "م", ";\n;\\\n"};
        for (String v : values) {
            TipMessage built = TipMessage.builder("RTr").add("Rx", v).add("n", 1).build();
            TipMessage parsed = TipMessage.parse(built.toWire());
            assertEquals("value '" + v.replace("\n", "\\n") + "'", v, parsed.get("Rx").get());
            assertEquals("1", parsed.get("n").get());
            assertEquals(built.toWire(), parsed.toWire());
        }
    }

    @Test public void malformedMessagesAreRejected() {
        rejects("");
        rejects("\n");
        rejects("RTr");                  // no delimiter after the type
        rejects("RTR;");                 // type without lower-case terminator
        rejects("RTr;n12");              // value not terminated
        rejects("RTr;n12\n");            // same, with newline
        rejects("RTr;1x;");              // tag starts with a digit
        rejects("RTr;R x;");             // space in tag
        rejects("RTr;Rxabc\\");          // dangling escape
        rejects("RTr;Rxa\nb;\n");        // raw newline inside a value
        rejects("RTr;n1;\n\n");          // second newline
        rejects("RTr;N");                // tag cut short
        rejects(";n1;");                 // missing type
    }

    @Test public void parseExceptionIsAnIoException() {
        assertTrue(new TipParseException("x") instanceof IOException);
    }

    // ------------------------------------------------------------------ toString (used by logging)

    @Test public void toStringMasksThePassword() {
        TipMessage m = TipMessage.builder("LGn").add("UId", "alice").add("PWd", "s3cret").build();
        String s = m.toString();
        assertEquals("LGn;UIdalice;PWd***;", s);
        assertFalse(s.contains("s3cret"));
        assertTrue("toWire still has the real value", m.toWire().contains("s3cret"));
    }

    @Test public void toStringOfOrdinaryMessageIsReadable() throws Exception {
        assertEquals("RTr;n7;Sy1010;", TipMessage.parse("RTr;n7;Sy1010;\n").toString());
    }

    @Test public void toStringKeepsOneLine() {
        assertFalse(TipMessage.builder("RTr").add("Rx", "a\nb").build().toString().contains("\n"));
    }

    @Test public void maskingAppliesToParsedMessagesToo() throws Exception {
        assertFalse(TipMessage.parse("LGn;UIda;PWdtopsecret;\n").toString().contains("topsecret"));
    }

    @Test public void tagConstantsMatchTheCHeader() {
        assertEquals("LGn", TipTags.LOGON);
        assertEquals("LGf", TipTags.LOGOFF);
        assertEquals("EOs", TipTags.END_OF_STREAM);
        assertEquals("GEn", TipTags.GENERAL_REPLY);
        assertEquals("HBt", TipTags.HEARTBEAT);
        assertEquals("RTr", TipTags.REALTIME);
        assertEquals("BDa", TipTags.BASIC_DATA);
        assertEquals("UId", TipTags.USER_ID);
        assertEquals("PWd", TipTags.PASSWORD);
        assertEquals("VEr", TipTags.PROTOCOL_VERSION);
        assertEquals("Rc", TipTags.REPLY_CODE);
        assertEquals("Rx", TipTags.REPLY_TEXT);
        assertEquals("n", TipTags.SEQUENCE_NUMBER);
        assertEquals("QDAy", TipTags.REQUEST_DATE);
        assertEquals("QSCo", TipTags.SCOPE);
        assertEquals("Sn", TipTags.START_SEQ_NO);
        assertEquals("En", TipTags.END_SEQ_NO);
        assertEquals("Qt", TipTags.REQUEST_TYPE);
        assertEquals(';', TipTags.DELIMITER);
        assertEquals('\n', TipTags.END_OF_MESSAGE);
        assertEquals('\\', TipTags.ESCAPE);
    }
}
