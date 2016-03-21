package ru.ncapital.gateways.micexfast.messagehandlers;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.openfast.GroupValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ncapital.gateways.micexfast.MarketDataManager;
import ru.ncapital.gateways.micexfast.Utils;
import ru.ncapital.gateways.micexfast.domain.DepthLevel;
import ru.ncapital.gateways.micexfast.domain.MdEntryType;
import ru.ncapital.gateways.micexfast.domain.MdUpdateAction;
import ru.ncapital.gateways.micexfast.domain.TradingSessionId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by egore on 12/7/15.
 */
public class OrderListMessageHandler extends AMessageHandler {

    private Map<String, List<DepthLevel>> depthLevelMap = new HashMap<String, List<DepthLevel>>();

    protected String lastDealNumber;

    @AssistedInject
    public OrderListMessageHandler(MarketDataManager marketDataManager,
                                   @Assisted TradingSessionId[] allowedTradingSessionIds,
                                   @Assisted String[] allowedSymbols) {

        super(marketDataManager, allowedTradingSessionIds, allowedSymbols);
    }

    @Override
    protected Logger getLogger() {
        return LoggerFactory.getLogger("OrderListMessageHandler");
    }

    @Override
    protected void onSnapshotMdEntry(String symbol, GroupValue mdEntry, long inTime) {
        MdEntryType mdEntryType = MdEntryType.convert(mdEntry.getString("MDEntryType").charAt(0));

        if (mdEntryType == null) {
            logger.error("Unknown mdEntryType : " + mdEntry);
            return;
        }

        DepthLevel depthLevel = null;
        switch (mdEntryType) {
            case BID:
                depthLevel =
                        new DepthLevel(symbol,
                                MdUpdateAction.INSERT,
                                mdEntry.getString("MDEntryID"),
                                mdEntry.getDouble("MDEntryPx"),
                                mdEntry.getDouble("MDEntrySize"),
                                null,
                                true);

                depthLevel.setMdEntryTime(Utils.getEntryTimeInTicks(mdEntry));
                break;
            case OFFER:
                depthLevel =
                        new DepthLevel(symbol,
                                MdUpdateAction.INSERT,
                                mdEntry.getString("MDEntryID"),
                                mdEntry.getDouble("MDEntryPx"),
                                mdEntry.getDouble("MDEntrySize"),
                                null,
                                false);

                depthLevel.setMdEntryTime(Utils.getEntryTimeInTicks(mdEntry));
                break;
            case EMPTY:
                depthLevel = new DepthLevel(symbol, MdUpdateAction.SNAPSHOT);
                break;
            default:
                logger.warn("Unhandled snapshot mdEntry " + mdEntry.toString());
                break;
        }

        if (depthLevel != null) {
            List<DepthLevel> depthLevelList = depthLevelMap.get(symbol);
            if (depthLevelList == null) {
                depthLevelList = new ArrayList<DepthLevel>();
                depthLevelMap.put(symbol, depthLevelList);
            }
            depthLevelList.add(depthLevel);
        }
    }

    @Override
    public void onIncrementalMdEntry(String symbol, GroupValue mdEntry, long inTime) {
        MdEntryType mdEntryType = MdEntryType.convert(mdEntry.getString("MDEntryType").charAt(0));
        MdUpdateAction mdUpdateAction = MdUpdateAction.convert(mdEntry.getString("MDUpdateAction").charAt(0));

        if (mdEntryType == null) {
            logger.error("Unknown mdEntryType : " + mdEntry);
            return;
        }

        DepthLevel depthLevel = null;
        switch (mdEntryType) {
            case BID:
                depthLevel =
                        new DepthLevel(symbol,
                                mdUpdateAction,
                                mdEntry.getString("MDEntryID"),
                                mdEntry.getDouble("MDEntryPx"),
                                mdUpdateAction == MdUpdateAction.DELETE ? 0.0 : mdEntry.getDouble("MDEntrySize"),
                                mdEntry.getString("DealNumber"),
                                true);

                depthLevel.setMdEntryTime(Utils.getEntryTimeInTicks(mdEntry));
                break;
            case OFFER:
                depthLevel =
                        new DepthLevel(symbol,
                                mdUpdateAction,
                                mdEntry.getString("MDEntryID"),
                                mdEntry.getDouble("MDEntryPx"),
                                mdUpdateAction == MdUpdateAction.DELETE ? 0.0 : mdEntry.getDouble("MDEntrySize"),
                                mdEntry.getString("DealNumber"),
                                false);

                depthLevel.setMdEntryTime(Utils.getEntryTimeInTicks(mdEntry));
                break;
            case EMPTY:
                depthLevel = new DepthLevel(symbol, MdUpdateAction.SNAPSHOT);
                break;
            default:
                logger.warn("Unhandled incremental mdEntry " + mdEntry.toString());
                break;
        }

        if (depthLevel != null) {
            List<DepthLevel> depthLevelList = depthLevelMap.get(symbol);
            if (depthLevelList == null) {
                depthLevelList = new ArrayList<DepthLevel>();
                depthLevelMap.put(symbol, depthLevelList);
            }
            depthLevelList.add(depthLevel);
        }
    }

    @Override
    protected void onBeforeSnapshot(String symbol, long inTime) {
        List<DepthLevel> depthLevelList = new ArrayList<DepthLevel>();
        depthLevelMap.put(symbol, depthLevelList);
        depthLevelList.add(new DepthLevel(symbol, MdUpdateAction.SNAPSHOT));
    }

    @Override
    protected void onAfterSnapshot(String symbol, long inTime) {
        for (List<DepthLevel> depthLevelList : depthLevelMap.values()) {
            marketDataManager.onDepthLevels(depthLevelList.toArray(new DepthLevel[0]), inTime);
        }
        depthLevelMap.clear();
    }

    @Override
    public void beforeIncremental(GroupValue mdEntry, long inTime) {
        String dealNumber = mdEntry.getString("DealNumber");
        if (dealNumber!= null && lastDealNumber != dealNumber) {
            lastDealNumber = dealNumber;
        } else {
            mdEntry.setString("DealNumber", null);
        }
    }

    @Override
    public void flushIncrementals(long inTime) {
        for (List<DepthLevel> depthLevelList : depthLevelMap.values()) {
            marketDataManager.onDepthLevels(depthLevelList.toArray(new DepthLevel[0]), inTime);
        }
        depthLevelMap.clear();
    }

    @Override
    public String getType() {
        return "OrderList";
    }
}