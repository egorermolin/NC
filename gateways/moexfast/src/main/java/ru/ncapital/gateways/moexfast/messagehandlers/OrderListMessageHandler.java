package ru.ncapital.gateways.moexfast.messagehandlers;

import org.openfast.GroupValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ncapital.gateways.moexfast.IGatewayConfiguration;
import ru.ncapital.gateways.moexfast.MarketDataManager;
import ru.ncapital.gateways.moexfast.Utils;
import ru.ncapital.gateways.moexfast.connection.MarketType;
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

    private String lastTradeId;

    private final Utils.SecondFractionFactor mdEntryFractionFactor;

    public OrderListMessageHandler(MarketDataManager<T> marketDataManager, IGatewayConfiguration configuration) {
        super(marketDataManager, configuration);

        this.publicTradesFromOrderList = configuration.publicTradesFromOrdersList();
        switch(configuration.getVersion()) {
            case V2016:
                mdEntryFractionFactor = Utils.SecondFractionFactor.MILLISECONDS;
                break;
            case V2017:
            default:
                if (configuration.getMarketType() == MarketType.FUT)
                    mdEntryFractionFactor = Utils.SecondFractionFactor.NANOSECONDS;
                else
                    mdEntryFractionFactor = Utils.SecondFractionFactor.MILLISECONDS;

                break;
        }
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
                depthLevel.getPerformanceData().setExchangeTime(Utils.getEntryTimeInTicks(mdEntry, mdEntryFractionFactor));
                getDepthLevelList(exchangeSecurityId).add(depthLevel);
                break;
            case EMPTY:
                depthLevel = getDepthLevelList(exchangeSecurityId).get(0);
                depthLevel.getPerformanceData().setExchangeTime(Utils.getEntryTimeInTicks(mdEntry, mdEntryFractionFactor));
                break;
        }
    }

    @Override
    public void onIncrementalMdEntry(T exchangeSecurityId, GroupValue mdEntry, PerformanceData perfData) {
        if (isOTC(mdEntry))
            return;

        DepthLevel<T> depthLevel = marketDataManager.createDepthLevel(exchangeSecurityId);
        MdEntryType mdEntryType = getMdEntryType(mdEntry);
        switch (mdEntryType) {
            case BID:
            case OFFER:
                depthLevel.setMdUpdateAction(getMdUpdateAction(mdEntry));
                depthLevel.setMdEntryId(getMdEntryId(mdEntry));
                depthLevel.setMdEntryPx(getMdEntryPx(mdEntry));
                depthLevel.setMdEntrySize(getMdEntrySize(mdEntry));
                depthLevel.setIsBid(mdEntryType == MdEntryType.BID);
                depthLevel.setTradeId(getTradeId(mdEntry));
                depthLevel.setMdFlags(getMdFlags(mdEntry));
                depthLevel.getPerformanceData().updateFrom(perfData).setExchangeTime(Utils.getEntryTimeInTicks(mdEntry, mdEntryFractionFactor));
                getDepthLevelList(exchangeSecurityId).add(depthLevel);
                break;
            case EMPTY:
                depthLevel = marketDataManager.createDepthLevel(exchangeSecurityId);
                depthLevel.setMdUpdateAction(MdUpdateAction.SNAPSHOT);
                getDepthLevelList(exchangeSecurityId).add(depthLevel);
                break;
            default:
                depthLevel = null;
                break;
        }

        // trade
        if (publicTradesFromOrderList && isTrade(depthLevel)) {
            PublicTrade<T> publicTrade = marketDataManager.createPublicTrade(exchangeSecurityId);
            publicTrade.setMdEntryId(depthLevel.getMdEntryId());
            publicTrade.setLastPx(getLastPx(mdEntry));
            publicTrade.setLastSize(getLastSize(mdEntry));
            publicTrade.setIsBid(depthLevel.getIsBid());
            publicTrade.setTradeId(depthLevel.getTradeId());
            publicTrade.getPerformanceData().updateFrom(depthLevel.getPerformanceData());
            depthLevel.setPublicTrade(publicTrade);
        }
    }

    private boolean isTrade(DepthLevel<T> depthLevel) {
        if (depthLevel == null)
            return false;

        if (depthLevel.getTradeId() == null)
            return false;

        if (depthLevel.getTradeId().equals(lastTradeId))
            return false;

        lastTradeId = depthLevel.getTradeId();
        return true;
    }

    protected abstract double getLastPx(GroupValue mdEntry);

    protected abstract long getLastSize(GroupValue mdEntry);

    protected abstract long getMdFlags(GroupValue mdEntry);

    protected abstract boolean isOTC(GroupValue mdEntry);

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
                marketDataManager.onDepthLevels(depthLevelsToArray(depthLevelList), true);

        depthLevelMap.clear();
    }

    @Override
    public void flushIncrementals() {
         for (List<DepthLevel<T>> depthLevelList : depthLevelMap.values())
            if (depthLevelList.size() > 0)
                marketDataManager.onDepthLevels(depthLevelsToArray(depthLevelList));

        depthLevelMap.clear();
    }

    protected abstract DepthLevel<T>[] depthLevelsToArray(List<DepthLevel<T>> list);
}