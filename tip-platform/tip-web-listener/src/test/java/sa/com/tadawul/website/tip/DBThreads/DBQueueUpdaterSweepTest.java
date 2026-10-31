package sa.com.tadawul.website.tip.DBThreads;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;

import sa.com.tadawul.website.tip.queue.Buffer;
import sa.com.tadawul.website.tip.vo.BestBidOffVO;
import sa.com.tadawul.website.tip.vo.TIP_BaseVO;
import sa.com.tadawul.website.tip.vo.TIP_ShareVO;

/** The stream-based sweep must queue exactly what the old index loop queued. */
class DBQueueUpdaterSweepTest {

    private static void flush(int total, TIP_ShareVO[] src, int count, Buffer q) throws Exception {
        Method m = DBQueueUpdater.class.getDeclaredMethod("flushUpdated", int.class, TIP_BaseVO[].class, int.class, UnaryOperator.class, Buffer.class);
        m.setAccessible(true);
        UnaryOperator<TIP_ShareVO> copy = TIP_ShareVO::new;
        m.invoke(null, total, src, count, copy, q);
    }

    @Test
    void queuesCopiesOfDirtyEntriesInOrderAndClearsFlags() throws Exception {
        TIP_ShareVO[] shares = new TIP_ShareVO[6];
        for (int i = 0; i < shares.length; i++) {
            shares[i] = new TIP_ShareVO();
            shares[i].fk_te_symbol = "S" + i;
        }
        shares[1].isUpdated = true;
        shares[3].isUpdated = true;
        shares[5].isUpdated = true;                              // beyond 'count': must be left alone
        Buffer q = new Buffer("t");
        flush(2, shares, 5, q);
        assertEquals(2, q.size());
        TIP_ShareVO first = (TIP_ShareVO) q.getFrame();
        assertEquals("S1", first.fk_te_symbol);
        assertTrue(first != shares[1], "a copy is queued, not the live object");
        q.releaseFrame();
        assertEquals("S3", ((TIP_ShareVO) q.getFrame()).fk_te_symbol);
        assertFalse(shares[1].isUpdated);
        assertFalse(shares[3].isUpdated);
        assertTrue(shares[5].isUpdated);
    }

    @Test
    void nothingHappensWhenCounterIsZero() throws Exception {
        TIP_ShareVO[] shares = { new TIP_ShareVO() };
        shares[0].isUpdated = true;
        Buffer q = new Buffer("t");
        flush(0, shares, 1, q);
        assertEquals(0, q.size());
        assertTrue(shares[0].isUpdated);
    }

    @Test
    void orderBookCopyIsDeepAndIndependent() {
        BestBidOffVO[] live = TIP_BaseVO.getInitailBestBidsOffers();
        live[0].price = 32.45;
        BestBidOffVO[] copy = BestBidOffVO.copyOf(live, live.length);
        assertEquals(32.45, copy[0].price, 0.0);
        assertEquals(10, copy[9].level);
        live[0].price = 1.0;
        assertEquals(32.45, copy[0].price, 0.0);
    }
}
