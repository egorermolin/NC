package ru.ncapital.gateways.moexfast;

import com.google.inject.Inject;
import org.apache.log4j.*;
import ru.ncapital.gateways.micexfast.InstrumentManager;
import ru.ncapital.gateways.micexfast.MarketDataManager;
import ru.ncapital.gateways.micexfast.domain.MicexInstrument;
import ru.ncapital.gateways.moexfast.connection.ConnectionManager;
import ru.ncapital.gateways.moexfast.domain.Subscription;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by egore on 24.12.2015.
 */

public abstract class GatewayManager implements IGatewayManager {

    @Inject
    protected ConnectionManager connectionManager;

    @Inject
    protected InstrumentManager instrumentManager;

    @Inject
    protected MarketDataManager marketDataManager;

    protected boolean isListenSnapshotChannelOnlyIfNeeded;

    protected AtomicBoolean started = new AtomicBoolean(false);

    public GatewayManager configure(IGatewayConfiguration configuration) {
        isListenSnapshotChannelOnlyIfNeeded = configuration.isListenSnapshotChannelOnlyIfNeeded();

        // hack to avoid circular injection
        instrumentManager.setMarketDataManager(marketDataManager);
        marketDataManager.setInstrumentManager(instrumentManager);

        instrumentManager.setGatewayManager(this);

        return this;
    }

    public static void removeAppenders() {
        Logger.getRootLogger().removeAllAppenders();
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

    public static void addCustomAppender(Appender appender) {
        Logger.getRootLogger().addAppender(appender);
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
