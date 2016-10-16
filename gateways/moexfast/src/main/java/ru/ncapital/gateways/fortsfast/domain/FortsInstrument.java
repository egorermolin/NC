package ru.ncapital.gateways.fortsfast.domain;

import ru.ncapital.gateways.moexfast.domain.impl.Instrument;

/**
 * Created by Egor on 04-Oct-16.
 */
public class FortsInstrument extends Instrument<Long> {
    public FortsInstrument(String symbol, long exchangeSecurityId) {
        super(symbol, symbol, exchangeSecurityId);
    }
}
