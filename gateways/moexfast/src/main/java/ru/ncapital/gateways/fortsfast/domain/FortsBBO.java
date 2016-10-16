package ru.ncapital.gateways.fortsfast.domain;

import ru.ncapital.gateways.moexfast.domain.impl.BBO;

/**
 * Created by egore on 10/15/16.
 */
public class FortsBBO extends BBO<Long> {
    public FortsBBO(String securityId, Long exchangeSecurityId) {
        super(securityId, exchangeSecurityId);
    }
}
