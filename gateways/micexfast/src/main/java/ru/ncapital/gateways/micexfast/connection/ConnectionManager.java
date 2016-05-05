package ru.ncapital.gateways.micexfast.connection;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ncapital.gateways.micexfast.*;
import ru.ncapital.gateways.micexfast.connection.messageprocessors.sequencevalidators.IMessageSequenceValidator;
import ru.ncapital.gateways.micexfast.connection.multicast.MessageReader;
import ru.ncapital.gateways.micexfast.connection.multicast.MessageReaderStarter;
import ru.ncapital.gateways.micexfast.connection.multicast.MessageReaderStopper;
import ru.ncapital.gateways.micexfast.messagehandlers.MessageHandlerType;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by egore on 12/19/15.
 */

@Singleton
public class ConnectionManager {
    private final ExecutorService starter = Executors.newCachedThreadPool();

    private final ScheduledExecutorService snapshotStarter = Executors.newScheduledThreadPool(1);

    private Map<ConnectionId, MessageReader> multicastReceivers = new HashMap<ConnectionId, MessageReader>();

    private Logger logger = LoggerFactory.getLogger("ConnectionManager");

    private MarketType marketType = MarketType.CURR;

    @Inject
    public ConnectionManager(ConfigurationManager configurationManager, MarketDataManager marketDataManager, InstrumentManager instrumentManager) {
        for (ConnectionId connectionId : ConnectionId.values()) {
            MessageReader mcr = new MessageReader(connectionId, configurationManager, marketDataManager, instrumentManager);
            try {
                mcr.init("info");
                multicastReceivers.put(connectionId, mcr);
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
                starter.execute(new MessageReaderStarter(logger, multicastReceivers.get(ConnectionId.CURR_INSTRUMENT_SNAP_A)));
                starter.execute(new MessageReaderStarter(logger, multicastReceivers.get(ConnectionId.CURR_INSTRUMENT_SNAP_B)));
                break;
            case FOND:
                starter.execute(new MessageReaderStarter(logger, multicastReceivers.get(ConnectionId.FOND_INSTRUMENT_SNAP_A)));
                starter.execute(new MessageReaderStarter(logger, multicastReceivers.get(ConnectionId.FOND_INSTRUMENT_SNAP_B)));
                break;
        }
    }

    public void stopInstrument() {
        switch (marketType) {
            case CURR:
                starter.execute(new MessageReaderStopper(logger, multicastReceivers.get(ConnectionId.CURR_INSTRUMENT_SNAP_A)));
                starter.execute(new MessageReaderStopper(logger, multicastReceivers.get(ConnectionId.CURR_INSTRUMENT_SNAP_B)));
                break;
            case FOND:
                starter.execute(new MessageReaderStopper(logger, multicastReceivers.get(ConnectionId.FOND_INSTRUMENT_SNAP_A)));
                starter.execute(new MessageReaderStopper(logger, multicastReceivers.get(ConnectionId.FOND_INSTRUMENT_SNAP_B)));
                break;
        }
    }

    public void startSnapshot(MessageHandlerType type) {
        switch (marketType) {
            case CURR:
                switch (type) {
                    case ORDER_LIST:
                        starter.execute(new MessageReaderStarter(logger, multicastReceivers.get(ConnectionId.CURR_ORDER_LIST_SNAP_A)));
                        starter.execute(new MessageReaderStarter(logger, multicastReceivers.get(ConnectionId.CURR_ORDER_LIST_SNAP_B)));
                        break;
                    case STATISTICS:
                        starter.execute(new MessageReaderStarter(logger, multicastReceivers.get(ConnectionId.CURR_STATISTICS_SNAP_A)));
                        starter.execute(new MessageReaderStarter(logger, multicastReceivers.get(ConnectionId.CURR_STATISTICS_SNAP_B)));
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
                        starter.execute(new MessageReaderStarter(logger, multicastReceivers.get(ConnectionId.FOND_ORDER_LIST_SNAP_A)));
                        starter.execute(new MessageReaderStarter(logger, multicastReceivers.get(ConnectionId.FOND_ORDER_LIST_SNAP_B)));
                        break;
                    case STATISTICS:
                        starter.execute(new MessageReaderStarter(logger, multicastReceivers.get(ConnectionId.FOND_STATISTICS_SNAP_A)));
                        starter.execute(new MessageReaderStarter(logger, multicastReceivers.get(ConnectionId.FOND_STATISTICS_SNAP_B)));
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
                        starter.execute(new MessageReaderStopper(logger, multicastReceivers.get(ConnectionId.CURR_ORDER_LIST_SNAP_A)));
                        starter.execute(new MessageReaderStopper(logger, multicastReceivers.get(ConnectionId.CURR_ORDER_LIST_SNAP_B)));
                        break;
                    case STATISTICS:
                        starter.execute(new MessageReaderStopper(logger, multicastReceivers.get(ConnectionId.CURR_STATISTICS_SNAP_A)));
                        starter.execute(new MessageReaderStopper(logger, multicastReceivers.get(ConnectionId.CURR_STATISTICS_SNAP_B)));
                        break;
                    case PUBLIC_TRADES:
                        if (logger.isDebugEnabled())
                            logger.error("NO SNAPSHOT CHANNEL FOR PUBLIC TRADES");

                        break;
                }
            case FOND:
                switch (type) {
                    case ORDER_LIST:
                        starter.execute(new MessageReaderStopper(logger, multicastReceivers.get(ConnectionId.FOND_ORDER_LIST_SNAP_A)));
                        starter.execute(new MessageReaderStopper(logger, multicastReceivers.get(ConnectionId.FOND_ORDER_LIST_SNAP_B)));
                        break;
                    case STATISTICS:
                        starter.execute(new MessageReaderStopper(logger, multicastReceivers.get(ConnectionId.FOND_STATISTICS_SNAP_A)));
                        starter.execute(new MessageReaderStopper(logger, multicastReceivers.get(ConnectionId.FOND_STATISTICS_SNAP_B)));
                        break;
                    case PUBLIC_TRADES:
                        if (logger.isDebugEnabled())
                            logger.error("NO SNAPSHOT CHANNEL FOR PUBLIC TRADES");

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
                        starter.execute(new MessageReaderStarter(logger, multicastReceivers.get(ConnectionId.CURR_ORDER_LIST_INCR_A)));
                        starter.execute(new MessageReaderStarter(logger, multicastReceivers.get(ConnectionId.CURR_ORDER_LIST_INCR_B)));
                        break;
                    case STATISTICS:
                        starter.execute(new MessageReaderStarter(logger, multicastReceivers.get(ConnectionId.CURR_STATISTICS_INCR_A)));
                        starter.execute(new MessageReaderStarter(logger, multicastReceivers.get(ConnectionId.CURR_STATISTICS_INCR_B)));
                        break;
                    case PUBLIC_TRADES:
                        starter.execute(new MessageReaderStarter(logger, multicastReceivers.get(ConnectionId.CURR_PUB_TRADES_INCR_A)));
                        starter.execute(new MessageReaderStarter(logger, multicastReceivers.get(ConnectionId.CURR_PUB_TRADES_INCR_B)));
                        break;
                }
                break;
            case FOND:
                switch (type) {
                    case ORDER_LIST:
                        starter.execute(new MessageReaderStarter(logger, multicastReceivers.get(ConnectionId.FOND_ORDER_LIST_INCR_A)));
                        starter.execute(new MessageReaderStarter(logger, multicastReceivers.get(ConnectionId.FOND_ORDER_LIST_INCR_B)));
                        break;
                    case STATISTICS:
                        starter.execute(new MessageReaderStarter(logger, multicastReceivers.get(ConnectionId.FOND_STATISTICS_INCR_A)));
                        starter.execute(new MessageReaderStarter(logger, multicastReceivers.get(ConnectionId.FOND_STATISTICS_INCR_B)));
                        break;
                    case PUBLIC_TRADES:
                        starter.execute(new MessageReaderStarter(logger, multicastReceivers.get(ConnectionId.FOND_PUB_TRADES_INCR_A)));
                        starter.execute(new MessageReaderStarter(logger, multicastReceivers.get(ConnectionId.FOND_PUB_TRADES_INCR_B)));
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
                        starter.execute(new MessageReaderStopper(logger, multicastReceivers.get(ConnectionId.CURR_ORDER_LIST_INCR_A)));
                        starter.execute(new MessageReaderStopper(logger, multicastReceivers.get(ConnectionId.CURR_ORDER_LIST_INCR_B)));
                        break;
                    case STATISTICS:
                        starter.execute(new MessageReaderStopper(logger, multicastReceivers.get(ConnectionId.CURR_STATISTICS_INCR_A)));
                        starter.execute(new MessageReaderStopper(logger, multicastReceivers.get(ConnectionId.CURR_STATISTICS_INCR_B)));
                        break;
                    case PUBLIC_TRADES:
                        starter.execute(new MessageReaderStopper(logger, multicastReceivers.get(ConnectionId.CURR_PUB_TRADES_INCR_A)));
                        starter.execute(new MessageReaderStopper(logger, multicastReceivers.get(ConnectionId.CURR_PUB_TRADES_INCR_B)));
                        break;
                }
                break;
            case FOND:
                switch (type) {
                    case ORDER_LIST:
                        starter.execute(new MessageReaderStopper(logger, multicastReceivers.get(ConnectionId.FOND_ORDER_LIST_INCR_A)));
                        starter.execute(new MessageReaderStopper(logger, multicastReceivers.get(ConnectionId.FOND_ORDER_LIST_INCR_B)));
                        break;
                    case STATISTICS:
                        starter.execute(new MessageReaderStopper(logger, multicastReceivers.get(ConnectionId.FOND_STATISTICS_INCR_A)));
                        starter.execute(new MessageReaderStopper(logger, multicastReceivers.get(ConnectionId.FOND_STATISTICS_INCR_B)));
                        break;
                    case PUBLIC_TRADES:
                        starter.execute(new MessageReaderStopper(logger, multicastReceivers.get(ConnectionId.FOND_PUB_TRADES_INCR_A)));
                        starter.execute(new MessageReaderStopper(logger, multicastReceivers.get(ConnectionId.FOND_PUB_TRADES_INCR_B)));
                        break;
                }
                break;
        }
    }

    public void startInstrumentStatus() {
        switch(marketType) {
            case CURR:
                starter.execute(new MessageReaderStarter(logger, multicastReceivers.get(ConnectionId.CURR_INSTRUMENT_INCR_A)));
                starter.execute(new MessageReaderStarter(logger, multicastReceivers.get(ConnectionId.CURR_INSTRUMENT_INCR_B)));
                break;
            case FOND:
                starter.execute(new MessageReaderStarter(logger, multicastReceivers.get(ConnectionId.FOND_INSTRUMENT_INCR_A)));
                starter.execute(new MessageReaderStarter(logger, multicastReceivers.get(ConnectionId.FOND_INSTRUMENT_INCR_B)));
                break;
        }
    }

    public void stopInstrumentStatus() {
        switch(marketType) {
            case CURR:
                starter.execute(new MessageReaderStopper(logger, multicastReceivers.get(ConnectionId.CURR_INSTRUMENT_INCR_A)));
                starter.execute(new MessageReaderStopper(logger, multicastReceivers.get(ConnectionId.CURR_INSTRUMENT_INCR_B)));
                break;
            case FOND:
                starter.execute(new MessageReaderStopper(logger, multicastReceivers.get(ConnectionId.FOND_INSTRUMENT_INCR_A)));
                starter.execute(new MessageReaderStopper(logger, multicastReceivers.get(ConnectionId.FOND_INSTRUMENT_INCR_B)));
                break;
        }
    }

    public void shutdown() {
        starter.shutdown();
        try {
            while (!starter.isTerminated()) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            Utils.printStackTrace(e, logger, "InterruptedException occurred..");
        }
    }

    public void scheduleSnapshotWatch(IMessageSequenceValidator sequenceValidatorToWatch) {
        snapshotStarter.scheduleAtFixedRate(new Runnable() {
            private IMessageSequenceValidator sequenceValidator = sequenceValidatorToWatch;

            private boolean isRecovering = sequenceValidator.isRecovering();

            @Override
            public void run() {
                synchronized (sequenceValidator) {
                    if (sequenceValidator.isRecovering() && !isRecovering) {
                        ConnectionManager.this.startSnapshot(sequenceValidator.getType());
                        isRecovering = true;
                    } else if (!sequenceValidator.isRecovering() && isRecovering) {
                        ConnectionManager.this.stopSnapshot(sequenceValidator.getType());
                        isRecovering = false;
                    }
                }
            }
        }, 600, 1, TimeUnit.SECONDS);
    }
}
