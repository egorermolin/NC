package ru.ncapital.gateways.moexfast.messagehandlers;

import org.openfast.GroupValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ncapital.gateways.moexfast.IGatewayConfiguration;
import ru.ncapital.gateways.moexfast.MarketDataManager;
import ru.ncapital.gateways.moexfast.Utils;
import ru.ncapital.gateways.moexfast.domain.MdEntryType;
import ru.ncapital.gateways.moexfast.domain.MdUpdateAction;
import ru.ncapital.gateways.moexfast.domain.impl.BBO;
import ru.ncapital.gateways.moexfast.domain.impl.PublicTrade;
import ru.ncapital.gateways.moexfast.performance.PerformanceData;

/**
 * Created by egore on 1/21/16.
 */
public abstract class StatisticsMessageHandler<T> extends AMessageHandler<T> {
    private BBO<T> bbo;

    private PublicTrade<T> publicTrade;

    private MdEntryHandler<T> mdEntryHandler = new MdEntryHandler<>(this);

    public StatisticsMessageHandler(MarketDataManager<T> marketDataManager, IGatewayConfiguration configuration) {
        super(marketDataManager, configuration);
    }

    @Override
    protected Logger getLogger() {
        return LoggerFactory.getLogger(getClass().getSimpleName());
    }

    @Override
    public MessageHandlerType getType() {
        return MessageHandlerType.STATISTICS;
    }

    @Override
    protected void onBeforeSnapshot(T exchangeSecurityId) {
        bbo = marketDataManager.createBBO(exchangeSecurityId);
    }

    @Override
    protected void onSnapshotMdEntry(T exchangeSecurityId, GroupValue mdEntry) {
        onMdEntry(exchangeSecurityId, mdEntry, null);
    }

    @Override
    protected void onAfterSnapshot(T exchangeSecurityId) {
        marketDataManager.onBBO(bbo);
    }

    @Override
    protected void onIncrementalMdEntry(T exchangeSecurityId, GroupValue mdEntry, PerformanceData perfData) {
        if (bbo == null) {
            bbo = marketDataManager.createBBO(exchangeSecurityId);
            bbo.getPerformanceData().updateFrom(perfData);
        }

        if (bbo.getExchangeSecurityId().equals(exchangeSecurityId)) {
            onMdEntry(exchangeSecurityId, mdEntry, perfData);
        } else {
            // flush existing bbo
            marketDataManager.onBBO(bbo);
            bbo = null;

            // process another bbo
            onIncrementalMdEntry(exchangeSecurityId, mdEntry, perfData);
        }
    }

    @Override
    public void flushIncrementals() {
        if (bbo != null)
            marketDataManager.onBBO(bbo);

        if (publicTrade != null)
            marketDataManager.onPublicTrade(publicTrade);
    }

    private BBO<T> getOrCreateBBO(T exchangeSecurityId, PerformanceData perfData) {
        if (bbo == null) {
            bbo = marketDataManager.createBBO(exchangeSecurityId);
            bbo.getPerformanceData().updateFrom(perfData);

            return bbo;
        }

        if (bbo.getExchangeSecurityId().equals(exchangeSecurityId))
            return bbo;

        marketDataManager.onBBO(bbo);
        bbo = null;

        return getOrCreateBBO(exchangeSecurityId, perfData);
    }

    private PublicTrade<T> getOrCreatePublicTrade(T exchangeSecurityId, PerformanceData perfData) {
        if (publicTrade == null) {
            publicTrade = marketDataManager.createPublicTrade(exchangeSecurityId);
            publicTrade.getPerformanceData().updateFrom(perfData);

            return publicTrade;
        }

        marketDataManager.onPublicTrade(publicTrade);
        publicTrade = null;

        return getOrCreatePublicTrade(exchangeSecurityId, perfData);
    }

    private void onMdEntry(T exchangeSecurityId, GroupValue mdEntry, PerformanceData perfData) {
        MdEntryType mdEntryType = getMdEntryType(mdEntry);
        switch (mdEntryType) {
            case BID:
            case OFFER:
            case LAST:
            case HIGH:
            case LOW:
            case OPENING:
            case CLOSING:
                getOrCreateBBO(exchangeSecurityId, perfData);
                break;
            case TRADE:
                getOrCreatePublicTrade(exchangeSecurityId, perfData);
                break;
        }

        switch (mdEntryType) {
            case BID:
                bbo.setBidPx(getMdEntryPx(mdEntry));
                bbo.setBidSize(getMdEntrySize(mdEntry));
                break;
            case OFFER:
                bbo.setOfferPx(getMdEntryPx(mdEntry));
                bbo.setOfferSize(getMdEntrySize(mdEntry));
                break;
            case LAST:
                bbo.setLastPx(getMdEntryPx(mdEntry));
                bbo.setLastSize(getMdEntrySize(mdEntry));
                bbo.getPerformanceData().setExchangeTime(Utils.getEntryTimeInTicks(mdEntry));
                break;
            case LOW:
                bbo.setLowPx(getMdEntryPx(mdEntry));
                break;
            case HIGH:
                bbo.setHighPx(getMdEntryPx(mdEntry));
                break;
            case OPENING:
                bbo.setOpenPx(getMdEntryPx(mdEntry));
                break;
            case CLOSING:
                bbo.setClosePx(getMdEntryPx(mdEntry));
                break;
            case TRADE:
                publicTrade.setTradeId(getTradeId(mdEntry));
                publicTrade.setLastPx(getLastPx(mdEntry));
                publicTrade.setLastSize(getLastSize(mdEntry));
                publicTrade.setIsBid(getTradeIsBid(mdEntry));
                publicTrade.getPerformanceData().setExchangeTime(Utils.getEntryTimeInTicks(mdEntry));
                break;
            case EMPTY:
                bbo.setEmpty(true);
                break;
            default:
                break;
        }
    }
}
