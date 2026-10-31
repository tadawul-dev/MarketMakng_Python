package tip.client;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tip.xm.XmCrypt;
import tip.xm.XmError;
import tip.xm.XmException;
import tip.xm.XmService;

/**
 * Connectivity verification for a TIP server. For every address it walks through the layers one at a time and
 * reports the result and the time each one took, so a failure points at the layer that is broken:
 *
 * <ol>
 *   <li><b>dns</b> - the host name resolves</li>
 *   <li><b>tcp</b> - a TCP connection can be opened (and is closed again)</li>
 *   <li><b>session</b> - an XMP session is established, including the key exchange when encryption is configured</li>
 *   <li><b>logon</b> - the server accepts the account (only when {@code -u/-p} are given)</li>
 *   <li><b>first-message</b> - the server sends something after logon (only with {@code -w})</li>
 *   <li><b>logoff</b> - orderly logoff</li>
 * </ol>
 *
 * <p>No data is requested, so the check is safe to run against a production system and from monitoring.
 * Exit code: 0 when every address passed, 1 when at least one failed, 2 for a usage error.
 */
public final class TipConnectCheck {
    static final String USAGE = """
            usage: tipcheck -a address [-a address ...] [-u uid -p pwd] [-x protversion] [-z]
                            [-E RC4|DES -K hexkey [-I keyid]] [-T secs] [-t secs] [-w secs] [-v|-vv|-vvv]
              -a address       host:port to check; repeat to check several servers (each one is tested on its own)
              -u uid / -p pwd  also log on with this account (without them only the transport is checked)
              -x protversion   protocol version sent at logon
              -z               ask for compression
              -E alg -K key    use encryption with this pre-shared key (hex); -I key id (default 0)
              -T secs          connect timeout (default 5)
              -t secs          timeout for each server reply (default 10)
              -w secs          after logon, wait up to this long for the first message from the server
                               (nothing received is only a warning)
              -v               technical log on stderr (-vv DEBUG, -vvv TRACE)
            exit code: 0 all addresses passed, 1 at least one failed, 2 usage error
            """;

    private static Logger log() {
        return LogManager.getLogger("tip.client.check");
    }

    public enum Status { OK, WARN, FAIL }

    /** One verified layer. */
    public record Step(String name, Status status, long millis, String detail) {}

    /** Outcome for one address. */
    public record Result(XmService.Address address, List<Step> steps) {
        /** True when no step failed (warnings do not count). */
        public boolean ok() { return steps.stream().noneMatch(s -> s.status() == Status.FAIL); }

        public Optional<Step> step(String name) { return steps.stream().filter(s -> s.name().equals(name)).findFirst(); }

        public List<String> stepNames() { return steps.stream().map(Step::name).toList(); }
    }

    /** Parsed command line. {@code uid}/{@code pwd} are both null for a transport-only check. */
    public record Options(List<XmService.Address> addresses, String uid, String pwd, String version, boolean compression,
                          XmCrypt.Algorithm crypt, byte[] key, int keyId,
                          int connectSecs, int replySecs, int waitSecs, int verbosity) {
        @Override public String toString() {
            return "Options[addresses=" + addresses + ", uid=" + uid + ", pwd=" + (pwd == null ? null : "***") + ", crypt=" + crypt + "]";
        }
    }

    private TipConnectCheck() {}

    /** @throws IllegalArgumentException with a user-readable message for any bad or missing option */
    public static Options parse(String[] args) {
        List<XmService.Address> addresses = new ArrayList<>();
        String uid = null, pwd = null, version = null;
        boolean compression = false;
        XmCrypt.Algorithm crypt = null;
        byte[] key = null;
        int keyId = 0, connectSecs = 5, replySecs = 10, waitSecs = 0, verbosity = 0;

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
                    case "-x" -> version = args[++i];
                    case "-z" -> compression = true;
                    case "-E" -> {
                        String v = args[++i].toUpperCase();
                        crypt = switch (v) {
                            case "RC4" -> XmCrypt.Algorithm.RC4;
                            case "DES" -> XmCrypt.Algorithm.DES;
                            default -> throw new IllegalArgumentException("-E must be RC4 or DES, got '" + v + "'");
                        };
                    }
                    case "-K" -> {
                        try {
                            key = HexFormat.of().parseHex(args[++i]);
                        } catch (IllegalArgumentException e) {
                            throw new IllegalArgumentException("-K must be a hex string with an even number of digits");
                        }
                    }
                    case "-I" -> keyId = Integer.parseInt(args[++i]);
                    case "-T" -> connectSecs = Integer.parseInt(args[++i]);
                    case "-t" -> replySecs = Integer.parseInt(args[++i]);
                    case "-w" -> waitSecs = Integer.parseInt(args[++i]);
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
        if ((uid == null) != (pwd == null)) throw new IllegalArgumentException("-u and -p must be given together");
        if ((crypt == null) != (key == null)) throw new IllegalArgumentException("-E and -K must be given together");
        if (connectSecs < 1) throw new IllegalArgumentException("-T must be at least 1");
        if (replySecs < 1) throw new IllegalArgumentException("-t must be at least 1");
        if (waitSecs < 0) throw new IllegalArgumentException("-w must not be negative");
        if (waitSecs > 0 && uid == null) throw new IllegalArgumentException("-w needs -u and -p (the server sends data after logon)");
        return new Options(List.copyOf(addresses), uid, pwd, version, compression, crypt, key, keyId,
                connectSecs, replySecs, waitSecs, verbosity);
    }

    // ------------------------------------------------------------------ the checks

    /** Runs every check against one address. Never throws for network problems; they become FAIL steps. */
    public static Result check(XmService.Address address, Options o) {
        List<Step> steps = new ArrayList<>();
        log().log(Level.INFO, "checking {}", address);

        // 1. dns
        long t0 = System.nanoTime();
        try {
            InetAddress[] ips = InetAddress.getAllByName(address.host());
            steps.add(new Step("dns", Status.OK, since(t0), String.join(", ", Arrays.stream(ips).map(InetAddress::getHostAddress).toList())));
        } catch (IOException e) {
            return fail(address, steps, "dns", t0, "cannot resolve " + address.host() + ": " + e.getMessage());
        }

        // 2. tcp
        t0 = System.nanoTime();
        try (Socket probe = new Socket()) {
            probe.connect(new InetSocketAddress(address.host(), address.port()), o.connectSecs() * 1000);
            steps.add(new Step("tcp", Status.OK, since(t0), "connected to " + probe.getRemoteSocketAddress()));
        } catch (IOException e) {
            return fail(address, steps, "tcp", t0, describe(e));
        }

        // 3. session (XMP, key exchange)
        TipClient.Builder b = TipClient.builder().compression(o.compression())
                .connectTimeout(Duration.ofSeconds(o.connectSecs())).recvTimeout(Duration.ofSeconds(o.replySecs()));
        t0 = System.nanoTime();
        try {
            if (o.crypt() != null) b.initialCrypt(XmCrypt.create(o.crypt(), o.key(), o.keyId()));
        } catch (IOException e) {
            return fail(address, steps, "session", t0, "invalid encryption key: " + e.getMessage());
        }
        try (TipClient tip = b.build()) {
            try {
                tip.connect(address.host(), address.port());
                steps.add(new Step("session", Status.OK, since(t0), "compression=" + (o.compression() ? "on" : "off")
                        + " encryption=" + (o.crypt() == null ? "off" : o.crypt() + " (key exchange done)")));
            } catch (IOException e) {
                return fail(address, steps, "session", t0, describe(e));
            }
            if (o.uid() == null) return new Result(address, steps);          // transport-only check

            // 4. logon
            t0 = System.nanoTime();
            try {
                TipMessage reply = tip.logon(o.uid(), o.pwd(), o.version());
                String rc = reply.get(TipTags.REPLY_CODE).orElse("-"), rx = reply.get(TipTags.REPLY_TEXT).orElse("");
                String detail = reply.type() + " Rc=" + rc + (rx.isEmpty() ? "" : " Rx=" + rx);
                if (!"0".equals(rc)) return fail(address, steps, "logon", t0, "logon refused: " + detail);
                steps.add(new Step("logon", Status.OK, since(t0), detail));
            } catch (IOException e) {
                return fail(address, steps, "logon", t0, describe(e));
            }

            // 5. first message (optional)
            if (o.waitSecs() > 0) {
                t0 = System.nanoTime();
                try {
                    tip.setRecvTimeout(Duration.ofSeconds(o.waitSecs()));
                    String raw = tip.nextRaw();
                    steps.add(new Step("first-message", Status.OK, since(t0), "received " + firstWord(raw)));
                } catch (IOException e) {
                    boolean quiet = e instanceof XmException x && x.error() == XmError.TIMEOUT;
                    if (!quiet) return fail(address, steps, "first-message", t0, describe(e));
                    steps.add(new Step("first-message", Status.WARN, since(t0), "nothing received within " + o.waitSecs() + " s"));
                    return new Result(address, steps);                     // a timeout closes the client, so there is nothing to log off
                }
            }

            return finishWithLogoff(address, steps, tip, o);
        }
    }

    private static Result finishWithLogoff(XmService.Address address, List<Step> steps, TipClient tip, Options o) {
        long t0 = System.nanoTime();
        if (!tip.isConnected()) {
            steps.add(new Step("logoff", Status.WARN, 0, "connection already closed, nothing to log off"));
            return new Result(address, steps);
        }
        try {
            tip.setRecvTimeout(Duration.ofSeconds(Math.min(o.replySecs(), 3)));
            tip.logoff();
            String detail = "sent";
            try {
                detail = "reply " + firstWord(tip.nextRaw());
            } catch (IOException ignored) {
                // many servers just close the connection after LGf, that is a normal ending
            }
            steps.add(new Step("logoff", Status.OK, since(t0), detail));
        } catch (IOException e) {
            steps.add(new Step("logoff", Status.WARN, since(t0), describe(e)));
        }
        return new Result(address, steps);
    }

    // ------------------------------------------------------------------ reporting

    /** Checks every address and prints a report. @return exit code, 0 when all passed, otherwise 1 */
    public static int run(Options o, PrintStream out) {
        out.println("TIP connectivity check: " + o.addresses().size() + " address(es)"
                + (o.uid() == null ? ", transport only (no -u/-p)" : ", logon as " + o.uid()));
        int passed = 0;
        for (XmService.Address a : o.addresses()) {
            Result r = check(a, o);
            print(r, out);
            if (r.ok()) passed++;
        }
        out.println();
        out.println(passed + " of " + o.addresses().size() + " address(es) OK");
        log().log(Level.INFO, "{} of {} addresses OK", passed, o.addresses().size());
        return passed == o.addresses().size() ? 0 : 1;
    }

    static void print(Result r, PrintStream out) {
        out.println();
        out.println(r.address());
        for (Step s : r.steps()) {
            String tag = switch (s.status()) { case OK -> "  OK  "; case WARN -> " WARN "; case FAIL -> " FAIL "; };
            out.printf("  [%s] %-13s %5d ms  %s%n", tag, s.name(), s.millis(), s.detail());
        }
        out.println("  => " + (r.ok() ? "OK" : "FAILED"));
    }

    // ------------------------------------------------------------------ helpers

    private static Result fail(XmService.Address a, List<Step> steps, String name, long t0, String detail) {
        steps.add(new Step(name, Status.FAIL, since(t0), detail));
        log().log(Level.WARN, "{}: {} failed: {}", a, name, detail);
        return new Result(a, steps);
    }

    private static long since(long t0) { return (System.nanoTime() - t0) / 1_000_000; }

    private static String firstWord(String raw) {
        int i = raw.indexOf(';');
        return i > 0 ? raw.substring(0, i) : raw.strip();
    }

    private static String describe(IOException e) {
        String m = e.getMessage();
        return m == null || m.isBlank() ? e.toString() : m;
    }

    public static void main(String[] args) {
        if (System.getProperty("log4j2.configurationFile") == null && System.getProperty("log4j.configurationFile") == null) {
            System.setProperty("log4j2.configurationFile", TipClientMain.DEFAULT_LOG_CONFIG);
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
        TipClientMain.configureLogging(o.verbosity());
        System.exit(run(o, new PrintStream(System.out, true, StandardCharsets.UTF_8)));
    }
}
