package ru.ncapital.gateways.fortsfast.domain;

import ru.ncapital.gateways.moexfast.domain.MdUpdateAction;
import ru.ncapital.gateways.moexfast.domain.impl.DepthLevel;

/**
 * Created by egore on 10/15/16.
 */
public class FortsDepthLevel extends DepthLevel<Long> {
    public FortsDepthLevel(String securityId, Long exchangeSecurityId, MdUpdateAction mdUpdateAction, String mdEntryId, double mdEntryPx, double mdEntrySize, String dealNumber, boolean isBid) {
        super(securityId, exchangeSecurityId, mdUpdateAction, mdEntryId, mdEntryPx, mdEntrySize, dealNumber, isBid);
    }

    public FortsDepthLevel(String securityId, Long exchangeSecurityId, MdUpdateAction action) {
        super(securityId, exchangeSecurityId, action);
    }
}
