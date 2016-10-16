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
        return LoggerFactory.getLogger("OrderListMessageHandler");
    }

    @Override
    protected void onSnapshotMdEntry(T exchangeSecurityId, GroupValue mdEntry) {
        MdEntryType mdEntryType = getMdEntryType(mdEntry);
        DepthLevel<T> depthLevel = null;
        switch (mdEntryType) {
            case BID:
                depthLevel =
                        createDepthLevel(
                                marketDataManager.convertExchangeSecurityIdToSecurityId(exchangeSecurityId),
                                exchangeSecurityId,
                                MdUpdateAction.INSERT,
                                getMdEntryId(mdEntry),
                                getMdEntryPx(mdEntry),
                                getMdEntrySize(mdEntry),
                                null,
                                true);

                depthLevel.getPerformanceData().setExchangeTime(Utils.getEntryTimeInTicks(mdEntry));
                break;
            case OFFER:
                depthLevel =
                        createDepthLevel(
                                marketDataManager.convertExchangeSecurityIdToSecurityId(exchangeSecurityId),
                                exchangeSecurityId,
                                MdUpdateAction.INSERT,
                                getMdEntryId(mdEntry),
                                getMdEntryPx(mdEntry),
                                getMdEntrySize(mdEntry),
                                null,
                                false);

                depthLevel.getPerformanceData().setExchangeTime(Utils.getEntryTimeInTicks(mdEntry));
                break;
            case EMPTY:
                break;
            default:
                logger.warn("Unhandled snapshot mdEntry " + mdEntry.toString());
                break;
        }

        if (depthLevel != null) {
            List<DepthLevel<T>> depthLevelList = depthLevelMap.get(exchangeSecurityId);
            if (depthLevelList == null) {
                depthLevelList = createDepthLevels();
                depthLevelMap.put(exchangeSecurityId, depthLevelList);
            }
            depthLevelList.add(depthLevel);
        }
    }

    @Override
    public void onIncrementalMdEntry(T exchangeSecurityId, GroupValue mdEntry, PerformanceData perfData) {
        MdEntryType mdEntryType = getMdEntryType(mdEntry);
        MdUpdateAction mdUpdateAction = getMdUpdateAction(mdEntry);
        DepthLevel<T> depthLevel = null;
        switch (mdEntryType) {
            case BID:
                depthLevel =
                        createDepthLevel(
                                marketDataManager.convertExchangeSecurityIdToSecurityId(exchangeSecurityId),
                                exchangeSecurityId,
                                mdUpdateAction,
                                getMdEntryId(mdEntry),
                                getMdEntryPx(mdEntry),
                                mdUpdateAction == MdUpdateAction.DELETE ? 0.0 : getMdEntrySize(mdEntry),
                                getTradeId(mdEntry),
                                true);

                depthLevel.getPerformanceData().updateFrom(perfData).setExchangeTime(Utils.getEntryTimeInTicks(mdEntry));
                break;
            case OFFER:
                depthLevel =
                        createDepthLevel(
                                marketDataManager.convertExchangeSecurityIdToSecurityId(exchangeSecurityId),
                                exchangeSecurityId,
                                mdUpdateAction,
                                getMdEntryId(mdEntry),
                                getMdEntryPx(mdEntry),
                                mdUpdateAction == MdUpdateAction.DELETE ? 0.0 : getMdEntrySize(mdEntry),
                                getTradeId(mdEntry),
                                false);

                depthLevel.getPerformanceData().updateFrom(perfData).setExchangeTime(Utils.getEntryTimeInTicks(mdEntry));
                break;
            case EMPTY:
                depthLevel = marketDataManager.createSnapshotDepthLevel(exchangeSecurityId);
                break;
            default:
                logger.warn("Unhandled incremental mdEntry " + mdEntry.toString());
                break;
        }

        if (depthLevel != null) {
            List<DepthLevel<T>> depthLevelList = depthLevelMap.get(exchangeSecurityId);
            if (depthLevelList == null) {
                depthLevelList = createDepthLevels();
                depthLevelMap.put(exchangeSecurityId, depthLevelList);
            }
            depthLevelList.add(depthLevel);
        }
    }

    @Override
    protected void onBeforeSnapshot(T exchangeSecurityId) {
        List<DepthLevel<T>> depthLevelList = createDepthLevels();
        depthLevelMap.put(exchangeSecurityId, depthLevelList);
        depthLevelList.add(marketDataManager.createSnapshotDepthLevel(exchangeSecurityId));
    }

    @Override
    protected void onAfterSnapshot(T exchangeSecurityId) {
        for (List<DepthLevel<T>> depthLevelList : depthLevelMap.values()) {
            marketDataManager.onDepthLevels(convertDepthLevels(depthLevelList));
        }
        depthLevelMap.clear();
    }

    @Override
    public void flushIncrementals() {
        for (List<DepthLevel<T>> depthLevelList : depthLevelMap.values()) {
            marketDataManager.onDepthLevels(convertDepthLevels(depthLevelList));
        }
        depthLevelMap.clear();
    }

    @Override
    public MessageHandlerType getType() {
        return MessageHandlerType.ORDER_LIST;
    }

    protected abstract DepthLevel<T> createDepthLevel(String securityId, T exchangeSecurityId, MdUpdateAction action, String mdEntryId, double mdEntryPx, double mdEntrySize, String tradeId, boolean isBid);

    protected abstract List<DepthLevel<T>> createDepthLevels();

    protected abstract DepthLevel<T>[] convertDepthLevels(List<DepthLevel<T>> depthLevels);
}