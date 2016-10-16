package ru.ncapital.gateways.fortsfast.domain;

import ru.ncapital.gateways.moexfast.domain.impl.PublicTrade;

/**
 * Created by egore on 10/15/16.
 */
public class FortsPublicTrade extends PublicTrade<Long> {
    public FortsPublicTrade(String securityId, Long exchangeSecurityId, String tradeId, double lastPx, double lastSize, boolean isBid) {
        super(securityId, exchangeSecurityId, tradeId, lastPx, lastSize, isBid);
    }
}
