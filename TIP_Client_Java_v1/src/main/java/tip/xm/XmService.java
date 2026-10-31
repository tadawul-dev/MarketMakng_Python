package tip.xm;

import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A list of server addresses for fail-over (port of {@code xmservice.c}).
 * Each new connection starts at the next address (round robin) and falls through
 * to the others if it cannot connect.
 */
public final class XmService {
    private static final Logger LOG = LogManager.getLogger("tip.xm.service");
    public static final int MAX_ENTRIES = 10;

    public record Address(String host, int port) {
        @Override public String toString() { return host + ":" + port; }
    }

    private final List<Address> entries = new ArrayList<>();
    private int lastIndex;

    public synchronized XmService add(String host, int port) throws XmException {
        if (host == null || host.isBlank() || port < 1 || port > 65535) {
            throw new XmException(XmError.BAD_PARAM, "address " + host + ":" + port);
        }
        if (entries.size() >= MAX_ENTRIES) throw new XmException(XmError.NO_ROOM);
        entries.add(new Address(host, port));
        LOG.log(Level.DEBUG, "added address {}:{} ({} total)", host, port, entries.size());
        return this;
    }

    public synchronized int size() { return entries.size(); }

    /** The order in which addresses are tried for the next connection. */
    synchronized List<Address> nextAttemptOrder() throws XmException {
        int n = entries.size();
        if (n == 0) throw new XmException(XmError.NO_SERVICE_ENTRIES);
        List<Address> order = new ArrayList<>(n);
        if (n == 1) {
            order.add(entries.get(0));
            return order;
        }
        int start = lastIndex % n;
        for (int j = 0; j < n; j++) order.add(entries.get((start + j) % n));
        lastIndex = (start + 1) % n;
        LOG.log(Level.TRACE, "next attempt order {}", order);
        return order;
    }
}
