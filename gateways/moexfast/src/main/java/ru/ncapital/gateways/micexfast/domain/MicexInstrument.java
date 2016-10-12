package ru.ncapital.gateways.micexfast.domain;

import ru.ncapital.gateways.moexfast.domain.Instrument;

/**
 * Created by egore on 24.12.2015.
 */
public class MicexInstrument extends Instrument {

    public static String BOARD_SEPARATOR = ";";

    private String tradingSessionId;

    private ProductType productType;

    public MicexInstrument(String symbol, String tradingSessionId) {
        super(symbol, getSecurityId(symbol, tradingSessionId));
        this.tradingSessionId = tradingSessionId;
    }

    @Override
    public String getFullname() {
        return "[SecurityId: " + getSecurityId() + "]" + ((getProductType() == null) ? "" : ("[Product: " + getProductType().toString() + "]"));
    }

    @Override
    public String getName() {
        return "MicexInstrument";
    }

    public String getTradingSessionId() {
        return tradingSessionId;
    }

    public static String getSecurityId(String symbol, String tradingSessionId) {
        return symbol + MicexInstrument.BOARD_SEPARATOR + tradingSessionId;
    }

    public ProductType getProductType() {
        return this.productType;
    }

    public void setProductType(int productType) {
        this.productType = ProductType.convert(productType);
    }
}
