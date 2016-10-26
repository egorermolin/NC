package ru.ncapital.gateways.fortsfast.messagehandlers;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.openfast.GroupValue;
import org.openfast.Message;
import ru.ncapital.gateways.moexfast.IGatewayConfiguration;
import ru.ncapital.gateways.moexfast.MarketDataManager;
import ru.ncapital.gateways.moexfast.domain.MdEntryType;
import ru.ncapital.gateways.moexfast.domain.MdUpdateAction;
import ru.ncapital.gateways.moexfast.domain.impl.DepthLevel;
import ru.ncapital.gateways.moexfast.messagehandlers.OrderListMessageHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Egor on 30-Sep-16.
 */
public class FortsOrderListMessageHandler extends OrderListMessageHandler<Long> {

    @AssistedInject
    public FortsOrderListMessageHandler(MarketDataManager<Long> marketDataManager, @Assisted IGatewayConfiguration configuration) {
        super(marketDataManager, configuration);
    }

    @Override
    protected DepthLevel<Long>[] depthLevelsToArray(List<DepthLevel<Long>> list) {
        @SuppressWarnings("unchecked")
        DepthLevel<Long>[] array = new DepthLevel[list.size()];
        for (int i = 0; i < array.length; ++i)
            array[i] = list.get(i);

        return array;
    }

    @Override
    protected Long getExchangeSecurityId(Message readMessage) {
        return readMessage.getLong("SecurityID");
    }

    @Override
    protected Long getExchangeSecurityId(GroupValue mdEntry) {
        return mdEntry.getLong("SecurityID");
    }

    @Override
    protected String getTradeId(GroupValue mdEntry) {
        return String.valueOf(mdEntry.getLong("TradeID"));
    }
}
