package ru.ncapital.gateways.micexfast.messagehandlers;

import org.openfast.GroupValue;
import org.openfast.Message;
import org.openfast.SequenceValue;
import org.slf4j.Logger;
import ru.ncapital.gateways.micexfast.MarketDataManager;
import ru.ncapital.gateways.micexfast.domain.TradingSessionId;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by egore on 1/21/16.
 */
public abstract class AMessageHandler implements IMessageHandler {

    private Set<TradingSessionId> allowedTradingSessionIds = new HashSet<TradingSessionId>();

    private Set<String> allowedSymbols = new HashSet<String>();

    protected Logger logger = getLogger();

    protected MarketDataManager marketDataManager;

    protected AMessageHandler(MarketDataManager marketDataManager, TradingSessionId[] tradingSessionIds, String[] symbols) {
        this.marketDataManager = marketDataManager;
        this.allowedTradingSessionIds.addAll(Arrays.asList(tradingSessionIds));
        this.allowedSymbols.addAll(Arrays.asList(symbols));
        if (allowedSymbols.contains("*"))
            allowedSymbols.clear();
    }

    @Override
    public void onSnapshot(Message readMessage, long inTime) {
        String symbol = readMessage.getString("Symbol");
        String tradingSessionId = readMessage.getString("TradingSessionID");
        boolean firstFragment = readMessage.getInt("RouteFirst") == 1;
        boolean lastFragment = readMessage.getInt("LastFragment") == 1;

        if (allowedSymbols.isEmpty() || allowedTradingSessionIds.contains(TradingSessionId.convert(tradingSessionId))) {
        } else {
            if (logger.isTraceEnabled())
                logger.trace("Snapshot Filtered by TradingSessionId" + symbol + ":" + tradingSessionId);

            return;
        }

        if (allowedSymbols.isEmpty() || allowedSymbols.contains(symbol)) {
        } else {
            if (logger.isTraceEnabled())
                logger.trace("Snapshot Filtered bySymbol" + symbol + ":" + tradingSessionId);

            return;
        }

        if (logger.isTraceEnabled())
            logger.trace("SNAPSHOT " + readMessage);

        if (firstFragment)
            onBeforeSnapshot(symbol, inTime);

        SequenceValue mdEntries = readMessage.getSequence("GroupMDEntries");
        for (int i = 0; i < mdEntries.getLength(); ++i) {
            onSnapshotMdEntry(symbol, mdEntries.get(i), inTime);
        }

        if (lastFragment)
            onAfterSnapshot(symbol, inTime);
    }

    @Override
    public void onIncremental(GroupValue mdEntry, long inTime) {
        String symbol = mdEntry.getString("Symbol");
        String tradingSessionId = mdEntry.getString("TradingSessionID");

        beforeIncremental(mdEntry, inTime);

        if (allowedTradingSessionIds.isEmpty() || allowedTradingSessionIds.contains(TradingSessionId.convert(tradingSessionId))) {
        } else {
            if (logger.isTraceEnabled())
                logger.trace("Incremental Filtered by TradingSessionId " + symbol + ":" + tradingSessionId);

            return;
        }

        if (allowedSymbols.isEmpty() || allowedSymbols.contains(symbol)) {
        } else {
            if (logger.isTraceEnabled())
                logger.trace("Incremental Filtered by Symbol " + symbol);

            return;
        }

        if (logger.isTraceEnabled())
            logger.trace("INCREMENTAL " + mdEntry);

        onIncrementalMdEntry(symbol, mdEntry, inTime);
    }

    protected abstract Logger getLogger();

    protected abstract void onBeforeSnapshot(String symbol, long inTime);

    protected abstract void onAfterSnapshot(String symbol, long inTime);

    protected abstract void onSnapshotMdEntry(String symbol, GroupValue mdEntry, long inTime);

    protected abstract void onIncrementalMdEntry(String symbol, GroupValue mdEntry, long inTime);
}
