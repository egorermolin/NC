package ru.ncapital.gateways.moexfast.messagehandlers;

import org.openfast.GroupValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ncapital.gateways.moexfast.IGatewayConfiguration;
import ru.ncapital.gateways.moexfast.MarketDataManager;
import ru.ncapital.gateways.moexfast.Utils;
import ru.ncapital.gateways.moexfast.domain.MdEntryType;
import ru.ncapital.gateways.moexfast.domain.MdUpdateAction;
import ru.ncapital.gateways.moexfast.domain.impl.DepthLevel;
import ru.ncapital.gateways.moexfast.domain.impl.PublicTrade;
import ru.ncapital.gateways.moexfast.performance.PerformanceData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by egore on 12/7/15.
 */
public abstract class OrderListMessageHandler<T> extends AMessageHandler<T> {

    private Map<T, List<DepthLevel<T>>> depthLevelMap = new HashMap<>();

    private boolean publicTradesFromOrderList;

    public OrderListMessageHandler(MarketDataManager<T> marketDataManager, IGatewayConfiguration configuration) {
        super(marketDataManager, configuration);
    }

    public void setPublicTradesFromOrderList(boolean value) {
        this.publicTradesFromOrderList = value;
    }

    @Override
    protected Logger getLogger() {
        return LoggerFactory.getLogger(getClass().getSimpleName());
    }

    @Override
    public MessageHandlerType getType() {
        return MessageHandlerType.ORDER_LIST;
    }

    private List<DepthLevel<T>> getDepthLevelList(T exchangeSecurityId) {
        List<DepthLevel<T>> depthLevelList = depthLevelMap.get(exchangeSecurityId);
        if (depthLevelList == null) {
            depthLevelList = new ArrayList<>();
            depthLevelMap.put(exchangeSecurityId, depthLevelList);
        }
        return depthLevelList;
    }

    @Override
    protected void onSnapshotMdEntry(T exchangeSecurityId, GroupValue mdEntry) {
        MdEntryType mdEntryType = getMdEntryType(mdEntry);
        switch (mdEntryType) {
            case BID:
            case OFFER:
                DepthLevel<T> depthLevel = marketDataManager.createDepthLevel(exchangeSecurityId);
                depthLevel.setMdUpdateAction(MdUpdateAction.INSERT);
                depthLevel.setMdEntryId(getMdEntryId(mdEntry));
                depthLevel.setMdEntryPx(getMdEntryPx(mdEntry));
                depthLevel.setMdEntrySize(getMdEntrySize(mdEntry));
                depthLevel.setIsBid(mdEntryType == MdEntryType.BID);
                depthLevel.getPerformanceData().setExchangeTime(Utils.getEntryTimeInTicks(mdEntry));
                getDepthLevelList(exchangeSecurityId).add(depthLevel);
                break;
            case EMPTY:
                depthLevel = getDepthLevelList(exchangeSecurityId).get(0);
                depthLevel.getPerformanceData().setExchangeTime(Utils.getEntryTimeInTicks(mdEntry));
                break;
        }
    }

    @Override
    public void onIncrementalMdEntry(T exchangeSecurityId, GroupValue mdEntry, PerformanceData perfData) {
        DepthLevel<T> depthLevel = null;
        MdEntryType mdEntryType = getMdEntryType(mdEntry);
        switch (mdEntryType) {
            case BID:
            case OFFER:
                depthLevel = marketDataManager.createDepthLevel(exchangeSecurityId);
                depthLevel.setMdUpdateAction(getMdUpdateAction(mdEntry));
                depthLevel.setMdEntryId(getMdEntryId(mdEntry));
                depthLevel.setMdEntryPx(getMdEntryPx(mdEntry));
                depthLevel.setMdEntrySize(depthLevel.getMdUpdateAction() == MdUpdateAction.DELETE ? 0.0 : getMdEntrySize(mdEntry));
                depthLevel.setIsBid(mdEntryType == MdEntryType.BID);
                depthLevel.setTradeId(getTradeId(mdEntry));
                depthLevel.getPerformanceData().updateFrom(perfData).setExchangeTime(Utils.getEntryTimeInTicks(mdEntry));
                getDepthLevelList(exchangeSecurityId).add(depthLevel);
                break;
            case EMPTY:
                depthLevel = marketDataManager.createDepthLevel(exchangeSecurityId);
                depthLevel.setMdUpdateAction(MdUpdateAction.SNAPSHOT);
                getDepthLevelList(exchangeSecurityId).add(depthLevel);
                break;
        }

        // trade
        if (depthLevel.getTradeId() != null) {
            // technical trade
            if (depthLevel.getMdUpdateAction() == MdUpdateAction.INSERT)
                getDepthLevelList(exchangeSecurityId).remove(depthLevel);

            if (publicTradesFromOrderList) {
                PublicTrade<T> publicTrade = marketDataManager.createPublicTrade(exchangeSecurityId);
                publicTrade.setTradeId(depthLevel.getTradeId());
                publicTrade.setLastPx(depthLevel.getMdEntryPx());
                publicTrade.setLastSize(depthLevel.getMdEntryPx());
                publicTrade.setIsBid(depthLevel.getIsBid());
                publicTrade.getPerformanceData().updateFrom(depthLevel.getPerformanceData());

                marketDataManager.onPublicTrade(publicTrade);
            }
        }
    }

    @Override
    protected void onBeforeSnapshot(T exchangeSecurityId) {
        DepthLevel<T> depthLevel = marketDataManager.createDepthLevel(exchangeSecurityId);
        depthLevel.setMdUpdateAction(MdUpdateAction.SNAPSHOT);
        getDepthLevelList(exchangeSecurityId).add(depthLevel);
    }

    @Override
    protected void onAfterSnapshot(T exchangeSecurityId) {
        for (List<DepthLevel<T>> depthLevelList : depthLevelMap.values())
            marketDataManager.onDepthLevels(depthLevelList.toArray(new DepthLevel[0]));

        depthLevelMap.clear();
    }

    @Override
    public void flushIncrementals() {
        for (List<DepthLevel<T>> depthLevelList : depthLevelMap.values())
            marketDataManager.onDepthLevels(depthLevelList.toArray(new DepthLevel[0]));

        depthLevelMap.clear();
    }
}