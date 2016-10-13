package ru.ncapital.gateways.moexfast;

import com.google.inject.Inject;
import org.slf4j.Logger;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.HeartbeatProcessor;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.IIncrementalProcessor;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.IProcessor;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.ISnapshotProcessor;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.sequencevalidators.MessageSequenceValidatorFactory;
import ru.ncapital.gateways.moexfast.domain.*;
import ru.ncapital.gateways.moexfast.messagehandlers.MessageHandlerFactory;
import ru.ncapital.gateways.moexfast.messagehandlers.MessageHandlerType;
import ru.ncapital.gateways.moexfast.performance.IGatewayPerformanceLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by egore on 12/7/15.
 */

public abstract class MarketDataManager {
    private ConcurrentHashMap<String, Subscription> subscriptions = new ConcurrentHashMap<>();

    private ConcurrentHashMap<String, BBO> bbos = new ConcurrentHashMap<>();

    private Logger logger = getLogger();

    private OrderDepthEngine orderDepthEngine = new OrderDepthEngine();

    private IMarketDataHandler marketDataHandler;

    protected ISnapshotProcessor snapshotProcessorForOrderList;

    protected ISnapshotProcessor snapshotProcessorForStatistics;

    protected IIncrementalProcessor incrementalProcessorForOrderList;

    protected IIncrementalProcessor incrementalProcessorForStatistics;

    protected IIncrementalProcessor incrementalProcessorForPublicTrades;

    @Inject
    protected MessageSequenceValidatorFactory messageSequenceValidatorFactory;

    @Inject
    protected MessageHandlerFactory messageHandlerFactory;

    @Inject
    protected HeartbeatProcessor heartbeatProcessor;

    protected InstrumentManager instrumentManager;

    private IGatewayPerformanceLogger performanceLogger;

    private boolean feedStatusUP = false;

    private boolean feedStatusALL = true;

    public void setInstrumentManager(InstrumentManager instrumentManager) {
        this.instrumentManager = instrumentManager;
    }

    public MarketDataManager configure(IGatewayConfiguration configuration) {
        marketDataHandler = configuration.getMarketDataHandler();
        performanceLogger = configuration.getPerformanceLogger();

        return this;
    }

    public boolean subscribe(Subscription subscription) {
        if (logger.isTraceEnabled())
            logger.trace("onSubscribe " + subscription.getSubscriptionKey());

        if (subscriptions.putIfAbsent(subscription.getSubscriptionKey(), subscription) == null) {
            if (logger.isDebugEnabled())
                logger.debug("Added subscription for " + subscription.getSubscriptionKey());
        }

        BBO currentBBO = getOrCreateBBO(subscription.getSubscriptionKey());
        synchronized (currentBBO) {
            List<DepthLevel> depthLevelsToSend = new ArrayList<>();
            depthLevelsToSend.add(new DepthLevel(subscription.getSubscriptionKey(), MdUpdateAction.SNAPSHOT));
            orderDepthEngine.getDepthLevels(subscription.getSubscriptionKey(), depthLevelsToSend);

            marketDataHandler.onBBO(currentBBO);
            marketDataHandler.onDepthLevels(depthLevelsToSend.toArray(new DepthLevel[0]));
            marketDataHandler.onStatistics(currentBBO);
            marketDataHandler.onTradingStatus(currentBBO);
        }
        return true;
    }

    private BBO getOrCreateBBO(String securityId) {
        BBO bbo = bbos.get(securityId);
        if (bbo == null) {
            bbo = new BBO(securityId);
            if (bbos.putIfAbsent(securityId, bbo) == null)
                return bbo;

            return bbos.get(securityId);
        }
        return bbo;
    }

    public void onBBO(BBO newBBO) {
        long gatewayOutTime = 0;
        if (logger.isTraceEnabled())
            logger.trace("onBBO " + newBBO.getSecurityId());

        BBO currentBBO = getOrCreateBBO(newBBO.getSecurityId());
        synchronized (currentBBO) {
            boolean[] changed = orderDepthEngine.updateBBO(currentBBO, newBBO);

            if (subscriptions.containsKey(currentBBO.getSecurityId())) {
                gatewayOutTime = Utils.currentTimeInTicks();

                if (changed[0])
                    marketDataHandler.onBBO(currentBBO);
                if (changed[1])
                    marketDataHandler.onStatistics(currentBBO);
                if (changed[2])
                    marketDataHandler.onTradingStatus(currentBBO);
            }
        }

        logPerformance(currentBBO, gatewayOutTime);
    }

    public void onDepthLevels(DepthLevel[] depthLevels) {
        long gatewayOutTime = 0;
        if (logger.isTraceEnabled())
            logger.trace("onDepthLevel " + depthLevels[0].getSecurityId());

        BBO currentBBO = getOrCreateBBO(depthLevels[0].getSecurityId());
        synchronized (currentBBO) {
            List<DepthLevel> depthLevelsToSend = new ArrayList<>();
            orderDepthEngine.onDepthLevels(depthLevels, depthLevelsToSend);

            if (subscriptions.containsKey(depthLevels[0].getSecurityId())) {
                gatewayOutTime = Utils.currentTimeInTicks();
                marketDataHandler.onDepthLevels(depthLevelsToSend.toArray(new DepthLevel[0]));
            }
        }

        logPerformance(depthLevels, gatewayOutTime);
    }


    public void onPublicTrade(PublicTrade publicTrade) {
        long gatewayOutTime = 0;
        if (logger.isTraceEnabled())
            logger.trace("onPublicTrade " + publicTrade.getSecurityId());

        BBO currentBBO = getOrCreateBBO(publicTrade.getSecurityId());
        synchronized (currentBBO) {
            orderDepthEngine.onPublicTrade(publicTrade);

            if (subscriptions.containsKey(publicTrade.getSecurityId())) {
                gatewayOutTime = Utils.currentTimeInTicks();
                marketDataHandler.onPublicTrade(publicTrade);
            }
        }

        logPerformance(publicTrade, gatewayOutTime);
    }

    private void logPerformance(BBO bbo, long gatewayOutTime) {
        if (performanceLogger == null)
            return;

        if (bbo.getPerformanceData() == null)
            return;

        bbo.getPerformanceData().setGatewayOutTime(gatewayOutTime);
        performanceLogger.notifyBBOPerformance(bbo.getPerformanceData());
    }

    private void logPerformance(DepthLevel[] depthLevels, long gatewayOutTime) {
         if (performanceLogger == null)
            return;

        for (DepthLevel depthLevel : depthLevels) {
            if (depthLevel.getPerformanceData() == null)
                continue;

            depthLevel.getPerformanceData().setGatewayOutTime(gatewayOutTime);
            performanceLogger.notifyOrderListPerformance(depthLevel.getPerformanceData());
        }
    }

    private void logPerformance(PublicTrade publicTrade, long gatewayOutTime) {
        if (performanceLogger == null)
            return;

        if (publicTrade.getPerformanceData() == null)
            return;

        publicTrade.getPerformanceData().setGatewayOutTime(gatewayOutTime);
        performanceLogger.notifyPublicTradePerformance(publicTrade.getPerformanceData());
    }

    public IProcessor getHeartbeatProcessor() {
        return heartbeatProcessor;
    }

    public ISnapshotProcessor getSnapshotProcessor(MessageHandlerType type) {
        switch (type) {
            case ORDER_LIST:
                return snapshotProcessorForOrderList;
            case STATISTICS:
                return snapshotProcessorForStatistics;
        }

        throw new RuntimeException("Unknown Snapshot Processor " + type);
    }

    public IIncrementalProcessor getIncrementalProcessor(MessageHandlerType type) {
        switch (type) {
            case ORDER_LIST:
                return incrementalProcessorForOrderList;
            case PUBLIC_TRADES:
                return incrementalProcessorForPublicTrades;
            case STATISTICS:
                return incrementalProcessorForStatistics;
        }

        throw new RuntimeException("Unknown Incremental Processor " + type);
    }

    public ThreadLocal<Long> getSnapshotProcessorInTimestamp(MessageHandlerType type) {
        return getSnapshotProcessor(type).getInTimestampHolder();
    }

    public ThreadLocal<Long> getIncrementalProcessorInTimestamp(MessageHandlerType type) {
        return getIncrementalProcessor(type).getInTimestampHolder();
    }

    public void setIncrementalProcessorIsPrimary(MessageHandlerType type, boolean isPrimary) {
        getIncrementalProcessor(type).setIsPrimary(isPrimary);
    }

    public boolean isAllowedInstrument(String securityId) {
        return instrumentManager.isAllowedInstrument(securityId);
    }

    public boolean isAllowedInstrument(Long securityId) {
        return instrumentManager.isAllowedInstrument(securityId);
    }

    public void setRecovery(String securityId, boolean isUp, boolean orderList) {
        BBO bbo = new BBO(securityId);
        bbo.setInRecovery(isUp, orderList ? 0 : 1);
        onBBO(bbo);
    }

    public void onFeedStatus(boolean up, boolean all) {
        boolean changed = false;
        if (feedStatusUP != up) {
            feedStatusUP = up;
            changed = true;
        }
        if (feedStatusALL != all) {
            feedStatusALL = all;
            changed = true;
        }
        if (changed)
            marketDataHandler.onFeedStatus(up, all);
    }

    public abstract Logger getLogger();
}