package ru.ncapital.gateways.moexfast;

import ru.ncapital.gateways.moexfast.domain.intf.*;

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

    void onNews(INews news);

    void onFeedStatus(boolean up, boolean all);
}
