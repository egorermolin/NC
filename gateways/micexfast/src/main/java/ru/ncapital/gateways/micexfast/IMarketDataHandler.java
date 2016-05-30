package ru.ncapital.gateways.micexfast;

import ru.ncapital.gateways.micexfast.domain.*;

/**
 * Created by egore on 12/9/15.
 */
public interface IMarketDataHandler {
    void onBBO(BBO bbo, PerformanceData perfData);

    void onDepthLevels(DepthLevel[] depthLevels, PerformanceData perfData);

    void onPublicTrade(PublicTrade publicTrade, PerformanceData perfData);

    void onStatistics(BBO bbo, PerformanceData perfData);

    void onTradingStatus(BBO bbo, PerformanceData perfData);

    void onInstruments(Instrument[] instruments);

    void onFeedStatus(boolean up, boolean all);
}
