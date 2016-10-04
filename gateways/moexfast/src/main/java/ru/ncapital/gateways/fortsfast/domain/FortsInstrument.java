// Copyright 2016 Itiviti Group All rights reserved.
// Reproduction in whole or in part in any form or medium without express
// written permission of Orc Software AB is strictly prohibited.
package ru.ncapital.gateways.fortsfast.domain;

import ru.ncapital.gateways.moexfast.domain.Instrument;

/**
 * Created by Egor on 04-Oct-16.
 */
public class FortsInstrument extends Instrument {
    public FortsInstrument(String symbol, String securityId) {
        super(symbol, securityId);
    }

    @Override
    public String getFullname() {
        return "[SecurityId: " + getSecurityId() + "][Symbol: " + getSymbol() + "]";
    }

    @Override
    public String getName() {
        return "FortsInstrument";
    }
}
