package ru.ncapital.gateways.micexfast.domain;

/**
 * Created by egore on 12/7/15.
 */
public class BBO {
    private final String securityId;

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

    private long lastTime;

    private String tradingStatus;

    private boolean[] inRecovery = new boolean[] {false, false};

    private boolean[] isInRecoverySet = new boolean[] {false, false};

    public BBO(String securityId) {
        this.securityId = securityId;
    }

    public String getSecurityId() {
        return securityId;
    }

    public double getBidPx() {
        return bidPx;
    }

    public void setBidPx(double bidPx) {
        this.bidPx = bidPx;
    }

    public double getOfferPx() {
        return offerPx;
    }

    public void setOfferPx(double offerPx) {
        this.offerPx = offerPx;
    }

    public double getBidSize() {
        return bidSize;
    }

    public void setBidSize(double bidSize) {
        this.bidSize = bidSize;
    }

    public double getOfferSize() {
        return offerSize;
    }

    public void setOfferSize(double offerSize) {
        this.offerSize = offerSize;
    }

    public String getTradingStatus() {
        return tradingStatus;
    }

    public void setTradingStatus(String tradingStatus) {
        this.tradingStatus = tradingStatus;
    }

    public double getLastPx() {
        return lastPx;
    }

    public void setLastPx(double lastPx) {
        this.lastPx = lastPx;
    }

    public double getLastSize() {
        return lastSize;
    }

    public void setLastSize(double lastSize) {
        this.lastSize = lastSize;
    }

    public double getLowPx() {
        return lowPx;
    }

    public void setLowPx(double lowPx) {
        this.lowPx = lowPx;
    }

    public double getHighPx() {
        return highPx;
    }

    public void setHighPx(double highPx) {
        this.highPx = highPx;
    }

    public double getOpenPx() {
        return openPx;
    }

    public void setOpenPx(double openPx) {
        this.openPx = openPx;
    }

    public double getClosePx() {
        return closePx;
    }

    public void setClosePx(double closePx) {
        this.closePx = closePx;
    }

    public long getLastTime() {
        return lastTime;
    }

    public void setLastTime(long lastTime) {
        this.lastTime = lastTime;
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
                ", bidPx=" + bidPx +
                ", offerPx=" + offerPx +
                ", bidSize=" + bidSize +
                ", offerSize=" + offerSize +
                ", lastPx=" + lastPx +
                ", lastSize=" + lastSize +
                ", lastTime=" + lastTime +
                ", lowPx=" + lowPx +
                ", highPx=" + highPx +
                ", openPx=" + openPx +
                ", closePx=" + closePx +
                ", inRecovery=" + inRecovery +
                ", tradingStatus='" + tradingStatus + '\'' +
                '}';
    }
}
