package tip.xm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * XMP extension header: a sequence of {@code tag(1) length(1) data(length)} entries
 * (port of {@code XMP_AddExtHdrEntry} / {@code XMP_GetExtHdrEntry}). Immutable.
 */
public final class ExtHeader {
    public record Entry(int tag, byte[] data) {
        public Entry {
            data = data.clone();
        }

        @Override
        public byte[] data() { return data.clone(); }

        /** Big-endian int32 at the start of the data. */
        public int intValue() {
            if (data.length < 4) throw new IllegalStateException("entry has " + data.length + " bytes");
            return ((data[0] & 0xFF) << 24) | ((data[1] & 0xFF) << 16) | ((data[2] & 0xFF) << 8) | (data[3] & 0xFF);
        }
    }

    public static final ExtHeader EMPTY = new ExtHeader(List.of());

    private final List<Entry> entries;

    private ExtHeader(List<Entry> entries) {
        this.entries = entries;
    }

    public List<Entry> entries() { return entries; }

    public boolean isEmpty() { return entries.isEmpty(); }

    public Optional<Entry> find(int tag) {
        for (Entry e : entries) {
            if (e.tag() == tag) return Optional.of(e);
        }
        return Optional.empty();
    }

    public int encodedLength() {
        int n = 0;
        for (Entry e : entries) n += 2 + e.data.length;
        return n;
    }

    public ExtHeader with(int tag, byte[] data) {
        if (encodedLength() + 2 + data.length > Xmp.MAX_EXT_HDR_SIZE) {
            throw new IllegalArgumentException("extension header would exceed " + Xmp.MAX_EXT_HDR_SIZE + " bytes");
        }
        List<Entry> l = new ArrayList<>(entries);
        l.add(new Entry(tag, data));
        return new ExtHeader(Collections.unmodifiableList(l));
    }

    public ExtHeader withByte(int tag, int value) {
        return with(tag, new byte[] {(byte) value});
    }

    public ExtHeader withInt(int tag, int value) {
        return with(tag, new byte[] {(byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8), (byte) value});
    }

    byte[] encode() {
        byte[] out = new byte[encodedLength()];
        int p = 0;
        for (Entry e : entries) {
            out[p++] = (byte) e.tag();
            out[p++] = (byte) e.data.length;
            System.arraycopy(e.data, 0, out, p, e.data.length);
            p += e.data.length;
        }
        return out;
    }

    /** Decodes {@code len} bytes; like the C code, an incomplete trailing entry is silently skipped. */
    static ExtHeader decode(byte[] buf, int off, int len) {
        if (len == 0) return EMPTY;
        List<Entry> l = new ArrayList<>();
        int p = 0;
        while (p <= len - 2) {
            int tag = buf[off + p] & 0xFF;
            int dl = buf[off + p + 1] & 0xFF;
            if (dl > len - p - 2) break;
            byte[] d = new byte[dl];
            System.arraycopy(buf, off + p + 2, d, 0, dl);
            l.add(new Entry(tag, d));
            p += dl + 2;
        }
        return new ExtHeader(Collections.unmodifiableList(l));
    }
}
