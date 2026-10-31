package tip.client;

import java.io.IOException;
import java.io.PrintStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import tip.xm.XmService;

/**
 * Command-line client, same options as the C sample plus {@code -v} (logging) and {@code -t}.
 *
 * <p>Messages go to <b>stdout</b>, logs to <b>stderr</b>. The program ends with exit code 0 when
 * the server sends the end-of-stream message ({@code EOs}), 1 when the connection fails, and 2
 * for a usage error.
 */
public final class TipClientMain {
    static final String USAGE = """
            usage: tipclient -a address -u uid -p pwd {-R|-B} [-s scope] [-d YYYYMMDD] [-x protversion]
                             [-b n] [-e n] [-z] [-q queuealert] [-t secs] [-v|-vv|-vvv]
              -a address       host:port; repeat for fail-over
              -u uid / -p pwd  account name / password
              -R               request realtime (RTr) data
              -B               request basic (BDa) data
              -s scope         1 none, 2 updates, 3 all (default 3)
              -d YYYYMMDD      request date (default today)
              -x protversion   protocol version sent at logon (default none)
              -b n / -e n      recover from / to sequence number (default 1 / open)
              -z               enable compression
              -q ms            queue alert threshold (default 500)
              -t secs          fail if nothing is received for this long (default: wait forever)
              -v               log INFO (-vv DEBUG, -vvv TRACE incl. every message) to stderr
            """;

    /** Default configuration for the command line program (see src/main/resources/tip-log4j2.xml). */
    static final String DEFAULT_LOG_CONFIG = "tip-log4j2.xml";

    /** Looked up lazily: Log4j reads its configuration when the first logger is created, and main() must pick the file first. */
    private static Logger log() {
        return LogManager.getLogger("tip.client.main");
    }

    /** Parsed command line. */
    public record Options(List<XmService.Address> addresses, String uid, String pwd,
                          TipClient.RequestType request, int scope, int date, String version,
                          long from, long to, boolean compression, int queueAlert, int recvSecs,
                          int verbosity) {}

    private TipClientMain() {}

    /** @throws IllegalArgumentException with a user-readable message for any bad or missing option */
    public static Options parse(String[] args) {
        List<XmService.Address> addresses = new ArrayList<>();
        String uid = null, pwd = null, version = null;
        TipClient.RequestType request = null;
        int scope = 3, date = TipClient.currentDate(), queueAlert = 500, recvSecs = 0, verbosity = 0;
        long from = 1, to = 0;
        boolean compression = false;

        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            try {
                switch (a) {
                    case "-a" -> {
                        String v = args[++i];
                        int c = v.lastIndexOf(':');
                        if (c <= 0) throw new IllegalArgumentException("address must be host:port, got '" + v + "'");
                        int port = Integer.parseInt(v.substring(c + 1));
                        if (port < 1 || port > 65535) throw new IllegalArgumentException("port out of range: " + port);
                        addresses.add(new XmService.Address(v.substring(0, c), port));
                    }
                    case "-u" -> uid = args[++i];
                    case "-p" -> pwd = args[++i];
                    case "-R" -> request = TipClient.RequestType.REALTIME;
                    case "-B" -> request = TipClient.RequestType.BASIC_DATA;
                    case "-s" -> scope = Integer.parseInt(args[++i]);
                    case "-d" -> date = Integer.parseInt(args[++i]);
                    case "-x" -> version = args[++i];
                    case "-b" -> from = Long.parseLong(args[++i]);
                    case "-e" -> to = Long.parseLong(args[++i]);
                    case "-z" -> compression = true;
                    case "-q" -> queueAlert = Integer.parseInt(args[++i]);
                    case "-t" -> recvSecs = Integer.parseInt(args[++i]);
                    case "-v" -> verbosity = Math.max(verbosity, 1);
                    case "-vv" -> verbosity = Math.max(verbosity, 2);
                    case "-vvv" -> verbosity = 3;
                    default -> throw new IllegalArgumentException("unknown option " + a);
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new IllegalArgumentException("option " + a + " needs a value");
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("option " + a + " needs a number: " + e.getMessage());
            }
        }
        if (addresses.isEmpty()) throw new IllegalArgumentException("missing -a address");
        if (uid == null) throw new IllegalArgumentException("missing -u uid");
        if (pwd == null) throw new IllegalArgumentException("missing -p pwd");
        if (request == null) throw new IllegalArgumentException("one of -R or -B is required");
        if (scope < 1 || scope > 3) throw new IllegalArgumentException("scope must be 1, 2 or 3");
        if (from <= 0) throw new IllegalArgumentException("-b must be > 0");
        if (to != 0 && to < from) throw new IllegalArgumentException("-e must not be less than -b");
        return new Options(List.copyOf(addresses), uid, pwd, request, scope, date, version, from, to,
                compression, queueAlert, recvSecs, verbosity);
    }

    /**
     * Connects, logs on, sends the request and prints every message to {@code out} until the
     * server ends the stream or the connection fails.
     *
     * @return process exit code: 0 after {@code EOs}, 1 on any I/O failure
     */
    public static int run(Options o, PrintStream out) {
        TipClient.Builder b = TipClient.builder().compression(o.compression()).queueAlertMillis(o.queueAlert());
        if (o.recvSecs() > 0) b.recvTimeout(Duration.ofSeconds(o.recvSecs()));
        try (TipClient tip = b.build()) {
            tip.connect(o.addresses());
            TipMessage reply = tip.logon(o.uid(), o.pwd(), o.version());
            out.print("logon reply: " + reply.toWire());
            tip.streamingRequest(o.request(), TipClient.Scope.of(o.scope()), o.date(), o.from(), o.to());
            for (;;) {
                String raw = tip.nextRaw();
                out.print(raw);
                if (raw.startsWith(TipTags.END_OF_STREAM + TipTags.DELIMITER)) {
                    log().log(Level.INFO, "end of stream");
                    try { tip.logoff(); } catch (IOException ignored) { /* server may already have closed */ }
                    return 0;
                }
            }
        } catch (IOException e) {
            log().log(Level.ERROR, "failed: {}", e.getMessage());
            return 1;
        }
    }

    public static void main(String[] args) {
        // Must happen before the first Log4j logger is created. An explicit -Dlog4j2.configurationFile always wins.
        if (System.getProperty("log4j2.configurationFile") == null && System.getProperty("log4j.configurationFile") == null) {
            System.setProperty("log4j2.configurationFile", DEFAULT_LOG_CONFIG);
        }
        Options o;
        try {
            o = parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.err.print(USAGE);
            System.exit(2);
            return;
        }
        configureLogging(o.verbosity());
        System.exit(run(o, System.out));
    }

    /**
     * 0 = warnings and errors only, 1 = INFO, 2 = DEBUG, 3 = TRACE. Sets the level of the {@code tip} logger tree
     * on top of whatever the XML configuration says (log4j-core is needed for this, the library itself only needs log4j-api).
     */
    static void configureLogging(int verbosity) {
        Configurator.setLevel("tip", levelFor(verbosity));
    }

    static Level levelFor(int verbosity) {
        return switch (verbosity) {
            case 0 -> Level.WARN;
            case 1 -> Level.INFO;
            case 2 -> Level.DEBUG;
            default -> Level.TRACE;
        };
    }
}
