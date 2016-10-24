package ru.ncapital.gateways.moexfast.messagehandlers;

import org.openfast.GroupValue;
import ru.ncapital.gateways.moexfast.Utils;
import ru.ncapital.gateways.moexfast.domain.MdEntryType;
import ru.ncapital.gateways.moexfast.domain.impl.BBO;
import ru.ncapital.gateways.moexfast.domain.impl.PublicTrade;

/**
 * Created by egore on 10/22/16.
 */
class MdEntryHandler<T> {

    private AMessageHandler<T> messageHandler;

    MdEntryHandler(AMessageHandler<T> messageHandler) {
        this.messageHandler = messageHandler;
    }


}
