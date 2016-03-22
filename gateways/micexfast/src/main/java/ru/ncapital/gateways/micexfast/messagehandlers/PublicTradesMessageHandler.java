package ru.ncapital.gateways.micexfast.messagehandlers;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.openfast.GroupValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ncapital.gateways.micexfast.IGatewayConfiguration;
import ru.ncapital.gateways.micexfast.MarketDataManager;
import ru.ncapital.gateways.micexfast.Utils;
import ru.ncapital.gateways.micexfast.domain.MdEntryType;
import ru.ncapital.gateways.micexfast.domain.PublicTrade;
import ru.ncapital.gateways.micexfast.domain.TradingSessionId;

/**
 * Created by egore on 1/28/16.
 */
public class PublicTradesMessageHandler extends AMessageHandler {

    @AssistedInject
    public PublicTradesMessageHandler(MarketDataManager marketDataManager,
                                      @Assisted IGatewayConfiguration configuration) {

        super(marketDataManager, configuration);
    }

    @Override
    protected Logger getLogger() {
        return LoggerFactory.getLogger("PublicTradesMessageHandler");
    }

    @Override
    protected void onBeforeSnapshot(String securityId, long inTime) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    protected void onAfterSnapshot(String securityId, long inTime) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    protected void onSnapshotMdEntry(String securityId, GroupValue mdEntry, long inTime) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    protected void onIncrementalMdEntry(String securityId, GroupValue mdEntry, long inTime) {
        MdEntryType mdEntryType = MdEntryType.convert(mdEntry.getString("MDEntryType").charAt(0));

        if (mdEntryType == null) {
            logger.error("Unknown mdEntryType : " + mdEntry);
            return;
        }

        PublicTrade publicTrade = null;
        switch (mdEntryType) {
            case TRADE:
                publicTrade = new PublicTrade(securityId,
                        mdEntry.getString("MDEntryID"),
                        mdEntry.getDouble("MDEntryPx"),
                        mdEntry.getDouble("MDEntrySize"),
                        mdEntry.getString("OrderSide").charAt(0) == 'B');

                publicTrade.setLastTime(Utils.getEntryTimeInTicks(mdEntry));
                break;
            case EMPTY:
                break;
        }

        if (publicTrade != null)
            marketDataManager.onPublicTrade(publicTrade, inTime);
    }

    @Override
    public void flushIncrementals(long inTime) {
    }

    @Override
    public void beforeIncremental(GroupValue mdEntry, long inTime) {
    }

    @Override
    public String getType() {
        return "PublicTradesMessageHandler";
    }
}
