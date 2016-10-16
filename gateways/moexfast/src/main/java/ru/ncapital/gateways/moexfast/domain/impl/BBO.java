package ru.ncapital.gateways.moexfast.domain.impl;

import ru.ncapital.gateways.moexfast.domain.intf.IBBO;
import ru.ncapital.gateways.moexfast.performance.PerformanceData;

/**
 * Created by egore on 12/7/15.
 */
public class BBO<T> implements IBBO {
    private final String securityId;

    private final T exchangeSecurityId;

    private double bidPx;

    private double offerPx;

    private double bidSize;

    private double offerSize;

    private double lastPx;

    private double lastSize;

    private double lowPx;

    private double highPx;

    private double openPx;

    private double closePx;

    private PerformanceData performanceData;

    private String tradingStatus;

    private boolean[] inRecovery = new boolean[] {false, false};

    private boolean[] isInRecoverySet = new boolean[] {false, false};

    public BBO(String securityId, T exchangeSecurityId) {
        this.securityId = securityId;
        this.exchangeSecurityId = exchangeSecurityId;
        this.performanceData = new PerformanceData();
    }

    @Override
    public String getSecurityId() {
        return securityId;
    }

    public T getExchangeSecurityId() { return exchangeSecurityId; }

    @Override
    public double getBidPx() {
        return bidPx;
    }

    public void setBidPx(double bidPx) {
        this.bidPx = bidPx;
    }

    @Override
    public double getOfferPx() {
        return offerPx;
    }

    public void setOfferPx(double offerPx) {
        this.offerPx = offerPx;
    }

    @Override
    public double getBidSize() {
        return bidSize;
    }

    public void setBidSize(double bidSize) {
        this.bidSize = bidSize;
    }

    @Override
    public double getOfferSize() {
        return offerSize;
    }

    public void setOfferSize(double offerSize) {
        this.offerSize = offerSize;
    }

    @Override
    public String getTradingStatus() {
        return tradingStatus;
    }

    public void setTradingStatus(String tradingStatus) {
        this.tradingStatus = tradingStatus;
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
    public double getLowPx() {
        return lowPx;
    }

    public void setLowPx(double lowPx) {
        this.lowPx = lowPx;
    }

    @Override
    public double getHighPx() {
        return highPx;
    }

    public void setHighPx(double highPx) {
        this.highPx = highPx;
    }

    @Override
    public double getOpenPx() {
        return openPx;
    }

    public void setOpenPx(double openPx) {
        this.openPx = openPx;
    }

    @Override
    public double getClosePx() {
        return closePx;
    }

    public void setClosePx(double closePx) {
        this.closePx = closePx;
    }

    @Override
    public PerformanceData getPerformanceData() {
        return performanceData;
    }

    public boolean isInRecovery(int i) {
        return inRecovery[i];
    }

    public boolean isInRecoverySet(int i) {
        return isInRecoverySet[i];
    }

    public void setInRecovery(boolean inRecovery, int i) {
        this.inRecovery[i] = inRecovery;
        this.isInRecoverySet[i] = true;
    }

    @Override
    public String toString() {
        return "BBO{" +
                "securityId='" + securityId + '\'' +
                ", exchangeSecurityId='" + exchangeSecurityId + '\'' +
                ", bidPx=" + bidPx +
                ", offerPx=" + offerPx +
                ", bidSize=" + bidSize +
                ", offerSize=" + offerSize +
                ", lastPx=" + lastPx +
                ", lastSize=" + lastSize +
                ", performanceData=" + performanceData +
                ", lowPx=" + lowPx +
                ", highPx=" + highPx +
                ", openPx=" + openPx +
                ", closePx=" + closePx +
                ", inRecovery=" + inRecovery[0] + inRecovery[1] +
                ", tradingStatus='" + tradingStatus + '\'' +
                '}';
    }
}
