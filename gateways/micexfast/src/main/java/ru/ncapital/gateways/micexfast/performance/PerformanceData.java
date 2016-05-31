package ru.ncapital.gateways.micexfast.performance;

/**
 * Created by egore on 5/30/16.
 */
public class PerformanceData {

    private long exchangeEntryTime;

    private long exchangeSendingTime;

    private long gatewayInTime;

    private long gatewayDequeTime;

    private long gatewayOutTime;

    public PerformanceData() {
    }

    public long getGatewayInTime() {
        return gatewayInTime;
    }

    public PerformanceData setGatewayInTime(long gatewayInTime) {
        this.gatewayInTime = gatewayInTime;
        return this;
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

    public PerformanceData updateFrom(PerformanceData perfData) {
        if (perfData == null) {
            setExchangeEntryTime(0);
            setExchangeSendingTime(0);
            setGatewayInTime(0);
            setGatewayDequeTime(0);
            setGatewayOutTime(0);
        } else {
            setExchangeEntryTime(perfData.getExchangeEntryTime());
            setExchangeSendingTime(perfData.getExchangeSendingTime());
            setGatewayInTime(perfData.getGatewayInTime());
            setGatewayDequeTime(perfData.getGatewayDequeTime());
            setGatewayOutTime(perfData.getGatewayOutTime());
        }
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PerformanceData that = (PerformanceData) o;

        if (exchangeEntryTime != that.exchangeEntryTime) return false;
        if (exchangeSendingTime != that.exchangeSendingTime) return false;
        if (gatewayInTime != that.gatewayInTime) return false;
        if (gatewayDequeTime != that.gatewayDequeTime) return false;
        return gatewayOutTime == that.gatewayOutTime;

    }

    @Override
    public int hashCode() {
        int result = (int) (exchangeEntryTime ^ (exchangeEntryTime >>> 32));
        result = 31 * result + (int) (exchangeSendingTime ^ (exchangeSendingTime >>> 32));
        result = 31 * result + (int) (gatewayInTime ^ (gatewayInTime >>> 32));
        result = 31 * result + (int) (gatewayDequeTime ^ (gatewayDequeTime >>> 32));
        result = 31 * result + (int) (gatewayOutTime ^ (gatewayOutTime >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "PerformanceData{" +
                "exchangeEntryTime=" + exchangeEntryTime +
                ", exchangeSendingTime=" + exchangeSendingTime +
                ", gatewayInTime=" + gatewayInTime +
                ", gatewayDequeTime=" + gatewayDequeTime +
                ", gatewayOutTime=" + gatewayOutTime +
                '}';
    }

}
