package ru.ncapital.gateways.moexfast.messagehandlers;

import org.openfast.GroupValue;
import ru.ncapital.gateways.moexfast.Utils;
import ru.ncapital.gateways.moexfast.domain.MdEntryType;
import ru.ncapital.gateways.moexfast.domain.impl.BBO;

/**
 * Created by egore on 10/22/16.
 */
class MdEntryHandler<T> {

    private AMessageHandler<T> messageHandler;

    MdEntryHandler(AMessageHandler<T> messageHandler) {
        this.messageHandler = messageHandler;
    }

    boolean onMdEntry(BBO<T> bbo, GroupValue mdEntry) {
        MdEntryType mdEntryType = messageHandler.getMdEntryType(mdEntry);
        switch (mdEntryType) {
            case BID:
                bbo.setBidPx(messageHandler.getMdEntryPx(mdEntry));
                bbo.setBidSize(messageHandler.getMdEntrySize(mdEntry));
                break;
            case OFFER:
                bbo.setOfferPx(messageHandler.getMdEntryPx(mdEntry));
                bbo.setOfferSize(messageHandler.getMdEntrySize(mdEntry));
                break;
            case LAST:
                bbo.setLastPx(messageHandler.getLastPx(mdEntry));
                bbo.setLastSize(messageHandler.getLastSize(mdEntry));
                bbo.getPerformanceData().setExchangeTime(Utils.getEntryTimeInTicks(mdEntry));
                break;
            case LOW:
                bbo.setLowPx(messageHandler.getMdEntryPx(mdEntry));
                break;
            case HIGH:
                bbo.setHighPx(messageHandler.getMdEntryPx(mdEntry));
                break;
            case OPENING:
                bbo.setOpenPx(messageHandler.getMdEntryPx(mdEntry));
                break;
            case CLOSING:
                bbo.setClosePx(messageHandler.getMdEntryPx(mdEntry));
                break;
            case EMPTY:
                bbo.setEmpty(true);
                break;
            default:
                return false;
        }
        return true;
    }

}
