package ru.ncapital.gateways.micexfast.domain;

import sun.misc.Perf;

/**
 * Created by egore on 12/14/15.
 */
public class DepthLevel implements Comparable<DepthLevel> {
    public String securityId;

    public String mdEntryId;

    public MdUpdateAction mdUpdateAction;

    public double mdEntryPx;

    public double mdEntrySize;

    public PerformanceData perfData;

    public boolean isBid;

    public String dealNumber;

    public DepthLevel(String securityId, MdUpdateAction mdUpdateAction, String mdEntryId, double mdEntryPx, double mdEntrySize, String dealNumber, boolean isBid) {
        this.securityId = securityId;
        this.mdUpdateAction = mdUpdateAction;
        this.mdEntryId = mdEntryId;
        this.mdEntryPx = mdEntryPx;
        this.mdEntrySize = mdEntrySize;
        this.dealNumber = dealNumber;
        this.isBid = isBid;
    }

    public DepthLevel(String securityId, MdUpdateAction mdUpdateAction) {
        this.securityId = securityId;
        this.mdUpdateAction = mdUpdateAction;
    }

    public void setMdUpdateAction(MdUpdateAction mdUpdateAction) {
        this.mdUpdateAction = mdUpdateAction;
    }

    public void setMdEntrySize(double mdEntrySize) {
        this.mdEntrySize = mdEntrySize;
    }

    public String getSecurityId() {
        return securityId;
    }

    public String getMdEntryId() {
        return mdEntryId;
    }

    public MdUpdateAction getMdUpdateAction() {
        return mdUpdateAction;
    }

    public double getMdEntryPx() {
        return mdEntryPx;
    }

    public double getMdEntrySize() {
        return mdEntrySize;
    }

    public String getDealNumber() {
        return dealNumber;
    }

    public boolean isBid() {
        return isBid;
    }

    public PerformanceData getPerformanceData() {
        return perfData;
    }

    public void setPerformanceData(PerformanceData perfData) {
        this.perfData = perfData;
    }

    @Override
    public int compareTo(DepthLevel depthLevel) {
        int c = Double.compare(this.mdEntryPx, depthLevel.mdEntryPx);

        if (c == 0)
            return this.mdEntryId.compareTo(depthLevel.mdEntryId);
        else
            return c;
    }

    @Override
    public String toString() {
        return "DepthLevel{" +
                "securityId='" + securityId + '\'' +
                ", mdEntryId='" + mdEntryId + '\'' +
                ", mdUpdateAction=" + mdUpdateAction +
                ", mdEntryPx=" + mdEntryPx +
                ", mdEntrySize=" + mdEntrySize +
                ", perfData=" + perfData +
                ", isBid=" + isBid +
                '}';
    }
}
