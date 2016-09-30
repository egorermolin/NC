package ru.ncapital.gateways.micexfast;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import org.apache.log4j.*;
import ru.ncapital.gateways.moexfast.connection.ConnectionManager;
import ru.ncapital.gateways.micexfast.domain.MicexInstrument;
import ru.ncapital.gateways.moexfast.domain.Subscription;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by egore on 24.12.2015.
 */

@Singleton
public class GatewayManager implements IGatewayManager {
    @Inject
    private ConnectionManager connectionManager;

    @Inject
    private InstrumentManager instrumentManager;

    @Inject
    private MarketDataManager marketDataManager;

    private boolean isListenSnapshotChannelOnlyIfNeeded;

    private AtomicBoolean started = new AtomicBoolean(false);

    public GatewayManager configure(IGatewayConfiguration configuration) {
        isListenSnapshotChannelOnlyIfNeeded = configuration.isListenSnapshotChannelOnlyIfNeeded();

        // hack to avoid circular injection
        instrumentManager.setMarketDataManager(marketDataManager);
        marketDataManager.setInstrumentManager(instrumentManager);

        instrumentManager.setGatewayManager(this);

        return this;
    }

    public static void removeAppenders() {
        org.apache.log4j.Logger.getRootLogger().removeAllAppenders();
    }

    public static void addConsoleAppender(String pattern, Level level) {
        ConsoleAppender console = new ConsoleAppender(); //create appender

        // String PATTERN = "%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1} - %m%n";
        console.setLayout(new PatternLayout(pattern));
        console.setThreshold(level);
        console.activateOptions();

        org.apache.log4j.Logger.getRootLogger().addAppender(console);
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

        org.apache.log4j.Logger.getRootLogger().addAppender(file);
    }

    public static void addCustomAppender(Appender appender) {
        org.apache.log4j.Logger.getRootLogger().addAppender(appender);
    }

    public static void setLogLevel(Level level) {
        String[] loggers = {"GatewayManager",
                "HeartbeatProcessor",
            "CURR-IDF-A-Processor", "CURR-IDF-B-Processor",
            "CURR-ISF-A-Processor", "CURR-ISF-B-Processor",
            "CURR-OLR-A-Processor", "CURR-OLR-B-Processor",
            "CURR-OLS-A-Processor", "CURR-OLS-B-Processor",
            "CURR-MSR-A-Processor", "CURR-MSR-B-Processor",
            "CURR-MSS-A-Processor", "CURR-MSS-B-Processor",
            "CURR-TLR-A-Processor", "CURR-TLR-A-Processor",
            "FOND-IDF-A-Processor", "FOND-IDF-B-Processor",
            "FOND-ISF-A-Processor", "FOND-ISF-B-Processor",
            "FOND-OLR-A-Processor", "FOND-OLR-B-Processor",
            "FOND-OLS-A-Processor", "FOND-OLS-B-Processor",
            "FOND-MSR-A-Processor", "FOND-MSR-B-Processor",
            "FOND-MSS-A-Processor", "FOND-MSS-B-Processor",
            "FOND-TLR-A-Processor", "FOND-TLR-A-Processor"};

        for (String logger : loggers)
            org.apache.log4j.Logger.getLogger(logger).setLevel(level);
    }

    public static IGatewayManager create(IGatewayConfiguration configuration) {
        Injector injector = Guice.createInjector(new GatewayModule());

        if (!injector.getInstance(ConfigurationManager.class).configure(configuration).checkInterfaces())
            return null;

        injector.getInstance(InstrumentManager.class).configure(configuration);
        injector.getInstance(MarketDataManager.class).configure(configuration);
        injector.getInstance(ConnectionManager.class).configure(configuration);

        return injector.getInstance(GatewayManager.class).configure(configuration);
    }

    @Override
    public void start() {
        if (!started.getAndSet(true))
            connectionManager.start(isListenSnapshotChannelOnlyIfNeeded);
    }

    @Override
    public void stop() {
        if (started.getAndSet(false))
            connectionManager.shutdown();
    }

    public void onInstrumentDownloadFinished(Collection<MicexInstrument> instruments) {
        connectionManager.onInstrumentDownloadFinished(instruments);
    }

    @Override
    public void subscribeForMarketData(String subscriptionCode) {
        marketDataManager.subscribe(new Subscription(subscriptionCode));
    }
}
