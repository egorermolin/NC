package ru.ncapital.gateways.micexfast.connection;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ncapital.gateways.micexfast.*;
import ru.ncapital.gateways.micexfast.connection.multicast.MessageReader;
import ru.ncapital.gateways.micexfast.connection.multicast.MessageReaderStarter;
import ru.ncapital.gateways.micexfast.connection.multicast.MessageReaderStopper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by egore on 12/19/15.
 */

@Singleton
public class ConnectionManager {
    private final ExecutorService starter = Executors.newCachedThreadPool();

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

    public void startSnapshot(boolean withStatistics) {
        switch (marketType) {
            case CURR:
                starter.execute(new MessageReaderStarter(logger, multicastReceivers.get(ConnectionId.CURR_ORDER_LIST_SNAP_A)));
                starter.execute(new MessageReaderStarter(logger, multicastReceivers.get(ConnectionId.CURR_ORDER_LIST_SNAP_B)));

                if (withStatistics) {
                    starter.execute(new MessageReaderStarter(logger, multicastReceivers.get(ConnectionId.CURR_STATISTICS_SNAP_A)));
                    starter.execute(new MessageReaderStarter(logger, multicastReceivers.get(ConnectionId.CURR_STATISTICS_SNAP_B)));
                }
                break;
            case FOND:
                starter.execute(new MessageReaderStarter(logger, multicastReceivers.get(ConnectionId.FOND_ORDER_LIST_SNAP_A)));
                starter.execute(new MessageReaderStarter(logger, multicastReceivers.get(ConnectionId.FOND_ORDER_LIST_SNAP_B)));

                if (withStatistics) {
                    starter.execute(new MessageReaderStarter(logger, multicastReceivers.get(ConnectionId.FOND_STATISTICS_SNAP_A)));
                    starter.execute(new MessageReaderStarter(logger, multicastReceivers.get(ConnectionId.FOND_STATISTICS_SNAP_B)));
                }
                break;
        }
    }

    public void stopSnapshot(boolean withStatistics) {
        switch (marketType) {
            case CURR:
                starter.execute(new MessageReaderStopper(logger, multicastReceivers.get(ConnectionId.CURR_ORDER_LIST_SNAP_A)));
                starter.execute(new MessageReaderStopper(logger, multicastReceivers.get(ConnectionId.CURR_ORDER_LIST_SNAP_B)));

                if (withStatistics) {
                    starter.execute(new MessageReaderStopper(logger, multicastReceivers.get(ConnectionId.CURR_STATISTICS_SNAP_A)));
                    starter.execute(new MessageReaderStopper(logger, multicastReceivers.get(ConnectionId.CURR_STATISTICS_SNAP_B)));
                }
                break;
            case FOND:
                starter.execute(new MessageReaderStopper(logger, multicastReceivers.get(ConnectionId.FOND_ORDER_LIST_SNAP_A)));
                starter.execute(new MessageReaderStopper(logger, multicastReceivers.get(ConnectionId.FOND_ORDER_LIST_SNAP_B)));

                if (withStatistics) {
                    starter.execute(new MessageReaderStopper(logger, multicastReceivers.get(ConnectionId.FOND_STATISTICS_SNAP_A)));
                    starter.execute(new MessageReaderStopper(logger, multicastReceivers.get(ConnectionId.FOND_STATISTICS_SNAP_B)));
                }
                break;
        }
    }

    public void startIncremental(boolean withStatistics, boolean withPublicTrade) {
        switch (marketType) {
            case CURR:
                starter.execute(new MessageReaderStarter(logger, multicastReceivers.get(ConnectionId.CURR_ORDER_LIST_INCR_A)));
                starter.execute(new MessageReaderStarter(logger, multicastReceivers.get(ConnectionId.CURR_ORDER_LIST_INCR_B)));

                if (withStatistics) {
                    starter.execute(new MessageReaderStarter(logger, multicastReceivers.get(ConnectionId.CURR_STATISTICS_INCR_A)));
                    starter.execute(new MessageReaderStarter(logger, multicastReceivers.get(ConnectionId.CURR_STATISTICS_INCR_B)));
                }

                if (withPublicTrade) {
                    starter.execute(new MessageReaderStarter(logger, multicastReceivers.get(ConnectionId.CURR_PUB_TRADES_INCR_A)));
                    starter.execute(new MessageReaderStarter(logger, multicastReceivers.get(ConnectionId.CURR_PUB_TRADES_INCR_B)));
                }
                break;
            case FOND:
                starter.execute(new MessageReaderStarter(logger, multicastReceivers.get(ConnectionId.FOND_ORDER_LIST_INCR_A)));
                starter.execute(new MessageReaderStarter(logger, multicastReceivers.get(ConnectionId.FOND_ORDER_LIST_INCR_B)));

                if (withStatistics) {
                    starter.execute(new MessageReaderStarter(logger, multicastReceivers.get(ConnectionId.FOND_STATISTICS_INCR_A)));
                    starter.execute(new MessageReaderStarter(logger, multicastReceivers.get(ConnectionId.FOND_STATISTICS_INCR_B)));
                }

                if (withPublicTrade) {
                    starter.execute(new MessageReaderStarter(logger, multicastReceivers.get(ConnectionId.FOND_PUB_TRADES_INCR_A)));
                    starter.execute(new MessageReaderStarter(logger, multicastReceivers.get(ConnectionId.FOND_PUB_TRADES_INCR_B)));
                }
                break;
        }
    }

    public void stopIncremental(boolean withStatistics, boolean withPublicTrade) {
        switch (marketType) {
            case CURR:
                starter.execute(new MessageReaderStopper(logger, multicastReceivers.get(ConnectionId.CURR_ORDER_LIST_INCR_A)));
                starter.execute(new MessageReaderStopper(logger, multicastReceivers.get(ConnectionId.CURR_ORDER_LIST_INCR_B)));

                if (withStatistics) {
                    starter.execute(new MessageReaderStopper(logger, multicastReceivers.get(ConnectionId.CURR_STATISTICS_INCR_A)));
                    starter.execute(new MessageReaderStopper(logger, multicastReceivers.get(ConnectionId.CURR_STATISTICS_INCR_B)));
                }

                if (withPublicTrade) {
                    starter.execute(new MessageReaderStopper(logger, multicastReceivers.get(ConnectionId.CURR_PUB_TRADES_INCR_A)));
                    starter.execute(new MessageReaderStopper(logger, multicastReceivers.get(ConnectionId.CURR_PUB_TRADES_INCR_B)));
                }
                break;
            case FOND:
                starter.execute(new MessageReaderStopper(logger, multicastReceivers.get(ConnectionId.FOND_ORDER_LIST_INCR_A)));
                starter.execute(new MessageReaderStopper(logger, multicastReceivers.get(ConnectionId.FOND_ORDER_LIST_INCR_B)));

                if (withStatistics) {
                    starter.execute(new MessageReaderStopper(logger, multicastReceivers.get(ConnectionId.FOND_STATISTICS_INCR_A)));
                    starter.execute(new MessageReaderStopper(logger, multicastReceivers.get(ConnectionId.FOND_STATISTICS_INCR_B)));
                }

                if (withPublicTrade) {
                    starter.execute(new MessageReaderStopper(logger, multicastReceivers.get(ConnectionId.FOND_PUB_TRADES_INCR_A)));
                    starter.execute(new MessageReaderStopper(logger, multicastReceivers.get(ConnectionId.FOND_PUB_TRADES_INCR_B)));
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
}
