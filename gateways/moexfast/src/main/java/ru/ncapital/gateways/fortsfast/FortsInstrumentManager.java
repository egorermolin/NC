package ru.ncapital.gateways.fortsfast;

import com.google.inject.Singleton;
import org.openfast.Message;
import ru.ncapital.gateways.fortsfast.domain.FortsInstrument;
import ru.ncapital.gateways.moexfast.IGatewayConfiguration;
import ru.ncapital.gateways.moexfast.InstrumentManager;
import ru.ncapital.gateways.moexfast.domain.impl.Instrument;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by egore on 24.12.2015.
 */

@Singleton
public class FortsInstrumentManager extends InstrumentManager<Long> {

    private Set<String> allowedUnderlyings = new HashSet<>();

    @Override
    public InstrumentManager<Long> configure(IGatewayConfiguration configuration) {
        IFortsGatewayConfiguration fortsConfiguration = (IFortsGatewayConfiguration) configuration;

        this.allowedUnderlyings.addAll(Arrays.asList(fortsConfiguration.getAllowedUnderlyings()));
        if (allowedUnderlyings.contains("*"))
            allowedUnderlyings.clear();

        return super.configure(configuration);
    }

    @Override
    public boolean isAllowedInstrument(Instrument<Long> instrument) {
        if (super.isAllowedInstrument(instrument))
            return true;

        if (allowedUnderlyings.isEmpty() || allowedUnderlyings.contains(instrument.getUnderlying())) {
            // do nothing
        } else {
            if (getLogger().isTraceEnabled())
                getLogger().trace("Ignored by Underlying: " + instrument.getId());

            addInstrumentToIgnored(instrument);
            return false;
        }

        return true;
    }

    @Override
    protected String createTradingStatusForInstrumentStatus(Message readMessage) {
        return getTradingStatus(readMessage);
    }
    
    @Override
    protected Instrument<Long> createInstrument(Message readMessage) {
        return new FortsInstrument(
                readMessage.getString("Symbol"),
                FortsInstrument.getExchangeSecurityId(readMessage.getLong("SecurityID"))
        );
    }

    @Override
    protected Instrument<Long> createFullInstrument(Message readMessage) {
        FortsInstrument instrument = new FortsInstrument(
                readMessage.getString("Symbol"),
                FortsInstrument.getExchangeSecurityId(readMessage.getLong("SecurityID"))
        );

        if (readMessage.getValue("Currency") != null)
            instrument.setCurrency(readMessage.getString("Currency"));
        else
            instrument.setCurrency("RUB");

        if (readMessage.getSequence("Underlyings") != null && readMessage.getSequence("Underlyings").getLength() > 0) {
            if (readMessage.getSequence("Underlyings").get(0).getValue("UnderlyingSymbol") != null)
                instrument.setUnderlying(readMessage.getSequence("Underlyings").get(0).getString("UnderlyingSymbol"));
        }

        if (readMessage.getValue("SecurityAltID") != null)
            instrument.setDescription(readMessage.getString("SecurityAltID"));

        if (readMessage.getValue("ContractMultiplier") != null)
            instrument.setLotSize(readMessage.getInt("ContractMultiplier"));

        if (readMessage.getValue("MinPriceIncrement") != null)
            instrument.setTickSize(readMessage.getDouble("MinPriceIncrement"));

        if (readMessage.getValue("MaturityDate") != null)
            instrument.setMaturityDate(readMessage.getString("MaturityDate"));

        if (readMessage.getValue("MinPriceIncrement") != null
                && readMessage.getValue("MinPriceIncrementAmount") != null)
            instrument.setMultiplier(readMessage.getDouble("MinPriceIncrement")
                    / readMessage.getDouble("MinPriceIncrementAmount"));

        instrument.setTradingStatus(getTradingStatus(readMessage));

        return instrument;
    }

    private String getTradingStatus(Message readMessage) {
        StringBuilder tradingStatus = new StringBuilder();
        //tradingStatus.append(
        //        readMessage.getValue("TradingSessionID") != null ? readMessage.getInt("TradingSessionID") : "0"
        //);
        //tradingStatus.append('-');
        tradingStatus.append(
                readMessage.getValue("SecurityTradingStatus") != null ? readMessage.getInt("SecurityTradingStatus") : "NA"
        );
        return tradingStatus.toString();
    }
}

