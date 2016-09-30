package ru.ncapital.gateways.moexfast.domain;

import ru.ncapital.gateways.moexfast.performance.PerformanceData;

/**
 * Created by egore on 1/28/16.
 */
public class PublicTrade {
    private String securityId;

    private String id;

    private double lastPx;

    private double lastSize;

    private boolean isBid;

    private PerformanceData performanceData;

    public PublicTrade(String securityId, String id, double lastPx, double lastSize, boolean isBid) {
        this.securityId = securityId;
        this.id = id;
        this.lastPx = lastPx;
        this.lastSize = lastSize;
        this.isBid = isBid;
        this.performanceData = new PerformanceData();
    }

    public String getSecurityId() {
        return securityId;
    }

    public String getId() {
        return id;
    }

    public double getLastPx() {
        return lastPx;
    }

    public double getLastSize() {
        return lastSize;
    }

    public boolean isBid() {
        return isBid;
    }

    public PerformanceData getPerformanceData() {
        return performanceData;
    }

    @Override
    public String toString() {
        return "PublicTrade{" +
                "securityId='" + securityId + '\'' +
                ", id=" + id +
                ", isBid=" + isBid +
                ", lastPx=" + lastPx +
                ", lastSize=" + lastSize +
                ", performanceData=" + performanceData +
                '}';
    }
}
