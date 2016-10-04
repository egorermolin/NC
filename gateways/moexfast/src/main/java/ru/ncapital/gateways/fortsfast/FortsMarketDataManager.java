package ru.ncapital.gateways.fortsfast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ncapital.gateways.moexfast.MarketDataManager;

/**
 * Created by Egor on 03-Oct-16.
 */
public class FortsMarketDataManager extends MarketDataManager {
    @Override
    public Logger getLogger() {
        return LoggerFactory.getLogger("FortsMarketDataManager");
    }

}
