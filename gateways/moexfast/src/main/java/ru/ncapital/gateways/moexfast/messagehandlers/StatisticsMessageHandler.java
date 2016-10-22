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
import ru.ncapital.gateways.moexfast.performance.PerformanceData;

/**
 * Created by egore on 1/21/16.
 */
public abstract class StatisticsMessageHandler<T> extends AMessageHandler<T> {
    private BBO<T> bbo;

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
        mdEntryHandler.onMdEntry(bbo, mdEntry);
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
            mdEntryHandler.onMdEntry(bbo, mdEntry);
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
        // if there was registered bbo update
        if (bbo != null)
            marketDataManager.onBBO(bbo);
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
