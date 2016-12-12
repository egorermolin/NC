package ru.ncapital.gateways.moexfast.domain.impl;

import ru.ncapital.gateways.moexfast.domain.intf.IBBO;
import ru.ncapital.gateways.moexfast.domain.intf.IChannelStatus;
import ru.ncapital.gateways.moexfast.performance.PerformanceData;

import java.util.HashMap;
import java.util.Map;

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

    private boolean empty;

    private Map<IChannelStatus.ChannelType, Boolean> inRecovery = new HashMap<>();

    private Map<IChannelStatus.ChannelType, Boolean> isInRecoverySet = new HashMap<>();

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

    public boolean isEmpty() {
        return empty;
    }

    public void setEmpty(boolean empty) {
        this.empty = empty;
    }

    @Override
    public PerformanceData getPerformanceData() {
        return performanceData;
    }

    @Override
    public boolean isInRecovery(IChannelStatus.ChannelType channelType) {
        return inRecovery.get(channelType);
    }

    @Override
    public boolean isInRecoverySet(IChannelStatus.ChannelType channelType) {
        if (isInRecoverySet.containsKey(channelType))
            return isInRecoverySet.get(channelType);

        return false;
    }

    public void setInRecovery(boolean inRecovery, IChannelStatus.ChannelType channelType) {
        this.inRecovery.put(channelType, inRecovery);
        this.isInRecoverySet.put(channelType, true);
    }

    public String toStringRecovery(String securityId) {
        return (securityId == null ? "" : "securityId='" + securityId + '\'') +
                (isInRecoverySet.containsKey(IChannelStatus.ChannelType.OrderList) ?
                        ("inRecoveryOrderList=" + inRecovery.get(IChannelStatus.ChannelType.OrderList)) : ", N/A") +
                (isInRecoverySet.containsKey(IChannelStatus.ChannelType.BBOAndStatistics) ?
                        (", inRecoveryBBOAndStatistics=" + inRecovery.get(IChannelStatus.ChannelType.BBOAndStatistics)) : ", N/A") +
                (isInRecoverySet.containsKey(IChannelStatus.ChannelType.BBO) ?
                        (", inRecoveryBBO=" + inRecovery.get(IChannelStatus.ChannelType.BBO)) : ", N/A") +
                (isInRecoverySet.containsKey(IChannelStatus.ChannelType.Statistics) ?
                        (", inRecoveryStatistics=" + inRecovery.get(IChannelStatus.ChannelType.Statistics)) : ", N/A");

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
                ", " + toStringRecovery(securityId) +
                ", tradingStatus='" + tradingStatus + '\'' +
                '}';
    }
}
