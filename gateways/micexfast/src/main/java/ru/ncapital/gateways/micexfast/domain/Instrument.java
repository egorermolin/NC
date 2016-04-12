package ru.ncapital.gateways.micexfast.domain;

/**
 * Created by egore on 24.12.2015.
 */
public class Instrument {

    public static String BOARD_SEPARATOR = ";";

    private String symbol;

    private String tradingSessionId;

    private String securityId;

    private String currency;

    private String description;

    private int lotSize;

    private double tickSize;

    private String tradingStatus;

    private double multiplier;

    private String underlying;

    private ProductType productType;

    public Instrument(String symbol, String tradingSessionId) {
        this.symbol = symbol;
        this.tradingSessionId = tradingSessionId;
        this.securityId = symbol + Instrument.BOARD_SEPARATOR + tradingSessionId;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getSecurityId() {
        return securityId;
    }

    public String getTradingSessionId() {
        return tradingSessionId;
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

    public ProductType getProductType() {
        return this.productType;
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

    public void setProductType(int productType) {
        this.productType = ProductType.convert(productType);
    }

    @Override
    public String toString() {
        return "Instrument{" +
                "symbol='" + symbol + '\'' +
                ", tradingSessionId='" + tradingSessionId + '\'' +
                ", securityId='" + securityId + '\'' +
                ", currency='" + currency + '\'' +
                ", description='" + description + '\'' +
                ", lotSize=" + lotSize +
                ", tickSize=" + tickSize +
                ", tradingStatus='" + tradingStatus + '\'' +
                ", multiplier=" + multiplier +
                ", underlying='" + underlying + '\'' +
                ", productType=" + productType +
                '}';
    }
}
