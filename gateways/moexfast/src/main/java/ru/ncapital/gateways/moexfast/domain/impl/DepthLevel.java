package ru.ncapital.gateways.moexfast.domain.impl;

import ru.ncapital.gateways.moexfast.domain.intf.IDepthLevel;
import ru.ncapital.gateways.moexfast.domain.MdUpdateAction;
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

    public String dealNumber;

    public DepthLevel(String securityId, T exchangeSecurityId, MdUpdateAction mdUpdateAction, String mdEntryId, double mdEntryPx, double mdEntrySize, String dealNumber, boolean isBid) {
        this.securityId = securityId;
        this.exchangeSecurityId = exchangeSecurityId;
        this.mdUpdateAction = mdUpdateAction;
        this.mdEntryId = mdEntryId;
        this.mdEntryPx = mdEntryPx;
        this.mdEntrySize = mdEntrySize;
        this.dealNumber = dealNumber;
        this.isBid = isBid;
        this.performanceData = new PerformanceData();
    }

    public DepthLevel(String securityId, T exchangeSecurityId, MdUpdateAction mdUpdateAction) {
        this.securityId = securityId;
        this.exchangeSecurityId = exchangeSecurityId;
        this.mdUpdateAction = mdUpdateAction;
        this.performanceData = new PerformanceData();
    }

   // public void setMdUpdateAction(MdUpdateAction mdUpdateAction) {
   //     this.mdUpdateAction = mdUpdateAction;
   // }

   // public void setMdEntrySize(double mdEntrySize) {
   //     this.mdEntrySize = mdEntrySize;
   // }

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

    @Override
    public MdUpdateAction getMdUpdateAction() {
        return mdUpdateAction;
    }

    @Override
    public double getMdEntryPx() {
        return mdEntryPx;
    }

    @Override
    public double getMdEntrySize() {
        return mdEntrySize;
    }

    @Override
    public String getDealNumber() {
        return dealNumber;
    }

    @Override
    public boolean isBid() {
        return isBid;
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
                ", performanceData=" + performanceData +
                ", isBid=" + isBid +
                '}';
    }
}
