package ru.ncapital.gateways.micexfast;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import org.apache.log4j.*;
import ru.ncapital.gateways.moexfast.GatewayManager;
import ru.ncapital.gateways.moexfast.IGatewayConfiguration;
import ru.ncapital.gateways.moexfast.IGatewayManager;
import ru.ncapital.gateways.moexfast.connection.ConnectionManager;

/**
 * Created by egore on 24.12.2015.
 */

@Singleton
public class MicexGatewayManager extends GatewayManager {

     public static void setLogLevel(Level level) {
        String[] loggers = {"MicexGatewayManager",
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

    public static IGatewayManager create(IMicexGatewayConfiguration configuration) {
        Injector injector = Guice.createInjector(new GatewayModule());

        if (!injector.getInstance(MicexConfigurationManager.class).configure(configuration).checkInterfaces())
            return null;

        injector.getInstance(InstrumentManager.class).configure(configuration);
        injector.getInstance(MarketDataManager.class).configure(configuration);
        injector.getInstance(ConnectionManager.class).configure(configuration);

        return injector.getInstance(MicexGatewayManager.class).configure(configuration);
    }
}
