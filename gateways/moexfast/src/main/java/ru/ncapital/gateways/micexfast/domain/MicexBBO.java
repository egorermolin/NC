package ru.ncapital.gateways.micexfast.domain;

import ru.ncapital.gateways.moexfast.domain.impl.BBO;

/**
 * Created by egore on 10/15/16.
 */
public class MicexBBO extends BBO<String> {
    public MicexBBO(String exchangeSecurityId) {
        super(exchangeSecurityId, exchangeSecurityId);
    }
}
