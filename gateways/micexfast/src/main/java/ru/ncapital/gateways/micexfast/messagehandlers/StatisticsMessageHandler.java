package ru.ncapital.gateways.micexfast.messagehandlers;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.openfast.GroupValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ncapital.gateways.micexfast.IGatewayConfiguration;
import ru.ncapital.gateways.micexfast.MarketDataManager;
import ru.ncapital.gateways.micexfast.Utils;
import ru.ncapital.gateways.micexfast.domain.BBO;
import ru.ncapital.gateways.micexfast.domain.MdEntryType;
import ru.ncapital.gateways.micexfast.domain.PerformanceData;

/**
 * Created by egore on 1/21/16.
 */
public class StatisticsMessageHandler extends AMessageHandler {

    private BBO bbo;

    @AssistedInject
    public StatisticsMessageHandler(MarketDataManager marketDataManager,
                                    @Assisted IGatewayConfiguration configuration) {

        super(marketDataManager, configuration);
    }

    @Override
    protected Logger getLogger() {
        return LoggerFactory.getLogger("StatisticMessageHandler");
    }

    private void onMdEntry(GroupValue mdEntry) {
        MdEntryType mdEntryType = MdEntryType.convert(mdEntry.getString("MDEntryType").charAt(0));

        if (mdEntryType == null)
            return;

        switch (mdEntryType) {
            case BID:
                bbo.setBidPx(mdEntry.getDouble("MDEntryPx"));
                bbo.setBidSize(mdEntry.getDouble("MDEntrySize"));
                break;

            case OFFER:
                bbo.setOfferPx(mdEntry.getDouble("MDEntryPx"));
                bbo.setOfferSize(mdEntry.getDouble("MDEntrySize"));
                break;

            case LAST:
                bbo.setLastPx(mdEntry.getDouble("MDEntryPx"));
                bbo.setLastSize(mdEntry.getDouble("MDEntrySize"));
                bbo.getPerformanceData().setExchangeEntryTime(Utils.getEntryTimeInTicks(mdEntry));
                break;

            case LOW:
                bbo.setLowPx(mdEntry.getDouble("MDEntryPx"));
                break;

            case HIGH:
                bbo.setHighPx(mdEntry.getDouble("MDEntryPx"));
                break;

            case OPENING:
                bbo.setOpenPx(mdEntry.getDouble("MDEntryPx"));
                break;

            case CLOSING:
                bbo.setClosePx(mdEntry.getDouble("MDEntryPx"));
                break;
        }
    }

    @Override
    protected void onBeforeSnapshot(String securityId) {
        bbo = new BBO(securityId);
        bbo.setPerformanceData(new PerformanceData(0));
    }

    @Override
    protected void onSnapshotMdEntry(String securityId, GroupValue mdEntry) {
        onMdEntry(mdEntry);
    }

    @Override
    protected void onAfterSnapshot(String securityId) {
        marketDataManager.onBBO(bbo);
    }

    @Override
    protected void onIncrementalMdEntry(String securityId, GroupValue mdEntry, PerformanceData perfData) {
        bbo = new BBO(securityId);
        bbo.setPerformanceData(perfData);
        onMdEntry(mdEntry);
        marketDataManager.onBBO(bbo);
    }

    @Override
    public void flushIncrementals() {
    }

    @Override
    public void onBeforeIncremental(GroupValue mdEntry) {
    }

    @Override
    public MessageHandlerType getType() {
        return MessageHandlerType.STATISTICS;
    }
}
