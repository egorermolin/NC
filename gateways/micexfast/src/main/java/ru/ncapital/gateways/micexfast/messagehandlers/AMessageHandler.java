package ru.ncapital.gateways.micexfast.messagehandlers;

import org.openfast.GroupValue;
import org.openfast.Message;
import org.openfast.SequenceValue;
import org.slf4j.Logger;
import ru.ncapital.gateways.micexfast.IGatewayConfiguration;
import ru.ncapital.gateways.micexfast.MarketDataManager;
import ru.ncapital.gateways.micexfast.domain.Instrument;
import ru.ncapital.gateways.micexfast.domain.TradingSessionId;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by egore on 1/21/16.
 */
public abstract class AMessageHandler implements IMessageHandler {

    protected Logger logger = getLogger();

    protected MarketDataManager marketDataManager;

    protected AMessageHandler(MarketDataManager marketDataManager, IGatewayConfiguration configuration) {
        this.marketDataManager = marketDataManager;
    }

    @Override
    public boolean isAllowedUpdate(String symbol, String trandingSessionId) {
        return this.marketDataManager.isAllowedInstrument(symbol, trandingSessionId);
    }

    @Override
    public void onSnapshot(Message readMessage, long inTime) {
        String symbol = readMessage.getString("Symbol");
        String tradingSessionId = readMessage.getString("TradingSessionID");
        String securityId = symbol + Instrument.BOARD_SEPARATOR + tradingSessionId;
        boolean firstFragment = readMessage.getInt("RouteFirst") == 1;
        boolean lastFragment = readMessage.getInt("LastFragment") == 1;

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
        String securityId = symbol + Instrument.BOARD_SEPARATOR + tradingSessionId;

        beforeIncremental(mdEntry, inTime);

        onIncrementalMdEntry(securityId, mdEntry, inTime);
    }

    protected abstract Logger getLogger();

    protected abstract void onBeforeSnapshot(String securityId, long inTime);

    protected abstract void onAfterSnapshot(String securityId, long inTime);

    protected abstract void onSnapshotMdEntry(String securityId, GroupValue mdEntry, long inTime);

    protected abstract void onIncrementalMdEntry(String securityId, GroupValue mdEntry, long inTime);
}
