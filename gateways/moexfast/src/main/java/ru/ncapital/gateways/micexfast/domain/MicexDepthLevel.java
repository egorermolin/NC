package ru.ncapital.gateways.micexfast.domain;

import ru.ncapital.gateways.moexfast.domain.MdUpdateAction;
import ru.ncapital.gateways.moexfast.domain.impl.DepthLevel;

/**
 * Created by egore on 10/15/16.
 */
public class MicexDepthLevel extends DepthLevel<String> {
    public MicexDepthLevel(String exchangeSecurityId, MdUpdateAction mdUpdateAction, String mdEntryId, double mdEntryPx, double mdEntrySize, String dealNumber, boolean isBid) {
        super(exchangeSecurityId, exchangeSecurityId, mdUpdateAction, mdEntryId, mdEntryPx, mdEntrySize, dealNumber, isBid);
    }

    public MicexDepthLevel(String exchangeSecurityId, MdUpdateAction action) {
        super(exchangeSecurityId, exchangeSecurityId, action);
    }
}
