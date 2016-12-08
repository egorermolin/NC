package ru.ncapital.gateways.moexfast.domain.intf;

/**
 * Created by egore on 8/12/2016.
 */
public interface IChannelStatus {
    enum ChannelType {
          Instrument, BBOAndStatistics, BBO, Statistics, PublicTrade, OrderList, All
    }

    // -1: A and B down
    //  0: A or B down
    //  1: A and B up
    int isChannelUp(ChannelType type);
}
