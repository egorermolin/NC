package ru.ncapital.gateways.micexfast.performance;

/**
 * Created by egore on 10.02.2016.
 */
public interface IGatewayPerformanceLogger {
    /*
     * @start and @end are .NET ticks UTC
     */
    void notify(long start, long end, String info);
}
