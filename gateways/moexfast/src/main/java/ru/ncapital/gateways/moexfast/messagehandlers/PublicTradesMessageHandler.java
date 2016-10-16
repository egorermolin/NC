package ru.ncapital.gateways.moexfast.messagehandlers;

import org.openfast.GroupValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ncapital.gateways.moexfast.IGatewayConfiguration;
import ru.ncapital.gateways.moexfast.MarketDataManager;
import ru.ncapital.gateways.moexfast.Utils;
import ru.ncapital.gateways.moexfast.domain.MdEntryType;
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
        return LoggerFactory.getLogger("PublicTradesMessageHandler");
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
        MdEntryType mdEntryType = MdEntryType.convert(mdEntry.getString("MDEntryType").charAt(0));

        if (mdEntryType == null)
            return;

        PublicTrade<T> publicTrade = null;
        switch (mdEntryType) {
            case TRADE:
                publicTrade = createPublicTrade(exchangeSecurityId, mdEntry);

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

    private PublicTrade<T> createPublicTrade(T exchangeSecurityId, GroupValue mdEntry) {
        return createPublicTrade(
                marketDataManager.convertExchangeSecurityIdToSecurityId(exchangeSecurityId),
                exchangeSecurityId,
                getMdEntryId(mdEntry),
                getLastPx(mdEntry),
                getMdEntrySize(mdEntry),
                getMdEntryIsBid(mdEntry)
        );
    }

    protected abstract PublicTrade<T> createPublicTrade(String securityId, T exchangeSecurityId, String mdEntryId, double mdEntryPx, double mdEntrySize, boolean isBid);

}
