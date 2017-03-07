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
import ru.ncapital.gateways.moexfast.connection.messageprocessors.IProcessor;
import ru.ncapital.gateways.moexfast.messagehandlers.MessageHandlerType;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.*;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.MembershipKey;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by egore on 12/8/15.
 */
public class MessageReader implements IMulticastEventListener {

    protected MessageReaderStatistics statistics;

    private IProcessor processor;

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
        this.logger = LoggerFactory.getLogger(connectionId.getConnectionId() + "-MessageReader");
        this.statistics = new MessageReaderStatistics(this.logger);

        if (logger.isDebugEnabled())
            logger.debug("Created [Connection: " + connectionId.getConnectionId() + "]");

        boolean fortsOrderList = false;
        switch (this.connectionId) {
            case FUT_ORDER_LIST_INCR_A:
            case FUT_ORDER_LIST_SNAP_A:
            case FUT_ORDER_LIST_INCR_B:
            case FUT_ORDER_LIST_SNAP_B:
                fortsOrderList = true;
                break;
        }

        if (connectionId.isPrimary())
            this.intf = configurationManager.getPrimaryNetworkInterface(fortsOrderList);
        else
            this.intf = configurationManager.getSecondaryNetworkInterface(fortsOrderList);

        if (this.intf == null)
            return;

        this.fastTemplatesFile = configurationManager.getFastTemplatesFile();

        this.marketDataManager = marketDataManager;
        this.instrumentManager = instumentManager;
    }

    private void setProcessor(IProcessor processor) {
        this.processor = processor;
    }

    public IProcessor getProcessor() {
        return this.processor;
    }

    public DatagramChannel openChannel() throws IOException {
        if (connection == null)
            throw new RuntimeException("Connection " + connectionId.getConnectionId() + " is not created");

        return DatagramChannel.open(StandardProtocolFamily.INET)
                .setOption(StandardSocketOptions.SO_REUSEADDR, true)
                .bind(new InetSocketAddress(connection.getPort()));
    }

    public NetworkInterface getNetworkInterface(String name) throws SocketException {
        return ConfigurationManager.convertNetworkInterface(name);
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
        if (intf == null) {
            logger.info("Network interface is not configured for " + connectionId.toString());
            return;
        }

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
                getNetworkInterface(intf),
                InetAddress.getByName(connection.getSource()));

        if (logger.isDebugEnabled())
            logger.debug("Joined [Group: " + connection.getIp() + "(" + InetAddress.getByName(connection.getIp()) + ")]" +
                    "[Interface: " + intf + "(" + getNetworkInterface(intf).toString() + ")]" +
                    "[Source: " + connection.getSource() + "(" + InetAddress.getByName(connection.getSource()) + ")]" +
                    "[Key: " + membership.toString() + "]");

        multicastInputStream = new MoexFastMulticastInputStream(this, channel, logger, asynch, connectionId);
        messageReader = new MessageInputStream(multicastInputStream);

        Map<String, MessageTemplate> messageTemplates = new HashMap<>();
        for (MessageTemplate template : new XMLMessageTemplateLoader().load(new FileInputStream(fastTemplatesFile)))
            messageTemplates.put(template.getName(), template);

        switch (connectionId) {
            case FUT_INSTRUMENT_SNAP_A:
            case FUT_INSTRUMENT_SNAP_B:
                for (String name : new String[]{"SecurityDefinition", "SequenceReset", "Heartbeat"}) {
                    MessageTemplate template = messageTemplates.get(name);
                    messageReader.registerTemplate(Integer.valueOf(template.getId()), template);
                }
                break;
            case FUT_INSTRUMENT_INCR_A:
            case FUT_INSTRUMENT_INCR_B:
                for (String name : new String[]{"SecurityStatus", "SequenceReset", "Heartbeat"}) {
                    MessageTemplate template = messageTemplates.get(name);
                    messageReader.registerTemplate(Integer.valueOf(template.getId()), template);
                }
                break;
            case FUT_ORDER_LIST_SNAP_A:
            case FUT_ORDER_LIST_SNAP_B:
                for (String name : new String[]{"OrdersBook", "TradingSessionStatus", "SequenceReset", "Heartbeat"}) {
                    MessageTemplate template = messageTemplates.get(name);
                    messageReader.registerTemplate(Integer.valueOf(template.getId()), template);
                }
                break;
            case FUT_ORDER_LIST_INCR_A:
            case FUT_ORDER_LIST_INCR_B:
                for (String name : new String[]{"OrdersLog", "TradingSessionStatus", "SequenceReset", "Heartbeat"}) {
                    MessageTemplate template = messageTemplates.get(name);
                    messageReader.registerTemplate(Integer.valueOf(template.getId()), template);
                }
                break;
            case FUT_ORDER_BOOK_SNAP_A:
            case FUT_ORDER_BOOK_SNAP_B:
            case FUT_STATISTICS_SNAP_A:
            case FUT_STATISTICS_SNAP_B:
                for (String name : new String[]{"DefaultSnapshotMessage", "TradingSessionStatus", "SequenceReset", "Heartbeat"}) {
                    MessageTemplate template = messageTemplates.get(name);
                    messageReader.registerTemplate(Integer.valueOf(template.getId()), template);
                }
                break;
            case FUT_ORDER_BOOK_INCR_A:
            case FUT_ORDER_BOOK_INCR_B:
            case FUT_STATISTICS_INCR_A:
            case FUT_STATISTICS_INCR_B:
                for (String name : new String[]{"DefaultIncrementalRefreshMessage", "TradingSessionStatus", "SequenceReset", "Heartbeat"}) {
                    MessageTemplate template = messageTemplates.get(name);
                    messageReader.registerTemplate(Integer.valueOf(template.getId()), template);
                }
                break;
            case FUT_NEWS_INCR_A:
            case FUT_NEWS_INCR_B:
                for (String name : new String[]{"News", "SequenceReset", "Heartbeat"}) {
                    MessageTemplate template = messageTemplates.get(name);
                    messageReader.registerTemplate(Integer.valueOf(template.getId()), template);
                }
                break;
            default:
                for (MessageTemplate template : messageTemplates.values())
                    messageReader.registerTemplate(Integer.valueOf(template.getId()), template);

                break;
        }

        if (instrumentManager == null && marketDataManager == null) {
            if (statistics.isActive()) {
                registerMessageHandler(new MessageHandler() {
                    @Override
                    public void handleMessage(Message readMessage, Context context, Coder coder) {
                        long decodedTimeInTodayMicros = Utils.currentTimeInTodayMicros();
                        long receivedTimeInTodayMicros = Utils.convertTicksToTodayMicros(inTimestamp.get());
                        long sendingTimeInTodayMicros = Utils.convertTodayToTodayMicros(readMessage.getLong("SendingTime") % 1_00_00_00_000_000L);

                        if (readMessage.getString("MessageType").equals("X")) {
                            SequenceValue mdEntries = readMessage.getSequence("GroupMDEntries");
                            for (int i = 0; i < mdEntries.getLength(); ++i) {
                                long entryTimeInTodayMicros = Utils.getEntryTimeInTodayMicros(mdEntries.get(i), Utils.SecondFractionFactor.NANOSECONDS);

                                statistics.addItem(readMessage.getInt("MsgSeqNum"),
                                        entryTimeInTodayMicros, sendingTimeInTodayMicros,
                                        receivedTimeInTodayMicros, decodedTimeInTodayMicros
                                );
                            }
                        }
                    }
                });
            }
            multicastInputStream.setInTimestamp(initAndGetInTimestamp(null));
        } else {
            switch (connectionId) {
                // ========= //
                case CURR_INSTRUMENT_INCR_A:
                case CURR_INSTRUMENT_INCR_B:
                case FOND_INSTRUMENT_INCR_A:
                case FOND_INSTRUMENT_INCR_B:
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("f"), instrumentManager);
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("0"), marketDataManager.getHeartbeatProcessor());
                    multicastInputStream.setInTimestamp(initAndGetInTimestamp(null));
                    setProcessor(instrumentManager);
                    break;
                case FUT_INSTRUMENT_INCR_A:
                case FUT_INSTRUMENT_INCR_B:
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("SecurityStatus"), instrumentManager);
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("SequenceReset"), instrumentManager);
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("Heartbeat"), marketDataManager.getHeartbeatProcessor());
                    multicastInputStream.setInTimestamp(initAndGetInTimestamp(null));
                    setProcessor(instrumentManager);
                    break;

                // ========= //
                case CURR_INSTRUMENT_SNAP_A:
                case CURR_INSTRUMENT_SNAP_B:
                case FOND_INSTRUMENT_SNAP_A:
                case FOND_INSTRUMENT_SNAP_B:
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("d"), instrumentManager);
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("0"), marketDataManager.getHeartbeatProcessor());
                    multicastInputStream.setInTimestamp(initAndGetInTimestamp(null));
                    setProcessor(instrumentManager);
                    break;
                case FUT_INSTRUMENT_SNAP_A:
                case FUT_INSTRUMENT_SNAP_B:
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("SecurityDefinition"), instrumentManager);
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("SequenceReset"), instrumentManager);
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("Heartbeat"), marketDataManager.getHeartbeatProcessor());
                    multicastInputStream.setInTimestamp(initAndGetInTimestamp(null));
                    setProcessor(instrumentManager);
                    break;

                // ========= //
                case CURR_ORDER_LIST_INCR_A:
                    marketDataManager.setIncrementalProcessorIsPrimary(MessageHandlerType.ORDER_LIST, true);
                case CURR_ORDER_LIST_INCR_B:
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("X-OLR-CURR"), marketDataManager.getIncrementalProcessor(MessageHandlerType.ORDER_LIST));
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("0"), marketDataManager.getHeartbeatProcessor());
                    multicastInputStream.setInTimestamp(initAndGetInTimestamp(marketDataManager.getIncrementalProcessorInTimestamp(MessageHandlerType.ORDER_LIST)));
                    setProcessor(marketDataManager.getIncrementalProcessor(MessageHandlerType.ORDER_LIST));
                    break;
                case FOND_ORDER_LIST_INCR_A:
                    marketDataManager.setIncrementalProcessorIsPrimary(MessageHandlerType.ORDER_LIST, true);
                case FOND_ORDER_LIST_INCR_B:
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("X-OLR-FOND"), marketDataManager.getIncrementalProcessor(MessageHandlerType.ORDER_LIST));
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("0"), marketDataManager.getHeartbeatProcessor());
                    multicastInputStream.setInTimestamp(initAndGetInTimestamp(marketDataManager.getIncrementalProcessorInTimestamp(MessageHandlerType.ORDER_LIST)));
                    setProcessor(marketDataManager.getIncrementalProcessor(MessageHandlerType.ORDER_LIST));
                    break;
                case FUT_ORDER_LIST_INCR_A:
                    marketDataManager.setIncrementalProcessorIsPrimary(MessageHandlerType.ORDER_LIST, true);
                case FUT_ORDER_LIST_INCR_B:
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("OrdersLog"), marketDataManager.getIncrementalProcessor(MessageHandlerType.ORDER_LIST));
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("SequenceReset"), marketDataManager.getIncrementalProcessor(MessageHandlerType.ORDER_LIST));
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("Heartbeat"), marketDataManager.getHeartbeatProcessor());
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("TradingSessionStatus"), marketDataManager.getIncrementalProcessor(MessageHandlerType.ORDER_LIST));
                    multicastInputStream.setInTimestamp(initAndGetInTimestamp(marketDataManager.getIncrementalProcessorInTimestamp(MessageHandlerType.ORDER_LIST)));
                    setProcessor(marketDataManager.getIncrementalProcessor(MessageHandlerType.ORDER_LIST));
                    break;

                // ========= //
                case CURR_ORDER_LIST_SNAP_A:
                case CURR_ORDER_LIST_SNAP_B:
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("W-OLS-CURR"), marketDataManager.getSnapshotProcessor(MessageHandlerType.ORDER_LIST));
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("0"), marketDataManager.getHeartbeatProcessor());
                    multicastInputStream.setInTimestamp(initAndGetInTimestamp(marketDataManager.getSnapshotProcessorInTimestamp(MessageHandlerType.ORDER_LIST)));
                    setProcessor(marketDataManager.getSnapshotProcessor(MessageHandlerType.ORDER_LIST));
                    break;
                case FOND_ORDER_LIST_SNAP_A:
                case FOND_ORDER_LIST_SNAP_B:
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("W-OLS-FOND"), marketDataManager.getSnapshotProcessor(MessageHandlerType.ORDER_LIST));
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("0"), marketDataManager.getHeartbeatProcessor());
                    multicastInputStream.setInTimestamp(initAndGetInTimestamp(marketDataManager.getSnapshotProcessorInTimestamp(MessageHandlerType.ORDER_LIST)));
                    setProcessor(marketDataManager.getSnapshotProcessor(MessageHandlerType.ORDER_LIST));
                    break;
                case FUT_ORDER_LIST_SNAP_A:
                case FUT_ORDER_LIST_SNAP_B:
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("OrdersBook"), marketDataManager.getSnapshotProcessor(MessageHandlerType.ORDER_LIST));
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("SequenceReset"), marketDataManager.getSnapshotProcessor(MessageHandlerType.ORDER_LIST));
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("Heartbeat"), marketDataManager.getHeartbeatProcessor());
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("TradingSessionStatus"), marketDataManager.getSnapshotProcessor(MessageHandlerType.ORDER_LIST));
                    multicastInputStream.setInTimestamp(initAndGetInTimestamp(marketDataManager.getSnapshotProcessorInTimestamp(MessageHandlerType.ORDER_LIST)));
                    setProcessor(marketDataManager.getSnapshotProcessor(MessageHandlerType.ORDER_LIST));
                    break;

                // ========= //
                case CURR_STATISTICS_INCR_A:
                    marketDataManager.setIncrementalProcessorIsPrimary(MessageHandlerType.STATISTICS, true);
                case CURR_STATISTICS_INCR_B:
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("X-MSR-CURR"), marketDataManager.getIncrementalProcessor(MessageHandlerType.STATISTICS));
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("0"), marketDataManager.getHeartbeatProcessor());
                    multicastInputStream.setInTimestamp(initAndGetInTimestamp(marketDataManager.getIncrementalProcessorInTimestamp(MessageHandlerType.STATISTICS)));
                    setProcessor(marketDataManager.getIncrementalProcessor(MessageHandlerType.STATISTICS));
                    break;
                case FOND_STATISTICS_INCR_A:
                    marketDataManager.setIncrementalProcessorIsPrimary(MessageHandlerType.STATISTICS, true);
                case FOND_STATISTICS_INCR_B:
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("X-MSR-FOND"), marketDataManager.getIncrementalProcessor(MessageHandlerType.STATISTICS));
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("0"), marketDataManager.getHeartbeatProcessor());
                    multicastInputStream.setInTimestamp(initAndGetInTimestamp(marketDataManager.getIncrementalProcessorInTimestamp(MessageHandlerType.STATISTICS)));
                    setProcessor(marketDataManager.getIncrementalProcessor(MessageHandlerType.STATISTICS));
                    break;
                case FUT_STATISTICS_INCR_A:
                    marketDataManager.setIncrementalProcessorIsPrimary(MessageHandlerType.STATISTICS, true);
                case FUT_STATISTICS_INCR_B:
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("DefaultIncrementalRefreshMessage"), marketDataManager.getIncrementalProcessor(MessageHandlerType.STATISTICS));
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("SequenceReset"), marketDataManager.getIncrementalProcessor(MessageHandlerType.STATISTICS));
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("Heartbeat"), marketDataManager.getHeartbeatProcessor());
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("TradingSessionStatus"), marketDataManager.getIncrementalProcessor(MessageHandlerType.STATISTICS));
                    multicastInputStream.setInTimestamp(initAndGetInTimestamp(marketDataManager.getIncrementalProcessorInTimestamp(MessageHandlerType.STATISTICS)));
                    setProcessor(marketDataManager.getIncrementalProcessor(MessageHandlerType.STATISTICS));
                    break;

                // ========= //
                case CURR_STATISTICS_SNAP_A:
                case CURR_STATISTICS_SNAP_B:
                case FOND_STATISTICS_SNAP_A:
                case FOND_STATISTICS_SNAP_B:
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("W-Generic"), marketDataManager.getSnapshotProcessor(MessageHandlerType.STATISTICS));
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("0"), marketDataManager.getHeartbeatProcessor());
                    multicastInputStream.setInTimestamp(initAndGetInTimestamp(marketDataManager.getSnapshotProcessorInTimestamp(MessageHandlerType.STATISTICS)));
                    setProcessor(marketDataManager.getSnapshotProcessor(MessageHandlerType.STATISTICS));
                    break;
                case FUT_STATISTICS_SNAP_A:
                case FUT_STATISTICS_SNAP_B:
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("DefaultSnapshotMessage"), marketDataManager.getSnapshotProcessor(MessageHandlerType.STATISTICS));
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("SequenceReset"), marketDataManager.getSnapshotProcessor(MessageHandlerType.STATISTICS));
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("Heartbeat"), marketDataManager.getHeartbeatProcessor());
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("TradingSessionStatus"), marketDataManager.getSnapshotProcessor(MessageHandlerType.STATISTICS));
                    multicastInputStream.setInTimestamp(initAndGetInTimestamp(marketDataManager.getSnapshotProcessorInTimestamp(MessageHandlerType.STATISTICS)));
                    setProcessor(marketDataManager.getSnapshotProcessor(MessageHandlerType.STATISTICS));
                    break;

                // ========= //
                case CURR_PUB_TRADES_INCR_A:
                    marketDataManager.setIncrementalProcessorIsPrimary(MessageHandlerType.PUBLIC_TRADES, true);
                case CURR_PUB_TRADES_INCR_B:
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("X-TLR-CURR"), marketDataManager.getIncrementalProcessor(MessageHandlerType.PUBLIC_TRADES));
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("0"), marketDataManager.getHeartbeatProcessor());
                    multicastInputStream.setInTimestamp(initAndGetInTimestamp(marketDataManager.getIncrementalProcessorInTimestamp(MessageHandlerType.PUBLIC_TRADES)));
                    setProcessor(marketDataManager.getIncrementalProcessor(MessageHandlerType.PUBLIC_TRADES));
                    break;
                case FOND_PUB_TRADES_INCR_A:
                    marketDataManager.setIncrementalProcessorIsPrimary(MessageHandlerType.PUBLIC_TRADES, true);
                case FOND_PUB_TRADES_INCR_B:
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("X-TLR-FOND"), marketDataManager.getIncrementalProcessor(MessageHandlerType.PUBLIC_TRADES));
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("0"), marketDataManager.getHeartbeatProcessor());
                    multicastInputStream.setInTimestamp(initAndGetInTimestamp(marketDataManager.getIncrementalProcessorInTimestamp(MessageHandlerType.PUBLIC_TRADES)));
                    setProcessor(marketDataManager.getIncrementalProcessor(MessageHandlerType.PUBLIC_TRADES));
                    break;

                // ========= //
                case FUT_ORDER_BOOK_INCR_A:
                    marketDataManager.setIncrementalProcessorIsPrimary(MessageHandlerType.ORDER_BOOK, true);
                case FUT_ORDER_BOOK_INCR_B:
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("DefaultIncrementalRefreshMessage"), marketDataManager.getIncrementalProcessor(MessageHandlerType.ORDER_BOOK));
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("SequenceReset"), marketDataManager.getIncrementalProcessor(MessageHandlerType.ORDER_BOOK));
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("Heartbeat"), marketDataManager.getHeartbeatProcessor());
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("TradingSessionStatus"), marketDataManager.getIncrementalProcessor(MessageHandlerType.ORDER_BOOK));
                    multicastInputStream.setInTimestamp(initAndGetInTimestamp(marketDataManager.getIncrementalProcessorInTimestamp(MessageHandlerType.ORDER_BOOK)));
                    setProcessor(marketDataManager.getIncrementalProcessor(MessageHandlerType.ORDER_BOOK));
                    break;
                case FUT_ORDER_BOOK_SNAP_A:
                case FUT_ORDER_BOOK_SNAP_B:
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("DefaultSnapshotMessage"), marketDataManager.getSnapshotProcessor(MessageHandlerType.ORDER_BOOK));
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("SequenceReset"), marketDataManager.getSnapshotProcessor(MessageHandlerType.ORDER_BOOK));
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("Heartbeat"), marketDataManager.getHeartbeatProcessor());
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("TradingSessionStatus"), marketDataManager.getSnapshotProcessor(MessageHandlerType.ORDER_BOOK));
                    multicastInputStream.setInTimestamp(initAndGetInTimestamp(marketDataManager.getSnapshotProcessorInTimestamp(MessageHandlerType.ORDER_BOOK)));
                    setProcessor(marketDataManager.getSnapshotProcessor(MessageHandlerType.ORDER_BOOK));
                    break;

                // ========= //
                case FUT_NEWS_INCR_A:
                case FUT_NEWS_INCR_B:
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("News"), marketDataManager.getNewsProcessor());
                    messageReader.addMessageHandler(messageReader.getTemplateRegistry().get("Heartbeat"), marketDataManager.getHeartbeatProcessor());
                    setProcessor(marketDataManager.getNewsProcessor());
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

    protected SequenceValue getMdEntries(Message readMessage) {
        throw new RuntimeException("Not Implemented");
    }
}
