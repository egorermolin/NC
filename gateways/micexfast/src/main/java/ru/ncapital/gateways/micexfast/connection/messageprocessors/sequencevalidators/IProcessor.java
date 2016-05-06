// Copyright 2016 Orc Software AB All rights reserved.
// Reproduction in whole or in part in any form or medium without express
// written permission of Orc Software AB is strictly prohibited.
package ru.ncapital.gateways.micexfast.connection.messageprocessors.sequencevalidators;

import org.openfast.MessageHandler;

/**
 * Created by egore on 5/6/16.
 */
public interface IProcessor extends MessageHandler {
    
    ThreadLocal<Long> getInTimestampHolder();

    void setIsPrimary(boolean isPrimary);

    IMessageSequenceValidator getSequenceValidator();
}
