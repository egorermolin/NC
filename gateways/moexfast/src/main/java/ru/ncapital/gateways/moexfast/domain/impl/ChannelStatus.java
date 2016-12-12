package ru.ncapital.gateways.moexfast.domain.impl;

import ru.ncapital.gateways.moexfast.connection.ConnectionId;
import ru.ncapital.gateways.moexfast.domain.intf.IChannelStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by egore on 8/12/2016.
 */
public class ChannelStatus implements IChannelStatus {

    private Map<ChannelType, Integer> channelStatuses = new HashMap<>();

    public static ChannelType convert(ConnectionId connectionId) {
        switch (connectionId) {
            case CURR_INSTRUMENT_INCR_A:
            case CURR_INSTRUMENT_INCR_B:
            // case CURR_INSTRUMENT_SNAP_A:
            //case CURR_INSTRUMENT_SNAP_B:
            case FOND_INSTRUMENT_INCR_A:
            case FOND_INSTRUMENT_INCR_B:
            // case FOND_INSTRUMENT_SNAP_A:
            // case FOND_INSTRUMENT_SNAP_B:
            case FUT_INSTRUMENT_INCR_A:
            case FUT_INSTRUMENT_INCR_B:
            // case FUT_INSTRUMENT_SNAP_A:
            // case FUT_INSTRUMENT_SNAP_B:
                return ChannelType.Instrument;
            case CURR_ORDER_LIST_INCR_A:
            case CURR_ORDER_LIST_INCR_B:
            // case CURR_ORDER_LIST_SNAP_A:
            // case CURR_ORDER_LIST_SNAP_B:
            case FOND_ORDER_LIST_INCR_A:
            case FOND_ORDER_LIST_INCR_B:
            // case FOND_ORDER_LIST_SNAP_A:
            // case FOND_ORDER_LIST_SNAP_B:
            case FUT_ORDER_LIST_INCR_A:
            case FUT_ORDER_LIST_INCR_B:
            // case FUT_ORDER_LIST_SNAP_A:
            // case FUT_ORDER_LIST_SNAP_B:
                return ChannelType.OrderList;
            case CURR_STATISTICS_INCR_A:
            case CURR_STATISTICS_INCR_B:
            // case CURR_STATISTICS_SNAP_A:
            // case CURR_STATISTICS_SNAP_B:
            case FOND_STATISTICS_INCR_A:
            case FOND_STATISTICS_INCR_B:
            // case FOND_STATISTICS_SNAP_A:
            // case FOND_STATISTICS_SNAP_B:
                return ChannelType.BBOAndStatistics;
            case FUT_ORDER_BOOK_INCR_A:
            case FUT_ORDER_BOOK_INCR_B:
            // case FUT_ORDER_BOOK_SNAP_A:
            // case FUT_ORDER_BOOK_SNAP_B:
                return ChannelType.BBO;
            case FUT_STATISTICS_INCR_A:
            case FUT_STATISTICS_INCR_B:
            // case FUT_STATISTICS_SNAP_A:
            // case FUT_STATISTICS_SNAP_B:
                return ChannelType.Statistics;
            case CURR_PUB_TRADES_INCR_A:
            case CURR_PUB_TRADES_INCR_B:
            // case CURR_PUB_TRADES_SNAP_A:
            // case CURR_PUB_TRADES_SNAP_B:
            case FOND_PUB_TRADES_INCR_A:
            case FOND_PUB_TRADES_INCR_B:
            // case FOND_PUB_TRADES_SNAP_A:
            // case FOND_PUB_TRADES_SNAP_B:
                return ChannelType.PublicTrade;
        }
        return null;
    }

    @Override
    public int isChannelUp(ChannelType type) {
        return channelStatuses.containsKey(type) ? channelStatuses.get(type) : -1;
    }

    public void addChannel(ChannelType type) {
        if (type == null)
            return;

        if (channelStatuses.containsKey(type))
            return;

        channelStatuses.put(type, -1);
    }

    public void addChannelUp(ChannelType type) {
        if (type == null)
            return;

        channelStatuses.put(type, isChannelUp(type) + 1);
    }

    public ChannelStatus checkAll() {
        boolean hasPartial = true;
        boolean hasFull = true;
        for (Integer channelStatus : channelStatuses.values()) {
            hasPartial = hasPartial && channelStatus > -1;
            hasFull = hasFull && channelStatus > 0;
        }

        if (hasFull)
            channelStatuses.put(ChannelType.All, 1);
        else if (hasPartial)
            channelStatuses.put(ChannelType.All, 0);
        else
            channelStatuses.put(ChannelType.All, -1);

        return this;
    }
}
