// Copyright 2016 Orc Software AB All rights reserved.
// Reproduction in whole or in part in any form or medium without express
// written permission of Orc Software AB is strictly prohibited.
package ru.ncapital.gateways.micexfast.messagehandlers;

import org.openfast.Message;
import org.openfast.MessageHandler;

/**
 * Created by egore on 5/5/16.
 */
public enum MessageHandlerType {
    ORDER_LIST("OrderList"),
    STATISTICS("Statistics"),
    PUBLIC_TRADES("PublicTrades"),
    HEARTBEAT("Heartbeat");

    private String description;

    MessageHandlerType(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }
}
