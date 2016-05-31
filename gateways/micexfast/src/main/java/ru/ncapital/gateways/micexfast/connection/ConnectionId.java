package ru.ncapital.gateways.micexfast.connection;

import java.util.HashMap;

/**
 * Created by egore on 2/3/16.
 */
public enum ConnectionId {
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
    FOND_PUB_TRADES_INCR_B("FOND-TLR-B");

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
