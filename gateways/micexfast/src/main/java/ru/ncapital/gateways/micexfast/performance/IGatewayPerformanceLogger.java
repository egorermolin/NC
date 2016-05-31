package ru.ncapital.gateways.micexfast.performance;

import ru.ncapital.gateways.micexfast.domain.PerformanceData;

/**
 * Created by egore on 10.02.2016.
 */
public interface IGatewayPerformanceLogger {

    void notifyBBOPerformance(PerformanceData perfData);

    void notifyOrderListPerformance(PerformanceData perfData);

    void notifyPublicTradePerformance(PerformanceData perfData);
}
