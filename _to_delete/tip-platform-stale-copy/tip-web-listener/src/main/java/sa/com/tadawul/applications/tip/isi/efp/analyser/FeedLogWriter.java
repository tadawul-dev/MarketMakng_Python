package sa.com.tadawul.applications.tip.isi.efp.analyser;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import sa.com.tadawul.website.tip.shared.TipThreads;

/**
 * Append-only writer for the raw TIP feed log that is replayed for file-based recovery.
 * <p>
 * The legacy reader called {@code write()} + {@code flush()} on an unbuffered
 * {@code FileOutputStream} for every message, i.e. one {@code write(2)} system call per message on
 * the hot reader thread, and encoded with the platform default charset (which changed to UTF-8 in
 * JDK 18, JEP 400).
 * <p>
 * Now messages go into a 64 KiB buffer that a background daemon flushes every
 * {@code flushIntervalMillis}. Setting FEED_LOG_FLUSH_INTERVAL_MS=0 in efp.ini restores
 * flush-per-message. If the process is killed, at most one interval of feed log is lost; on
 * restart the replay resumes from the last sequence number it finds and TIP re-sends the rest.
 * <p>
 * Since 2.1 the log holds the <b>exact bytes received from TIP</b> (one message per line), so no
 * charset conversion happens on the hot path and the replay decodes the bytes with the same
 * {@link FeedTextDecoder} as live data.
 */
final class FeedLogWriter implements Closeable {

    private static final Logger logger = LogManager.getLogger(FeedLogWriter.class);
    private static final int BUFFER_SIZE = 64 * 1024;

    private final OutputStream out;
    private final long flushIntervalMillis;
    private final Thread flusher;
    private boolean dirty;          // guarded by this
    private volatile boolean closed;

    FeedLogWriter(String path, long flushIntervalMillis) throws IOException {
        this.out = new BufferedOutputStream(new FileOutputStream(path, true), BUFFER_SIZE);
        this.flushIntervalMillis = flushIntervalMillis;
        if (flushIntervalMillis > 0) {
            this.flusher = TipThreads.startDaemon("FeedLogFlusher", this::flushLoop);
        } else {
            this.flusher = null;
        }
    }

    /** Appends the raw bytes of one message, adding the '\n' terminator if it is missing. */
    synchronized void write(byte[] body) throws IOException {
        out.write(body);
        if (body.length == 0 || body[body.length - 1] != '\n') {
            out.write('\n');
        }
        if (flusher == null) {
            out.flush();
        } else {
            dirty = true;
        }
    }

    private void flushLoop() {
        while (!closed) {
            try {
                Thread.sleep(flushIntervalMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            flushQuietly();
        }
    }

    private synchronized void flushQuietly() {
        if (dirty && !closed) {
            try {
                out.flush();
                dirty = false;
            } catch (IOException e) {
                logger.error("Feed log flush error: " + e);
            }
        }
    }

    @Override
    public void close() throws IOException {
        closed = true;
        if (flusher != null) {
            flusher.interrupt();
        }
        synchronized (this) {
            out.close(); // flushes
        }
    }
}
