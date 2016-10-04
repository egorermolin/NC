package ru.ncapital.gateways.moexfast.domain;

/**
 * Created by Egor on 30-Sep-16.
 */
public abstract class Instrument {
    private String symbol;

    private String securityId;

    private String currency;

    private String description;

    private int lotSize;

    private double tickSize;

    private String tradingStatus;

    private double multiplier;

    private String underlying;

    public Instrument(String symbol, String securityId) {
        this.securityId = securityId;
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getSecurityId() {
        return securityId;
    }

    public String getCurrency() {
        return currency;
    }

    public String getDescription() {
        return description;
    }

    public int getLotSize() {
        return lotSize;
    }

    public double getTickSize() {
        return tickSize;
    }

    public String getTradingStatus() {
        return tradingStatus;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public String getUnderlying() {
        return underlying;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setLotSize(int lotSize) {
        this.lotSize = lotSize;
    }

    public void setTickSize(double tickSize) {
        this.tickSize = tickSize;
    }

    public void setTradingStatus(String tradingStatus) {
        this.tradingStatus = tradingStatus;
    }

    public void setMultiplier(double multiplier) {
        this.multiplier = multiplier;
    }

    public void setUnderlying(String underlying) {
        this.underlying = underlying;
    }

    @Override
    public String toString() {
        return "Instrument{" +
                "symbol='" + symbol + '\'' +
                ", securityId='" + securityId + '\'' +
                ", currency='" + currency + '\'' +
                ", description='" + description + '\'' +
                ", lotSize=" + lotSize +
                ", tickSize=" + tickSize +
                ", tradingStatus='" + tradingStatus + '\'' +
                ", multiplier=" + multiplier +
                ", underlying='" + underlying + '\'' +
                '}';
    }

    public abstract String getFullname();

    public abstract String getName();
}
