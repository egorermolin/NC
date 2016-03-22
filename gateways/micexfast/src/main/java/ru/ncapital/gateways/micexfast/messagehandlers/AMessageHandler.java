package ru.ncapital.gateways.micexfast.messagehandlers;

import org.openfast.GroupValue;
import org.openfast.Message;
import org.openfast.SequenceValue;
import org.slf4j.Logger;
import ru.ncapital.gateways.micexfast.IGatewayConfiguration;
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

    private Set<String> allowedSecurityIds = new HashSet<String>();

    protected Logger logger = getLogger();

    protected MarketDataManager marketDataManager;

    protected boolean addBoardToSecurityId = false;

    protected AMessageHandler(MarketDataManager marketDataManager, IGatewayConfiguration configuration) {
        this.marketDataManager = marketDataManager;
        this.addBoardToSecurityId = configuration.addBoardToSecurityId();
        this.allowedTradingSessionIds.addAll(Arrays.asList(configuration.getAllowedTradingSessionIds(configuration.getMarketType())));
        this.allowedSecurityIds.addAll(Arrays.asList(configuration.getAllowedSecurityIds(configuration.getMarketType())));
        if (allowedSecurityIds.contains("*"))
            allowedSecurityIds.clear();
    }

    @Override
    public void onSnapshot(Message readMessage, long inTime) {
        String symbol = readMessage.getString("Symbol");
        String tradingSessionId = readMessage.getString("TradingSessionID");
        boolean firstFragment = readMessage.getInt("RouteFirst") == 1;
        boolean lastFragment = readMessage.getInt("LastFragment") == 1;

        if (allowedSecurityIds.isEmpty() || allowedTradingSessionIds.contains(TradingSessionId.convert(tradingSessionId))) {
        } else {
            if (logger.isTraceEnabled())
                logger.trace("Snapshot Filtered by TradingSessionId" + symbol + ":" + tradingSessionId);

            return;
        }

        if (allowedSecurityIds.isEmpty() || allowedSecurityIds.contains(symbol)) {
        } else {
            if (logger.isTraceEnabled())
                logger.trace("Snapshot Filtered bySymbol" + symbol + ":" + tradingSessionId);

            return;
        }

        if (logger.isTraceEnabled())
            logger.trace("SNAPSHOT " + readMessage);

        String securityId = symbol;
        if (addBoardToSecurityId)
            securityId += ":" + tradingSessionId;

        if (firstFragment)
            onBeforeSnapshot(securityId, inTime);

        SequenceValue mdEntries = readMessage.getSequence("GroupMDEntries");
        for (int i = 0; i < mdEntries.getLength(); ++i) {
            onSnapshotMdEntry(securityId, mdEntries.get(i), inTime);
        }

        if (lastFragment)
            onAfterSnapshot(securityId, inTime);
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

        if (allowedSecurityIds.isEmpty() || allowedSecurityIds.contains(symbol)) {
        } else {
            if (logger.isTraceEnabled())
                logger.trace("Incremental Filtered by Symbol " + symbol);

            return;
        }

        if (logger.isTraceEnabled())
            logger.trace("INCREMENTAL " + mdEntry);

        String securityId = symbol;
        if (addBoardToSecurityId)
            securityId += ":" + tradingSessionId;

        onIncrementalMdEntry(securityId, mdEntry, inTime);
    }

    protected abstract Logger getLogger();

    protected abstract void onBeforeSnapshot(String securityId, long inTime);

    protected abstract void onAfterSnapshot(String securityId, long inTime);

    protected abstract void onSnapshotMdEntry(String securityId, GroupValue mdEntry, long inTime);

    protected abstract void onIncrementalMdEntry(String securityId, GroupValue mdEntry, long inTime);
}
