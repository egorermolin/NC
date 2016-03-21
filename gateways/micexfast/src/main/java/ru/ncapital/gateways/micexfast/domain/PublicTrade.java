package ru.ncapital.gateways.micexfast.domain;

/**
 * Created by egore on 1/28/16.
 */
public class PublicTrade {
    private String symbol;

    private String id;

    private double lastPx;

    private double lastSize;

    private boolean isBid;

    private long lastTime;

    public PublicTrade(String symbol, String id,  double lastPx, double lastSize, boolean isBid) {
        this.symbol = symbol;
        this.id = id;
        this.lastPx = lastPx;
        this.lastSize = lastSize;
        this.isBid = isBid;
    }

    public String getSymbol() {
        return symbol;
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

    public long getLastTime() {
        return lastTime;
    }

    public void setLastTime(long lastTime) {
        this.lastTime = lastTime;
    }

    @Override
    public String toString() {
        return "PublicTrade{" +
                "symbol='" + symbol + '\'' +
                ", id=" + id +
                ", isBid=" + isBid +
                ", lastPx=" + lastPx +
                ", lastSize=" + lastSize +
                ", lastTime=" + lastTime +
                '}';
    }
}
