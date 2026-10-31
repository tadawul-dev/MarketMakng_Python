package sa.com.tadawul.website.tip.shared;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Central thread factory.
 * <p>
 * The legacy code submitted each long-running worker to its own
 * {@code Executors.newFixedThreadPool(n)} that was never shut down, so threads showed up in logs
 * and thread dumps as anonymous {@code pool-3-thread-7}, and an uncaught error silently killed a
 * worker. Here every worker gets a meaningful name and an uncaught-exception handler that logs.
 * <ul>
 *   <li><b>Platform threads</b> for the permanent pipeline workers (reader, dispatcher, parsers,
 *       DB writers): they are CPU/JNI/JDBC bound, live for the whole session and gain nothing
 *       from virtual threads.</li>
 *   <li><b>Virtual threads</b> for short-lived helpers that mostly sleep or wait on I/O (the
 *       async EOD tasks).</li>
 * </ul>
 */
public final class TipThreads {

    private static final Logger logger = LogManager.getLogger(TipThreads.class);

    private static final Thread.UncaughtExceptionHandler LOG_UNCAUGHT =
            (t, e) -> logger.fatal("Uncaught exception in thread " + t.getName() + " - thread terminated", e);

    private TipThreads() {
    }

    /** Starts a named, non-daemon platform thread (same lifecycle as the old pool threads). */
    public static Thread startWorker(String name, Runnable task) {
        logger.info("Starting worker thread {}", name);
        return Thread.ofPlatform()
                .name(name)
                .daemon(false)
                .uncaughtExceptionHandler(LOG_UNCAUGHT)
                .start(task);
    }

    /** Starts a named daemon platform thread for background housekeeping. */
    public static Thread startDaemon(String name, Runnable task) {
        return Thread.ofPlatform()
                .name(name)
                .daemon(true)
                .uncaughtExceptionHandler(LOG_UNCAUGHT)
                .start(task);
    }

    /** Starts a named virtual thread for a short-lived, mostly-waiting task. */
    public static Thread startVirtual(String name, Runnable task) {
        logger.info("Starting virtual thread {}", name);
        return Thread.ofVirtual()
                .name(name)
                .uncaughtExceptionHandler(LOG_UNCAUGHT)
                .start(task);
    }
}
