package ru.ncapital.gateways.micexfast.connection;

import java.util.HashMap;

/**
 * Created by egore on 2/3/16.
 */
public enum ConnectionId {
    CURR_INSTRUMENT_SNAP_A("CURR-IDF-A", true),
    CURR_INSTRUMENT_SNAP_B("CURR-IDF-B", false),
    CURR_INSTRUMENT_INCR_A("CURR-ISF-A", true),
    CURR_INSTRUMENT_INCR_B("CURR-ISF-B", false),
    CURR_ORDER_LIST_SNAP_A("CURR-OLS-A", true),
    CURR_ORDER_LIST_SNAP_B("CURR-OLS-B", false),
    CURR_ORDER_LIST_INCR_A("CURR-OLR-A", true),
    CURR_ORDER_LIST_INCR_B("CURR-OLR-B", false),
    CURR_STATISTICS_SNAP_A("CURR-MSS-A", true),
    CURR_STATISTICS_SNAP_B("CURR-MSS-B", false),
    CURR_STATISTICS_INCR_A("CURR-MSR-A", true),
    CURR_STATISTICS_INCR_B("CURR-MSR-B", false),
    CURR_PUB_TRADES_INCR_A("CURR-TLR-A", true),
    CURR_PUB_TRADES_INCR_B("CURR-TLR-B", false),
    CURR_PUB_TRADES_SNAP_A("CURR-TLS-A", true),
    CURR_PUB_TRADES_SNAP_B("CURR-TLS-B", false),

    FOND_INSTRUMENT_SNAP_A("FOND-IDF-A", true),
    FOND_INSTRUMENT_SNAP_B("FOND-IDF-B", false),
    FOND_INSTRUMENT_INCR_A("FOND-ISF-A", true),
    FOND_INSTRUMENT_INCR_B("FOND-ISF-B", false),
    FOND_ORDER_LIST_SNAP_A("FOND-OLS-A", true),
    FOND_ORDER_LIST_SNAP_B("FOND-OLS-B", false),
    FOND_ORDER_LIST_INCR_A("FOND-OLR-A", true),
    FOND_ORDER_LIST_INCR_B("FOND-OLR-B", false),
    FOND_STATISTICS_SNAP_A("FOND-MSS-A", true),
    FOND_STATISTICS_SNAP_B("FOND-MSS-B", false),
    FOND_STATISTICS_INCR_A("FOND-MSR-A", true),
    FOND_STATISTICS_INCR_B("FOND-MSR-B", false),
    FOND_PUB_TRADES_INCR_A("FOND-TLR-A", true),
    FOND_PUB_TRADES_INCR_B("FOND-TLR-B", false),
    FOND_PUB_TRADES_SNAP_A("FOND-TLS-A", true),
    FOND_PUB_TRADES_SNAP_B("FOND-TLS-B", false);

    private final String connectionId;

    private final boolean primary;

    ConnectionId(String connectionId, boolean primary) {
        this.connectionId = connectionId;
        this.primary = primary;
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
        return this.primary;
    }

    public static ConnectionId convert(String connectionId) { return actionMap.get(connectionId); }

    @Override
    public String toString() {
        return connectionId;
    }
}
