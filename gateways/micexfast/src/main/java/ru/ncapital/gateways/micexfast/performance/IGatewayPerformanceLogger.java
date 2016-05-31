package ru.ncapital.gateways.micexfast.performance;

/**
 * Created by egore on 10.02.2016.
 */
public interface IGatewayPerformanceLogger {

    void notifyBBOPerformance(PerformanceData perfData);

    void notifyOrderListPerformance(PerformanceData perfData);

    void notifyPublicTradePerformance(PerformanceData perfData);
}
