package ru.ncapital.gateways.moexfast.messagehandlers;

import ru.ncapital.gateways.moexfast.domain.intf.IChannelStatus;

/**
 * Created by egore on 5/5/16.
 */
public enum MessageHandlerType {
    ORDER_LIST("OrderList"),
    STATISTICS("Statistics"),
    PUBLIC_TRADES("PublicTrades"),
    ORDER_BOOK("OrderBook");

    private String description;

    MessageHandlerType(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }

    public boolean equals(String type) {
        return description.equals(type);
    }

    public IChannelStatus.ChannelType convert() {
        switch (this) {
            case ORDER_BOOK:
                return IChannelStatus.ChannelType.BBO;
            case ORDER_LIST:
                return IChannelStatus.ChannelType.OrderList;
            case STATISTICS:
                return IChannelStatus.ChannelType.Statistics;
            case PUBLIC_TRADES:
                return IChannelStatus.ChannelType.PublicTrade;
        }
        return null;
    }
}
