package ru.ncapital.gateways.micexfast.connection.messageprocessors.sequencevalidators;

import com.google.inject.name.Named;

/**
 * Created by egore on 2/3/16.
 */
public interface MessageSequenceValidatorFactory {
    @Named("orderlist") IMessageSequenceValidator createMessageSequenceValidatorForOrderList();
    @Named("statistics") IMessageSequenceValidator createMessageSequenceValidatorForStatistics();
    @Named("publictrades") IMessageSequenceValidator createMessageSequenceValidatorForPublicTrades();
}
