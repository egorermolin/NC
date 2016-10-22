package ru.ncapital.gateways.moexfast.messagehandlers;

import org.openfast.GroupValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ncapital.gateways.moexfast.IGatewayConfiguration;
import ru.ncapital.gateways.moexfast.MarketDataManager;
import ru.ncapital.gateways.moexfast.Utils;
import ru.ncapital.gateways.moexfast.domain.MdEntryType;
import ru.ncapital.gateways.moexfast.domain.MdUpdateAction;
import ru.ncapital.gateways.moexfast.domain.impl.PublicTrade;
import ru.ncapital.gateways.moexfast.performance.PerformanceData;

/**
 * Created by egore on 1/28/16.
 */
public abstract class PublicTradesMessageHandler<T> extends AMessageHandler<T> {

    public PublicTradesMessageHandler(MarketDataManager<T> marketDataManager, IGatewayConfiguration configuration) {
        super(marketDataManager, configuration);
    }

    @Override
    protected Logger getLogger() {
        return LoggerFactory.getLogger(getClass().getSimpleName());
    }

    @Override
    public MessageHandlerType getType() {
        return MessageHandlerType.PUBLIC_TRADES;
    }

    @Override
    protected void onBeforeSnapshot(T exchangeSecurityId) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    protected void onAfterSnapshot(T exchangeSecurityId) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    protected void onSnapshotMdEntry(T exchangeSecurityId, GroupValue mdEntry) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    protected void onIncrementalMdEntry(T exchangeSecurityId, GroupValue mdEntry, PerformanceData perfData) {
        MdEntryType mdEntryType = getMdEntryType(mdEntry);
        PublicTrade<T> publicTrade = null;
        switch (mdEntryType) {
            case TRADE:
                publicTrade = marketDataManager.createPublicTrade(exchangeSecurityId);

                publicTrade.setTradeId(getTradeId(mdEntry));
                publicTrade.setLastPx(getLastPx(mdEntry));
                publicTrade.setLastSize(getLastSize(mdEntry));
                publicTrade.setIsBid(getTradeIsBid(mdEntry));
                publicTrade.getPerformanceData().updateFrom(perfData).setExchangeTime(Utils.getEntryTimeInTicks(mdEntry));

                marketDataManager.onPublicTrade(publicTrade);
                break;
            default:
                break;
        }

        if (publicTrade != null)
            marketDataManager.onPublicTrade(publicTrade);
    }

    @Override
    public void flushIncrementals() {
    }

    @Override
    protected final MdUpdateAction getMdUpdateAction(GroupValue mdEntry) {
        throw new RuntimeException();
    }

    @Override
    protected final String getMdEntryId(GroupValue mdEntry) {
        throw new RuntimeException();
    }

    @Override
    protected final double getMdEntryPx(GroupValue mdEntry) {
        throw new RuntimeException();
    }

    @Override
    protected final double getMdEntrySize(GroupValue mdEntry) {
        throw new RuntimeException();
    }
}
