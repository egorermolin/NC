package ru.ncapital.gateways.micexfast.connection.multicast;

import org.apache.log4j.Level;
import ru.ncapital.gateways.micexfast.*;
import ru.ncapital.gateways.moexfast.ConfigurationManager;
import ru.ncapital.gateways.moexfast.NullGatewayConfiguration;
import ru.ncapital.gateways.moexfast.connection.ConnectionId;
import ru.ncapital.gateways.moexfast.connection.multicast.MessageReader;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by egore on 12/8/15.
 */
public class MicexMessageReader extends MessageReader {

    public MicexMessageReader(ConnectionId connectionId, ConfigurationManager configurationManager, MarketDataManager marketDataManager, InstrumentManager instumentManager) {
        super(connectionId, configurationManager, marketDataManager, instumentManager);
    }

    public static void main(final String[] args) throws IOException {
        // org.apache.log4j.Logger.getLogger("MessageReader").setLevel(Level.INFO);
        MicexGatewayManager.addConsoleAppender("%d{HH:mm:ss} %m%n", Level.TRACE);
        MicexGatewayManager.addFileAppender("log/log.mr.out", "%d{HH:mm:ss} %m%n", Level.TRACE);
        if (args.length < 5) {
            System.err.println("Usage MulticastReader <connectionId> <fast_templates> <interface[s]> <connections_file> <debug, trace, stats> [stats_filename]");
            return;
        }

        ConfigurationManager configurationManager = new MicexConfigurationManager().configure(new MicexNullGatewayConfiguration() {
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
        });

        if (ConnectionId.convert(args[0]) == null) {
            StringBuilder sb = new StringBuilder();
            for (ConnectionId id : configurationManager.getAllConnectionIds())
                sb.append(id.getConnectionId()).append(' ');
            System.err.println("Valid connectionIds are: " + sb.toString());
            return;
        }

        final MicexMessageReader mr = new MicexMessageReader(ConnectionId.convert(args[0]), configurationManager, null, null);
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
                    mr.stats.dump();
                }
            }, 1, 1, TimeUnit.SECONDS);
        }

        mr.start();
    }
}
