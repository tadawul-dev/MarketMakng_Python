package sa.com.tadawul.website.tip.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class SymbolMapTest {

    @Test
    void behavesLikeHashMapIncludingNulls() {
        Map<String, Integer> legacy = new HashMap<>();
        Map<String, Integer> map = new SymbolMap<>();
        for (Map<String, Integer> m : java.util.List.of(legacy, map)) {
            m.put("1120", 3);
            m.put(null, 7);
            m.put("2222", null);
        }
        for (String k : new String[] { "1120", null, "2222", "none" }) {
            assertEquals(legacy.get(k), map.get(k), "get " + k);
            assertEquals(legacy.containsKey(k), map.containsKey(k), "containsKey " + k);
        }
        assertEquals(legacy.size(), map.size());
        assertEquals(legacy.remove(null), map.remove(null));
        assertNull(map.get(null));
        assertFalse(map.containsKey(null));
        assertEquals(legacy, map);
    }

    @Test
    void concurrentWriterAndReaders() throws Exception {
        Map<String, Integer> map = new SymbolMap<>();
        AtomicInteger errors = new AtomicInteger();
        Thread writer = new Thread(() -> {
            for (int i = 0; i < 200_000; i++) {
                map.put("S" + i, i);
            }
        });
        Thread[] readers = new Thread[4];
        for (int r = 0; r < readers.length; r++) {
            readers[r] = new Thread(() -> {
                for (int i = 0; i < 200_000; i++) {
                    Integer v = map.get("S" + i);
                    if (v != null && v != i) {
                        errors.incrementAndGet();
                    }
                }
            });
        }
        writer.start();
        for (Thread t : readers) {
            t.start();
        }
        writer.join();
        for (Thread t : readers) {
            t.join();
        }
        assertEquals(0, errors.get());
        assertEquals(200_000, map.size());
        assertTrue(map.containsKey("S199999"));
    }
}
