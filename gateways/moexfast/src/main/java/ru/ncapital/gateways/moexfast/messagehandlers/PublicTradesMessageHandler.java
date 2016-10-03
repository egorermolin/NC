package ru.ncapital.gateways.moexfast.messagehandlers;

import org.openfast.GroupValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ncapital.gateways.moexfast.IGatewayConfiguration;
import ru.ncapital.gateways.micexfast.MarketDataManager;
import ru.ncapital.gateways.moexfast.Utils;
import ru.ncapital.gateways.moexfast.domain.MdEntryType;
import ru.ncapital.gateways.moexfast.performance.PerformanceData;
import ru.ncapital.gateways.moexfast.domain.PublicTrade;

/**
 * Created by egore on 1/28/16.
 */
public abstract class PublicTradesMessageHandler extends AMessageHandler {

    public PublicTradesMessageHandler(MarketDataManager marketDataManager, IGatewayConfiguration configuration) {
        super(marketDataManager, configuration);
    }

    @Override
    protected Logger getLogger() {
        return LoggerFactory.getLogger("PublicTradesMessageHandler");
    }

    @Override
    protected void onBeforeSnapshot(String securityId) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    protected void onAfterSnapshot(String securityId) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    protected void onSnapshotMdEntry(String securityId, GroupValue mdEntry) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    protected void onIncrementalMdEntry(String securityId, GroupValue mdEntry, PerformanceData perfData) {
        MdEntryType mdEntryType = MdEntryType.convert(mdEntry.getString("MDEntryType").charAt(0));

        if (mdEntryType == null)
            return;

        PublicTrade publicTrade = null;
        switch (mdEntryType) {
            case TRADE:
                publicTrade = new PublicTrade(securityId,
                        mdEntry.getString("MDEntryID"),
                        mdEntry.getDouble("MDEntryPx"),
                        mdEntry.getDouble("MDEntrySize"),
                        mdEntry.getString("OrderSide").charAt(0) == 'B');

                publicTrade.getPerformanceData().updateFrom(perfData).setExchangeTime(Utils.getEntryTimeInTicks(mdEntry));
                break;
            case EMPTY:
                break;
        }

        if (publicTrade != null)
            marketDataManager.onPublicTrade(publicTrade);
    }

    @Override
    public void flushIncrementals() {
    }

    @Override
    public MessageHandlerType getType() {
        return MessageHandlerType.PUBLIC_TRADES;
    }
}
