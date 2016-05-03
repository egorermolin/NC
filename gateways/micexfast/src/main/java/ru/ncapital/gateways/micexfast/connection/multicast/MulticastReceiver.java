package ru.ncapital.gateways.micexfast.connection.multicast;

import org.apache.log4j.Level;
import org.openfast.Context;
import org.openfast.Message;
import org.openfast.MessageHandler;
import org.openfast.MessageInputStream;
import org.openfast.codec.Coder;
import org.openfast.logging.FastMessageLogger;
import org.openfast.template.MessageTemplate;
import org.openfast.template.loader.XMLMessageTemplateLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ncapital.gateways.micexfast.*;
import ru.ncapital.gateways.micexfast.connection.Connection;
import ru.ncapital.gateways.micexfast.connection.ConnectionId;
import ru.ncapital.gateways.micexfast.domain.ProductType;
import ru.ncapital.gateways.micexfast.domain.TradingSessionId;
import ru.ncapital.gateways.micexfast.performance.IGatewayPerformanceLogger;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.MembershipKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by egore on 12/8/15.
 */
public class MulticastReceiver implements IEventListener {

    private class Statistics {
        private int totalNumberOfMessages = 0;

        private List<Double> latencies = new ArrayList<Double>();

        synchronized void addValue(double latency) {
            latencies.add(latency);
        }

        synchronized String dump() {
            totalNumberOfMessages += latencies.size();
            Collections.sort(latencies);
            double totalLatency = 0.0;
            for (Double latency : latencies)
                totalLatency += latency;

            StringBuilder sb = new StringBuilder();

            sb.append("[Total: ").append(totalNumberOfMessages).append("]");
            if (latencies.size() > 0) {
                sb.append("[Last: ").append(latencies.size()).append("]");
                sb.append("[MinL: ").append(latencies.get(0)).append("]");
                sb.append("[MedL: ").append(latencies.get(latencies.size() / 2)).append("]");
                sb.append("[MaxL: ").append(latencies.get(latencies.size() - 1)).append("]");
                sb.append("[AvgL: ").append(String.format("%.2f", totalLatency / latencies.size())).append("]");
            }
            latencies.clear();

            return sb.toString();
        }
    }

    private Statistics stats = new Statistics();

    private Connection connection;

    private ConnectionId connectionId;

    private String intf;

    private DatagramChannel channel;

    private MembershipKey membership;

    private MessageInputStream messageReader;

    private MicexFastMulticastInputStream multicastInputStream;

    private String fastTemplatesFile;

    private MarketDataManager marketDataManager;

    private InstrumentManager instrumentManager;

    private final boolean asynch;

    private volatile boolean running;

    private Logger logger;

    private String level;

    private ThreadLocal<Long> inTimestamp;

    private long received;

    public MulticastReceiver(ConnectionId connectionId, ConfigurationManager configurationManager, MarketDataManager marketDataManager, InstrumentManager instumentManager) {
        this.connectionId = connectionId;
        this.asynch = configurationManager.isAsynchChannelReader();
        this.connection = configurationManager.getConnection(connectionId);
        this.intf = this.connectionId.isPrimary() ? configurationManager.getPrimaryNetworkInterface() : configurationManager.getSecondaryNetworkInterface();
        this.fastTemplatesFile = configurationManager.getFastTemplatesFile();

        this.marketDataManager = marketDataManager;
        this.instrumentManager = instumentManager;

        this.logger = LoggerFactory.getLogger(connectionId.getConnectionId() + "-MulticastReceiver");

        if (logger.isDebugEnabled())
            logger.debug("Created [Connection: " + connectionId.getConnectionId() + "]");
    }

    public void init(String level) throws IOException {
        this.level = level;
        this.channel = DatagramChannel.open(StandardProtocolFamily.INET)
                .setOption(StandardSocketOptions.SO_REUSEADDR, true)
                .bind(new InetSocketAddress(connection.getPort()));

        this.channel.setOption(StandardSocketOptions.SO_RCVBUF, 1000000);
        this.channel.setOption(StandardSocketOptions.IP_MULTICAST_IF, NetworkInterface.getByName(intf));

        if (logger.isDebugEnabled())
            logger.debug("Opened channel on [Port: " + connection.getPort() + "]");
    }

    public void destroy() throws IOException {
        channel.close();
    }

    public void start() {
        Thread.currentThread().setName(connectionId.toString());

        running = true;

        try {
            connect();
            multicastInputStream.start();
            run();
        } catch (IOException e) {
            Utils.printStackTrace(e, logger);
            running = false;
        }
    }

    public void stop() {
        running = false;

        try {
            disconnect();
            multicastInputStream.stop();
            destroy();
        } catch (IOException e) {
            Utils.printStackTrace(e, logger);
        }
    }

    private void connect() throws IOException {
        membership = channel.join(InetAddress.getByName(connection.getIp()),
                NetworkInterface.getByName(intf),
                InetAddress.getByName(connection.getSource()));

        if (logger.isDebugEnabled())
            logger.debug("Joined [Group: " + connection.getIp() + "(" + InetAddress.getByName(connection.getIp()) + ")]" +
                                "[Interface: " + intf + "(" + NetworkInterface.getByName(intf).toString() + ")]" +
                                "[Source: " + connection.getSource() + "(" + InetAddress.getByName(connection.getSource()) + ")]" +
                                "[Key: " + membership.toString() + "]");

        System.out.println(channel);
        channel.receive(ByteBuffer.allocate(1500));

        multicastInputStream = new MicexFastMulticastInputStream(channel, logger, this, asynch);
        messageReader = new MessageInputStream(multicastInputStream);

        for (MessageTemplate template : new XMLMessageTemplateLoader()
                .load(new FileInputStream(fastTemplatesFile)))
            messageReader.registerTemplate(Integer.valueOf(template.getId()), template);

        if (instrumentManager == null && marketDataManager == null) {
            registerMessageHandler(new MessageHandler() {
                @Override
                public void handleMessage(Message readMessage, Context context, Coder coder) {
                    long sendingTime = readMessage.getLong("SendingTime");
                    long currentTime = Utils.currentTimeInToday();

                    stats.addValue(currentTime - sendingTime % (1000L * 100L * 100L * 100L));
                }
            });
            multicastInputStream.setInTimestamp(inTimestamp = new ThreadLocal<Long>() {
                @Override
                public Long initialValue() {
                    return new Long(0);
                }
            });
        } else {
            switch (connectionId) {
                case CURR_INSTRUMENT_INCR_A:
                case FOND_INSTRUMENT_INCR_A:
                case CURR_INSTRUMENT_INCR_B:
                case FOND_INSTRUMENT_INCR_B:
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("f"), instrumentManager);
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("0"), marketDataManager.getHeartbeatProcessor());
                    multicastInputStream.setInTimestamp(inTimestamp = new ThreadLocal<Long>() {
                        @Override
                        public Long initialValue() {
                            return new Long(0);
                        }
                    });
                    break;

                case FOND_INSTRUMENT_SNAP_A:
                case CURR_INSTRUMENT_SNAP_A:
                case FOND_INSTRUMENT_SNAP_B:
                case CURR_INSTRUMENT_SNAP_B:
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("d"), instrumentManager);
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("0"), marketDataManager.getHeartbeatProcessor());
                    multicastInputStream.setInTimestamp(inTimestamp = new ThreadLocal<Long>() {
                        @Override
                        public Long initialValue() {
                            return new Long(0);
                        }
                    });
                    break;

                case CURR_ORDER_LIST_INCR_A:
                    marketDataManager.setIncrementalProcessorForOrderListIsPrimary(true);
                case CURR_ORDER_LIST_INCR_B:
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("X-OLR-CURR"), marketDataManager.getIncrementalProcessorForOrderList());
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("0"), marketDataManager.getHeartbeatProcessor());
                    multicastInputStream.setInTimestamp(inTimestamp = marketDataManager.getIncrementalProcessorForOrderListInTimestamp());
                    break;

                case FOND_ORDER_LIST_INCR_A:
                    marketDataManager.setIncrementalProcessorForOrderListIsPrimary(true);
                case FOND_ORDER_LIST_INCR_B:
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("X-OLR-FOND"), marketDataManager.getIncrementalProcessorForOrderList());
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("0"), marketDataManager.getHeartbeatProcessor());
                    multicastInputStream.setInTimestamp(inTimestamp = marketDataManager.getIncrementalProcessorForOrderListInTimestamp());
                    break;

                case CURR_ORDER_LIST_SNAP_A:
                case CURR_ORDER_LIST_SNAP_B:
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("W-OLS-CURR"), marketDataManager.getSnapshotProcessorForOrderList());
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("0"), marketDataManager.getHeartbeatProcessor());
                    multicastInputStream.setInTimestamp(inTimestamp = marketDataManager.getSnapshotProcessorForOrderListInTimestamp());
                    break;

                case FOND_ORDER_LIST_SNAP_A:
                case FOND_ORDER_LIST_SNAP_B:
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("W-OLS-FOND"), marketDataManager.getSnapshotProcessorForOrderList());
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("0"), marketDataManager.getHeartbeatProcessor());
                    multicastInputStream.setInTimestamp(inTimestamp = marketDataManager.getSnapshotProcessorForOrderListInTimestamp());
                    break;

                case CURR_STATISTICS_INCR_A:
                    marketDataManager.setIncrementalProcessorForStatisticsIsPrimary(true);
                case CURR_STATISTICS_INCR_B:
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("X-MSR-CURR"), marketDataManager.getIncrementalProcessorForStatistics());
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("0"), marketDataManager.getHeartbeatProcessor());
                    multicastInputStream.setInTimestamp(inTimestamp = marketDataManager.getIncrementalProcessorForStatisticsInTimestamp());
                    break;

                case FOND_STATISTICS_INCR_A:
                    marketDataManager.setIncrementalProcessorForStatisticsIsPrimary(true);
                case FOND_STATISTICS_INCR_B:
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("X-MSR-FOND"), marketDataManager.getIncrementalProcessorForStatistics());
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("0"), marketDataManager.getHeartbeatProcessor());
                    multicastInputStream.setInTimestamp(inTimestamp = marketDataManager.getIncrementalProcessorForStatisticsInTimestamp());
                    break;

                case CURR_STATISTICS_SNAP_A:
                case FOND_STATISTICS_SNAP_A:
                case CURR_STATISTICS_SNAP_B:
                case FOND_STATISTICS_SNAP_B:
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("W-Generic"), marketDataManager.getSnapshotProcessorForStatistics());
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("0"), marketDataManager.getHeartbeatProcessor());
                    multicastInputStream.setInTimestamp(inTimestamp = marketDataManager.getSnapshotProcessorForStatisticsInTimestamp());
                    break;

                case CURR_PUB_TRADES_INCR_A:
                    marketDataManager.setIncrementalProcessorForPublicTradesIsPrimary(true);
                case CURR_PUB_TRADES_INCR_B:
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("X-TLR-CURR"), marketDataManager.getIncrementalProcessorForPublicTrades());
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("0"), marketDataManager.getHeartbeatProcessor());
                    multicastInputStream.setInTimestamp(inTimestamp = marketDataManager.getIncrementalProcessorForPublicTradesInTimestamp());
                    break;

                case FOND_PUB_TRADES_INCR_A:
                    marketDataManager.setIncrementalProcessorForPublicTradesIsPrimary(true);
                case FOND_PUB_TRADES_INCR_B:
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("X-TLR-FOND"), marketDataManager.getIncrementalProcessorForPublicTrades());
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("0"), marketDataManager.getHeartbeatProcessor());
                    multicastInputStream.setInTimestamp(inTimestamp = marketDataManager.getIncrementalProcessorForPublicTradesInTimestamp());
                    break;
            }
        }

        messageReader.setBlockReader(new MicexBlockReader());

        if ("debug".equals(level))
            enableDebug();

        if ("trace".equals(level))
            enableTrace();
    }

    private void registerMessageHandler(MessageHandler messageHandler) {
        messageReader.addMessageHandler(messageHandler);
    }

    private void disconnect() throws IOException {
        if (membership != null)
            membership.drop();
    }

    @Override
    public void onException(Exception e) {
        if (e instanceof AsynchronousCloseException) {
            logger.info("Channel closed");
        } else {
            logger.warn("Failed to read from channel, restarting ...");
            Utils.printStackTrace(e, logger);

            stop();

            start();
        }
    }

    private void run() throws IOException {
        logger.info("READY...");

        while (running) {
            try {
                messageReader.readMessage();
                received = inTimestamp.get();
            } catch (Exception e) {
                Utils.printStackTrace(e, logger);
            }
        }
    }

    public void enableTrace() {
        enableDebug();
        messageReader.getContext().setTraceEnabled(true);
    }

    public void enableDebug() {
        org.apache.log4j.Logger.getLogger(connectionId + "-MulticastReceiver").setLevel(Level.TRACE);
        messageReader.getContext().setLogger(new FastMessageLogger() {
            @Override
            public void log(Message message, byte[] bytes, Direction direction) {
                if (logger.isTraceEnabled())
                    logger.trace("(IN) " + Utils.convertTicksToToday(received) + " " + message.toString());
            }
        });
    }

    @Override
    public String toString() {
        return connectionId.getConnectionId();
    }

    public static void main(final String[] args) throws IOException {
        // org.apache.log4j.Logger.getLogger("MulticastReceiver").setLevel(Level.INFO);
        GatewayManager.addConsoleAppender("%d{HH:mm:ss} %m%n", Level.TRACE);
        GatewayManager.addFileAppender("log/log.mr.out", "%d{HH:mm:ss} %m%n", Level.TRACE);
        if (args.length < 4) {
            System.err.println("Usage MulticastReader <connectionId> <fast_templates> <interfaces> <connections_file> [debug,trace]");
            return;
        }

        if (ConnectionId.convert(args[0]) == null) {
            StringBuilder sb = new StringBuilder();
            for (ConnectionId id : ConnectionId.values())
                sb.append(id.getConnectionId()).append(' ');
            System.err.println("Valid connectionIds are: " + sb.toString());
            return;
        }

        final MulticastReceiver mr = new MulticastReceiver(
                ConnectionId.convert(args[0]),
                new ConfigurationManager().configure(
                        new NullGatewayConfiguration() {
                            @Override
                            public String getFastTemplatesFile() {
                                return args[1];
                            }

                            @Override
                            public String getNetworkInterface() {
                                return args[2];
                            }

                            @Override
                            public String getConnectionsFile() {
                                return args[3];
                            }

                            @Override
                            public boolean isAsynchChannelReader() {
                                return true;
                            }
                        }),
                null, null);

        mr.init(args.length > 4 ? args[4] : "info");

        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                int dumpStatistics = 0;
                while (mr.running) {
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        break;
                    }

                    if (Utils.currentTimeInTicks() - mr.received > 10 * 1000 * 1000 * 10)
                       mr.logger.warn("No messages received last 10 seconds ...");

                    dumpStatistics++;
                    if (dumpStatistics == 1) {
                        mr.logger.info(mr.stats.dump());
                        dumpStatistics = 0;
                    }
                }
            }
        });

        mr.start();
    }
}
