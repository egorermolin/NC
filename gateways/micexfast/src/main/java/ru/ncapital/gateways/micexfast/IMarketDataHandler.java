package ru.ncapital.gateways.micexfast;

import ru.ncapital.gateways.micexfast.domain.*;

/**
 * Created by egore on 12/9/15.
 */
public interface IMarketDataHandler {
    void onBBO(BBO bbo);

    void onDepthLevels(DepthLevel[] depthLevels);

    void onPublicTrade(PublicTrade publicTrade);

    void onStatistics(BBO bbo);

    void onTradingStatus(BBO bbo);

    void onInstruments(Instrument[] instruments);

    void onFeedStatus(boolean up, boolean all);
}
