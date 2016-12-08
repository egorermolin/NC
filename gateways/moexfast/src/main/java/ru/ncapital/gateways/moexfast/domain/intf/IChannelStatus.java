package ru.ncapital.gateways.moexfast.domain.intf;

import sun.nio.cs.UTF_32BE_BOM;

/**
 * Created by egore on 8/12/2016.
 */
public interface IChannelStatus {
    enum ChannelType {
          Instrument, BBOAndStatistics, PublicTrade, OrderList, All
    }

    // -1: A and B down
    //  0: A or B down
    //  1: A and B up
    int isChannelUp(ChannelType type);
}
