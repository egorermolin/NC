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

    protected MarketDataManager marketDataManager;

    protected IGatewayConfiguration gatewayConfiguration;

    protected AMessageHandler(MarketDataManager marketDataManager, IGatewayConfiguration gatewayConfiguration) {
        this.marketDataManager = marketDataManager;
        this.gatewayConfiguration = gatewayConfiguration;
    }

    @Override
    public boolean isAllowedUpdate(T securityId) {
        if (securityId instanceof String) // MICEX
            return marketDataManager.isAllowedInstrument((String) securityId);
        else if (securityId instanceof Long) // FORTS
            return marketDataManager.isAllowedInstrument((Long) securityId);
        else
            throw new RuntimeException("Unknown securityId type");
    }

    @Override
    public void onSnapshot(Message readMessage) {
        String securityId = getSecurityId(readMessage);
        boolean firstFragment = readMessage.getInt("RouteFirst") == 1;
        boolean lastFragment = readMessage.getInt("LastFragment") == 1;

        if (firstFragment)
            onBeforeSnapshot(securityId);

        SequenceValue mdEntries = readMessage.getSequence("GroupMDEntries");
        for (int i = 0; i < mdEntries.getLength(); ++i) {
            onSnapshotMdEntry(securityId, mdEntries.get(i));
        }

        if (lastFragment)
            onAfterSnapshot(securityId);
    }

    @Override
    public void onIncremental(GroupValue mdEntry, PerformanceData perfData) {
        String securityId = getSecurityId(mdEntry);

        onIncrementalMdEntry(securityId, mdEntry, perfData);
    }

    protected abstract Logger getLogger();

    protected abstract void onBeforeSnapshot(String securityId);

    protected abstract void onAfterSnapshot(String securityId);

    protected abstract void onSnapshotMdEntry(String securityId, GroupValue mdEntry);

    protected abstract void onIncrementalMdEntry(String securityId, GroupValue mdEntry, PerformanceData perfData);

    protected abstract String getSecurityId(Message readMessage);

    protected abstract String getSecurityId(GroupValue mdEntry);

    protected abstract String getMdEntryId(GroupValue mdEntry);

    protected abstract double getMdEntryPx(GroupValue mdEntry);

    protected abstract double getMdEntrySize(GroupValue mdEntry);

    protected abstract String getTradeId(GroupValue mdEntry);

    protected abstract MdEntryType getMdEntryType(GroupValue mdEntry);

    protected abstract MdUpdateAction getMdUpdateAction(GroupValue mdEntry);
}
