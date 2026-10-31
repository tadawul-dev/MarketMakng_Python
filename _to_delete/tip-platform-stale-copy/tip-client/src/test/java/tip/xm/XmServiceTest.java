package tip.xm;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Test;

public class XmServiceTest {
    private static XmError addError(XmService s, String host, int port) {
        try {
            s.add(host, port);
            fail("expected XmException for " + host + ":" + port);
            return null;
        } catch (XmException e) {
            return e.error();
        }
    }

    @Test public void emptyServiceHasNoEntries() {
        try {
            new XmService().nextAttemptOrder();
            fail();
        } catch (XmException e) {
            assertEquals(XmError.NO_SERVICE_ENTRIES, e.error());
        }
    }

    @Test public void singleAddress() throws Exception {
        XmService s = new XmService().add("a", 1);
        assertEquals(1, s.size());
        assertEquals(List.of(new XmService.Address("a", 1)), s.nextAttemptOrder());
        assertEquals(List.of(new XmService.Address("a", 1)), s.nextAttemptOrder());
    }

    @Test public void startingPointRotatesRoundRobin() throws Exception {
        XmService s = new XmService().add("a", 1).add("b", 2).add("c", 3);
        assertEquals("a b c", names(s.nextAttemptOrder()));
        assertEquals("b c a", names(s.nextAttemptOrder()));
        assertEquals("c a b", names(s.nextAttemptOrder()));
        assertEquals("a b c", names(s.nextAttemptOrder()));
    }

    @Test public void everyOrderContainsEveryAddressOnce() throws Exception {
        XmService s = new XmService();
        for (int i = 1; i <= 10; i++) s.add("h" + i, i);
        for (int round = 0; round < 25; round++) {
            List<XmService.Address> o = s.nextAttemptOrder();
            assertEquals(10, o.size());
            assertEquals(10, o.stream().distinct().count());
        }
    }

    @Test public void rejectsBadAddresses() {
        XmService s = new XmService();
        assertEquals(XmError.BAD_PARAM, addError(s, null, 80));
        assertEquals(XmError.BAD_PARAM, addError(s, "", 80));
        assertEquals(XmError.BAD_PARAM, addError(s, "   ", 80));
        assertEquals(XmError.BAD_PARAM, addError(s, "h", 0));
        assertEquals(XmError.BAD_PARAM, addError(s, "h", -5));
        assertEquals(XmError.BAD_PARAM, addError(s, "h", 65536));
        assertEquals(0, s.size());
    }

    @Test public void acceptsBoundaryPorts() throws Exception {
        XmService s = new XmService().add("h", 1).add("h", 65535);
        assertEquals(2, s.size());
    }

    @Test public void atMostTenEntries() throws Exception {
        XmService s = new XmService();
        for (int i = 0; i < XmService.MAX_ENTRIES; i++) s.add("h", i + 1);
        assertEquals(XmError.NO_ROOM, addError(s, "h", 99));
        assertEquals(10, s.size());
    }

    @Test public void addressToString() {
        assertEquals("host:4000", new XmService.Address("host", 4000).toString());
    }

    private static String names(List<XmService.Address> l) {
        return String.join(" ", l.stream().map(XmService.Address::host).toList());
    }
}
