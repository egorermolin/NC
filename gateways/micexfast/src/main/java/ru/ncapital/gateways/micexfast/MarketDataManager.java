package ru.ncapital.gateways.micexfast;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ncapital.gateways.micexfast.connection.messageprocessors.*;
import ru.ncapital.gateways.micexfast.connection.messageprocessors.sequencevalidators.IMessageSequenceValidator;
import ru.ncapital.gateways.micexfast.connection.messageprocessors.sequencevalidators.MessageSequenceValidatorFactory;
import ru.ncapital.gateways.micexfast.domain.*;
import ru.ncapital.gateways.micexfast.messagehandlers.IMessageHandler;
import ru.ncapital.gateways.micexfast.messagehandlers.MessageHandlerFactory;
import ru.ncapital.gateways.micexfast.messagehandlers.MessageHandlerType;
import ru.ncapital.gateways.micexfast.performance.IGatewayPerformanceLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by egore on 12/7/15.
 */

@Singleton
public class MarketDataManager {
    private ConcurrentHashMap<String, Subscription> subscriptions = new ConcurrentHashMap<String, Subscription>();

    private ConcurrentHashMap<String, BBO> bbos = new ConcurrentHashMap<String, BBO>();

    private Logger logger = LoggerFactory.getLogger("MarketDataManager");

    private OrderDepthEngine orderDepthEngine = new OrderDepthEngine();

    private IMarketDataHandler marketDataHandler;

    private ISnapshotProcessor snapshotProcessorForOrderList;

    private ISnapshotProcessor snapshotProcessorForStatistics;

    private IIncrementalProcessor incrementalProcessorForOrderList;

    private IIncrementalProcessor incrementalProcessorForStatistics;

    private IIncrementalProcessor incrementalProcessorForPublicTrades;

    @Inject
    private MessageSequenceValidatorFactory messageSequenceValidatorFactory;

    @Inject
    private MessageHandlerFactory messageHandlerFactory;

    @Inject
    private HeartbeatProcessor heartbeatProcessor;

    private InstrumentManager instrumentManager;

    private IGatewayPerformanceLogger performanceLogger;

    private boolean feedStatusUP = false;

    private boolean feedStatusALL = true;

//    private long debugWarningSilentPeriod = 0;

    public void setInstrumentManager(InstrumentManager instrumentManager) {
        this.instrumentManager = instrumentManager;
    }

    public MarketDataManager configure(IGatewayConfiguration configuration) {
        marketDataHandler = configuration.getMarketDataHandler();
        performanceLogger = configuration.getPerformanceLogger();

        IMessageHandler messageHandlerForOrderList = messageHandlerFactory.createOrderListMessageHandler(configuration);
        IMessageHandler messageHandlerForStatistics = messageHandlerFactory.createStatisticsMessageHandler(configuration);
        IMessageHandler messageHandlerForPublicTrades = messageHandlerFactory.createPublicTradesMessageHandler(configuration);

        IMessageSequenceValidator sequenceValidatorForOrderList = messageSequenceValidatorFactory.createMessageSequenceValidatorForOrderList();
        IMessageSequenceValidator sequenceValidatorForStatistics = messageSequenceValidatorFactory.createMessageSequenceValidatorForStatistics();
        IMessageSequenceValidator sequenceValidatorForPublicTrades = messageSequenceValidatorFactory.createMessageSequenceValidatorForPublicTrades();

        snapshotProcessorForOrderList = new SnapshotProcessor(messageHandlerForOrderList, sequenceValidatorForOrderList);
        snapshotProcessorForStatistics = new SnapshotProcessor(messageHandlerForStatistics, sequenceValidatorForStatistics);

        incrementalProcessorForOrderList = new IncrementalProcessor(messageHandlerForOrderList, sequenceValidatorForOrderList);
        incrementalProcessorForStatistics = new IncrementalProcessor(messageHandlerForStatistics, sequenceValidatorForStatistics);
        incrementalProcessorForPublicTrades = new IncrementalProcessor(messageHandlerForPublicTrades, sequenceValidatorForPublicTrades);

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
        if (logger.isTraceEnabled())
            logger.trace("onBBO " + newBBO.getSecurityId());

        BBO currentBBO = getOrCreateBBO(newBBO.getSecurityId());
        synchronized (currentBBO) {
            boolean[] changed = orderDepthEngine.updateBBO(currentBBO, newBBO);

            if (subscriptions.containsKey(newBBO.getSecurityId())) {
                if (changed[0])
                    marketDataHandler.onBBO(currentBBO);
                if (changed[1])
                    marketDataHandler.onStatistics(currentBBO);
                if (changed[2])
                    marketDataHandler.onTradingStatus(currentBBO);
            }
        }

        logPerformance(newBBO);
    }

    public void onDepthLevels(DepthLevel[] depthLevels) {
        if (logger.isTraceEnabled())
            logger.trace("onDepthLevel " + depthLevels[0].getSecurityId());

        BBO currentBBO = getOrCreateBBO(depthLevels[0].getSecurityId());
        synchronized (currentBBO) {
            List<DepthLevel> depthLevelsToSend = new ArrayList<>();
            orderDepthEngine.onDepthLevels(depthLevels, depthLevelsToSend);

            if (subscriptions.containsKey(depthLevels[0].getSecurityId()))
                marketDataHandler.onDepthLevels(depthLevelsToSend.toArray(new DepthLevel[0]));
        }

        logPerformance(depthLevels);
    }


    public void onPublicTrade(PublicTrade publicTrade) {
        if (logger.isTraceEnabled())
            logger.trace("onPublicTrade " + publicTrade.getSecurityId());

        BBO currentBBO = getOrCreateBBO(publicTrade.getSecurityId());
        synchronized (currentBBO) {
            orderDepthEngine.onPublicTrade(publicTrade);

            if (subscriptions.containsKey(publicTrade.getSecurityId()))
                marketDataHandler.onPublicTrade(publicTrade);
        }

        logPerformance(publicTrade);
    }

    private void logPerformance(BBO bbo) {
        if (performanceLogger == null)
            return;

        if (bbo.getPerformanceData() == null)
            return;

        performanceLogger.notifyBBOPerformance(bbo.getPerformanceData());
    }

    private void logPerformance(DepthLevel[] depthLevels) {
         if (performanceLogger == null)
            return;

        for (DepthLevel depthLevel : depthLevels) {

//            if (logger.isDebugEnabled()) {
//                if (depthLevel.getPerformanceData().getExchangeSendingTime()
//                        - depthLevel.getPerformanceData().getExchangeEntryTime()
//                        > 10_000_0L) { // 10ms in ticks
//                    if (debugWarningSilentPeriod == 0 || depthLevel.getPerformanceData().getExchangeSendingTime()
//                            > debugWarningSilentPeriod) {
//                        logger.debug("MDEntryTime is more than 10ms lower than SendingTime ["
//                                + (depthLevel.getPerformanceData().getExchangeSendingTime()
//                                    - depthLevel.getPerformanceData().getExchangeEntryTime()) + "]");
//
//                        debugWarningSilentPeriod = depthLevel.getPerformanceData().getExchangeSendingTime() + 60_000_000_0L; // 60s in ticks
//                    }
//                }
//            }
            if (depthLevel.getPerformanceData() == null)
                continue;

            performanceLogger.notifyOrderListPerformance(depthLevel.getPerformanceData());
        }
    }

    private void logPerformance(PublicTrade publicTrade) {
        if (performanceLogger == null)
            return;

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

    public boolean isAllowedInstrument(String symbol, String tradingSessionId) {
        return instrumentManager.isAllowedInstrument(new Instrument(symbol, tradingSessionId));
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
}