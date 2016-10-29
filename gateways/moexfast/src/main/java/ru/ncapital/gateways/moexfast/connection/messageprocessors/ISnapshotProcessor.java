package ru.ncapital.gateways.moexfast.connection.messageprocessors;

import ru.ncapital.gateways.moexfast.connection.messageprocessors.sequencevalidators.IMessageSequenceValidator;

public interface ISnapshotProcessor extends IProcessor {
    void reset();

    IMessageSequenceValidator getSequenceValidator();
}
