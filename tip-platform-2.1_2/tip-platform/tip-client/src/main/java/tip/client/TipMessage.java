package tip.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A TIP text message: {@code TYPE;TagValue;TagValue;...;\n}.
 *
 * <p>A tag is zero or more upper-case letters followed by exactly one lower-case letter
 * ({@code UId}, {@code QDAy}, {@code n}); the value runs up to the next unescaped {@code ;}.
 * A backslash escapes the following character. Port of {@code AppendTag}/{@code ParseMsg}.
 */
public final class TipMessage {
    public record Field(String tag, String value) {}

    private final String type;
    private final List<Field> fields;

    private TipMessage(String type, List<Field> fields) {
        this.type = type;
        this.fields = List.copyOf(fields);
    }

    public String type() { return type; }

    public List<Field> fields() { return fields; }

    /** First field with this tag. */
    public Optional<String> get(String tag) {
        for (Field f : fields) if (f.tag.equals(tag)) return Optional.of(f.value);
        return Optional.empty();
    }

    // ------------------------------------------------------------------ building

    public static Builder builder(String type) {
        return new Builder(type);
    }

    public static final class Builder {
        private final String type;
        private final List<Field> fields = new ArrayList<>();

        private Builder(String type) {
            checkTag(type);
            this.type = type;
        }

        public Builder add(String tag, String value) {
            checkTag(tag);
            fields.add(new Field(tag, value == null ? "" : value));
            return this;
        }

        public Builder add(String tag, long value) {
            return add(tag, Long.toString(value));
        }

        public TipMessage build() {
            return new TipMessage(type, fields);
        }

        /** Shortcut for {@code build().toWire()}. */
        public String toWire() {
            return build().toWire();
        }
    }

    private static void checkTag(String tag) {
        boolean ok = tag != null && !tag.isEmpty();
        if (ok) {
            for (int i = 0; i < tag.length() - 1 && ok; i++) ok = tag.charAt(i) >= 'A' && tag.charAt(i) <= 'Z';
            char last = tag.charAt(tag.length() - 1);
            ok = ok && last >= 'a' && last <= 'z';
        }
        if (!ok) throw new IllegalArgumentException("invalid TIP tag: " + tag);
    }

    /** Serializes to the wire format, including the trailing newline. */
    public String toWire() {
        StringBuilder sb = new StringBuilder().append(type).append(TipTags.DELIMITER);
        for (Field f : fields) {
            sb.append(f.tag);
            for (int i = 0; i < f.value.length(); i++) {
                char c = f.value.charAt(i);
                if (c == TipTags.DELIMITER || c == TipTags.ESCAPE || c == TipTags.END_OF_MESSAGE) sb.append(TipTags.ESCAPE);
                sb.append(c);
            }
            sb.append(TipTags.DELIMITER);
        }
        return sb.append(TipTags.END_OF_MESSAGE).toString();
    }

    // ------------------------------------------------------------------ parsing

    /** Parses one message. A single trailing newline is accepted but not required. */
    public static TipMessage parse(String text) throws TipParseException {
        int end = text.length();
        if (end > 0 && text.charAt(end - 1) == TipTags.END_OF_MESSAGE) end--;
        int[] pos = {0};

        String type = readTag(text, end, pos);
        if (type == null) throw new TipParseException("missing message type");
        if (pos[0] >= end || text.charAt(pos[0]) != TipTags.DELIMITER) throw new TipParseException("missing field delimiter after message type");
        pos[0]++;

        List<Field> fields = new ArrayList<>();
        while (pos[0] < end) {
            String tag = readTag(text, end, pos);
            if (tag == null) throw new TipParseException("incomplete tag at offset " + pos[0]);
            fields.add(new Field(tag, readValue(text, end, pos)));
        }
        return new TipMessage(type, fields);
    }

    /** Reads letters up to and including the first lower-case one; null if the text ends first. */
    private static String readTag(String s, int end, int[] pos) throws TipParseException {
        int start = pos[0];
        int p = start;
        while (p < end) {
            char c = s.charAt(p++);
            boolean upper = c >= 'A' && c <= 'Z';
            boolean lower = c >= 'a' && c <= 'z';
            if (!upper && !lower) throw new TipParseException("invalid character '" + c + "' in tag at offset " + (p - 1));
            if (lower) {
                pos[0] = p;
                return s.substring(start, p);
            }
        }
        return null;
    }

    private static String readValue(String s, int end, int[] pos) throws TipParseException {
        StringBuilder sb = new StringBuilder();
        int p = pos[0];
        while (p < end) {
            char c = s.charAt(p++);
            if (c == TipTags.END_OF_MESSAGE) throw new TipParseException("unexpected end-of-message in field value");
            if (c == TipTags.DELIMITER) {
                pos[0] = p;
                return sb.toString();
            }
            if (c == TipTags.ESCAPE) {
                if (p >= end) throw new TipParseException("dangling escape at end of message");
                c = s.charAt(p++);
            }
            sb.append(c);
        }
        throw new TipParseException("field value not terminated by ';'");
    }

    /**
     * Human-readable form for logs. The value of the password field ({@code PWd}) is replaced by
     * {@code ***}; use {@link #toWire()} for the real text.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(type).append(TipTags.DELIMITER);
        for (Field f : fields) {
            String v = TipTags.PASSWORD.equals(f.tag) ? "***" : f.value;
            sb.append(f.tag).append(v.replace("\n", "\\n")).append(TipTags.DELIMITER);
        }
        return sb.toString();
    }
}
