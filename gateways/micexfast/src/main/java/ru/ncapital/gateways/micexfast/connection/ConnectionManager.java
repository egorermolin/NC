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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by egore on 12/19/15.
 */

@Singleton
public class ConnectionManager {
    private final ExecutorService starter = Executors.newCachedThreadPool();

    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2);

    private Map<ConnectionId, MessageReader> messageReaders = new HashMap<>();

    private Logger logger = LoggerFactory.getLogger("ConnectionManager");

    private MarketType marketType = MarketType.CURR;

    private MarketDataManager marketDataManager;

    @Inject
    public ConnectionManager(ConfigurationManager configurationManager, MarketDataManager marketDataManager, InstrumentManager instrumentManager) {
        this.marketDataManager = marketDataManager;
        for (ConnectionId connectionId : ConnectionId.values()) {
            MessageReader mcr = new MessageReader(connectionId, configurationManager, marketDataManager, instrumentManager);
            try {
                mcr.init("info");
                messageReaders.put(connectionId, mcr);
            } catch (IOException e) {
                Utils.printStackTrace(e, logger, "IOException occurred while opening channel..");
            }
        }
    }

    private int checkMessageReaders() {
        final long threshold = 60L * 1000L * 1000L * 10L;
        int running = 0;
        int up = 0;
        long currentTime = Utils.currentTimeInTicks();
        for (MessageReader mr : messageReaders.values()) {
            if (mr.isRunning()) {
                running++;
                if (currentTime - mr.getLastReceivedTimestamp() < threshold)
                    up++;
            }
        }

        if (up == 0)
            return -1;

        return running - up;
    }

    public ConnectionManager configure(IGatewayConfiguration configuration) {
        this.marketType = configuration.getMarketType();
        return this;
    }

    public void start() {
        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                int result = checkMessageReaders();
                if (result != 0) {
                    if (result < 0)
                        marketDataManager.onFeedStatus(false, true);
                    else
                        marketDataManager.onFeedStatus(false, false);
                } else {
                    marketDataManager.onFeedStatus(true, true);
                }
            }
        }, 5, 1, TimeUnit.SECONDS);
    }

    public void startInstrument() {
        switch (marketType) {
            case CURR:
                starter.execute(new MessageReaderStarter(messageReaders.get(ConnectionId.CURR_INSTRUMENT_SNAP_A)));
                starter.execute(new MessageReaderStarter(messageReaders.get(ConnectionId.CURR_INSTRUMENT_SNAP_B)));
                break;
            case FOND:
                starter.execute(new MessageReaderStarter(messageReaders.get(ConnectionId.FOND_INSTRUMENT_SNAP_A)));
                starter.execute(new MessageReaderStarter(messageReaders.get(ConnectionId.FOND_INSTRUMENT_SNAP_B)));
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
                        starter.execute(new MessageReaderStarter(messageReaders.get(ConnectionId.CURR_ORDER_LIST_SNAP_A)));
                        starter.execute(new MessageReaderStarter(messageReaders.get(ConnectionId.CURR_ORDER_LIST_SNAP_B)));
                        break;
                    case STATISTICS:
                        starter.execute(new MessageReaderStarter(messageReaders.get(ConnectionId.CURR_STATISTICS_SNAP_A)));
                        starter.execute(new MessageReaderStarter(messageReaders.get(ConnectionId.CURR_STATISTICS_SNAP_B)));
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
                        starter.execute(new MessageReaderStarter(messageReaders.get(ConnectionId.FOND_ORDER_LIST_SNAP_A)));
                        starter.execute(new MessageReaderStarter(messageReaders.get(ConnectionId.FOND_ORDER_LIST_SNAP_B)));
                        break;
                    case STATISTICS:
                        starter.execute(new MessageReaderStarter(messageReaders.get(ConnectionId.FOND_STATISTICS_SNAP_A)));
                        starter.execute(new MessageReaderStarter(messageReaders.get(ConnectionId.FOND_STATISTICS_SNAP_B)));
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
                        starter.execute(new MessageReaderStarter(messageReaders.get(ConnectionId.CURR_ORDER_LIST_INCR_A)));
                        starter.execute(new MessageReaderStarter(messageReaders.get(ConnectionId.CURR_ORDER_LIST_INCR_B)));
                        break;
                    case STATISTICS:
                        starter.execute(new MessageReaderStarter(messageReaders.get(ConnectionId.CURR_STATISTICS_INCR_A)));
                        starter.execute(new MessageReaderStarter(messageReaders.get(ConnectionId.CURR_STATISTICS_INCR_B)));
                        break;
                    case PUBLIC_TRADES:
                        starter.execute(new MessageReaderStarter(messageReaders.get(ConnectionId.CURR_PUB_TRADES_INCR_A)));
                        starter.execute(new MessageReaderStarter(messageReaders.get(ConnectionId.CURR_PUB_TRADES_INCR_B)));
                        break;
                }
                break;
            case FOND:
                switch (type) {
                    case ORDER_LIST:
                        starter.execute(new MessageReaderStarter(messageReaders.get(ConnectionId.FOND_ORDER_LIST_INCR_A)));
                        starter.execute(new MessageReaderStarter(messageReaders.get(ConnectionId.FOND_ORDER_LIST_INCR_B)));
                        break;
                    case STATISTICS:
                        starter.execute(new MessageReaderStarter(messageReaders.get(ConnectionId.FOND_STATISTICS_INCR_A)));
                        starter.execute(new MessageReaderStarter(messageReaders.get(ConnectionId.FOND_STATISTICS_INCR_B)));
                        break;
                    case PUBLIC_TRADES:
                        starter.execute(new MessageReaderStarter(messageReaders.get(ConnectionId.FOND_PUB_TRADES_INCR_A)));
                        starter.execute(new MessageReaderStarter(messageReaders.get(ConnectionId.FOND_PUB_TRADES_INCR_B)));
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
                starter.execute(new MessageReaderStarter(messageReaders.get(ConnectionId.CURR_INSTRUMENT_INCR_A)));
                starter.execute(new MessageReaderStarter(messageReaders.get(ConnectionId.CURR_INSTRUMENT_INCR_B)));
                break;
            case FOND:
                starter.execute(new MessageReaderStarter(messageReaders.get(ConnectionId.FOND_INSTRUMENT_INCR_A)));
                starter.execute(new MessageReaderStarter(messageReaders.get(ConnectionId.FOND_INSTRUMENT_INCR_B)));
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

    public void shutdown() {
        int count = 5;
        starter.shutdown();
        try {
            while (!starter.isTerminated() && --count >= 0) {
                Thread.sleep(1000);
            }

            if (!starter.isTerminated()) {
                starter.shutdownNow();

                while (!starter.isTerminated()) {
                    Thread.sleep(1000);
                }
            }
        } catch (InterruptedException e) {
            Utils.printStackTrace(e, logger, "InterruptedException occurred..");
        }
    }

    public void scheduleSnapshotWatcher(final ISnapshotProcessor snapshotProcessorToWatch) {
        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            private ISnapshotProcessor snapshotProcessor = snapshotProcessorToWatch;

            private IMessageSequenceValidator sequenceValidator = snapshotProcessor.getSequenceValidator();

            private AtomicBoolean isRecovering;

            @Override
            public void run() {
                synchronized (sequenceValidator) {
                    if (isRecovering == null) {
                        isRecovering = new AtomicBoolean(sequenceValidator.isRecovering());
                        if (!isRecovering.get()) {
                            stopSnapshot(sequenceValidator.getType());
                            snapshotProcessor.reset();
                        }
                    }

                    if (sequenceValidator.isRecovering()) {
                        if (!isRecovering.getAndSet(true))
                            startSnapshot(sequenceValidator.getType());
                    } else {
                        if (isRecovering.getAndSet(false)) {
                            stopSnapshot(sequenceValidator.getType());
                            snapshotProcessor.reset();
                        }
                    }
                }
            }
        }, 1200, 1, TimeUnit.SECONDS);
    }

    public void stopSnapshotWatchers() {
        scheduledExecutorService.shutdown();
        try {
            while (!scheduledExecutorService.isTerminated()) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            Utils.printStackTrace(e, logger, "InterruptedException occurred..");
        }
    }
}
