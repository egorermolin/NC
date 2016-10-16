package ru.ncapital.gateways.micexfast.domain;

import ru.ncapital.gateways.moexfast.domain.impl.PublicTrade;

/**
 * Created by egore on 10/15/16.
 */
public class MicexPublicTrade extends PublicTrade<String> {
    public MicexPublicTrade(String exchangeSecurityId) {
        super(exchangeSecurityId, exchangeSecurityId);
    }
}
