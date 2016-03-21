package ru.ncapital.gateways.micexfast.domain;

/**
 * Created by egore on 12/14/15.
 */
public class DepthLevel implements Comparable<DepthLevel> {
    public String symbol;

    public String mdEntryId;

    public MdUpdateAction mdUpdateAction;

    public double mdEntryPx;

    public double mdEntrySize;

    public long mdEntryTime;

    public boolean isBid;

    public String dealNumber;

    public DepthLevel(String symbol, MdUpdateAction mdUpdateAction, String mdEntryId, double mdEntryPx, double mdEntrySize, String dealNumber, boolean isBid) {
        this.symbol = symbol;
        this.mdUpdateAction = mdUpdateAction;
        this.mdEntryId = mdEntryId;
        this.mdEntryPx = mdEntryPx;
        this.mdEntrySize = mdEntrySize;
        this.dealNumber = dealNumber;
        this.isBid = isBid;
    }

    public DepthLevel(String symbol, MdUpdateAction mdUpdateAction) {
        this.symbol = symbol;
        this.mdUpdateAction = mdUpdateAction;
    }

    public void setMdUpdateAction(MdUpdateAction mdUpdateAction) {
        this.mdUpdateAction = mdUpdateAction;
    }

    public void setMdEntrySize(double mdEntrySize) {
        this.mdEntrySize = mdEntrySize;
    }

    public String getSymbol() {
        return symbol;
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

    public long getMdEntryTime() {
        return mdEntryTime;
    }

    public void setMdEntryTime(long mdEntryTime) {
        this.mdEntryTime = mdEntryTime;
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
                "symbol='" + symbol + '\'' +
                ", mdEntryId='" + mdEntryId + '\'' +
                ", mdUpdateAction=" + mdUpdateAction +
                ", mdEntryPx=" + mdEntryPx +
                ", mdEntrySize=" + mdEntrySize +
                ", mdEntryTime=" + mdEntryTime +
                ", isBid=" + isBid +
                '}';
    }
}
