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
        String[] loggers = {"FortsGatewayManager", "FortsMarketDataManager", "FortsInstrumentManager",
                "HeartbeatProcessor",
                "FUT-INFO-R-A-Processor", "FUT-INFO-R-B-Processor",
                "FUT-INFO-I-A-Processor", "FUT-INFO-I-B-Processor",
                "FUT-TRADES-S-A-Processor", "FUT-TRADES-S-B-Processor",
                "FUT-TRADES-I-A-Processor", "FUT-TRADES-I-B-Processor",
                "FUT-BOOK-1-S-A-Processor", "FUT-BOOK-1-S-B-Processor",
                "FUT-BOOK-1-I-A-Processor", "FUT-BOOK-1-I-B-Processor",
                "ORDERS-LOG-S-A-Processor", "ORDERS-LOG-S-B-Processor",
                "ORDERS-LOG-I-A-Processor", "ORDERS-LOG-I-B-Processor",
        };

        for (String logger : loggers)
            Logger.getLogger(logger).setLevel(level);
    }

    public static IGatewayManager create(IFortsGatewayConfiguration configuration) {
        Injector injector = Guice.createInjector(new FortsGatewayModule());

        if (!injector.getInstance(FortsConfigurationManager.class).configure(configuration).checkInterfaces())
            return null;

        injector.getInstance(FortsInstrumentManager.class).configure(configuration);
        injector.getInstance(FortsMarketDataManager.class).configure(configuration);
        injector.getInstance(ConnectionManager.class).configure(configuration);

        return injector.getInstance(FortsGatewayManager.class).configure(configuration);
    }
}
