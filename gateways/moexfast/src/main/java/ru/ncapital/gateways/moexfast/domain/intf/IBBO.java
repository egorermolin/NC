package ru.ncapital.gateways.moexfast.domain.intf;

import ru.ncapital.gateways.moexfast.performance.PerformanceData;

/**
 * Created by egore on 10/15/16.
 */
public interface IBBO {
    String getSecurityId();

    double getBidPx();

    double getOfferPx();

    double getBidSize();

    double getOfferSize();

    String getTradingStatus();

    double getLastPx();

    double getLastSize();

    double getLowPx();

    double getHighPx();

    double getOpenPx();

    double getClosePx();

    PerformanceData getPerformanceData();
}
