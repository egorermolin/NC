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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PerformanceData that = (PerformanceData) o;

        if (exchangeEntryTime != that.exchangeEntryTime) return false;
        if (exchangeSendingTime != that.exchangeSendingTime) return false;
        if (gatewayReceiveTime != that.gatewayReceiveTime) return false;
        if (gatewayDequeTime != that.gatewayDequeTime) return false;
        return gatewayOutTime == that.gatewayOutTime;

    }

    @Override
    public int hashCode() {
        int result = (int) (exchangeEntryTime ^ (exchangeEntryTime >>> 32));
        result = 31 * result + (int) (exchangeSendingTime ^ (exchangeSendingTime >>> 32));
        result = 31 * result + (int) (gatewayReceiveTime ^ (gatewayReceiveTime >>> 32));
        result = 31 * result + (int) (gatewayDequeTime ^ (gatewayDequeTime >>> 32));
        result = 31 * result + (int) (gatewayOutTime ^ (gatewayOutTime >>> 32));
        return result;
    }
}
