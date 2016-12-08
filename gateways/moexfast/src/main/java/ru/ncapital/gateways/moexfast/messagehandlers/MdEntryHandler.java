package ru.ncapital.gateways.moexfast.messagehandlers;

/**
 * Created by egore on 10/22/16.
 */
class MdEntryHandler<T> {

    private AMessageHandler<T> messageHandler;

    MdEntryHandler(AMessageHandler<T> messageHandler) {
        this.messageHandler = messageHandler;
    }


}
