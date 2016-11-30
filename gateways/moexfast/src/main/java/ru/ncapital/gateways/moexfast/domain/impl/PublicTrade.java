package ru.ncapital.gateways.moexfast.domain.impl;

import ru.ncapital.gateways.moexfast.domain.intf.IPublicTrade;
import ru.ncapital.gateways.moexfast.performance.PerformanceData;

/**
 * Created by egore on 1/28/16.
 */
public class PublicTrade<T> implements IPublicTrade {
    private String securityId;

    private T exchangeSecurityId;

    private String mdEntryId;

    private String tradeId;

    private double lastPx;

    private double lastSize;

    private boolean isBid;

    private PerformanceData performanceData;

    public PublicTrade(String securityId, T exchangeSecurityId) {
        this.securityId = securityId;
        this.exchangeSecurityId = exchangeSecurityId;
        this.performanceData = new PerformanceData();
    }

    @Override
    public String getSecurityId() {
        return securityId;
    }

    public T getExchangeSecurityId() {
        return exchangeSecurityId;
    }

    public String getMdEntryId() { return mdEntryId; }

    public void setMdEntryId(String mdEntryId) {
        this.mdEntryId = mdEntryId;
    }

    @Override
    public String getTradeId() {
        return tradeId;
    }

    public void setTradeId(String tradeId) {
        this.tradeId = tradeId;
    }

    @Override
    public double getLastPx() {
        return lastPx;
    }

    public void setLastPx(double lastPx) {
        this.lastPx = lastPx;
    }

    @Override
    public double getLastSize() {
        return lastSize;
    }

    public void setLastSize(double lastSize) {
        this.lastSize = lastSize;
    }

    @Override
    public boolean isBid() {
        return isBid;
    }

    public void setIsBid(boolean isBid) {
        this.isBid = isBid;
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
