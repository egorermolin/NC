package ru.ncapital.gateways.fortsfast;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import ru.ncapital.gateways.micexfast.MicexGatewayModule;
import ru.ncapital.gateways.moexfast.GatewayManager;
import ru.ncapital.gateways.moexfast.IGatewayManager;
import ru.ncapital.gateways.moexfast.connection.ConnectionManager;

/**
 * Created by egore on 24.12.2015.
 */

@Singleton
public class FortsGatewayManager extends GatewayManager {

     public static void setLogLevel(Level level) {
        String[] loggers = {"FortsGatewayManager", "HeartbeatProcessor",
                "FUT-IDF-A-Processor", "FUT-IDF-B-Processor",
                "FUT-ISF-A-Processor", "FUT-ISF-B-Processor"
        };

        for (String logger : loggers)
            Logger.getLogger(logger).setLevel(level);
    }

    public static IGatewayManager create(IFortsGatewayConfiguration configuration) {
        Injector injector = Guice.createInjector(new MicexGatewayModule());

        if (!injector.getInstance(FortsConfigurationManager.class).configure(configuration).checkInterfaces())
            return null;

        injector.getInstance(FortsInstrumentManager.class).configure(configuration);
        injector.getInstance(FortsMarketDataManager.class).configure(configuration);
        injector.getInstance(ConnectionManager.class).configure(configuration);

        return injector.getInstance(FortsGatewayManager.class).configure(configuration);
    }
}
