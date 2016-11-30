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

    private Map<T, List<PublicTrade<T>>> publicTradeMap = new HashMap<>();

    private boolean publicTradesFromOrderList;

    private String lastTradeId;

    public OrderListMessageHandler(MarketDataManager<T> marketDataManager, IGatewayConfiguration configuration) {
        super(marketDataManager, configuration);

        this.publicTradesFromOrderList = configuration.publicTradesFromOrdersList();
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

    private List<PublicTrade<T>> getPublicTradeList(T exchangeSecurityId) {
        List<PublicTrade<T>> publicTradeList = publicTradeMap.get(exchangeSecurityId);
        if (publicTradeList == null) {
            publicTradeList = new ArrayList<>();
            publicTradeMap.put(exchangeSecurityId, publicTradeList);
        }
        return publicTradeList;
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
        if (publicTradesFromOrderList
                && depthLevel != null
                && depthLevel.getTradeId() != null
                && !depthLevel.getTradeId().equals(lastTradeId)) {

            PublicTrade<T> publicTrade = marketDataManager.createPublicTrade(exchangeSecurityId);
            publicTrade.setMdEntryId(depthLevel.getMdEntryId());
            publicTrade.setTradeId(depthLevel.getTradeId());
            publicTrade.setLastPx(depthLevel.getMdEntryPx());
            publicTrade.setIsBid(depthLevel.getIsBid());
            publicTrade.getPerformanceData().updateFrom(depthLevel.getPerformanceData());

            lastTradeId = depthLevel.getTradeId();

            // technical trade
            if (depthLevel.getMdUpdateAction() == MdUpdateAction.INSERT) {
                publicTrade.setLastSize(depthLevel.getMdEntrySize());
                getDepthLevelList(exchangeSecurityId).remove(depthLevel);
                getPublicTradeList(exchangeSecurityId).add(publicTrade);
            } else {
                depthLevel.setPublicTrade(publicTrade);
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
            if (depthLevelList.size() > 0)
                marketDataManager.onDepthLevels(depthLevelsToArray(depthLevelList));

        depthLevelMap.clear();
    }

    @Override
    public void flushIncrementals() {
         for (List<DepthLevel<T>> depthLevelList : depthLevelMap.values())
            if (depthLevelList.size() > 0)
                marketDataManager.onDepthLevels(depthLevelsToArray(depthLevelList));

        for (List<PublicTrade<T>> publicTradeList : publicTradeMap.values())
            for (PublicTrade<T> publicTrade : publicTradeList)
                marketDataManager.onPublicTrade(publicTrade);

        depthLevelMap.clear();
        publicTradeMap.clear();
    }

    protected abstract DepthLevel<T>[] depthLevelsToArray(List<DepthLevel<T>> list);
}