package ru.ncapital.gateways.moexfast.connection.messageprocessors.sequencevalidators;

import com.google.inject.name.Named;

/**
 * Created by egore on 2/3/16.
 */
public interface MessageSequenceValidatorFactory<T> {
    @Named("orderlist") IMessageSequenceValidator<T> createMessageSequenceValidatorForOrderList();
    @Named("statistics") IMessageSequenceValidator<T> createMessageSequenceValidatorForStatistics();
    @Named("publictrades") IMessageSequenceValidator<T> createMessageSequenceValidatorForPublicTrades();
}
