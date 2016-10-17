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

    private Set<Long> allowedSecurityIds = new HashSet<>();

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
                getLogger().trace(instrument.getName() + " Ignored by Underlying " + instrument.getId());

            addInstrumentToIgnored(instrument);
            return false;
        }

        return true;
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
    protected Instrument<Long> createInstrument(Message readMessage) {
        return new FortsInstrument(readMessage.getString("Symbol"), readMessage.getLong("SecurityId"));
    }

    @Override
    protected Instrument<Long> createFullInstrument(Message readMessage) {
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
        if (readMessage.getValue("SecurityTradingStatus") != null)
            tradingStatus.append(readMessage.getInt("SecurityTradingStatus"));
        else
            tradingStatus.append("20");
        instrument.setTradingStatus(tradingStatus.toString());

        return instrument;
    }
}

