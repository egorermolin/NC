package ru.ncapital.gateways.micexfast.messagehandlers;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.openfast.GroupValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ncapital.gateways.micexfast.IGatewayConfiguration;
import ru.ncapital.gateways.micexfast.MarketDataManager;
import ru.ncapital.gateways.micexfast.Utils;
import ru.ncapital.gateways.micexfast.domain.MdEntryType;
import ru.ncapital.gateways.micexfast.performance.PerformanceData;
import ru.ncapital.gateways.micexfast.domain.PublicTrade;

/**
 * Created by egore on 1/28/16.
 */
public class PublicTradesMessageHandler extends AMessageHandler {

    @AssistedInject
    public PublicTradesMessageHandler(MarketDataManager marketDataManager,
                                      @Assisted IGatewayConfiguration configuration) {

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
    public void onBeforeIncremental(GroupValue mdEntry) {
    }

    @Override
    public MessageHandlerType getType() {
        return MessageHandlerType.PUBLIC_TRADES;
    }
}
