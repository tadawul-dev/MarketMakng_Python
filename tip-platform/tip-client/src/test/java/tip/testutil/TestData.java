package tip.testutil;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.function.Consumer;

/** Helpers shared by the tests. */
public final class TestData {
    private TestData() {}

    private static final HexFormat HEX = HexFormat.of();

    public static byte[] hex(String s) { return HEX.parseHex(s); }

    public static String hex(byte[] b) { return HEX.formatHex(b); }

    public static byte[] bytes(int... v) {
        byte[] b = new byte[v.length];
        for (int i = 0; i < v.length; i++) b[i] = (byte) v[i];
        return b;
    }

    /** Lines of a classpath resource, without comments (#) and blank lines. */
    public static List<String> resourceLines(String name) throws IOException {
        try (InputStream in = TestData.class.getResourceAsStream(name)) {
            if (in == null) throw new IOException("missing test resource " + name);
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            List<String> out = new ArrayList<>();
            for (String l; (l = r.readLine()) != null; ) if (!l.isBlank() && !l.startsWith("#")) out.add(l);
            return out;
        }
    }

    /** An XMP frame carrying a TIP text message, as {@code TYPE;...} bytes. */
    public static byte[] frameBytes(String tipText) throws Exception {
        return new tip.xm.XmpFrame(tip.xm.Xmp.Type.BINARY_RECORD, tipText.getBytes(StandardCharsets.UTF_8)).encode();
    }

    /** Collects every Log4j event of the "tip" loggers while open. */
    public static final class LogCapture implements AutoCloseable {
        /** One captured log event, already formatted. */
        public record Entry(String logger, Level level, String message) {
            @Override public String toString() { return logger + " " + level + " " + message; }
        }

        public final List<Entry> records = new java.util.concurrent.CopyOnWriteArrayList<>();
        private final LoggerContext context = (LoggerContext) LogManager.getContext(false);
        private final Configuration config = context.getConfiguration();
        private final LoggerConfig previous;
        private final AbstractAppender appender = new AbstractAppender("tip-test-capture", null, null, true, Property.EMPTY_ARRAY) {
            @Override public void append(LogEvent e) {
                // read everything now: Log4j may reuse the event object afterwards
                records.add(new Entry(e.getLoggerName(), e.getLevel(), e.getMessage().getFormattedMessage()));
            }
        };

        /** Captures everything the "tip" loggers log at {@code level} or more severe, and keeps it off the console. */
        public LogCapture(Level level) {
            LoggerConfig existing = config.getLoggerConfig("tip");
            previous = "tip".equals(existing.getName()) ? existing : null;      // otherwise it is only the inherited root config
            appender.start();
            config.addAppender(appender);
            LoggerConfig capture = new LoggerConfig("tip", level, false);        // not additive: nothing reaches the console
            capture.addAppender(appender, null, null);
            config.removeLogger("tip");
            config.addLogger("tip", capture);
            context.updateLoggers();
        }

        /** All events rendered as "logger LEVEL message", one per line. */
        public String text() {
            StringBuilder sb = new StringBuilder();
            for (Entry e : records) sb.append(e).append('\n');
            return sb.toString();
        }

        public boolean has(Level level, String loggerPrefix, String contains) {
            return text().lines().anyMatch(l -> l.startsWith(loggerPrefix) && l.contains(" " + level + " ") && l.contains(contains));
        }

        @Override public void close() {
            config.removeLogger("tip");
            if (previous != null) config.addLogger("tip", previous);
            appender.stop();
            context.updateLoggers();
        }
    }
}
