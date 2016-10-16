package ru.ncapital.gateways.fortsfast.messagehandlers;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.openfast.GroupValue;
import org.openfast.Message;
import ru.ncapital.gateways.fortsfast.domain.FortsDepthLevel;
import ru.ncapital.gateways.micexfast.MicexMarketDataManager;
import ru.ncapital.gateways.micexfast.domain.MicexDepthLevel;
import ru.ncapital.gateways.micexfast.domain.MicexInstrument;
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
    protected DepthLevel<Long> createDepthLevel(String securityId, Long exchangeSecurityId, MdUpdateAction action, String mdEntryId, double mdEntryPx, double mdEntrySize, String tradeId, boolean isBid) {
        return new FortsDepthLevel(securityId, exchangeSecurityId, action, mdEntryId, mdEntryPx, mdEntrySize, tradeId, isBid);
    }

    @Override
    protected List<DepthLevel<Long>> createDepthLevels() {
        return new ArrayList<>();
    }

    @Override
    protected DepthLevel<Long>[] convertDepthLevels(List<DepthLevel<Long>> depthLevels) {
        return depthLevels.toArray((DepthLevel<Long>[]) new FortsDepthLevel[0]);
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
