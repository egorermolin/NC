package ru.ncapital.gateways.moexfast.domain.impl;

import ru.ncapital.gateways.moexfast.domain.intf.IPublicTrade;
import ru.ncapital.gateways.moexfast.performance.PerformanceData;

/**
 * Created by egore on 1/28/16.
 */
public class PublicTrade<T> implements IPublicTrade {
    private String securityId;

    private T exchangeSecurityId;

    private String tradeId;

    private double lastPx;

    private double lastSize;

    private boolean isBid;

    private PerformanceData performanceData;

    public PublicTrade(String securityId, T exchangeSecurityId, String tradeId, double lastPx, double lastSize, boolean isBid) {
        this.securityId = securityId;
        this.exchangeSecurityId = exchangeSecurityId;
        this.tradeId = tradeId;
        this.lastPx = lastPx;
        this.lastSize = lastSize;
        this.isBid = isBid;
        this.performanceData = new PerformanceData();
    }

    @Override
    public String getSecurityId() {
        return securityId;
    }

    public T getExchangeSecurityId() {
        return exchangeSecurityId;
    }

    @Override
    public String getTradeId() {
        return tradeId;
    }

    @Override
    public double getLastPx() {
        return lastPx;
    }

    @Override
    public double getLastSize() {
        return lastSize;
    }

    @Override
    public boolean isBid() {
        return isBid;
    }

    @Override
    public PerformanceData getPerformanceData() {
        return performanceData;
    }

    @Override
    public String toString() {
        return "PublicTrade{" +
                "securityId='" + securityId + '\'' +
                ", exchangeSecurityId='" + exchangeSecurityId + '\'' +
                ", tradeId=" + tradeId +
                ", isBid=" + isBid +
                ", lastPx=" + lastPx +
                ", lastSize=" + lastSize +
                ", performanceData=" + performanceData +
                '}';
    }
}
