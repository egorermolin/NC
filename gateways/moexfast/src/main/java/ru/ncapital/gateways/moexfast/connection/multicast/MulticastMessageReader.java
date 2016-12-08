package ru.ncapital.gateways.moexfast.connection.multicast;

import org.apache.log4j.*;
import ru.ncapital.gateways.micexfast.MicexInstrumentManager;
import ru.ncapital.gateways.moexfast.ConfigurationManager;
import ru.ncapital.gateways.moexfast.IGatewayConfiguration;
import ru.ncapital.gateways.moexfast.MarketDataManager;
import ru.ncapital.gateways.moexfast.NullGatewayConfiguration;
import ru.ncapital.gateways.moexfast.connection.Connection;
import ru.ncapital.gateways.moexfast.connection.ConnectionId;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by egore on 12/8/15.
 */
public class MulticastMessageReader extends MessageReader {

    public MulticastMessageReader(ConnectionId connectionId, ConfigurationManager configurationManager, MarketDataManager marketDataManager, MicexInstrumentManager instumentManager) {
        super(connectionId, configurationManager, marketDataManager, instumentManager);
    }

    public static void addConsoleAppender(String pattern, Level level) {
        ConsoleAppender console = new ConsoleAppender(); //create appender

        // String PATTERN = "%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1} - %m%n";
        console.setLayout(new PatternLayout(pattern));
        console.setThreshold(level);
        console.activateOptions();

        Logger.getRootLogger().addAppender(console);
    }

    public static void addFileAppender(String filename, String pattern, Level level) {
        RollingFileAppender file = new RollingFileAppender(); //create appender

        // String PATTERN = "%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1} - %m%n";
        file.setLayout(new PatternLayout(pattern));
        file.setThreshold(level);
        file.setFile(filename);
        file.setAppend(true);
        file.setMaxFileSize("200MB");
        file.setMaxBackupIndex(20);
        file.activateOptions();

        Logger.getRootLogger().addAppender(file);
    }

    public static void main(final String[] args) throws IOException {
        // org.apache.log4j.Logger.getLogger("MessageReader").setLevel(Level.INFO);
        addConsoleAppender("%d{HH:mm:ss} %m%n", Level.TRACE);
        addFileAppender("log/log.mr.out", "%d{HH:mm:ss} %m%n", Level.TRACE);
        if (args.length < 4) {
            System.err.println("Usage MulticastReader <connectionId> <fast_templates> <interface[s]> <debug, trace>");
            return;
        }

        ConfigurationManager configurationManager = new ConfigurationManager() {
            @Override
            public ConfigurationManager configure(IGatewayConfiguration configuration) {
                connections = new HashMap<ConnectionId, Connection>();

                Connection cA = new Connection(ConnectionId.MULTICAST_CHANNEL_A, 59598, "224.0.50.96", null);
                Connection cB = new Connection(ConnectionId.MULTICAST_CHANNEL_B, 59598, "224.0.50.224", null);

                connections.put(ConnectionId.MULTICAST_CHANNEL_A, cA);
                connections.put(ConnectionId.MULTICAST_CHANNEL_B, cB);

                return super.configure(configuration);
            }

            @Override
            public ConnectionId[] getAllConnectionIds() {
                return new ConnectionId[] {ConnectionId.MULTICAST_CHANNEL_A, ConnectionId.MULTICAST_CHANNEL_B};
            }
        }.configure(new NullGatewayConfiguration() {
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
                return null;
            }

            @Override
            public boolean isAsynchChannelReader() {
                return false;
            }
        });

        if (ConnectionId.convert(args[0]) == null) {
            StringBuilder sb = new StringBuilder();
            for (ConnectionId id : configurationManager.getAllConnectionIds())
                sb.append(id.getConnectionId()).append(' ');
            System.err.println("Valid connectionIds are: " + sb.toString());
            return;
        }

        final MulticastMessageReader mr = new MulticastMessageReader(ConnectionId.convert(args[0]), configurationManager, null, null);
        boolean statistics = false;
        switch (args[3]) {
            case "trace":
            case "debug":
                mr.init(args[3]);
                break;
        }

        if (statistics) {
            Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    mr.stats.dump();
                }
            }, 1, 1, TimeUnit.SECONDS);
        }

        mr.start();
    }
}
