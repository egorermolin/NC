package ru.ncapital.gateways.fortsfast.domain;

import ru.ncapital.gateways.moexfast.domain.Instrument;

/**
 * Created by Egor on 04-Oct-16.
 */
public class FortsInstrument extends Instrument {
    private long securityId;

    public FortsInstrument(String symbol, long securityId) {
        super(symbol, String.valueOf(securityId));

        this.securityId = securityId;
    }

    @Override
    public String getFullname() {
        return "[Symbol: " + getSymbol() + "][SecurityId: " + getSecurityId() + "]";
    }

    @Override
    public String getName() {
        return "FortsInstrument";
    }
}
