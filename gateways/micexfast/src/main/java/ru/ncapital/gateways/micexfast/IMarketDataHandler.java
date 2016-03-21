package ru.ncapital.gateways.micexfast;

import ru.ncapital.gateways.micexfast.domain.BBO;
import ru.ncapital.gateways.micexfast.domain.DepthLevel;
import ru.ncapital.gateways.micexfast.domain.Instrument;
import ru.ncapital.gateways.micexfast.domain.PublicTrade;

/**
 * Created by egore on 12/9/15.
 */
public interface IMarketDataHandler {
    void onBBO(BBO bbo, long inTimeInTicks);

    void onDepthLevels(DepthLevel[] depthLevels, long inTimeInTicks);

    void onPublicTrade(PublicTrade publicTrade, long inTimeInTicks);

    void onStatistics(BBO bbo, long inTimeInTicks);

    void onTradingStatus(BBO bbo, long inTimeInTicks);

    void onInstruments(Instrument[] instruments);
}
