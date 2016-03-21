package ru.ncapital.gateways.micexfast.messagehandlers;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.openfast.GroupValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ncapital.gateways.micexfast.MarketDataManager;
import ru.ncapital.gateways.micexfast.Utils;
import ru.ncapital.gateways.micexfast.domain.BBO;
import ru.ncapital.gateways.micexfast.domain.MdEntryType;
import ru.ncapital.gateways.micexfast.domain.TradingSessionId;

/**
 * Created by egore on 1/21/16.
 */
public class StatisticsMessageHandler extends AMessageHandler {

    private BBO bbo;

    @AssistedInject
    public StatisticsMessageHandler(MarketDataManager marketDataManager,
                                    @Assisted TradingSessionId[] allowedTradingSessionIds,
                                    @Assisted String[] allowedSymbols) {

        super(marketDataManager, allowedTradingSessionIds, allowedSymbols);
    }

    @Override
    protected Logger getLogger() {
        return LoggerFactory.getLogger("StatisticMessageHandler");
    }

    @Override
    protected void onSnapshotMdEntry(String symbol, GroupValue mdEntry, long inTime) {
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
                bbo.setLastTime(Utils.getEntryTimeInTicks(mdEntry));
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
    protected void onIncrementalMdEntry(String symbol, GroupValue mdEntry, long inTime) {
        onBeforeSnapshot(symbol, inTime);
        onSnapshotMdEntry(symbol, mdEntry, inTime);
        onAfterSnapshot(symbol, inTime);
    }

    @Override
    protected void onBeforeSnapshot(String symbol, long inTime) {
        bbo = new BBO(symbol);
    }

    @Override
    protected void onAfterSnapshot(String symbol, long inTime) {
        marketDataManager.onBBO(bbo, inTime);
    }

    @Override
    public void flushIncrementals(long inTime) {
    }

    @Override
    public void beforeIncremental(GroupValue mdEntry, long inTime) {
    }

    @Override
    public String getType() {
        return "Statistic";
    }
}
