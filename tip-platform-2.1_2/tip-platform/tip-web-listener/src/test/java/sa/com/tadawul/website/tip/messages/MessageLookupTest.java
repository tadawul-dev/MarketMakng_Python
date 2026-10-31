package sa.com.tadawul.website.tip.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Test;

import sa.com.tadawul.website.tip.shared.TipConstants;

/** The hash lookups must return exactly what the legacy linear scans returned, for every enum. */
class MessageLookupTest {

    private static final List<Class<? extends Enum<?>>> ENUMS = List.of(
            BasicDataBond.class, BasicDataIndex.class, BasicDataIndexMember.class, BasicDataList.class,
            BasicDataRight.class, BasicDataShare.class, EndOfStream.class, GeneralReply.class, IndexSummary.class,
            IndexUpdates.class, IndicativeOpening.class, LowVolmMessageType.class, MarketState.class,
            MessageType.class, OrderBookReferencePrice.class, OrderBookState.class, OrderBookSummary.class,
            OrderBooks.class, TStAndTRdMessageType.class, TradeHalt.class, Trades.class, TradesStatistics.class,
            TurnoverListUpdates.class);

    @Test
    void hashLookupEqualsLegacyLinearScan() throws Exception {
        for (Class<? extends Enum<?>> type : ENUMS) {
            Field strField = type.getDeclaredField("messageTypeStr");
            Field valField = type.getDeclaredField("value");
            strField.setAccessible(true);
            valField.setAccessible(true);
            Method lookup = type.getMethod("getMessageValue", String.class);
            Enum<?>[] constants = type.getEnumConstants();
            for (Enum<?> c : constants) {
                String key = (String) strField.get(c);
                assertEquals(legacy(constants, strField, valField, key), lookup.invoke(null, key), type.getSimpleName() + "." + c);
            }
            for (String unknown : new String[] { "zzz", "", "TLU", "tlu" }) {
                assertEquals(legacy(constants, strField, valField, unknown), lookup.invoke(null, unknown),
                        type.getSimpleName() + " unknown '" + unknown + "'");
            }
            assertEquals(TipConstants.NODATAFOUND, lookup.invoke(null, (Object) null));
        }
    }

    private static int legacy(Enum<?>[] constants, Field strField, Field valField, String message) throws Exception {
        for (Enum<?> c : constants) {
            if (strField.get(c).equals(message)) {
                return (int) valField.get(c);
            }
        }
        return TipConstants.NODATAFOUND;
    }

    @Test
    void orderBooksTypeLookup() {
        for (OrderBooks ob : OrderBooks.values()) {
            String key = keyOf(ob);
            OrderBooks expected = OrderBooks.NOT_VALID;
            for (OrderBooks c : OrderBooks.values()) { // legacy linear scan: first match wins
                if (keyOf(c).equals(key)) {
                    expected = c;
                    break;
                }
            }
            assertEquals(expected, OrderBooks.getMessageType(key), ob.name());
        }
        assertEquals(OrderBooks.NOT_VALID, OrderBooks.getMessageType("nope"));
        assertEquals(OrderBooks.NOT_VALID, OrderBooks.getMessageType(null));
    }

    private static String keyOf(OrderBooks ob) {
        try {
            Field f = OrderBooks.class.getDeclaredField("messageTypeStr");
            f.setAccessible(true);
            return (String) f.get(ob);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    void dispatcherTypes() {
        assertEquals(1, MessageType.getMessageValue("TLu"));
        assertEquals(2, MessageType.getMessageValue("INd"));
        assertEquals(8, MessageType.getMessageValue("OBs"));
        assertEquals(TipConstants.NODATAFOUND, MessageType.getMessageValue("BDSh"));
    }
}
