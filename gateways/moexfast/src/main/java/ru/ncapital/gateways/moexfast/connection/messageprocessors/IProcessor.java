// Copyright 2016 Orc Software AB All rights reserved.
// Reproduction in whole or in part in any form or medium without express
// written permission of Orc Software AB is strictly prohibited.
package ru.ncapital.gateways.moexfast.connection.messageprocessors;

import org.openfast.MessageHandler;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.sequencevalidators.IMessageSequenceValidator;

/**
 * Created by egore on 5/6/16.
 */
public interface IProcessor extends MessageHandler {

    void setIsPrimary(boolean isPrimary);

    ThreadLocal<Long> getInTimestampHolder();

    IMessageSequenceValidator getSequenceValidator();
}
