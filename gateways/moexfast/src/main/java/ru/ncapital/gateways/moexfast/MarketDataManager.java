package ru.ncapital.gateways.moexfast;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.*;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.sequencevalidators.MessageSequenceValidatorFactory;
import ru.ncapital.gateways.moexfast.domain.MdUpdateAction;
import ru.ncapital.gateways.moexfast.domain.Subscription;
import ru.ncapital.gateways.moexfast.domain.impl.BBO;
import ru.ncapital.gateways.moexfast.domain.impl.DepthLevel;
import ru.ncapital.gateways.moexfast.domain.impl.Instrument;
import ru.ncapital.gateways.moexfast.domain.impl.PublicTrade;
import ru.ncapital.gateways.moexfast.domain.intf.IBBO;
import ru.ncapital.gateways.moexfast.domain.intf.IChannelStatus;
import ru.ncapital.gateways.moexfast.domain.intf.IDepthLevel;
import ru.ncapital.gateways.moexfast.domain.intf.IPublicTrade;
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

    protected ISnapshotProcessor<T> snapshotProcessorForOrderList;

    protected ISnapshotProcessor<T> snapshotProcessorForStatistics;

    protected ISnapshotProcessor<T> snapshotProcessorForOrderBook;

    protected IIncrementalProcessor incrementalProcessorForOrderList;

    protected IIncrementalProcessor incrementalProcessorForStatistics;

    protected IIncrementalProcessor incrementalProcessorForOrderBook;

    protected IIncrementalProcessor incrementalProcessorForPublicTrades;

    @Inject
    protected MessageSequenceValidatorFactory<T> messageSequenceValidatorFactory;

    @Inject
    protected MessageHandlerFactory<T> messageHandlerFactory;

    private HeartbeatProcessor heartbeatProcessor;

    private NewsProcessor newsProcessor;

    private InstrumentManager<T> instrumentManager;

    private IGatewayPerformanceLogger performanceLogger;

    private boolean feedStatusUP = false;

    public MarketDataManager configure(IGatewayConfiguration configuration) {
        marketDataHandler = configuration.getMarketDataHandler();
        performanceLogger = configuration.getPerformanceLogger();
        heartbeatProcessor = new HeartbeatProcessor();
        newsProcessor = new NewsProcessor(marketDataHandler);
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
        return LoggerFactory.getLogger(getClass().getSimpleName());
    }

    public void setInstrumentManager(InstrumentManager<T> instrumentManager) {
        this.instrumentManager = instrumentManager;
    }

    public void onMarketReset(MessageHandlerType type) {
        for (Instrument<T> instrument : this.instrumentManager.getInstruments()) {
            T exchangeSecurityId = instrument.getExchangeSecurityId();
            switch (type) {
                case ORDER_BOOK:
                case STATISTICS:
                    BBO<T> bbo = getOrCreateBBO(exchangeSecurityId);
                    bbo.setEmpty(true);

                    onBBO(bbo);
                    break;
                case ORDER_LIST:
                    DepthLevel<T> depthLevel = createDepthLevel(exchangeSecurityId);
                    depthLevel.setMdUpdateAction(MdUpdateAction.SNAPSHOT);

                    onDepthLevels(new DepthLevel[]{ depthLevel }, true);
                    break;
                default:
                    break;
            }
        }
    }

    @SuppressWarnings("unchecked")
    boolean subscribe(Subscription subscription) {
        if (logger.isTraceEnabled())
            logger.trace("onSubscribe " + subscription.getSubscriptionKey());

        if (subscriptions.putIfAbsent(subscription.getSubscriptionKey(), subscription) == null) {
            if (logger.isDebugEnabled())
                logger.debug("Added subscription for " + subscription.getSubscriptionKey());
        }

        T exchangeSecurityId = convertSecurityIdToExchangeSecurityId(subscription.getSubscriptionKey());
        if (exchangeSecurityId == null) {
            logger.warn("Instrument not found [" + subscription.getSubscriptionKey() + "]");
            return false;
        }

        BBO<T> currentBBO = getOrCreateBBO(exchangeSecurityId);
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
        if (exchangeSecurityId == null)
            return null;

        BBO<T> bbo = bbosByExchangeSecurityId.get(exchangeSecurityId);
        if (bbo == null) {
            bbo = createBBO(exchangeSecurityId);
            if (bbo.getSecurityId() == null)
                return null;

            if (bbosByExchangeSecurityId.putIfAbsent(exchangeSecurityId, bbo) == null)
                return bbo;

            return bbosByExchangeSecurityId.get(exchangeSecurityId);
        }
        return bbo;
    }

    public void onBBO(BBO<T> newBBO) {
        onBBO(newBBO, false);
    }

    public void onBBO(BBO<T> newBBO, boolean isSnapshot) {
        long gatewayOutTime;
        BBO<T> currentBBO = getOrCreateBBO(newBBO.getExchangeSecurityId());
        if (currentBBO == null || currentBBO.getSecurityId() == null) {
            if (logger.isDebugEnabled())
                logger.debug("onBBO unknown instrument " + newBBO.getExchangeSecurityId());

            return;
        }

        if (logger.isTraceEnabled())
            logger.trace("onBBO " + newBBO.getSecurityId());

        synchronized (currentBBO) {
            if (isSnapshot) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Before BBO Snapshot [SecurityId: " + currentBBO.getSecurityId() + "]");
                    logger.trace(currentBBO.toString());
                }
            }

            boolean[] changed = orderDepthEngine.updateBBO(currentBBO, newBBO);

            if (isSnapshot) {
                if (logger.isTraceEnabled()) {
                    logger.trace("After BBO Snapshot [SecurityId: " + currentBBO.getSecurityId() + "]");
                    logger.trace(currentBBO.toString());
                }
            }

            if (subscriptions.containsKey(currentBBO.getSecurityId())) {
                gatewayOutTime = Utils.currentTimeInTicks();

                if (changed[0])
                    marketDataHandler.onBBO(currentBBO);
                if (changed[1])
                    marketDataHandler.onStatistics(currentBBO);
                if (changed[2])
                    marketDataHandler.onTradingStatus(currentBBO);

                logPerformance(currentBBO, gatewayOutTime);
            }
        }
    }

    public void onDepthLevels(DepthLevel<T>[] depthLevels) {
        onDepthLevels(depthLevels, false);
    }

    public void onDepthLevels(DepthLevel<T>[] depthLevels, boolean isSnapshot) {
        long gatewayOutTime;
        BBO<T> currentBBO = getOrCreateBBO(depthLevels[0].getExchangeSecurityId());
        if (currentBBO == null || currentBBO.getSecurityId() == null) {
            if (logger.isDebugEnabled())
                logger.debug("onDepthLevel unknown instrument " + depthLevels[0].getExchangeSecurityId());

            return;
        }

        if (logger.isTraceEnabled())
            logger.trace("onDepthLevel " + depthLevels[0].getSecurityId());

        List<IDepthLevel> depthLevelsBeforeUpdate;
        List<IDepthLevel> depthLevelsAfterUpdate;

        synchronized (currentBBO) {
            if (isSnapshot) {
                depthLevelsBeforeUpdate = new ArrayList<>();
                orderDepthEngine.getDepthLevels(depthLevels[0].getExchangeSecurityId(), depthLevelsBeforeUpdate);
                if (logger.isTraceEnabled()) {
                    StringBuilder sb = new StringBuilder("Before Depth Snapshot [SecurityId: ").append(currentBBO.getSecurityId()).append("]");
                    for (IDepthLevel depthLevel : depthLevelsBeforeUpdate) {
                        sb.append('\n').append(depthLevel.toShortString());
                    }
                    logger.trace(sb.toString());
                }
            }

            List<IDepthLevel> depthLevelsToSend = new ArrayList<>();
            List<IPublicTrade> publicTradesToSend = new ArrayList<>();
            orderDepthEngine.onDepthLevels(depthLevels, depthLevelsToSend, publicTradesToSend);

            if (isSnapshot) {
                depthLevelsAfterUpdate = new ArrayList<>();
                orderDepthEngine.getDepthLevels(depthLevels[0].getExchangeSecurityId(), depthLevelsAfterUpdate);
                if (logger.isTraceEnabled()) {
                    StringBuilder sb = new StringBuilder("After Depth Snapshot [SecurityId: ").append(currentBBO.getSecurityId()).append("]");
                    for (IDepthLevel depthLevel : depthLevelsAfterUpdate) {
                        sb.append('\n').append(depthLevel.toShortString());
                    }
                    logger.trace(sb.toString());
                }
            }

            if (subscriptions.containsKey(currentBBO.getSecurityId())) {
                gatewayOutTime = Utils.currentTimeInTicks();
                marketDataHandler.onDepthLevels(depthLevelsToSend.toArray(new IDepthLevel[0]));
                logPerformance(depthLevels, gatewayOutTime);

                for (IPublicTrade publicTrade : publicTradesToSend) {
                    gatewayOutTime = Utils.currentTimeInTicks();
                    marketDataHandler.onPublicTrade(publicTrade);
                    logPerformance(publicTrade, gatewayOutTime);
                }
            }
        }
    }


    public void onPublicTrade(PublicTrade<T> publicTrade) {
        long gatewayOutTime;
        BBO<T> currentBBO = getOrCreateBBO(publicTrade.getExchangeSecurityId());
        if (currentBBO == null || currentBBO.getSecurityId() == null) {
            if (logger.isDebugEnabled())
                logger.debug("onPublicTrade unknown instrument " + publicTrade.getExchangeSecurityId());

            return;
        }

        if (logger.isTraceEnabled())
            logger.trace("onPublicTrade " + publicTrade.getSecurityId());

        synchronized (currentBBO) {
            if (subscriptions.containsKey(currentBBO.getSecurityId())) {
                gatewayOutTime = Utils.currentTimeInTicks();
                marketDataHandler.onPublicTrade(publicTrade);
                logPerformance(publicTrade, gatewayOutTime);
            }
        }
    }

    private void logPerformance(IBBO bbo, long gatewayOutTime) {
        if (performanceLogger == null)
            return;

        if (bbo.getPerformanceData() == null)
            return;

        bbo.getPerformanceData().setGatewayOutTime(gatewayOutTime);
        performanceLogger.notifyBBOPerformance(bbo.getPerformanceData());
    }

    private void logPerformance(IDepthLevel[] depthLevels, long gatewayOutTime) {
         if (performanceLogger == null)
            return;

        for (IDepthLevel depthLevel : depthLevels) {
            if (depthLevel.getPerformanceData() == null)
                continue;

            depthLevel.getPerformanceData().setGatewayOutTime(gatewayOutTime);
            performanceLogger.notifyOrderListPerformance(depthLevel.getPerformanceData());
        }
    }

    private void logPerformance(IPublicTrade publicTrade, long gatewayOutTime) {
        if (performanceLogger == null)
            return;

        if (publicTrade.getPerformanceData() == null)
            return;

        publicTrade.getPerformanceData().setGatewayOutTime(gatewayOutTime);
        performanceLogger.notifyPublicTradePerformance(publicTrade.getPerformanceData());
    }

    public InstrumentManager<T> getInstrumentManager() {
        return instrumentManager;
    }

    public IProcessor getHeartbeatProcessor() {
        return heartbeatProcessor;
    }

    public IProcessor getNewsProcessor() {
        return newsProcessor;
    }

    public ISnapshotProcessor<T> getSnapshotProcessor(MessageHandlerType type) {
        switch (type) {
            case ORDER_LIST:
                return snapshotProcessorForOrderList;
            case STATISTICS:
                return snapshotProcessorForStatistics;
            case PUBLIC_TRADES:
                break;
            case ORDER_BOOK:
                return snapshotProcessorForOrderBook;
        }

        throw new RuntimeException("Unknown Snapshot Processor " + type);
    }

    public IIncrementalProcessor getIncrementalProcessor(MessageHandlerType type) {
        switch (type) {
            case ORDER_LIST:
                return incrementalProcessorForOrderList;
            case STATISTICS:
                return incrementalProcessorForStatistics;
            case PUBLIC_TRADES:
                return incrementalProcessorForPublicTrades;
            case ORDER_BOOK:
                return incrementalProcessorForOrderBook;
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

    public void setRecovery(T exchangeSecurityId, boolean inRecovery, IChannelStatus.ChannelType channelType) {
        BBO<T> bbo = createBBO(exchangeSecurityId);
        bbo.setInRecovery(inRecovery, channelType);
        if (getLogger().isDebugEnabled())
            getLogger().debug("Recovery updated [SecurityId: " + bbo.getSecurityId() + "][" + channelType + "]" + (inRecovery ? "[R]" : ""));

        onBBO(bbo);
    }

    public void onFeedStatus(boolean up) {
        boolean changed = false;
        if (feedStatusUP != up) {
            if (getLogger().isDebugEnabled())
                getLogger().debug("Feed Status [Current: " + (feedStatusUP ? "UP" : "DOWN") + "][Received: " + (up ? "UP" : "DOWN") + "]");

            feedStatusUP = up;
            changed = true;
        }
        if (changed)
            marketDataHandler.onFeedStatus(up);
    }
}