package ru.ncapital.gateways.fortsfast;

import com.google.inject.Singleton;
import org.openfast.Message;
import ru.ncapital.gateways.fortsfast.domain.FortsInstrument;
import ru.ncapital.gateways.micexfast.domain.MicexInstrument;
import ru.ncapital.gateways.moexfast.InstrumentManager;
import ru.ncapital.gateways.moexfast.domain.Instrument;

/**
 * Created by egore on 24.12.2015.
 */

@Singleton
public class FortsInstrumentManager extends InstrumentManager {
    @Override
    protected Instrument createInstrument(Message readMessage) {
        return new MicexInstrument(readMessage.getString("Symbol"), readMessage.getString("SecurityId"));
    }

    @Override
    protected Instrument createFullInstrument(Message readMessage) {
        String symbol = readMessage.getString("Symbol");
        long securityId = readMessage.getLong("SecurityId");

        FortsInstrument instrument = new FortsInstrument(symbol, securityId);
        if (readMessage.getValue("Currency") != null)
            instrument.setCurrency(readMessage.getString("Currency"));
        else
            instrument.setCurrency("RUB");

        if (readMessage.getSequence("NoUnderlyings") != null && readMessage.getSequence("NoUnderlyings").getLength() > 0) {
            if (readMessage.getSequence("NoUnderlyings").get(0).getValue("UnderlyingSymbol") != null)
                instrument.setUnderlying(readMessage.getSequence("NoUnderlyings").get(0).getString("UnderlyingSymbol"));
        }

        if (readMessage.getValue("ContractMultiplier") != null)
            instrument.setMultiplier(readMessage.getDouble("ContractMultiplier"));

        if (readMessage.getValue("MinPriceIncrement") != null)
            instrument.setTickSize(readMessage.getDouble("MinPriceIncrement"));

        StringBuilder tradingStatus = new StringBuilder();
        if (readMessage != null && readMessage.getValue("SecurityTradingStatus") != null)
            tradingStatus.append(readMessage.getInt("SecurityTradingStatus"));
        else
            tradingStatus.append("20");
        instrument.setTradingStatus(tradingStatus.toString());

        return instrument;
    }

    @Override
    protected String createTradingStatusForInstrumentStatus(Message readMessage) {
        StringBuilder tradingStatus = new StringBuilder();

        if (readMessage != null && readMessage.getValue("SecurityTradingStatus") != null)
            tradingStatus.append(readMessage.getInt("SecurityTradingStatus"));
        else
            tradingStatus.append("20");

        return tradingStatus.toString();
    }

    @Override
    protected Instrument[] getInstruments() {
        return instruments.values().toArray(new FortsInstrument[instruments.size()]);
    }}
