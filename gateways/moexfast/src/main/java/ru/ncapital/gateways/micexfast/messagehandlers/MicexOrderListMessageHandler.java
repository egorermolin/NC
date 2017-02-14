package ru.ncapital.gateways.micexfast.messagehandlers;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.openfast.GroupValue;
import org.openfast.Message;
import org.openfast.SequenceValue;
import ru.ncapital.gateways.micexfast.MicexMarketDataManager;
import ru.ncapital.gateways.micexfast.domain.MicexInstrument;
import ru.ncapital.gateways.moexfast.IGatewayConfiguration;
import ru.ncapital.gateways.moexfast.domain.impl.DepthLevel;
import ru.ncapital.gateways.moexfast.messagehandlers.OrderListMessageHandler;

import java.util.List;

/**
 * Created by Egor on 30-Sep-16.
 */
public class MicexOrderListMessageHandler extends OrderListMessageHandler<String> {
    @AssistedInject
    public MicexOrderListMessageHandler(MicexMarketDataManager marketDataManager, @Assisted IGatewayConfiguration configuration) {
        super(marketDataManager, configuration);
    }

    @Override
    protected double getLastPx(GroupValue mdEntry) {
        throw new UnsupportedOperationException("LastPx not supported for MICEX");
    }

    @Override
    protected long getLastSize(GroupValue mdEntry) {
        throw new UnsupportedOperationException("LastSize not supported for MICEX");
    }

    @Override
    protected long getMdFlags(GroupValue mdEntry) {
        return 0;
    }

    @Override
    protected boolean isOTC(GroupValue mdEntry) {
        return false;
    }

    @Override
    protected DepthLevel<String>[] depthLevelsToArray(List<DepthLevel<String>> list) {
        @SuppressWarnings("unchecked")
        DepthLevel<String>[] array = new DepthLevel[list.size()];
        for (int i = 0; i < array.length; ++i)
            array[i] = list.get(i);

        return array;
    }

    @Override
    protected String getExchangeSecurityId(Message readMessage) {
        String symbol = readMessage.getString("Symbol");
        String tradingSessionId = readMessage.getString("TradingSessionID");

        return MicexInstrument.getSecurityId(symbol, tradingSessionId);
    }

    @Override
    protected String getExchangeSecurityId(GroupValue mdEntry) {
        String symbol = mdEntry.getString("Symbol");
        String tradingSessionId = mdEntry.getString("TradingSessionID");

        return MicexInstrument.getSecurityId(symbol, tradingSessionId);
    }

    @Override
    protected String getTradeId(GroupValue mdEntry) {
        if (gatewayConfiguration.getVersion() == IGatewayConfiguration.Version.V2016)
            return mdEntry.getString("DealNumber");

        return null;
    }

    @Override
    protected SequenceValue getMdEntries(Message readMessage) {
        return readMessage.getSequence("GroupMDEntries");
    }
}
