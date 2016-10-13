package ru.ncapital.gateways.micexfast.domain;

import ru.ncapital.gateways.moexfast.domain.Instrument;

/**
 * Created by egore on 24.12.2015.
 */
public class MicexInstrument extends Instrument<String> {

    public static String BOARD_SEPARATOR = ";";

    private String tradingSessionId;

    private ProductType productType;

    public MicexInstrument(String symbol, String tradingSessionId, int productType) {
        super(symbol, getSecurityId(symbol, tradingSessionId), getSecurityId(symbol, tradingSessionId));
        this.tradingSessionId = tradingSessionId;
        this.productType = ProductType.convert(productType);
    }

    public static String getSecurityId(String symbol, String tradingSessionId) {
        return symbol + MicexInstrument.BOARD_SEPARATOR + tradingSessionId;
    }

    public String getTradingSessionId() {
        return tradingSessionId;
    }

    public ProductType getProductType() {
        return this.productType;
    }
}
