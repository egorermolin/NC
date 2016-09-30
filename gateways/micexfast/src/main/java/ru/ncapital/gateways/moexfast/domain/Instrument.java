// Copyright 2016 Itiviti Group All rights reserved.
// Reproduction in whole or in part in any form or medium without express
// written permission of Orc Software AB is strictly prohibited.
package ru.ncapital.gateways.moexfast.domain;

import ru.ncapital.gateways.micexfast.domain.ProductType;

/**
 * Created by Egor on 30-Sep-16.
 */
public class Instrument {
    private String symbol;

    private String securityId;

    private String currency;

    private String description;

    private int lotSize;

    private double tickSize;

    private String tradingStatus;

    private double multiplier;

    private String underlying;

    private ProductType productType;

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
        return "MicexInstrument{" +
                "symbol='" + symbol + '\'' +
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
