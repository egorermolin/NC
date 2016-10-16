package ru.ncapital.gateways.moexfast;

import ru.ncapital.gateways.moexfast.domain.intf.IBBO;
import ru.ncapital.gateways.moexfast.domain.intf.IDepthLevel;
import ru.ncapital.gateways.moexfast.domain.intf.IInstrument;
import ru.ncapital.gateways.moexfast.domain.intf.IPublicTrade;

/**
 * Created by egore on 12/9/15.
 */
public interface IMarketDataHandler {
    void onBBO(IBBO bbo);

    void onDepthLevels(IDepthLevel[] depthLevels);

    void onPublicTrade(IPublicTrade publicTrade);

    void onStatistics(IBBO bbo);

    void onTradingStatus(IBBO bbo);

    void onInstruments(IInstrument[] instruments);

    void onFeedStatus(boolean up, boolean all);
}
