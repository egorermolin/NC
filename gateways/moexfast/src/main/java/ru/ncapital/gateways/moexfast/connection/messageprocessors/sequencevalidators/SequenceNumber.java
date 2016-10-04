package ru.ncapital.gateways.moexfast.connection.messageprocessors.sequencevalidators;

/**
 * Created by egore on 12/28/15.
 */

class SequenceNumber {
    String securityId;

    int lastSeqNum = -1;

    int numberOfMissingSequences = 0;
}
