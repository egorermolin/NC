package ru.ncapital.gateways.moexfast.connection;

import java.util.HashMap;

/**
 * Created by egore on 2/3/16.
 */
public enum ConnectionId {
    MULTICAST_CHANNEL_A("MULTICAST-A"),
    MULTICAST_CHANNEL_B("MULTICAST-B"),

    CURR_INSTRUMENT_SNAP_A("CURR-IDF-A"),
    CURR_INSTRUMENT_SNAP_B("CURR-IDF-B"),
    CURR_INSTRUMENT_INCR_A("CURR-ISF-A"),
    CURR_INSTRUMENT_INCR_B("CURR-ISF-B"),
    CURR_ORDER_LIST_SNAP_A("CURR-OLS-A"),
    CURR_ORDER_LIST_SNAP_B("CURR-OLS-B"),
    CURR_ORDER_LIST_INCR_A("CURR-OLR-A"),
    CURR_ORDER_LIST_INCR_B("CURR-OLR-B"),
    CURR_STATISTICS_SNAP_A("CURR-MSS-A"),
    CURR_STATISTICS_SNAP_B("CURR-MSS-B"),
    CURR_STATISTICS_INCR_A("CURR-MSR-A"),
    CURR_STATISTICS_INCR_B("CURR-MSR-B"),
    CURR_PUB_TRADES_SNAP_A("CURR-TLS-A"),
    CURR_PUB_TRADES_SNAP_B("CURR-TLS-B"),
    CURR_PUB_TRADES_INCR_A("CURR-TLR-A"),
    CURR_PUB_TRADES_INCR_B("CURR-TLR-B"),

    FOND_INSTRUMENT_SNAP_A("FOND-IDF-A"),
    FOND_INSTRUMENT_SNAP_B("FOND-IDF-B"),
    FOND_INSTRUMENT_INCR_A("FOND-ISF-A"),
    FOND_INSTRUMENT_INCR_B("FOND-ISF-B"),
    FOND_ORDER_LIST_SNAP_A("FOND-OLS-A"),
    FOND_ORDER_LIST_SNAP_B("FOND-OLS-B"),
    FOND_ORDER_LIST_INCR_A("FOND-OLR-A"),
    FOND_ORDER_LIST_INCR_B("FOND-OLR-B"),
    FOND_STATISTICS_SNAP_A("FOND-MSS-A"),
    FOND_STATISTICS_SNAP_B("FOND-MSS-B"),
    FOND_STATISTICS_INCR_A("FOND-MSR-A"),
    FOND_STATISTICS_INCR_B("FOND-MSR-B"),
    FOND_PUB_TRADES_SNAP_A("FOND-TLS-A"),
    FOND_PUB_TRADES_SNAP_B("FOND-TLS-B"),
    FOND_PUB_TRADES_INCR_A("FOND-TLR-A"),
    FOND_PUB_TRADES_INCR_B("FOND-TLR-B"),

    FUT_INSTRUMENT_SNAP_A("FUT-INFO-R-A"),
    FUT_INSTRUMENT_SNAP_B("FUT-INFO-R-B"),
    FUT_INSTRUMENT_INCR_A("FUT-INFO-I-A"),
    FUT_INSTRUMENT_INCR_B("FUT-INFO-I-B"),
    FUT_ORDER_LIST_SNAP_A("ORDERS-LOG-S-A"),
    FUT_ORDER_LIST_SNAP_B("ORDERS-LOG-S-B"),
    FUT_ORDER_LIST_INCR_A("ORDERS-LOG-I-A"),
    FUT_ORDER_LIST_INCR_B("ORDERS-LOG-I-B"),
    FUT_ORDER_BOOK_SNAP_A("FUT-BOOK-1-S-A"),
    FUT_ORDER_BOOK_SNAP_B("FUT-BOOK-1-S-B"),
    FUT_ORDER_BOOK_INCR_A("FUT-BOOK-1-I-A"),
    FUT_ORDER_BOOK_INCR_B("FUT-BOOK-1-I-B"),
    FUT_STATISTICS_SNAP_A("FUT-TRADES-S-A"),
    FUT_STATISTICS_SNAP_B("FUT-TRADES-S-B"),
    FUT_STATISTICS_INCR_A("FUT-TRADES-I-A"),
    FUT_STATISTICS_INCR_B("FUT-TRADES-I-B"),
    FUT_NEWS_INCR_A("NEWS-I-A"),
    FUT_NEWS_INCR_B("NEWS-I-B");

    private final String connectionId;

    private final boolean primary;

    ConnectionId(String connectionId) {
        this.connectionId = connectionId;
        this.primary = connectionId.contains("-A");
    }

    private static HashMap<String, ConnectionId> actionMap = new HashMap<String, ConnectionId>();

    static {
        {
            for (ConnectionId action : ConnectionId.values()) {
                actionMap.put(action.getConnectionId(), action);
            }
        }
    }

    public String getConnectionId() {
        return connectionId;
    }

    public boolean isPrimary() {
        return primary;
    }

    public static ConnectionId convert(String connectionId) { return actionMap.get(connectionId); }

    @Override
    public String toString() {
        return connectionId;
    }
}
