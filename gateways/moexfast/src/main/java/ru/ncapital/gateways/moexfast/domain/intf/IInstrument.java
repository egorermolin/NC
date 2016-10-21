// Copyright 2016 Itiviti Group All rights reserved.
// Reproduction in whole or in part in any form or medium without express
// written permission of Orc Software AB is strictly prohibited.
package ru.ncapital.gateways.moexfast.domain.intf;

/**
 * Created by Egor on 13-Oct-16.
 */
public interface IInstrument {
    String getSymbol();

    String getSecurityId();

    String getCurrency();

    String getDescription();

    int getLotSize();

    double getTickSize();

    String getTradingStatus();

    double getMultiplier();

    String getUnderlying();

    String getName();
}
