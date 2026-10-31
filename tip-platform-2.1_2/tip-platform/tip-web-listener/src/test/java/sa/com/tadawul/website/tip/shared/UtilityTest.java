package sa.com.tadawul.website.tip.shared;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.text.SimpleDateFormat;
import java.util.Random;

import org.junit.jupiter.api.Test;

/** Proves the optimized helpers are drop-in equivalents of the legacy code they replace. */
class UtilityTest {

    private static final String LEGACY_SPLITVALUE = "[a-z]";

    @Test
    void splitTagValueMatchesLegacyRegexOnTypicalFields() {
        String[] samples = { "Tp12.5", "Si1010", "n42", "DDn-1.25", "ABC", "", "a", "Z", "Tx093015",
                "Aq1234567", "LDs", "Vc11234.56", "OSt6", "NAlالراجحي",
                "XYZ9a9b", "12ab", "Aéb", "Ab" };
        for (String s : samples) {
            assertArrayEquals(s.split(LEGACY_SPLITVALUE, 2), Utility.splitTagValue(s), "field: " + s);
        }
    }

    @Test
    void splitTagValueMatchesLegacyRegexOnRandomInput() {
        Random rnd = new Random(20261031L);
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789.-;: éا";
        for (int i = 0; i < 200_000; i++) {
            int len = rnd.nextInt(12);
            StringBuilder sb = new StringBuilder(len);
            for (int j = 0; j < len; j++) {
                sb.append(alphabet.charAt(rnd.nextInt(alphabet.length())));
            }
            String s = sb.toString();
            assertArrayEquals(s.split(LEGACY_SPLITVALUE, 2), Utility.splitTagValue(s), "field: " + s);
        }
    }

    @Test
    void formatTimeMatchesLegacy() {
        assertEquals("09:30:15", Utility.formatTime("093015"));
        assertEquals("15:00:00", Utility.formatTime("150000123"));
        assertThrows(StringIndexOutOfBoundsException.class, () -> Utility.formatTime("0930"));
    }

    @Test
    void appendTxWithTodayUsesFeedDate() {
        String saved = TipConstants.DB_FEED_REQ_DATE;
        try {
            TipConstants.DB_FEED_REQ_DATE = "2026-10-31 ";
            assertEquals("2026-10-31 10:01:02", Utility.appendTxWithToday("100102"));
            // must still be parseable exactly as before
            assertEquals("2026-10-31 10:01:02.0", java.sql.Timestamp.valueOf(Utility.appendTxWithToday("100102")).toString());
        } finally {
            TipConstants.DB_FEED_REQ_DATE = saved;
        }
    }

    @Test
    void formatDate() {
        assertEquals("2026-10-31", Utility.formatDate("20261031"));
    }

    @Test
    void threadSafeDateFormatKeepsLegacyLenientSemantics() throws Exception {
        ThreadSafeDateFormat f = new ThreadSafeDateFormat("yyyyMMdd");
        SimpleDateFormat legacy = new SimpleDateFormat("yyyyMMdd");
        for (String s : new String[] { "20261031", "20250132", "20240229", "20231301" }) {
            assertEquals(legacy.parse(s), f.parse(s), s);
        }
    }

    @Test
    void threadSafeDateFormatUnderConcurrency() throws Exception {
        ThreadSafeDateFormat f = new ThreadSafeDateFormat("yyyyMMdd");
        SimpleDateFormat reference = new SimpleDateFormat("yyyyMMdd");
        String[] inputs = { "20200101", "20211231", "20260615", "19991231" };
        long[] expected = new long[inputs.length];
        for (int i = 0; i < inputs.length; i++) {
            expected[i] = reference.parse(inputs[i]).getTime();
        }
        Thread[] threads = new Thread[8];
        java.util.concurrent.atomic.AtomicInteger errors = new java.util.concurrent.atomic.AtomicInteger();
        for (int t = 0; t < threads.length; t++) {
            final int offset = t;
            threads[t] = new Thread(() -> {
                try {
                    for (int i = 0; i < 20_000; i++) {
                        int k = (i + offset) % inputs.length;
                        if (f.parse(inputs[k]).getTime() != expected[k]) {
                            errors.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                }
            });
            threads[t].start();
        }
        for (Thread t : threads) {
            t.join();
        }
        assertEquals(0, errors.get());
    }
}
