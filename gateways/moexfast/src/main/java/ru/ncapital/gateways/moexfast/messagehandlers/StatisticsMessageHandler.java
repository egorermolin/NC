package ru.ncapital.gateways.moexfast.messagehandlers;

import org.openfast.GroupValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ncapital.gateways.moexfast.IGatewayConfiguration;
import ru.ncapital.gateways.moexfast.MarketDataManager;
import ru.ncapital.gateways.moexfast.Utils;
import ru.ncapital.gateways.moexfast.domain.MdUpdateAction;
import ru.ncapital.gateways.moexfast.domain.impl.BBO;
import ru.ncapital.gateways.moexfast.domain.MdEntryType;
import ru.ncapital.gateways.moexfast.performance.PerformanceData;

/**
 * Created by egore on 1/21/16.
 */
public abstract class StatisticsMessageHandler<T> extends AMessageHandler<T> {
    private BBO<T> bbo;

    public StatisticsMessageHandler(MarketDataManager<T> marketDataManager, IGatewayConfiguration configuration) {
        super(marketDataManager, configuration);
    }

    @Override
    protected Logger getLogger() {
        return LoggerFactory.getLogger(getClass().getName());
    }

    @Override
    public MessageHandlerType getType() {
        return MessageHandlerType.STATISTICS;
    }

    private boolean onMdEntry(GroupValue mdEntry) {
        MdEntryType mdEntryType = getMdEntryType(mdEntry);
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
                bbo.setLastPx(getLastPx(mdEntry));
                bbo.setLastSize(getLastSize(mdEntry));
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
            case EMPTY:
                bbo.setEmpty(true);
                break;
            default:
                return false;
        }
        return true;
    }

    @Override
    protected void onBeforeSnapshot(T exchangeSecurityId) {
        bbo = marketDataManager.createBBO(exchangeSecurityId);
    }

    @Override
    protected void onSnapshotMdEntry(T exchangeSecurityId, GroupValue mdEntry) {
        onMdEntry(mdEntry);
    }

    @Override
    protected void onAfterSnapshot(T exchangeSecurityId) {
        marketDataManager.onBBO(bbo);
    }

    @Override
    protected void onIncrementalMdEntry(T exchangeSecurityId, GroupValue mdEntry, PerformanceData perfData) {
        bbo = marketDataManager.createBBO(exchangeSecurityId);
        bbo.getPerformanceData().updateFrom(perfData);

        if (onMdEntry(mdEntry))
            marketDataManager.onBBO(bbo);
    }

    @Override
    public void flushIncrementals() {
    }

    @Override
    protected final MdUpdateAction getMdUpdateAction(GroupValue mdEntry) {
        throw new RuntimeException();
    }

    @Override
    protected String getMdEntryId(GroupValue mdEntry) {
        throw new RuntimeException();
    }

    @Override
    protected final String getTradeId(GroupValue mdEntry) {
        throw new RuntimeException();
    }

    @Override
    protected final boolean getTradeIsBid(GroupValue mdEntry) {
        throw new RuntimeException();
    }
}
