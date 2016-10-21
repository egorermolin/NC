package ru.ncapital.gateways.fortsfast.domain;

import ru.ncapital.gateways.moexfast.domain.impl.Instrument;

/**
 * Created by Egor on 04-Oct-16.
 */
public class FortsInstrument extends Instrument<Long> {
    public String securityType;

    public FortsInstrument(String symbol, long exchangeSecurityId) {
        super(
                symbol,
                symbol,
                exchangeSecurityId
        );
    }

    public static long getExchangeSecurityId(long exchangeSecurityId) {
        return (long) Math.floor(exchangeSecurityId / 256.0);
    }

    public void setSecurityType(String securityType) {
        this.securityType = securityType;
    }

    public String getSecurityType() {
        return securityType;
    }
}
