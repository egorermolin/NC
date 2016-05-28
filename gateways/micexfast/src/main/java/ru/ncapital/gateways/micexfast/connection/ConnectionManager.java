package ru.ncapital.gateways.micexfast.connection;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ncapital.gateways.micexfast.*;
import ru.ncapital.gateways.micexfast.connection.messageprocessors.ISnapshotProcessor;
import ru.ncapital.gateways.micexfast.connection.messageprocessors.sequencevalidators.IMessageSequenceValidator;
import ru.ncapital.gateways.micexfast.connection.multicast.MessageReader;
import ru.ncapital.gateways.micexfast.connection.multicast.MessageReaderStarter;
import ru.ncapital.gateways.micexfast.messagehandlers.MessageHandlerType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by egore on 12/19/15.
 */

@Singleton
public class ConnectionManager {
    private final ExecutorService starterService = Executors.newCachedThreadPool();

    private final ScheduledExecutorService scheduledService = Executors.newScheduledThreadPool(2);

    private ScheduledFuture<?> messageReadersWatcherFuture;

    private List<ScheduledFuture<?>> snapshotWatcherFutures = new ArrayList<>();

    private Map<ConnectionId, MessageReader> messageReaders = new HashMap<>();

    private Logger logger = LoggerFactory.getLogger("ConnectionManager");

    private MarketType marketType = MarketType.CURR;

    private MarketDataManager marketDataManager;

    private List<ISnapshotProcessor> snapshotProcessorsToWatch;

    @Inject
    public ConnectionManager(ConfigurationManager configurationManager, MarketDataManager marketDataManager, InstrumentManager instrumentManager) {
        this.marketDataManager = marketDataManager;
        for (ConnectionId connectionId : ConnectionId.values()) {
            MessageReader messageReader = new MessageReader(connectionId, configurationManager, marketDataManager, instrumentManager);
            try {
                messageReader.init("info");
                messageReaders.put(connectionId, messageReader);
            } catch (IOException e) {
                Utils.printStackTrace(e, logger, "IOException occurred while opening channel..");
            }
        }
    }


    public ConnectionManager configure(IGatewayConfiguration configuration) {
        this.marketType = configuration.getMarketType();
        return this;
    }

    public void startInstrument() {
        switch (marketType) {
            case CURR:
                starterService.execute(new MessageReaderStarter(messageReaders.get(ConnectionId.CURR_INSTRUMENT_SNAP_A)));
                starterService.execute(new MessageReaderStarter(messageReaders.get(ConnectionId.CURR_INSTRUMENT_SNAP_B)));
                break;
            case FOND:
                starterService.execute(new MessageReaderStarter(messageReaders.get(ConnectionId.FOND_INSTRUMENT_SNAP_A)));
                starterService.execute(new MessageReaderStarter(messageReaders.get(ConnectionId.FOND_INSTRUMENT_SNAP_B)));
                break;
        }
    }

    public void stopInstrument() {
        switch (marketType) {
            case CURR:
                messageReaders.get(ConnectionId.CURR_INSTRUMENT_SNAP_A).stop();
                messageReaders.get(ConnectionId.CURR_INSTRUMENT_SNAP_B).stop();
                break;
            case FOND:
                messageReaders.get(ConnectionId.FOND_INSTRUMENT_SNAP_A).stop();
                messageReaders.get(ConnectionId.FOND_INSTRUMENT_SNAP_B).stop();
                break;
        }
    }

    public void startSnapshot(MessageHandlerType type) {
        switch (marketType) {
            case CURR:
                switch (type) {
                    case ORDER_LIST:
                        starterService.execute(new MessageReaderStarter(messageReaders.get(ConnectionId.CURR_ORDER_LIST_SNAP_A)));
                        starterService.execute(new MessageReaderStarter(messageReaders.get(ConnectionId.CURR_ORDER_LIST_SNAP_B)));
                        break;
                    case STATISTICS:
                        starterService.execute(new MessageReaderStarter(messageReaders.get(ConnectionId.CURR_STATISTICS_SNAP_A)));
                        starterService.execute(new MessageReaderStarter(messageReaders.get(ConnectionId.CURR_STATISTICS_SNAP_B)));
                        break;
                    case PUBLIC_TRADES:
                        if (logger.isDebugEnabled())
                            logger.debug("NO SNAPSHOT CHANNEL FOR PUBLIC TRADES");

                        break;
                }
                break;
            case FOND:
                switch (type) {
                    case ORDER_LIST:
                        starterService.execute(new MessageReaderStarter(messageReaders.get(ConnectionId.FOND_ORDER_LIST_SNAP_A)));
                        starterService.execute(new MessageReaderStarter(messageReaders.get(ConnectionId.FOND_ORDER_LIST_SNAP_B)));
                        break;
                    case STATISTICS:
                        starterService.execute(new MessageReaderStarter(messageReaders.get(ConnectionId.FOND_STATISTICS_SNAP_A)));
                        starterService.execute(new MessageReaderStarter(messageReaders.get(ConnectionId.FOND_STATISTICS_SNAP_B)));
                        break;
                    case PUBLIC_TRADES:
                        if (logger.isDebugEnabled())
                            logger.debug("NO SNAPSHOT CHANNEL FOR PUBLIC TRADES");

                        break;
                }
                break;
        }
    }

    public void stopSnapshot(MessageHandlerType type) {
        switch (marketType) {
            case CURR:
                switch (type) {
                    case ORDER_LIST:
                        messageReaders.get(ConnectionId.CURR_ORDER_LIST_SNAP_A).stop();
                        messageReaders.get(ConnectionId.CURR_ORDER_LIST_SNAP_B).stop();
                        break;
                    case STATISTICS:
                        messageReaders.get(ConnectionId.CURR_STATISTICS_SNAP_A).stop();
                        messageReaders.get(ConnectionId.CURR_STATISTICS_SNAP_B).stop();
                        break;
                    case PUBLIC_TRADES:
                        if (logger.isDebugEnabled())
                            logger.debug("NO SNAPSHOT CHANNEL FOR PUBLIC TRADES");

                        break;
                }
                break;
            case FOND:
                switch (type) {
                    case ORDER_LIST:
                        messageReaders.get(ConnectionId.FOND_ORDER_LIST_SNAP_A).stop();
                        messageReaders.get(ConnectionId.FOND_ORDER_LIST_SNAP_B).stop();
                        break;
                    case STATISTICS:
                        messageReaders.get(ConnectionId.FOND_STATISTICS_SNAP_A).stop();
                        messageReaders.get(ConnectionId.FOND_STATISTICS_SNAP_B).stop();
                        break;
                    case PUBLIC_TRADES:
                        if (logger.isDebugEnabled())
                            logger.debug("NO SNAPSHOT CHANNEL FOR PUBLIC TRADES");

                        break;
                }
                break;
        }
    }

    public void startIncremental(MessageHandlerType type) {
        switch (marketType) {
            case CURR:
                switch (type) {
                    case ORDER_LIST:
                        starterService.execute(new MessageReaderStarter(messageReaders.get(ConnectionId.CURR_ORDER_LIST_INCR_A)));
                        starterService.execute(new MessageReaderStarter(messageReaders.get(ConnectionId.CURR_ORDER_LIST_INCR_B)));
                        break;
                    case STATISTICS:
                        starterService.execute(new MessageReaderStarter(messageReaders.get(ConnectionId.CURR_STATISTICS_INCR_A)));
                        starterService.execute(new MessageReaderStarter(messageReaders.get(ConnectionId.CURR_STATISTICS_INCR_B)));
                        break;
                    case PUBLIC_TRADES:
                        starterService.execute(new MessageReaderStarter(messageReaders.get(ConnectionId.CURR_PUB_TRADES_INCR_A)));
                        starterService.execute(new MessageReaderStarter(messageReaders.get(ConnectionId.CURR_PUB_TRADES_INCR_B)));
                        break;
                }
                break;
            case FOND:
                switch (type) {
                    case ORDER_LIST:
                        starterService.execute(new MessageReaderStarter(messageReaders.get(ConnectionId.FOND_ORDER_LIST_INCR_A)));
                        starterService.execute(new MessageReaderStarter(messageReaders.get(ConnectionId.FOND_ORDER_LIST_INCR_B)));
                        break;
                    case STATISTICS:
                        starterService.execute(new MessageReaderStarter(messageReaders.get(ConnectionId.FOND_STATISTICS_INCR_A)));
                        starterService.execute(new MessageReaderStarter(messageReaders.get(ConnectionId.FOND_STATISTICS_INCR_B)));
                        break;
                    case PUBLIC_TRADES:
                        starterService.execute(new MessageReaderStarter(messageReaders.get(ConnectionId.FOND_PUB_TRADES_INCR_A)));
                        starterService.execute(new MessageReaderStarter(messageReaders.get(ConnectionId.FOND_PUB_TRADES_INCR_B)));
                        break;
                }
                break;
        }
    }

    public void stopIncremental(MessageHandlerType type) {
        switch (marketType) {
            case CURR:
                switch (type) {
                    case ORDER_LIST:
                        messageReaders.get(ConnectionId.CURR_ORDER_LIST_INCR_A).stop();
                        messageReaders.get(ConnectionId.CURR_ORDER_LIST_INCR_B).stop();
                        break;
                    case STATISTICS:
                        messageReaders.get(ConnectionId.CURR_STATISTICS_INCR_A).stop();
                        messageReaders.get(ConnectionId.CURR_STATISTICS_INCR_B).stop();
                        break;
                    case PUBLIC_TRADES:
                        messageReaders.get(ConnectionId.CURR_PUB_TRADES_INCR_A).stop();
                        messageReaders.get(ConnectionId.CURR_PUB_TRADES_INCR_B).stop();
                        break;
                }
                break;
            case FOND:
                switch (type) {
                    case ORDER_LIST:
                        messageReaders.get(ConnectionId.FOND_ORDER_LIST_INCR_A).stop();
                        messageReaders.get(ConnectionId.FOND_ORDER_LIST_INCR_B).stop();
                        break;
                    case STATISTICS:
                        messageReaders.get(ConnectionId.FOND_STATISTICS_INCR_A).stop();
                        messageReaders.get(ConnectionId.FOND_STATISTICS_INCR_B).stop();
                        break;
                    case PUBLIC_TRADES:
                        messageReaders.get(ConnectionId.FOND_PUB_TRADES_INCR_A).stop();
                        messageReaders.get(ConnectionId.FOND_PUB_TRADES_INCR_B).stop();
                        break;
                }
                break;
        }
    }

    public void startInstrumentStatus() {
        switch(marketType) {
            case CURR:
                starterService.execute(new MessageReaderStarter(messageReaders.get(ConnectionId.CURR_INSTRUMENT_INCR_A)));
                starterService.execute(new MessageReaderStarter(messageReaders.get(ConnectionId.CURR_INSTRUMENT_INCR_B)));
                break;
            case FOND:
                starterService.execute(new MessageReaderStarter(messageReaders.get(ConnectionId.FOND_INSTRUMENT_INCR_A)));
                starterService.execute(new MessageReaderStarter(messageReaders.get(ConnectionId.FOND_INSTRUMENT_INCR_B)));
                break;
        }
    }

    public void stopInstrumentStatus() {
        switch(marketType) {
            case CURR:
                messageReaders.get(ConnectionId.CURR_INSTRUMENT_INCR_A).stop();
                messageReaders.get(ConnectionId.CURR_INSTRUMENT_INCR_B).stop();
                break;
            case FOND:
                messageReaders.get(ConnectionId.FOND_INSTRUMENT_INCR_A).stop();
                messageReaders.get(ConnectionId.FOND_INSTRUMENT_INCR_B).stop();
                break;
        }
    }

    public void start(boolean isListenSnapshotChannelOnlyIfNeeded) {
        startMessageReadersWatcher();

        startInstrument();
        startInstrumentStatus();
        for (MessageHandlerType type : MessageHandlerType.values()) {
            startIncremental(type);
            startSnapshot(type);
        }

        if (isListenSnapshotChannelOnlyIfNeeded) {
            snapshotProcessorsToWatch.add(marketDataManager.getSnapshotProcessor(MessageHandlerType.ORDER_LIST));
            snapshotProcessorsToWatch.add(marketDataManager.getSnapshotProcessor(MessageHandlerType.STATISTICS));
        }
    }

    public void onInstrumentDownloadFinished() {
        stopInstrument();

        startSnapshotWatchers();
    }

    private void restart() {
        List<MessageReader> stoppedMessageReaders = new ArrayList<>();
        for (MessageReader messageReader : messageReaders.values()) {
            if (messageReader.isRunning()) {
                stoppedMessageReaders.add(messageReader);
                messageReader.stop();
            }
        }

        stopMessageReaderWatcher();
        startMessageReadersWatcher();

        for (MessageReader messageReader : stoppedMessageReaders) {
            starterService.execute(new MessageReaderStarter(messageReader));
        }
    }

    public void shutdown() {
        for (MessageHandlerType type : MessageHandlerType.values()) {
            stopIncremental(type);
            stopSnapshot(type);
        }
        stopInstrumentStatus();
        stopInstrument();
        stopSnapshotWatchers();

        stopMessageReaderWatcher();

        shutdownScheduledService();
        shutdownStarterService();
    }

    private void startSnapshotWatchers() {
        if (snapshotProcessorsToWatch.isEmpty())
            return;

        class SnapshotProcessorWatchTask implements Runnable {
            private ISnapshotProcessor snapshotProcessorToWatch;

            private IMessageSequenceValidator sequenceValidator;

            private AtomicBoolean isRecovering;

            private SnapshotProcessorWatchTask(ISnapshotProcessor snapshotProcessorToWatch) {
                this.snapshotProcessorToWatch = snapshotProcessorToWatch;
                this.sequenceValidator = snapshotProcessorToWatch.getSequenceValidator();
            }

            @Override
            public void run() {
                synchronized (sequenceValidator) {
                    if (isRecovering == null) {
                        isRecovering = new AtomicBoolean(sequenceValidator.isRecovering());
                        if (!isRecovering.get()) {
                            stopSnapshot(sequenceValidator.getType());
                            snapshotProcessorToWatch.reset();
                        }
                    }

                    if (sequenceValidator.isRecovering()) {
                        if (!isRecovering.getAndSet(true))
                            startSnapshot(sequenceValidator.getType());
                    } else {
                        if (isRecovering.getAndSet(false)) {
                            stopSnapshot(sequenceValidator.getType());
                            snapshotProcessorToWatch.reset();
                        }
                    }
                }
            }
        }

        for (final ISnapshotProcessor snapshotProcessorToWatch : snapshotProcessorsToWatch) {
            snapshotWatcherFutures.add(
                    scheduledService.scheduleAtFixedRate(
                            new SnapshotProcessorWatchTask(snapshotProcessorToWatch), 1200, 1, TimeUnit.SECONDS
                    )
            );
        }
    }

    private void stopSnapshotWatchers() {
        if (snapshotProcessorsToWatch.isEmpty())
            return;

        for (ScheduledFuture<?> snapshotWatcherFuture : snapshotWatcherFutures) {
            snapshotWatcherFuture.cancel(false);
        }
        snapshotWatcherFutures.clear();
    }

    private int checkMessageReaders() {
        final long threshold = 10L * 1000L * 1000L * 10L;
        int running = 0;
        int up = 0;
        long currentTime = Utils.currentTimeInTicks();
        for (MessageReader messageReader : messageReaders.values()) {
            if (messageReader.isRunning()) {
                running++;
                if (currentTime - messageReader.getLastReceivedTimestamp() < threshold)
                    up++;
                else
                    //if (logger.isDebugEnabled())
                        logger.warn("Message reader [" + messageReader.getConnectionId() + "] is down since [" + Utils.convertTicksToTodayString(messageReader.getLastReceivedTimestamp()) + "]");
            }
        }

        if (up == 0)
            return -1;

        return running - up;
    }

    private void startMessageReadersWatcher() {
        messageReadersWatcherFuture = scheduledService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                int result = checkMessageReaders();
                if (result != 0) {
                    if (result < 0) {
                        marketDataManager.onFeedStatus(false, true);
                        restart();
                    } else {
                        marketDataManager.onFeedStatus(false, false);
                    }
                } else {
                    marketDataManager.onFeedStatus(true, true);
                }
            }
        }, 30, 1, TimeUnit.SECONDS);
    }

    private void stopMessageReaderWatcher() {
        if (messageReadersWatcherFuture != null) {
            messageReadersWatcherFuture.cancel(false);
        }
        messageReadersWatcherFuture = null;
    }

    private void shutdownStarterService() {
        int count = 50;
        starterService.shutdown();
        try {
            while (!starterService.isTerminated() && --count >= 0) {
                Thread.sleep(100);
            }

            if (!starterService.isTerminated()) {
                starterService.shutdownNow();

                while (!starterService.isTerminated()) {
                    Thread.sleep(100);
                }
            }
        } catch (InterruptedException e) {
            Utils.printStackTrace(e, logger, "InterruptedException occurred..");
        }
    }

    private void shutdownScheduledService() {
        int count = 50;
        scheduledService.shutdown();
        try {
            while (!scheduledService.isTerminated() && --count >= 0) {
                Thread.sleep(100);
            }
            if (!scheduledService.isTerminated()) {
                scheduledService.shutdownNow();

                while (!scheduledService.isTerminated()) {
                    Thread.sleep(100);
                }
            }
        } catch (InterruptedException e) {
            Utils.printStackTrace(e, logger, "InterruptedException occurred..");
        }
    }
}
