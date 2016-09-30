// Copyright 2016 Orc Software AB All rights reserved.
// Reproduction in whole or in part in any form or medium without express
// written permission of Orc Software AB is strictly prohibited.
package ru.ncapital.gateways.moexfast.connection.messageprocessors.sequencevalidators;

/**
 * Created by egore on 12/28/15.
 */

class SequenceNumber {
    String securityId;

    int lastSeqNum = -1;

    int numberOfMissingSequences = 0;
}
