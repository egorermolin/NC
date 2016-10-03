package ru.ncapital.gateways.micexfast.domain;

import ru.ncapital.gateways.moexfast.domain.Instrument;

/**
 * Created by egore on 24.12.2015.
 */
public class MicexInstrument extends Instrument {

    private static String BOARD_SEPARATOR = ";";

    private String tradingSessionId;

    public MicexInstrument(String symbol, String tradingSessionId) {
        super(symbol, getSecurityId(symbol, tradingSessionId));
        this.tradingSessionId = tradingSessionId;
    }

    public String getTradingSessionId() {
        return tradingSessionId;
    }

    public static String getSecurityId(String symbol, String tradingSessionId) {
        return symbol + MicexInstrument.BOARD_SEPARATOR + tradingSessionId;
    }
}
