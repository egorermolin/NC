package ru.ncapital.gateways.fortsfast.messagehandlers;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.openfast.GroupValue;
import org.openfast.Message;
import ru.ncapital.gateways.fortsfast.domain.FortsPublicTrade;
import ru.ncapital.gateways.micexfast.domain.MicexInstrument;
import ru.ncapital.gateways.moexfast.IGatewayConfiguration;
import ru.ncapital.gateways.moexfast.MarketDataManager;
import ru.ncapital.gateways.moexfast.domain.MdEntryType;
import ru.ncapital.gateways.moexfast.domain.MdUpdateAction;
import ru.ncapital.gateways.moexfast.domain.impl.PublicTrade;
import ru.ncapital.gateways.moexfast.messagehandlers.PublicTradesMessageHandler;

/**
 * Created by Egor on 30-Sep-16.
 */
public class FortsPublicTradesMessageHandler extends PublicTradesMessageHandler<Long> {
    @AssistedInject
    public FortsPublicTradesMessageHandler(MarketDataManager<Long> marketDataManager, @Assisted IGatewayConfiguration configuration) {
        super(marketDataManager, configuration);
    }

    @Override
    protected PublicTrade<Long> createPublicTrade(String securityId, Long exchangeSecurityId, String mdEntryId, double mdEntryPx, double mdEntrySize, boolean isBid) {
        return new FortsPublicTrade(securityId, exchangeSecurityId, mdEntryId, mdEntryPx, mdEntrySize, isBid);
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
    protected boolean getMdEntryIsBid(GroupValue mdEntry) {
        return getMdEntryType(mdEntry) == MdEntryType.BID;
    }

    @Override
    protected String getMdEntryId(GroupValue mdEntry) {
        return String.valueOf(mdEntry.getLong("MDEntryID"));
    }

    @Override
    protected double getMdEntryPx(GroupValue mdEntry) {
        return mdEntry.getDouble("MDEntryPx");
    }

    @Override
    protected double getLastPx(GroupValue mdEntry) {
        return mdEntry.getDouble("LastPx");
    }

    @Override
    protected double getMdEntrySize(GroupValue mdEntry) {
        return mdEntry.getLong("MDEntrySize");
    }

    @Override
    protected String getTradeId(GroupValue mdEntry) {
        return String.valueOf(mdEntry.getLong("TradeID"));
    }

    @Override
    protected MdEntryType getMdEntryType(GroupValue mdEntry) {
        return MdEntryType.convert(mdEntry.getString("MDEntryType").charAt(0));
    }

    @Override
    protected MdUpdateAction getMdUpdateAction(GroupValue mdEntry) {
        return MdUpdateAction.convert(String.valueOf(mdEntry.getInt("MDUpdateAction")).charAt(0));
    }
}
