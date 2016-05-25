package ru.ncapital.gateways.micexfast.connection.multicast;

import org.apache.log4j.Level;
import org.codehaus.groovy.tools.shell.IO;
import org.openfast.*;
import org.openfast.codec.Coder;
import org.openfast.error.FastException;
import org.openfast.logging.FastMessageLogger;
import org.openfast.template.MessageTemplate;
import org.openfast.template.loader.XMLMessageTemplateLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ncapital.gateways.micexfast.*;
import ru.ncapital.gateways.micexfast.connection.Connection;
import ru.ncapital.gateways.micexfast.connection.ConnectionId;
import ru.ncapital.gateways.micexfast.messagehandlers.MessageHandlerType;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.*;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.MembershipKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by egore on 12/8/15.
 */
public class MessageReader implements IMulticastEventListener {

    private class FineStatistics {
        private class FineStatisticsItem {
            // ALL TIMES IN TODAY MICROS (micros since midnight)

            private long entrTime;

            private long recvTime;

            private long decdTime;

            private FineStatisticsItem(long entrTime, long recvTime, long decdTime) {
                this.entrTime = entrTime;
                this.recvTime = recvTime;
                this.decdTime = decdTime;
            }
        }

        private List<FineStatisticsItem> items = new ArrayList<>();

        private long total = 0;

        private BufferedWriter writer;

        private boolean active = false;

        protected void initStatistics() {
            active = true;
        }

        protected void initStatisticsWritingToFile(String filename) {
            if (!active)
                return;

            try {
                writer = new BufferedWriter(new FileWriter(filename, false));
            } catch (IOException e) {
                System.err.println("FAILED TO OPEN FILE " + e.toString());
                writer = null;
            }
        }

        synchronized protected void addItem(long entrTime, long recvTime, long decdTime) {
            if (!active)
                return;

            items.add(new FineStatisticsItem(entrTime, recvTime, decdTime));
        }

        synchronized protected String dump() {
            if (!active)
                return null;

            total += items.size();
            StringBuilder sb = new StringBuilder();
            sb.append("[Total: ").append(total).append("]");
            if (!items.isEmpty()) {
                List<Long> latenciesEntrToRecv = new ArrayList<>();
                List<Long> latenciesRecvToDecd = new ArrayList<>();

                for (FineStatisticsItem item : items) {
                    latenciesEntrToRecv.add(item.recvTime - item.entrTime);
                    latenciesRecvToDecd.add(item.decdTime - item.recvTime);
                }

                Collections.sort(latenciesEntrToRecv);
                Collections.sort(latenciesRecvToDecd);

                sb.append("[Last: ").append(items.size()).append("]");
                sb.append("[MinL: ").append(String.format("%d", latenciesEntrToRecv.get(0)))
                        .append("|").append(String.format("%d", latenciesRecvToDecd.get(0))).append("]");
                sb.append("[MedL: ").append(String.format("%d", latenciesEntrToRecv.get(latenciesEntrToRecv.size() / 2)))
                        .append("|").append(String.format("%d", latenciesRecvToDecd.get(latenciesRecvToDecd.size() / 2))).append("]");
                sb.append("[MaxL: ").append(String.format("%d", latenciesEntrToRecv.get(latenciesEntrToRecv.size() - 1)))
                        .append("|").append(String.format("%d", latenciesRecvToDecd.get(latenciesRecvToDecd.size() - 1))).append("]");

                if (writer != null)
                    dumpToFile();

            }
            items.clear();
            return sb.toString();
        }

        synchronized protected void dumpToFile() {
            if (writer == null)
                return;

            try {
                writer.write(items.size());
                writer.newLine();

                for (FineStatisticsItem item : items) {
                    writer.write(new StringBuilder()
                            .append(item.entrTime).append(";")
                            .append(item.recvTime).append(";")
                            .append(item.decdTime).append(";")
                            .append(item.recvTime - item.entrTime).append(";")
                            .append(item.decdTime - item.recvTime).toString());

                    writer.newLine();
                }
                writer.flush();
            } catch (IOException e) {
                System.err.println("FAILED TO WRITE TO FILE " + e.toString());
            }
        }
    }

    private FineStatistics stats = new FineStatistics();

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

    private AtomicBoolean running = new AtomicBoolean(false);

    private Logger logger;

    private String level;

    private ThreadLocal<Long> inTimestamp;

    public MessageReader(ConnectionId connectionId, ConfigurationManager configurationManager, MarketDataManager marketDataManager, InstrumentManager instumentManager) {
        this.connectionId = connectionId;
        this.asynch = configurationManager.isAsynchChannelReader();
        this.connection = configurationManager.getConnection(connectionId);
        this.intf = this.connectionId.isPrimary() ? configurationManager.getPrimaryNetworkInterface() : configurationManager.getSecondaryNetworkInterface();
        this.fastTemplatesFile = configurationManager.getFastTemplatesFile();

        this.marketDataManager = marketDataManager;
        this.instrumentManager = instumentManager;

        this.logger = LoggerFactory.getLogger(connectionId.getConnectionId() + "-MessageReader");

        if (logger.isDebugEnabled())
            logger.debug("Created [Connection: " + connectionId.getConnectionId() + "]");
    }

    public DatagramChannel openChannel() throws IOException {
        return DatagramChannel.open(StandardProtocolFamily.INET)
                .setOption(StandardSocketOptions.SO_REUSEADDR, true)
                .bind(new InetSocketAddress(connection.getPort()));
    }

    public NetworkInterface getNetworkInterface(String name) throws SocketException {
        return NetworkInterface.getByName(name);
    }

    public void init(String level) throws IOException {
        this.level = level;
    }

    public void create() throws IOException {
        channel = openChannel();
        channel.setOption(StandardSocketOptions.SO_RCVBUF, 1000000);
        channel.setOption(StandardSocketOptions.IP_MULTICAST_IF, getNetworkInterface(intf));

        if (logger.isDebugEnabled())
            logger.debug("Opened channel on [Port: " + connection.getPort() + "]");
    }

    public void destroy() throws IOException {
        channel.disconnect();
        channel.close();
        channel = null;
    }

    public void start() {
        logger.info("START " + toString());

        if (running.getAndSet(true)) {
            if (logger.isDebugEnabled())
                logger.debug("Already STARTED");

            return;
        }

        try {
            create();
            connect();
            run();
        } catch (IOException e) {
            Utils.printStackTrace(e, logger, "IOException occurred while starting..");
            running.set(false);
        }
    }

    public void stop() {
        logger.info("STOP " + toString());

        if (!running.getAndSet(false)) {
            if (logger.isDebugEnabled())
                logger.debug("Already STOPPED");

            return;
        }

        try {
            disconnect();
            destroy();
        } catch (IOException e) {
            Utils.printStackTrace(e, logger, "IOException occurred while starting..");
        }

        if (logger.isDebugEnabled())
            logger.debug("STOPPED");
    }

    public boolean isRunning() {
        return running.get();
    }

    private void connect() throws IOException {
        membership = channel.join(InetAddress.getByName(connection.getIp()),
                NetworkInterface.getByName(intf),
                InetAddress.getByName(connection.getSource()));

        if (logger.isDebugEnabled())
            logger.debug("Joined [Group: " + connection.getIp() + "(" + InetAddress.getByName(connection.getIp()) + ")]" +
                                "[Interface: " + intf + "(" + getNetworkInterface(intf).toString() + ")]" +
                                "[Source: " + connection.getSource() + "(" + InetAddress.getByName(connection.getSource()) + ")]" +
                                "[Key: " + membership.toString() + "]");

        multicastInputStream = new MicexFastMulticastInputStream(this, channel, logger, asynch, connectionId);
        messageReader = new MessageInputStream(multicastInputStream);

        for (MessageTemplate template : new XMLMessageTemplateLoader()
                .load(new FileInputStream(fastTemplatesFile)))
            messageReader.registerTemplate(Integer.valueOf(template.getId()), template);

        if (instrumentManager == null && marketDataManager == null) {
            registerMessageHandler(new MessageHandler() {
                @Override
                public void handleMessage(Message readMessage, Context context, Coder coder) {
                    long decodedTimeInTodayMicros = Utils.currentTimeInTodayMicros();
                    // long sendingTimeInTodayMicros = Utils.convertTodayToTodayMicros(readMessage.getLong("SendingTime") % (1000L * 100L * 100L * 100L));
                    long receivedTimeInTodayMicros = Utils.convertTicksToTodayMicros(inTimestamp.get());

                    //stats.addValueReceivedToDecoded(receivedTimeInTodayMicros - decodedTimeInTodayMicros);

                    if (readMessage.getString("MessageType").equals("X")) {
                        SequenceValue mdEntries = readMessage.getSequence("GroupMDEntries");
                        for (int i = 0; i < mdEntries.getLength(); ++i) {
                            long entryTimeInTodayMicros = Utils.getEntryTimeInTodayMicros(mdEntries.get(i));

                            stats.addItem(entryTimeInTodayMicros, receivedTimeInTodayMicros, decodedTimeInTodayMicros);
//                            stats.addValueEntryToSending(sendingTimeInTodayMicros - entryTimeInTodayMicros);
//                            stats.addValueEntryToReceived(receivedTimeInTodayMicros - entryTimeInTodayMicros);
                        }
                    }
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
                    marketDataManager.setIncrementalProcessorIsPrimary(MessageHandlerType.ORDER_LIST, true);
                case CURR_ORDER_LIST_INCR_B:
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("X-OLR-CURR"), marketDataManager.getIncrementalProcessor(MessageHandlerType.ORDER_LIST));
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("0"), marketDataManager.getHeartbeatProcessor());
                    multicastInputStream.setInTimestamp(inTimestamp = marketDataManager.getIncrementalProcessorInTimestamp(MessageHandlerType.ORDER_LIST));
                    break;

                case FOND_ORDER_LIST_INCR_A:
                    marketDataManager.setIncrementalProcessorIsPrimary(MessageHandlerType.ORDER_LIST, true);
                case FOND_ORDER_LIST_INCR_B:
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("X-OLR-FOND"), marketDataManager.getIncrementalProcessor(MessageHandlerType.ORDER_LIST));
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("0"), marketDataManager.getHeartbeatProcessor());
                    multicastInputStream.setInTimestamp(inTimestamp = marketDataManager.getIncrementalProcessorInTimestamp(MessageHandlerType.ORDER_LIST));
                    break;

                case CURR_ORDER_LIST_SNAP_A:
                case CURR_ORDER_LIST_SNAP_B:
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("W-OLS-CURR"), marketDataManager.getSnapshotProcessor(MessageHandlerType.ORDER_LIST));
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("0"), marketDataManager.getHeartbeatProcessor());
                    multicastInputStream.setInTimestamp(inTimestamp = marketDataManager.getSnapshotProcessorInTimestamp(MessageHandlerType.ORDER_LIST));
                    break;

                case FOND_ORDER_LIST_SNAP_A:
                case FOND_ORDER_LIST_SNAP_B:
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("W-OLS-FOND"), marketDataManager.getSnapshotProcessor(MessageHandlerType.ORDER_LIST));
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("0"), marketDataManager.getHeartbeatProcessor());
                    multicastInputStream.setInTimestamp(inTimestamp = marketDataManager.getSnapshotProcessorInTimestamp(MessageHandlerType.ORDER_LIST));
                    break;

                case CURR_STATISTICS_INCR_A:
                    marketDataManager.setIncrementalProcessorIsPrimary(MessageHandlerType.STATISTICS, true);
                case CURR_STATISTICS_INCR_B:
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("X-MSR-CURR"), marketDataManager.getIncrementalProcessor(MessageHandlerType.STATISTICS));
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("0"), marketDataManager.getHeartbeatProcessor());
                    multicastInputStream.setInTimestamp(inTimestamp = marketDataManager.getIncrementalProcessorInTimestamp(MessageHandlerType.STATISTICS));
                    break;

                case FOND_STATISTICS_INCR_A:
                    marketDataManager.setIncrementalProcessorIsPrimary(MessageHandlerType.STATISTICS, true);
                case FOND_STATISTICS_INCR_B:
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("X-MSR-FOND"), marketDataManager.getIncrementalProcessor(MessageHandlerType.STATISTICS));
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("0"), marketDataManager.getHeartbeatProcessor());
                    multicastInputStream.setInTimestamp(inTimestamp = marketDataManager.getIncrementalProcessorInTimestamp(MessageHandlerType.STATISTICS));
                    break;

                case CURR_STATISTICS_SNAP_A:
                case FOND_STATISTICS_SNAP_A:
                case CURR_STATISTICS_SNAP_B:
                case FOND_STATISTICS_SNAP_B:
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("W-Generic"), marketDataManager.getSnapshotProcessor(MessageHandlerType.STATISTICS));
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("0"), marketDataManager.getHeartbeatProcessor());
                    multicastInputStream.setInTimestamp(inTimestamp = marketDataManager.getSnapshotProcessorInTimestamp(MessageHandlerType.STATISTICS));
                    break;

                case CURR_PUB_TRADES_INCR_A:
                    marketDataManager.setIncrementalProcessorIsPrimary(MessageHandlerType.PUBLIC_TRADES, true);
                case CURR_PUB_TRADES_INCR_B:
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("X-TLR-CURR"), marketDataManager.getIncrementalProcessor(MessageHandlerType.PUBLIC_TRADES));
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("0"), marketDataManager.getHeartbeatProcessor());
                    multicastInputStream.setInTimestamp(inTimestamp = marketDataManager.getIncrementalProcessorInTimestamp(MessageHandlerType.PUBLIC_TRADES));
                    break;

                case FOND_PUB_TRADES_INCR_A:
                    marketDataManager.setIncrementalProcessorIsPrimary(MessageHandlerType.PUBLIC_TRADES, true);
                case FOND_PUB_TRADES_INCR_B:
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("X-TLR-FOND"), marketDataManager.getIncrementalProcessor(MessageHandlerType.PUBLIC_TRADES));
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("0"), marketDataManager.getHeartbeatProcessor());
                    multicastInputStream.setInTimestamp(inTimestamp = marketDataManager.getIncrementalProcessorInTimestamp(MessageHandlerType.PUBLIC_TRADES));
                    break;
            }
        }

        messageReader.setBlockReader(new MicexFastMessageBlockReader(this));

        if ("debug".equals(level))
            enableDebug();

        if ("trace".equals(level))
            enableTrace();
    }

    private void registerMessageHandler(MessageHandler messageHandler) {
        messageReader.addMessageHandler(messageHandler);
    }

    private void disconnect() throws IOException {
        if (multicastInputStream.isRunning())
            multicastInputStream.stop();

        if (membership != null)
            membership.drop();
    }

    @Override
    public void onException(Exception e) {
        if (e instanceof AsynchronousCloseException ||
                e instanceof ClosedByInterruptException ||
                e instanceof InterruptedException) {

        } else if (e instanceof FastException) {
            if (e.getMessage().contains("The end of the input stream has been reached.")) {
                logger.info(e.toString());
            } else {
                Utils.printStackTrace(e, logger, "FastException occurred..");
            }
        } else if (e instanceof IOException) {
            Utils.printStackTrace(e, logger, "IOException occurred..");
        } else {
            Utils.printStackTrace(e, logger, "Exception occurred..");
        }
    }

    private void run() throws IOException {
        Thread.currentThread().setName(connectionId.toString());

        if (logger.isDebugEnabled())
            logger.debug("STARTED");

        if (!multicastInputStream.isRunning())
            multicastInputStream.start();

        while (running.get()) {
            try {
                messageReader.readMessage();
            } catch (Exception e) {
                onException(e);
            }
        }
    }

    public void enableTrace() {
        enableDebug();
        messageReader.getContext().setTraceEnabled(true);
    }

    public void enableDebug() {
        org.apache.log4j.Logger.getLogger(connectionId + "-MessageReader").setLevel(Level.TRACE);
        messageReader.getContext().setLogger(new FastMessageLogger() {
            @Override
            public void log(Message message, byte[] bytes, Direction direction) {
                if (logger.isTraceEnabled())
                    logger.trace("(IN) " + Utils.convertTicksToTodayMicros(inTimestamp.get()) + " " + message.toString());
            }
        });
    }

    @Override
    public String toString() {
        return connectionId.getConnectionId();
    }

    public static void main(final String[] args) throws IOException {
        // org.apache.log4j.Logger.getLogger("MessageReader").setLevel(Level.INFO);
        GatewayManager.addConsoleAppender("%d{HH:mm:ss} %m%n", Level.TRACE);
        GatewayManager.addFileAppender("log/log.mr.out", "%d{HH:mm:ss} %m%n", Level.TRACE);
        if (args.length < 5) {
            System.err.println("Usage MulticastReader <connectionId> <fast_templates> <interface[s]> <connections_file> <debug, trace, stats> [stats_filename]");
            return;
        }

        if (ConnectionId.convert(args[0]) == null) {
            StringBuilder sb = new StringBuilder();
            for (ConnectionId id : ConnectionId.values())
                sb.append(id.getConnectionId()).append(' ');
            System.err.println("Valid connectionIds are: " + sb.toString());
            return;
        }

        final MessageReader mr = new MessageReader(
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
                                return false;
                            }
                        }),
                null, null);

        boolean statistics = false;
        switch (args[4]) {
            case "trace":
            case "debug":
                mr.init(args[4]);
                break;
            case "stats":
                mr.init("info");
                mr.stats.initStatistics();
                if (args.length > 5) {
                    mr.stats.initStatisticsWritingToFile(args[5]);
                }
                statistics = true;
                break;
        }

        if (statistics) {
            Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                   synchronized (mr.stats) {
                        mr.logger.info("" + Utils.currentTimeInTodayMicros());
                        mr.logger.info(mr.stats.dump());
                    }
                }
            }, 1, 1, TimeUnit.SECONDS);
        }

        mr.start();
    }
}
