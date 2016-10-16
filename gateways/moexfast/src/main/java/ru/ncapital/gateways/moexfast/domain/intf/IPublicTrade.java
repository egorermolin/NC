package ru.ncapital.gateways.moexfast.domain.intf;

import ru.ncapital.gateways.moexfast.performance.PerformanceData;

/**
 * Created by egore on 10/15/16.
 */
public interface IPublicTrade {
    String getSecurityId();

    String getTradeId();

    double getLastPx();

    double getLastSize();

    boolean isBid();

    PerformanceData getPerformanceData();
}
