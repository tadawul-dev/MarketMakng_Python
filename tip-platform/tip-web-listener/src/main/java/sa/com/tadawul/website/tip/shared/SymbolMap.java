package sa.com.tadawul.website.tip.shared;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe map for the symbol registries that one thread writes while others read.
 * <p>
 * The {@code Symbol -> array index} maps in {@link TIPDataObjects} and the reference maps in
 * {@link LoadInitialData} are filled at start-up, but the low-volume parser thread also adds new
 * symbols during the session (BDSh/BDBo/BDIn) while the trade, order-book, index and TLU parser
 * threads are reading them. With {@code java.util.HashMap} that is a data race: a reader can miss
 * an entry or see a half-built table during a resize.
 * <p>
 * This class is a {@link ConcurrentHashMap} underneath (lock-free reads, fine-grained locking for
 * writes), but it keeps the {@code HashMap} contract that the existing code relies on:
 * {@code null} keys and values are allowed ({@code ConcurrentHashMap} itself would throw
 * {@code NullPointerException}). For example, {@code get(null)} simply returns {@code null}.
 *
 * @param <K> key type
 * @param <V> value type
 */
public final class SymbolMap<K, V> extends AbstractMap<K, V> {

    private static final Object NULL = new Object();

    private final ConcurrentHashMap<Object, Object> map;

    public SymbolMap() {
        this(16);
    }

    public SymbolMap(int expectedSize) {
        this.map = new ConcurrentHashMap<>(Math.max(expectedSize, 1));
    }

    private static Object mask(Object o) {
        return o == null ? NULL : o;
    }

    @SuppressWarnings("unchecked")
    private static <T> T unmask(Object o) {
        return o == NULL ? null : (T) o;
    }

    @Override
    public V get(Object key) {
        return unmask(map.get(mask(key)));
    }

    @Override
    public V put(K key, V value) {
        return unmask(map.put(mask(key), mask(value)));
    }

    @Override
    public V remove(Object key) {
        return unmask(map.remove(mask(key)));
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(mask(key));
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return new AbstractSet<>() {
            @Override
            public Iterator<Map.Entry<K, V>> iterator() {
                Iterator<Map.Entry<Object, Object>> it = map.entrySet().iterator();
                return new Iterator<>() {
                    @Override
                    public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override
                    public Map.Entry<K, V> next() {
                        Map.Entry<Object, Object> e = it.next();
                        return new SimpleImmutableEntry<>(unmask(e.getKey()), unmask(e.getValue()));
                    }

                    @Override
                    public void remove() {
                        it.remove();
                    }
                };
            }

            @Override
            public int size() {
                return map.size();
            }
        };
    }
}
