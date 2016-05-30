package ru.ncapital.gateways.micexfast.domain;

/**
 * Created by egore on 5/30/16.
 */
public class PerformanceData {

    private long exchangeEntryTime;

    private long exchangeSendingTime;

    private long gatewayReceiveTime;

    private long gatewayDequeTime;

    private long gatewayOutTime;

    public PerformanceData(long gatewayReceiveTime) {
        this.gatewayReceiveTime = gatewayReceiveTime;
    }

    public long getGatewayReceiveTime() {
        return gatewayReceiveTime;
    }

    public long getExchangeEntryTime() {
        return exchangeEntryTime;
    }

    public PerformanceData setExchangeEntryTime(long exchangeEntryTime) {
        this.exchangeEntryTime = exchangeEntryTime;
        return this;
    }

    public long getExchangeSendingTime() {
        return exchangeSendingTime;
    }

    public PerformanceData setExchangeSendingTime(long exchangeSendingTime) {
        this.exchangeSendingTime = exchangeSendingTime;
        return this;
    }

    public long getGatewayDequeTime() {
        return gatewayDequeTime;
    }

    public PerformanceData setGatewayDequeTime(long gatewayDequeTime) {
        this.gatewayDequeTime = gatewayDequeTime;
        return this;
    }

    public long getGatewayOutTime() {
        return gatewayOutTime;
    }

    public PerformanceData setGatewayOutTime(long gatewayOutTime) {
        this.gatewayOutTime = gatewayOutTime;
        return this;
    }
}
