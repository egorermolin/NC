package ru.ncapital.gateways.moexfast.messagehandlers;

import org.openfast.GroupValue;
import org.openfast.Message;
import org.openfast.SequenceValue;
import org.slf4j.Logger;
import ru.ncapital.gateways.moexfast.IGatewayConfiguration;
import ru.ncapital.gateways.moexfast.MarketDataManager;
import ru.ncapital.gateways.moexfast.domain.MdEntryType;
import ru.ncapital.gateways.moexfast.domain.MdUpdateAction;
import ru.ncapital.gateways.moexfast.performance.PerformanceData;

/**
 * Created by egore on 1/21/16.
 */
public abstract class AMessageHandler<T> implements IMessageHandler<T> {

    protected Logger logger = getLogger();

    protected MarketDataManager<T> marketDataManager;

    protected IGatewayConfiguration gatewayConfiguration;

    protected AMessageHandler(MarketDataManager<T> marketDataManager, IGatewayConfiguration gatewayConfiguration) {
        this.marketDataManager = marketDataManager;
        this.gatewayConfiguration = gatewayConfiguration;
    }

    @Override
    public boolean isAllowedUpdate(T exchangeSecurityId) {
        return marketDataManager.isAllowedInstrument(exchangeSecurityId);
    }

    @Override
    public void onSnapshot(Message readMessage) {
        T exchangeSecurityId = getExchangeSecurityId(readMessage);
        boolean firstFragment = readMessage.getInt("RouteFirst") == 1;
        boolean lastFragment = readMessage.getInt("LastFragment") == 1;

        if (firstFragment)
            onBeforeSnapshot(exchangeSecurityId);

        SequenceValue mdEntries = readMessage.getSequence("GroupMDEntries");
        for (int i = 0; i < mdEntries.getLength(); ++i) {
            onSnapshotMdEntry(exchangeSecurityId, mdEntries.get(i));
        }

        if (lastFragment)
            onAfterSnapshot(exchangeSecurityId);
    }

    @Override
    public void onIncremental(GroupValue mdEntry, PerformanceData perfData) {
        T exchangeSecurityId = getExchangeSecurityId(mdEntry);

        onIncrementalMdEntry(exchangeSecurityId, mdEntry, perfData);
    }

    protected abstract Logger getLogger();

    protected abstract void onBeforeSnapshot(T exchangeSecurityId);

    protected abstract void onAfterSnapshot(T exchangeSecurityId);

    protected abstract void onSnapshotMdEntry(T exchangeSecurityId, GroupValue mdEntry);

    protected abstract void onIncrementalMdEntry(T exchangeSecurityId, GroupValue mdEntry, PerformanceData perfData);

    protected abstract T getExchangeSecurityId(Message readMessage);

    protected abstract T getExchangeSecurityId(GroupValue mdEntry);

    protected abstract boolean getMdEntryIsBid(GroupValue mdEntry);

    protected abstract String getMdEntryId(GroupValue mdEntry);

    protected abstract double getMdEntryPx(GroupValue mdEntry);

    protected abstract double getLastPx(GroupValue mdEntry);

    protected abstract double getMdEntrySize(GroupValue mdEntry);

    protected abstract String getTradeId(GroupValue mdEntry);

    protected abstract MdEntryType getMdEntryType(GroupValue mdEntry);

    protected abstract MdUpdateAction getMdUpdateAction(GroupValue mdEntry);
}
