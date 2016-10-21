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
import ru.ncapital.gateways.moexfast.performance.PerformanceData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by egore on 12/7/15.
 */
public abstract class OrderListMessageHandler<T> extends AMessageHandler<T> {

    private Map<T, List<DepthLevel<T>>> depthLevelMap = new HashMap<>();

    public OrderListMessageHandler(MarketDataManager<T> marketDataManager, IGatewayConfiguration configuration) {
        super(marketDataManager, configuration);
    }

    @Override
    protected Logger getLogger() {
        return LoggerFactory.getLogger(getClass().getName());
    }

    @Override
    public MessageHandlerType getType() {
        return MessageHandlerType.ORDER_LIST;
    }


    private List<DepthLevel<T>> getDepthLevelList(T exchangeSecurityId) {
        List<DepthLevel<T>> depthLevelList = depthLevelMap.get(exchangeSecurityId);
        if (depthLevelList == null) {
            depthLevelList = createDepthLevelList();
            depthLevelMap.put(exchangeSecurityId, depthLevelList);
        }
        return depthLevelList;
    }

    @Override
    protected void onSnapshotMdEntry(T exchangeSecurityId, GroupValue mdEntry) {
        DepthLevel<T> depthLevel;
        switch (getMdEntryType(mdEntry)) {
            case BID:
                depthLevel = marketDataManager.createDepthLevel(exchangeSecurityId);

                depthLevel.setMdUpdateAction(MdUpdateAction.INSERT);
                depthLevel.setMdEntryId(getMdEntryId(mdEntry));
                depthLevel.setMdEntryPx(getMdEntryPx(mdEntry));
                depthLevel.setMdEntrySize(getMdEntrySize(mdEntry));
                depthLevel.setIsBid(true);
                depthLevel.getPerformanceData().setExchangeTime(Utils.getEntryTimeInTicks(mdEntry));

                getDepthLevelList(exchangeSecurityId).add(depthLevel);
                break;
            case OFFER:
                depthLevel = marketDataManager.createDepthLevel(exchangeSecurityId);

                depthLevel.setMdUpdateAction(MdUpdateAction.INSERT);
                depthLevel.setMdEntryId(getMdEntryId(mdEntry));
                depthLevel.setMdEntryPx(getMdEntryPx(mdEntry));
                depthLevel.setMdEntrySize(getMdEntrySize(mdEntry));
                depthLevel.setIsBid(false);
                depthLevel.getPerformanceData().setExchangeTime(Utils.getEntryTimeInTicks(mdEntry));

                getDepthLevelList(exchangeSecurityId).add(depthLevel);
                break;
            case EMPTY:
                break;
            default:
                logger.warn("Unknown snapshot mdEntry " + mdEntry.toString());
                break;
        }
    }

    @Override
    public void onIncrementalMdEntry(T exchangeSecurityId, GroupValue mdEntry, PerformanceData perfData) {
        DepthLevel<T> depthLevel;
        switch (getMdEntryType(mdEntry)) {
            case BID:
                depthLevel = marketDataManager.createDepthLevel(exchangeSecurityId);

                depthLevel.setMdUpdateAction(getMdUpdateAction(mdEntry));
                depthLevel.setMdEntryId(getMdEntryId(mdEntry));
                depthLevel.setMdEntryPx(getMdEntryPx(mdEntry));
                depthLevel.setMdEntrySize(depthLevel.getMdUpdateAction() == MdUpdateAction.DELETE ? 0.0 : getMdEntrySize(mdEntry));
                depthLevel.setTradeId(getTradeId(mdEntry));
                depthLevel.setIsBid(true);
                depthLevel.getPerformanceData().updateFrom(perfData).setExchangeTime(Utils.getEntryTimeInTicks(mdEntry));

                getDepthLevelList(exchangeSecurityId).add(depthLevel);
                break;
            case OFFER:
                depthLevel = marketDataManager.createDepthLevel(exchangeSecurityId);

                depthLevel.setMdUpdateAction(getMdUpdateAction(mdEntry));
                depthLevel.setMdEntryId(getMdEntryId(mdEntry));
                depthLevel.setMdEntryPx(getMdEntryPx(mdEntry));
                depthLevel.setMdEntrySize(depthLevel.getMdUpdateAction() == MdUpdateAction.DELETE ? 0.0 : getMdEntrySize(mdEntry));
                depthLevel.setTradeId(getTradeId(mdEntry));
                depthLevel.setIsBid(false);
                depthLevel.getPerformanceData().updateFrom(perfData).setExchangeTime(Utils.getEntryTimeInTicks(mdEntry));

                getDepthLevelList(exchangeSecurityId).add(depthLevel);
                break;
            case EMPTY:
                depthLevel = marketDataManager.createDepthLevel(exchangeSecurityId);

                depthLevel.setMdUpdateAction(MdUpdateAction.SNAPSHOT);

                getDepthLevelList(exchangeSecurityId).add(depthLevel);
                break;
            default:
                logger.warn("Unknown incremental mdEntry " + mdEntry.toString());
                break;
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
            marketDataManager.onDepthLevels(convertDepthLevels(depthLevelList));

        depthLevelMap.clear();
    }

    @Override
    public void flushIncrementals() {
        for (List<DepthLevel<T>> depthLevelList : depthLevelMap.values())
            marketDataManager.onDepthLevels(convertDepthLevels(depthLevelList));

        depthLevelMap.clear();
    }

    protected abstract List<DepthLevel<T>> createDepthLevelList();

    protected abstract DepthLevel<T>[] convertDepthLevels(List<DepthLevel<T>> depthLevels);

    @Override
    protected final double getLastPx(GroupValue mdEntry) {
        throw new RuntimeException();
    }

    @Override
    protected final double getLastSize(GroupValue mdEntry) {
        throw new RuntimeException();
    }

    @Override
    protected final boolean getTradeIsBid(GroupValue mdEntry) {
        throw new RuntimeException();
    }
}