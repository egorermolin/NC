package ru.ncapital.gateways.moexfast;

import ru.ncapital.gateways.micexfast.domain.*;
import ru.ncapital.gateways.moexfast.domain.BBO;
import ru.ncapital.gateways.moexfast.domain.DepthLevel;
import ru.ncapital.gateways.moexfast.domain.PublicTrade;

/**
 * Created by egore on 12/9/15.
 */
public interface IMarketDataHandler {
    void onBBO(BBO bbo);

    void onDepthLevels(DepthLevel[] depthLevels);

    void onPublicTrade(PublicTrade publicTrade);

    void onStatistics(BBO bbo);

    void onTradingStatus(BBO bbo);

    void onInstruments(MicexInstrument[] instruments);

    void onFeedStatus(boolean up, boolean all);
}
