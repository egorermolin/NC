package ru.ncapital.gateways.micexfast.messagehandlers;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.openfast.GroupValue;
import org.openfast.Message;
import ru.ncapital.gateways.micexfast.domain.MicexInstrument;
import ru.ncapital.gateways.moexfast.IGatewayConfiguration;
import ru.ncapital.gateways.moexfast.MarketDataManager;
import ru.ncapital.gateways.moexfast.domain.MdEntryType;
import ru.ncapital.gateways.moexfast.domain.MdUpdateAction;
import ru.ncapital.gateways.moexfast.messagehandlers.PublicTradesMessageHandler;

/**
 * Created by Egor on 30-Sep-16.
 */
public class MicexPublicTradesMessageHandler extends PublicTradesMessageHandler {
    @AssistedInject
    public MicexPublicTradesMessageHandler(MarketDataManager marketDataManager, @Assisted IGatewayConfiguration configuration) {
        super(marketDataManager, configuration);
    }

    @Override
    protected String getSecurityId(Message readMessage) {
        String symbol = readMessage.getString("Symbol");
        String tradingSessionId = readMessage.getString("TradingSessionID");

        return MicexInstrument.getSecurityId(symbol, tradingSessionId);
    }

    @Override
    protected String getSecurityId(GroupValue mdEntry) {
        String symbol = mdEntry.getString("Symbol");
        String tradingSessionId = mdEntry.getString("TradingSessionID");

        return MicexInstrument.getSecurityId(symbol, tradingSessionId);
    }

    @Override
    protected String getMdEntryId(GroupValue mdEntry) {
        return mdEntry.getString("MDEntryID");
    }

    @Override
    protected double getMdEntryPx(GroupValue mdEntry) {
        return mdEntry.getDouble("MDEntryPx");
    }

    @Override
    protected double getMdEntrySize(GroupValue mdEntry) {
        return mdEntry.getDouble("MDEntrySize");
    }

    @Override
    protected String getTradeId(GroupValue mdEntry) {
        return mdEntry.getString("DealNumber");
    }

    @Override
    protected MdEntryType getMdEntryType(GroupValue mdEntry) {
        return MdEntryType.convert(mdEntry.getString("MDEntryType").charAt(0));
    }

    @Override
    protected MdUpdateAction getMdUpdateAction(GroupValue mdEntry) {
        return MdUpdateAction.convert(mdEntry.getString("MDUpdateAction").charAt(0));
    }
}
