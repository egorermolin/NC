package ru.ncapital.gateways.fortsfast.messagehandlers;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.openfast.GroupValue;
import org.openfast.Message;
import org.openfast.SequenceValue;
import ru.ncapital.gateways.moexfast.IGatewayConfiguration;
import ru.ncapital.gateways.moexfast.MarketDataManager;
import ru.ncapital.gateways.moexfast.domain.impl.DepthLevel;
import ru.ncapital.gateways.moexfast.messagehandlers.OrderListMessageHandler;

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
    protected double getLastPx(GroupValue mdEntry) {
        if (mdEntry.getValue("LastPx") == null)
            return 0;

        return mdEntry.getDouble("LastPx");
    }

    @Override
    protected long getLastSize(GroupValue mdEntry) {
        if (mdEntry.getValue("LastQty") == null)
            return 0;

        return mdEntry.getLong("LastQty");
    }

    @Override
    protected long getMdFlags(GroupValue mdEntry) {
        if (mdEntry.getValue("MDFlags") == null)
            return 0;

        return mdEntry.getLong("MDFlags");
    }

    @Override
    protected boolean isOTC(GroupValue mdEntry) {
        long otc = getMdFlags(mdEntry) & 0x0004;

        return otc != 0;
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
        if (mdEntry.getValue("TradeID") == null)
            return null;

        return String.valueOf(mdEntry.getLong("TradeID"));
    }

    @Override
    protected SequenceValue getMdEntries(Message readMessage) {
        return readMessage.getSequence("MDEntries");
    }
}
