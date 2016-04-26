package ru.ncapital.gateways.micexfast;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.openfast.MessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ncapital.gateways.micexfast.connection.messageprocessors.HeartbeatProcessor;
import ru.ncapital.gateways.micexfast.connection.messageprocessors.IncrementalProcessor;
import ru.ncapital.gateways.micexfast.connection.messageprocessors.SnapshotProcessor;
import ru.ncapital.gateways.micexfast.connection.messageprocessors.sequencevalidators.IMessageSequenceValidator;
import ru.ncapital.gateways.micexfast.connection.messageprocessors.sequencevalidators.MessageSequenceValidatorFactory;
import ru.ncapital.gateways.micexfast.domain.*;
import ru.ncapital.gateways.micexfast.messagehandlers.IMessageHandler;
import ru.ncapital.gateways.micexfast.messagehandlers.MessageHandlerFactory;
import ru.ncapital.gateways.micexfast.performance.IGatewayPerformanceLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    private SnapshotProcessor snapshotProcessorForOrderList;

    private SnapshotProcessor snapshotProcessorForStatistics;

    private IncrementalProcessor incrementalProcessorForOrderList;

    private IncrementalProcessor incrementalProcessorForStatistics;

    private IncrementalProcessor incrementalProcessorForPublicTrades;

    @Inject
    private MessageSequenceValidatorFactory messageSequenceValidatorFactory;

    @Inject
    private MessageHandlerFactory messageHandlerFactory;

    @Inject
    private HeartbeatProcessor heartbeatProcessor;

    private InstrumentManager instrumentManager;

    private IGatewayPerformanceLogger performanceLogger;

    public MarketDataManager configure(IGatewayConfiguration configuration) {
        this.marketDataHandler = configuration.getMarketDataHandler();
        this.performanceLogger = configuration.getPerformanceLogger();

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
        long inTimeInTicks = Utils.currentTimeInTicks();
        if (logger.isTraceEnabled())
            logger.trace("onSubscribe " + subscription.getSubscriptionKey());

        if (subscriptions.putIfAbsent(subscription.getSubscriptionKey(), subscription) == null) {
            if (logger.isDebugEnabled())
                logger.debug("Added subscription for " + subscription.getSubscriptionKey());
        }

        BBO currentBBO = getOrCreateBBO(subscription.getSubscriptionKey());
        synchronized (currentBBO) {
            List<DepthLevel> depthLevelsToSend = new ArrayList<DepthLevel>();
            depthLevelsToSend.add(new DepthLevel(subscription.getSubscriptionKey(), MdUpdateAction.SNAPSHOT));
            orderDepthEngine.getDepthLevels(subscription.getSubscriptionKey(), depthLevelsToSend);

            marketDataHandler.onBBO(currentBBO, inTimeInTicks);
            marketDataHandler.onDepthLevels(depthLevelsToSend.toArray(new DepthLevel[0]), inTimeInTicks);
            marketDataHandler.onStatistics(currentBBO, inTimeInTicks);
            marketDataHandler.onTradingStatus(currentBBO, inTimeInTicks);
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

    public void onBBO(BBO newBBO, long inTimeInTicks) {
        if (logger.isTraceEnabled())
            logger.trace("onBBO " + newBBO.getSecurityId());

        BBO currentBBO = getOrCreateBBO(newBBO.getSecurityId());
        synchronized (currentBBO) {
            boolean[] changed = orderDepthEngine.updateBBO(currentBBO, newBBO);

            if (subscriptions.containsKey(newBBO.getSecurityId())) {
                if (changed[0])
                    marketDataHandler.onBBO(currentBBO, inTimeInTicks);
                if (changed[1])
                    marketDataHandler.onStatistics(currentBBO, inTimeInTicks);
                if (changed[2])
                    marketDataHandler.onTradingStatus(currentBBO, inTimeInTicks);
            }
        }
    }

    public void onDepthLevels(DepthLevel[] depthLevels, long inTimeInTicks) {
        if (logger.isTraceEnabled())
            logger.trace("onDepthLevel " + depthLevels[0].getSecurityId());

        BBO currentBBO = getOrCreateBBO(depthLevels[0].getSecurityId());
        synchronized (currentBBO) {
            List<DepthLevel> depthLevelsToSend = new ArrayList<DepthLevel>();
            orderDepthEngine.onDepthLevels(depthLevels, depthLevelsToSend);

            if (subscriptions.containsKey(depthLevels[0].getSecurityId()))
                marketDataHandler.onDepthLevels(depthLevelsToSend.toArray(new DepthLevel[0]), inTimeInTicks);
        }

        if (depthLevels[0].getMdUpdateAction() != MdUpdateAction.SNAPSHOT && performanceLogger != null) {
            for (DepthLevel depthLevel : depthLevels) {
                if (depthLevel.getMdEntryTime() > 0 && inTimeInTicks > 0) {
                    if ((inTimeInTicks - depthLevel.getMdEntryTime()) / 10000 > 1000)
                        logger.warn("onDepthLevels " + depthLevel.toString() + " processing time " + ((inTimeInTicks - depthLevel.getMdEntryTime()) / 10000) + "ms exceeds limit of 1000ms");

                    performanceLogger.notify(depthLevel.getMdEntryTime(), inTimeInTicks, "external");
                }
            }
        }
    }

    public void onPublicTrade(PublicTrade publicTrade, long inTimeInTicks) {
        if (logger.isTraceEnabled())
            logger.trace("onPublicTrade " + publicTrade.getSecurityId());

        BBO currentBBO = getOrCreateBBO(publicTrade.getSecurityId());
        synchronized (currentBBO) {
            orderDepthEngine.onPublicTrade(publicTrade);

            if (subscriptions.containsKey(publicTrade.getSecurityId()))
                marketDataHandler.onPublicTrade(publicTrade, inTimeInTicks);
        }
    }

    public MessageHandler getSnapshotProcessorForOrderList() {
        return snapshotProcessorForOrderList;
    }

    public MessageHandler getSnapshotProcessorForStatistics() {
        return snapshotProcessorForStatistics;
    }

    public MessageHandler getIncrementalProcessorForOrderList() {
        return incrementalProcessorForOrderList;
    }

    public MessageHandler getIncrementalProcessorForStatistics() {
        return incrementalProcessorForStatistics;
    }

    public MessageHandler getIncrementalProcessorForPublicTrades() {
        return incrementalProcessorForPublicTrades;
    }

    public MessageHandler getHeartbeatProcessor() {
        return heartbeatProcessor;
    }

    public ThreadLocal<Long> getIncrementalProcessorForOrderListInTimestamp() {
        return incrementalProcessorForOrderList.getInTimestampHolder();
    }

    public ThreadLocal<Long> getIncrementalProcessorForStatisticsInTimestamp() {
        return incrementalProcessorForStatistics.getInTimestampHolder();
    }

    public ThreadLocal<Long> getIncrementalProcessorForPublicTradesInTimestamp() {
        return incrementalProcessorForPublicTrades.getInTimestampHolder();
    }

    public ThreadLocal<Long> getSnapshotProcessorForOrderListInTimestamp() {
        return snapshotProcessorForOrderList.getInTimestampHolder();
    }

    public ThreadLocal<Long> getSnapshotProcessorForStatisticsInTimestamp() {
        return snapshotProcessorForStatistics.getInTimestampHolder();
    }

    public void setIncrementalProcessorForOrderListIsPrimary(boolean isPrimary) {
        this.incrementalProcessorForOrderList.setIsPrimary(isPrimary);
    }

    public void setIncrementalProcessorForStatisticsIsPrimary(boolean isPrimary) {
        this.incrementalProcessorForStatistics.setIsPrimary(isPrimary);
    }

    public void setIncrementalProcessorForPublicTradesIsPrimary(boolean isPrimary) {
        this.incrementalProcessorForPublicTrades.setIsPrimary(isPrimary);
    }

    public boolean isAllowedInstrument(String symbol, String tradingSessionId) {
        // return bbos.containsKey(symbol + Instrument.BOARD_SEPARATOR + tradingSessionId);
        return instrumentManager.isAllowedInstrument(new Instrument(symbol, tradingSessionId));
    }

    public void setRecovery(String securityId, boolean isUp, boolean orderList) {
        BBO bbo = new BBO(securityId);
        bbo.setInRecovery(isUp, orderList ? 0 : 1);
        onBBO(bbo, 0);
    }

    public void setInstrumentManager(InstrumentManager instrumentManager) {
        this.instrumentManager = instrumentManager;
    }
}