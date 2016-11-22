package ru.ncapital.gateways.moexfast.connection.messageprocessors.sequencevalidators;

/**
 * Created by egore on 12/28/15.
 */

class SequenceNumber<T> {
    T exchangeSecurityId;

    int lastSeqNum = 0;

    int numberOfMissingSequences = 0;
}
