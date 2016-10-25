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
        if (exchangeSecurityId >> 24 > 0)
            return exchangeSecurityId >> 8;
        else
            return exchangeSecurityId;
    }

    public void setSecurityType(String securityType) {
        this.securityType = securityType;
    }

    public String getSecurityType() {
        return securityType;
    }
}
