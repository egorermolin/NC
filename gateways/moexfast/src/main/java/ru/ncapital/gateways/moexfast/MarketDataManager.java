package ru.ncapital.gateways.moexfast;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.HeartbeatProcessor;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.IIncrementalProcessor;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.IProcessor;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.ISnapshotProcessor;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.sequencevalidators.MessageSequenceValidatorFactory;
import ru.ncapital.gateways.moexfast.domain.Subscription;
import ru.ncapital.gateways.moexfast.domain.impl.BBO;
import ru.ncapital.gateways.moexfast.domain.impl.DepthLevel;
import ru.ncapital.gateways.moexfast.domain.impl.PublicTrade;
import ru.ncapital.gateways.moexfast.domain.intf.IDepthLevel;
import ru.ncapital.gateways.moexfast.messagehandlers.MessageHandlerFactory;
import ru.ncapital.gateways.moexfast.messagehandlers.MessageHandlerType;
import ru.ncapital.gateways.moexfast.performance.IGatewayPerformanceLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by egore on 12/7/15.
 */

public abstract class MarketDataManager<T> {
    private ConcurrentHashMap<String, Subscription> subscriptions = new ConcurrentHashMap<>();

    private ConcurrentHashMap<T, BBO<T>> bbosByExchangeSecurityId = new ConcurrentHashMap<>();

    private Logger logger = getLogger();

    private OrderDepthEngine<T> orderDepthEngine = createDepthEngine();

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

    protected InstrumentManager<T> instrumentManager;

    private IGatewayPerformanceLogger performanceLogger;

    private boolean feedStatusUP = false;

    private boolean feedStatusALL = true;

    public MarketDataManager configure(IGatewayConfiguration configuration) {
        marketDataHandler = configuration.getMarketDataHandler();
        performanceLogger = configuration.getPerformanceLogger();

        return this;
    }

    protected abstract OrderDepthEngine<T> createDepthEngine();

    public abstract BBO<T> createBBO(T exchangeSecurityId);

    public abstract DepthLevel<T> createDepthLevel(T exchangeSecurityId);

    public abstract PublicTrade<T> createPublicTrade(T exchangeSecurityId);

    public T convertSecurityIdToExchangeSecurityId(String securityId) {
        return instrumentManager.getExchangeSecurityId(securityId);
    }

    public String convertExchangeSecurityIdToSecurityId(T exchangeSecurityId) {
        return instrumentManager.getSecurityId(exchangeSecurityId);
    }

    public boolean isAllowedInstrument(T exchangeSecurityId) {
        return instrumentManager.isAllowedInstrument(exchangeSecurityId);
    }

    public Logger getLogger() {
        return LoggerFactory.getLogger(getClass().getName());
    }

    public void setInstrumentManager(InstrumentManager<T> instrumentManager) {
        this.instrumentManager = instrumentManager;
    }

    public boolean subscribe(Subscription subscription) {
        if (logger.isTraceEnabled())
            logger.trace("onSubscribe " + subscription.getSubscriptionKey());

        if (subscriptions.putIfAbsent(subscription.getSubscriptionKey(), subscription) == null) {
            if (logger.isDebugEnabled())
                logger.debug("Added subscription for " + subscription.getSubscriptionKey());
        }

        BBO<T> currentBBO = getOrCreateBBO(
                convertSecurityIdToExchangeSecurityId(subscription.getSubscriptionKey()));

        synchronized (currentBBO) {
            List<IDepthLevel> depthLevelsToSend = new ArrayList<>();
            orderDepthEngine.getDepthLevels(currentBBO.getExchangeSecurityId(), depthLevelsToSend);

            marketDataHandler.onBBO(currentBBO);
            marketDataHandler.onDepthLevels(depthLevelsToSend.toArray(new IDepthLevel[0]));
            marketDataHandler.onStatistics(currentBBO);
            marketDataHandler.onTradingStatus(currentBBO);
        }
        return true;
    }

    private BBO<T> getOrCreateBBO(T exchangeSecurityId) {
        BBO<T> bbo = bbosByExchangeSecurityId.get(exchangeSecurityId);
        if (bbo == null) {
            bbo = createBBO(exchangeSecurityId);
            if (bbosByExchangeSecurityId.putIfAbsent(exchangeSecurityId, bbo) == null)
                return bbo;

            return bbosByExchangeSecurityId.get(exchangeSecurityId);
        }
        return bbo;
    }

    public void onBBO(BBO<T> newBBO) {
        long gatewayOutTime = 0;
        if (logger.isTraceEnabled())
            logger.trace("onBBO " + newBBO.getSecurityId());

        BBO<T> currentBBO = getOrCreateBBO(newBBO.getExchangeSecurityId());
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

    public void onDepthLevels(DepthLevel<T>[] depthLevels) {
        long gatewayOutTime = 0;
        if (logger.isTraceEnabled())
            logger.trace("onDepthLevel " + depthLevels[0].getSecurityId());

        BBO<T> currentBBO = getOrCreateBBO(depthLevels[0].getExchangeSecurityId());
        synchronized (currentBBO) {
            List<IDepthLevel> depthLevelsToSend = new ArrayList<>();
            orderDepthEngine.onDepthLevels(depthLevels, depthLevelsToSend);

            if (subscriptions.containsKey(currentBBO.getSecurityId())) {
                gatewayOutTime = Utils.currentTimeInTicks();
                marketDataHandler.onDepthLevels(depthLevelsToSend.toArray(new IDepthLevel[0]));
            }
        }

        logPerformance(depthLevels, gatewayOutTime);
    }


    public void onPublicTrade(PublicTrade<T> publicTrade) {
        long gatewayOutTime = 0;
        if (logger.isTraceEnabled())
            logger.trace("onPublicTrade " + publicTrade.getSecurityId());

        BBO<T> currentBBO = getOrCreateBBO(publicTrade.getExchangeSecurityId());
        synchronized (currentBBO) {
            if (subscriptions.containsKey(currentBBO.getSecurityId())) {
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

    public void setRecovery(T exchangeSecurityId, boolean isUp, boolean orderList) {
        BBO<T> bbo = createBBO(exchangeSecurityId);
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
}