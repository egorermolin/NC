package ru.ncapital.gateways.moexfast.connection.multicast;

import org.apache.log4j.Level;
import org.openfast.*;
import org.openfast.codec.Coder;
import org.openfast.error.FastException;
import org.openfast.logging.FastMessageLogger;
import org.openfast.template.MessageTemplate;
import org.openfast.template.loader.XMLMessageTemplateLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ncapital.gateways.moexfast.ConfigurationManager;
import ru.ncapital.gateways.moexfast.InstrumentManager;
import ru.ncapital.gateways.moexfast.MarketDataManager;
import ru.ncapital.gateways.moexfast.Utils;
import ru.ncapital.gateways.moexfast.connection.Connection;
import ru.ncapital.gateways.moexfast.connection.ConnectionId;
import ru.ncapital.gateways.moexfast.connection.multicast.quickdecoder.QuickDecoderMessageInputStream;
import ru.ncapital.gateways.moexfast.messagehandlers.IMessageHandler;
import ru.ncapital.gateways.moexfast.messagehandlers.MessageHandlerType;

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
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by egore on 12/8/15.
 */
public class MessageReader implements IMulticastEventListener {

    protected class Statistics {

        private static final int NUMBER_OF_LISTS = 100;

        private class StatisticsItem {
            // ALL TIMES IN TODAY MICROS (micros since midnight)

            private int seqNum;

            private long entrTime;

            private long sendTime;

            private long recvTime;

            private long decdTime;

            private StatisticsItem(int seqNum, long entrTime, long sendTime, long recvTime, long decdTime) {
                this.seqNum = seqNum;
                this.entrTime = entrTime;
                this.sendTime = sendTime;
                this.recvTime = recvTime;
                this.decdTime = decdTime;
            }
        }

        private List<List<StatisticsItem>> allItems = new ArrayList<>();

        private List<StatisticsItem> currentItems;

        private int currentItemsPos = 0;

        private long total = 0;

        private BufferedWriter writer;

        private boolean active = false;

        private Executor executor = Executors.newSingleThreadExecutor();

        public void initStatistics() {
            active = true;
            for (int i = 0; i < NUMBER_OF_LISTS; ++i)
                allItems.add(new ArrayList<StatisticsItem>());
            currentItems = allItems.get(0);
        }

        public void initStatisticsWritingToFile(String filename) {
            if (!active)
                return;

            try {
                writer = new BufferedWriter(new FileWriter(filename, false));
            } catch (IOException e) {
                System.err.println("FAILED TO OPEN FILE " + e.toString());
                writer = null;
            }
        }

        public synchronized void addItem(int seqNum, long entrTime, long sendTime, long recvTime, long decdTime) {
            if (!active)
                return;

            currentItems.add(new StatisticsItem(seqNum, entrTime, sendTime, recvTime, decdTime));
        }

        public void dump() {
            if (active) {
                int pos = currentItemsPos;

                logger.info(pos + " " + Utils.currentTimeInTodayMicros());

                List<StatisticsItem> lastItems = initNextAvaiableItems();

                calculateAndWriteToFile(pos, lastItems);
            }
        }

        private synchronized List<StatisticsItem> initNextAvaiableItems() {
            List<StatisticsItem> itemsToDump = currentItems;

            if (currentItemsPos + 1 == NUMBER_OF_LISTS)
                currentItemsPos = 0;

            currentItems = allItems.get(++currentItemsPos);
            currentItems.clear();

            return itemsToDump;
        }

        private void calculateAndWriteToFile(int pos, List<StatisticsItem> items) {
            if (items.isEmpty())
                return;

            class CalculateAndWriteTask implements Runnable {
                private int pos;

                private List<StatisticsItem> items;

                private CalculateAndWriteTask(int pos, List<StatisticsItem> items) {
                    this.pos = pos;
                    this.items = items;
                }

                @Override
                public void run() {
                    StringBuilder sb = new StringBuilder();
                    List<Long> latenciesEntrToRecv = new ArrayList<>();
                    List<Long> latenciesEntrToSend = new ArrayList<>();

                    for (StatisticsItem item : items) {
                        latenciesEntrToRecv.add(item.recvTime - item.entrTime);
                        latenciesEntrToSend.add(item.sendTime - item.entrTime);
                    }

                    Collections.sort(latenciesEntrToRecv);
                    Collections.sort(latenciesEntrToSend);

                    total += items.size();
                    sb.append("[Total: ").append(total).append("]");
                    sb.append("[Last: ").append(items.size()).append("]");
                    sb.append("[MinL: ").append(String.format("%d", latenciesEntrToRecv.get(0))).append("");
                    sb.append("|").append(String.format("%d", latenciesEntrToSend.get(0))).append("]");
                    sb.append("[MedL: ").append(String.format("%d", latenciesEntrToRecv.get(latenciesEntrToRecv.size() / 2))).append("");
                    sb.append("|").append(String.format("%d", latenciesEntrToSend.get(latenciesEntrToSend.size() / 2))).append("]");
                    sb.append("[MaxL: ").append(String.format("%d", latenciesEntrToRecv.get(latenciesEntrToRecv.size() - 1))).append("");
                    sb.append("|").append(String.format("%d", latenciesEntrToSend.get(latenciesEntrToSend.size() - 1))).append("]");

                    logger.info(pos + " " + Utils.currentTimeInTodayMicros() + " " + sb.toString());

                    if (writer == null)
                        return;

                    try {
                        writer.write(sb.toString());
                        writer.newLine();

                        for (StatisticsItem item : items) {
                            writer.write(new StringBuilder()
                                    .append(item.seqNum).append(";")
                                    .append(item.entrTime).append(";")
                                    .append(item.sendTime).append(";")
                                    .append(item.recvTime).append(";")
                                    .append(item.decdTime).append(";")
                                    .append(item.recvTime - item.entrTime).append(";")
                                    .append(item.sendTime - item.entrTime).append(";").toString());

                            writer.newLine();
                        }
                        writer.flush();
                    } catch (IOException e) {
                        System.err.println("FAILED TO WRITE TO FILE " + e.toString());
                    }
                }
            }

            executor.execute(new CalculateAndWriteTask(pos, items));
        }
    }

    protected Statistics stats = new Statistics();

    private Connection connection;

    private ConnectionId connectionId;

    private String intf;

    private DatagramChannel channel;

    private MembershipKey membership;

    private MessageInputStream messageReader;

    private MoexFastMulticastInputStream multicastInputStream;

    private String fastTemplatesFile;

    private MarketDataManager marketDataManager;

    private InstrumentManager instrumentManager;

    private final boolean asynch;

    private AtomicBoolean running = new AtomicBoolean(false);

    private Logger logger;

    private String level;

    private ThreadLocal<Long> inTimestamp;

    private volatile long receivedTimestamp;

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
        receivedTimestamp = Utils.currentTimeInTicks();

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

    public long getLastReceivedTimestamp() {
        return receivedTimestamp;
    }

    public ThreadLocal<Long> initAndGetInTimestamp(ThreadLocal<Long> inTimestamp) {
        if (inTimestamp == null)
            return this.inTimestamp = new ThreadLocal<Long>() {
                @Override
                public Long initialValue() {
                    return new Long(0);
                }
            };
        else
            return this.inTimestamp = inTimestamp;
    }

    public ConnectionId getConnectionId() {
        return connectionId;
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

        multicastInputStream = new MoexFastMulticastInputStream(this, channel, logger, asynch, connectionId);
        messageReader = new MessageInputStream(multicastInputStream);

        for (MessageTemplate template : new XMLMessageTemplateLoader().load(new FileInputStream(fastTemplatesFile)))
            messageReader.registerTemplate(Integer.valueOf(template.getId()), template);

        if (instrumentManager == null && marketDataManager == null) {
            registerMessageHandler(new MessageHandler()
            {
                @Override
                public void handleMessage(Message readMessage, Context context, Coder coder) {
                    long decodedTimeInTodayMicros = Utils.currentTimeInTodayMicros();
                    long receivedTimeInTodayMicros = Utils.convertTicksToTodayMicros(inTimestamp.get());
                    long sendingTimeInTodayMicros = Utils.convertTodayToTodayMicros((readMessage.getLong("SendingTime") % 1_00_00_00_000L) * 1_000L);

                    if (readMessage.getString("MessageType").equals("X")) {
                        SequenceValue mdEntries = readMessage.getSequence("GroupMDEntries");
                        for (int i = 0; i < mdEntries.getLength(); ++i) {
                            long entryTimeInTodayMicros = Utils.getEntryTimeInTodayMicros(mdEntries.get(i));

                            stats.addItem(readMessage.getInt("MsgSeqNum"),
                                    entryTimeInTodayMicros, sendingTimeInTodayMicros,
                                    receivedTimeInTodayMicros, decodedTimeInTodayMicros
                            );
                        }
                    }
                }
            });
            multicastInputStream.setInTimestamp(initAndGetInTimestamp(null));
        } else {
            switch (connectionId) {
                case CURR_INSTRUMENT_INCR_A:
                case CURR_INSTRUMENT_INCR_B:
                case FOND_INSTRUMENT_INCR_A:
                case FOND_INSTRUMENT_INCR_B:
                case FUT_INSTRUMENT_INCR_A:
                case FUT_INSTRUMENT_INCR_B:
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("f"), instrumentManager);
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("0"), marketDataManager.getHeartbeatProcessor());
                    multicastInputStream.setInTimestamp(initAndGetInTimestamp(null));
                    break;

                case CURR_INSTRUMENT_SNAP_A:
                case CURR_INSTRUMENT_SNAP_B:
                case FOND_INSTRUMENT_SNAP_A:
                case FOND_INSTRUMENT_SNAP_B:
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("d"), instrumentManager);
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("0"), marketDataManager.getHeartbeatProcessor());
                    multicastInputStream.setInTimestamp(initAndGetInTimestamp(null));
                    break;

                case FUT_INSTRUMENT_SNAP_A:
                case FUT_INSTRUMENT_SNAP_B:
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("d"), instrumentManager);
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("4"), instrumentManager);
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("0"), marketDataManager.getHeartbeatProcessor());
                    multicastInputStream.setInTimestamp(initAndGetInTimestamp(null));
                    break;

                case CURR_ORDER_LIST_INCR_A:
                    marketDataManager.setIncrementalProcessorIsPrimary(MessageHandlerType.ORDER_LIST, true);
                case CURR_ORDER_LIST_INCR_B:
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("X-OLR-CURR"), marketDataManager.getIncrementalProcessor(MessageHandlerType.ORDER_LIST));
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("0"), marketDataManager.getHeartbeatProcessor());
                    multicastInputStream.setInTimestamp(initAndGetInTimestamp(marketDataManager.getIncrementalProcessorInTimestamp(MessageHandlerType.ORDER_LIST)));
                    break;

                case FOND_ORDER_LIST_INCR_A:
                    marketDataManager.setIncrementalProcessorIsPrimary(MessageHandlerType.ORDER_LIST, true);
                case FOND_ORDER_LIST_INCR_B:
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("X-OLR-FOND"), marketDataManager.getIncrementalProcessor(MessageHandlerType.ORDER_LIST));
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("0"), marketDataManager.getHeartbeatProcessor());
                    multicastInputStream.setInTimestamp(initAndGetInTimestamp(marketDataManager.getIncrementalProcessorInTimestamp(MessageHandlerType.ORDER_LIST)));
                    break;

                case CURR_ORDER_LIST_SNAP_A:
                case CURR_ORDER_LIST_SNAP_B:
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("W-OLS-CURR"), marketDataManager.getSnapshotProcessor(MessageHandlerType.ORDER_LIST));
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("0"), marketDataManager.getHeartbeatProcessor());
                    multicastInputStream.setInTimestamp(initAndGetInTimestamp(marketDataManager.getSnapshotProcessorInTimestamp(MessageHandlerType.ORDER_LIST)));
                    break;

                case FOND_ORDER_LIST_SNAP_A:
                case FOND_ORDER_LIST_SNAP_B:
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("W-OLS-FOND"), marketDataManager.getSnapshotProcessor(MessageHandlerType.ORDER_LIST));
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("0"), marketDataManager.getHeartbeatProcessor());
                    multicastInputStream.setInTimestamp(initAndGetInTimestamp(marketDataManager.getSnapshotProcessorInTimestamp(MessageHandlerType.ORDER_LIST)));
                    break;

                case CURR_STATISTICS_INCR_A:
                    marketDataManager.setIncrementalProcessorIsPrimary(MessageHandlerType.STATISTICS, true);
                case CURR_STATISTICS_INCR_B:
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("X-MSR-CURR"), marketDataManager.getIncrementalProcessor(MessageHandlerType.STATISTICS));
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("0"), marketDataManager.getHeartbeatProcessor());
                    multicastInputStream.setInTimestamp(initAndGetInTimestamp(marketDataManager.getIncrementalProcessorInTimestamp(MessageHandlerType.STATISTICS)));
                    break;

                case FOND_STATISTICS_INCR_A:
                    marketDataManager.setIncrementalProcessorIsPrimary(MessageHandlerType.STATISTICS, true);
                case FOND_STATISTICS_INCR_B:
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("X-MSR-FOND"), marketDataManager.getIncrementalProcessor(MessageHandlerType.STATISTICS));
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("0"), marketDataManager.getHeartbeatProcessor());
                    multicastInputStream.setInTimestamp(initAndGetInTimestamp(marketDataManager.getIncrementalProcessorInTimestamp(MessageHandlerType.STATISTICS)));
                    break;

                case CURR_STATISTICS_SNAP_A:
                case CURR_STATISTICS_SNAP_B:
                case FOND_STATISTICS_SNAP_A:
                case FOND_STATISTICS_SNAP_B:
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("W-Generic"), marketDataManager.getSnapshotProcessor(MessageHandlerType.STATISTICS));
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("0"), marketDataManager.getHeartbeatProcessor());
                    multicastInputStream.setInTimestamp(initAndGetInTimestamp(marketDataManager.getSnapshotProcessorInTimestamp(MessageHandlerType.STATISTICS)));
                    break;

                case FUT_STATISTICS_SNAP_A:
                case FUT_STATISTICS_SNAP_B:
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("DefaultSnapshotMessage"), marketDataManager.getSnapshotProcessor(MessageHandlerType.STATISTICS));
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("0"), marketDataManager.getHeartbeatProcessor());
                    multicastInputStream.setInTimestamp(initAndGetInTimestamp(marketDataManager.getSnapshotProcessorInTimestamp(MessageHandlerType.STATISTICS)));
                    break;

                case CURR_PUB_TRADES_INCR_A:
                    marketDataManager.setIncrementalProcessorIsPrimary(MessageHandlerType.PUBLIC_TRADES, true);
                case CURR_PUB_TRADES_INCR_B:
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("X-TLR-CURR"), marketDataManager.getIncrementalProcessor(MessageHandlerType.PUBLIC_TRADES));
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("0"), marketDataManager.getHeartbeatProcessor());
                    multicastInputStream.setInTimestamp(initAndGetInTimestamp(marketDataManager.getIncrementalProcessorInTimestamp(MessageHandlerType.PUBLIC_TRADES)));
                    break;

                case FOND_PUB_TRADES_INCR_A:
                    marketDataManager.setIncrementalProcessorIsPrimary(MessageHandlerType.PUBLIC_TRADES, true);
                case FOND_PUB_TRADES_INCR_B:
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("X-TLR-FOND"), marketDataManager.getIncrementalProcessor(MessageHandlerType.PUBLIC_TRADES));
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("0"), marketDataManager.getHeartbeatProcessor());
                    multicastInputStream.setInTimestamp(initAndGetInTimestamp(marketDataManager.getIncrementalProcessorInTimestamp(MessageHandlerType.PUBLIC_TRADES)));
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
                receivedTimestamp = inTimestamp.get();
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

}
