package ru.ncapital.gateways.moexfast.domain.impl;

import ru.ncapital.gateways.moexfast.domain.MdUpdateAction;
import ru.ncapital.gateways.moexfast.domain.intf.IDepthLevel;
import ru.ncapital.gateways.moexfast.performance.PerformanceData;

/**
 * Created by egore on 12/14/15.
 */
public class DepthLevel<T> implements IDepthLevel {
    public String securityId;

    public T exchangeSecurityId;

    public String mdEntryId;

    public MdUpdateAction mdUpdateAction;

    public double mdEntryPx;

    public double mdEntrySize;

    public PerformanceData performanceData;

    public boolean isBid;

    public String tradeId;

    public DepthLevel(String securityId, T exchangeSecurityId) {
        this.securityId = securityId;
        this.exchangeSecurityId = exchangeSecurityId;
        this.performanceData = new PerformanceData();
    }

    public T getExchangeSecurityId() {
        return exchangeSecurityId;
    }

    @Override
    public String getSecurityId() {
        return securityId;
    }

    @Override
    public String getMdEntryId() {
        return mdEntryId;
    }

    public void setMdEntryId(String mdEntryId) {
        this.mdEntryId = mdEntryId;
    }

    @Override
    public MdUpdateAction getMdUpdateAction() {
        return mdUpdateAction;
    }

    public void setMdUpdateAction(MdUpdateAction mdUpdateAction) {
        this.mdUpdateAction = mdUpdateAction;
    }

    @Override
    public double getMdEntryPx() {
        return mdEntryPx;
    }

    public void setMdEntryPx(double mdEntryPx) {
        this.mdEntryPx = mdEntryPx;
    }

    @Override
    public double getMdEntrySize() {
        return mdEntrySize;
    }

    public void setMdEntrySize(double mdEntrySize) {
        this.mdEntrySize = mdEntrySize;
    }

    @Override
    public String getTradeId() {
        return tradeId;
    }

    public void setTradeId(String tradeId) {
        this.tradeId = tradeId;
    }

    @Override
    public boolean getIsBid() {
        return isBid;
    }

    public void setIsBid(boolean isBid) {
        this.isBid = isBid;
    }

    @Override
    public PerformanceData getPerformanceData() {
        return performanceData;
    }

    public int bidCompareTo(IDepthLevel depthLevel) {
        int c = Double.compare(depthLevel.getMdEntryPx(), this.getMdEntryPx());
        if (c == 0)
            return depthLevel.getMdEntryId().compareTo(this.getMdEntryId());
        else
            return c;
    }

    public int offerCompareTo(IDepthLevel depthLevel) {
        int c = Double.compare(this.mdEntryPx, depthLevel.getMdEntryPx());
        if (c == 0)
            return this.getMdEntryId().compareTo(depthLevel.getMdEntryId());
        else
            return c;
    }

    @Override
    public int compareTo(IDepthLevel depthLevel) {
        return offerCompareTo(depthLevel);
    }

    @Override
    public String toString() {
        return "DepthLevel{" +
                "securityId='" + securityId + '\'' +
                ", exchangeSecurityId='" + exchangeSecurityId + '\'' +
                ", mdEntryId='" + mdEntryId + '\'' +
                ", mdUpdateAction=" + mdUpdateAction +
                ", mdEntryPx=" + mdEntryPx +
                ", mdEntrySize=" + mdEntrySize +
                ", tradeId=" + tradeId +
                ", isBid=" + isBid +
                ", performanceData=" + performanceData +
                '}';
    }
}
