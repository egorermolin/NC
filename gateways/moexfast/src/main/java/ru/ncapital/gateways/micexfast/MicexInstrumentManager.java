package ru.ncapital.gateways.micexfast;

import com.google.inject.Singleton;
import org.openfast.GroupValue;
import org.openfast.Message;
import ru.ncapital.gateways.micexfast.domain.MicexInstrument;
import ru.ncapital.gateways.micexfast.domain.ProductType;
import ru.ncapital.gateways.micexfast.domain.TradingSessionId;
import ru.ncapital.gateways.moexfast.IGatewayConfiguration;
import ru.ncapital.gateways.moexfast.InstrumentManager;
import ru.ncapital.gateways.moexfast.domain.Instrument;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by egore on 24.12.2015.
 */

@Singleton
public class MicexInstrumentManager extends InstrumentManager {
    private Set<TradingSessionId> allowedTradingSessionIds = new HashSet<TradingSessionId>();

    private Set<ProductType> allowedProductTypes = new HashSet<ProductType>();

    @Override
    public InstrumentManager configure(IGatewayConfiguration configuration) {
        IMicexGatewayConfiguration micexConfiguration = (IMicexGatewayConfiguration) configuration;

        this.allowedTradingSessionIds.addAll(Arrays.asList(micexConfiguration.getAllowedTradingSessionIds()));
        this.allowedProductTypes.addAll(Arrays.asList(micexConfiguration.getAllowedProductTypes()));
        this.allowedSecurityIds.addAll(Arrays.asList(micexConfiguration.getAllowedSecurityIds()));
        if (allowedSecurityIds.contains("*"))
            allowedSecurityIds.clear();

        return super.configure(configuration);
    }

    @Override
    public boolean isAllowedInstrument(Instrument instrument) {
        if (!super.isAllowedInstrument(instrument))
            return false;

        MicexInstrument micexInstrument = (MicexInstrument) instrument;

        if (allowedTradingSessionIds.isEmpty() || allowedTradingSessionIds.contains(TradingSessionId.convert(micexInstrument.getTradingSessionId()))) {
        } else {
            if (getLogger().isTraceEnabled())
                getLogger().trace(instrument.getName() + " Ignored by TradingSessionId " + instrument.getFullname());

            ignoredInstruments.put(instrument.getSecurityId(), instrument);
            return false;
        }

        if (micexInstrument.getProductType() == null)
            return false;

        if (allowedProductTypes.isEmpty() || allowedProductTypes.contains(micexInstrument.getProductType())) {
        } else {
            if (getLogger().isTraceEnabled())
                getLogger().trace(instrument.getName() + " Ignored by ProductType " + instrument.getFullname());

            ignoredInstruments.put(instrument.getSecurityId(), instrument);
            return false;
        }

        return true;
    }

    @Override
    protected String createTradingStatusForInstrumentStatus(Message readMessage) {
        StringBuilder tradingStatus = new StringBuilder();
        if (readMessage != null && readMessage.getValue("TradingSessionSubID") != null)
            tradingStatus.append(readMessage.getString("TradingSessionSubID")).append("-");
        else
            tradingStatus.append("NA-");

        if (readMessage != null && readMessage.getValue("SecurityTradingStatus") != null)
            tradingStatus.append(readMessage.getInt("SecurityTradingStatus"));
        else
            tradingStatus.append("18");

        return tradingStatus.toString();
    }

    @Override
    protected Instrument[] getInstruments() {
        return instruments.values().toArray(new MicexInstrument[instruments.size()]);
    }

    @Override
    public Instrument createInstrument(Message readMessage) {
        return new MicexInstrument(readMessage.getString("Symbol"), readMessage.getString("TradingSessionID"));
    }

    @Override
    public Instrument createFullInstrument(Message readMessage) {
        String symbol = readMessage.getString("Symbol");
        String tradingSessionId = "UNKNOWN";

        if (readMessage.getSequence("MarketSegmentGrp") != null && readMessage.getSequence("MarketSegmentGrp").getLength() > 0) {
            GroupValue marketSegmentGrp = readMessage.getSequence("MarketSegmentGrp").get(0);
            if (marketSegmentGrp.getSequence("TradingSessionRulesGrp") != null && marketSegmentGrp.getSequence("TradingSessionRulesGrp").getLength() > 0) {
                GroupValue tradingSessionRulesGrp = marketSegmentGrp.getSequence("TradingSessionRulesGrp").get(0);
                if (tradingSessionRulesGrp.getValue("TradingSessionID") != null)
                    tradingSessionId = tradingSessionRulesGrp.getString("TradingSessionID");
            }
        }

        MicexInstrument instrument = new MicexInstrument(symbol, tradingSessionId);
        if (readMessage.getValue("Product") != null)
            instrument.setProductType(readMessage.getInt("Product"));
        else
            instrument.setProductType(-1);

        if (readMessage.getValue("Currency") != null)
            instrument.setCurrency(readMessage.getString("Currency"));
        else
            instrument.setCurrency("RUB");

        if (readMessage.getValue("EncodedShortSecurityDesc") != null)
            instrument.setUnderlying(readMessage.getString("EncodedShortSecurityDesc"));

        if (readMessage.getValue("SecurityDesc") != null)
            instrument.setDescription(readMessage.getString("SecurityDesc"));

        if (readMessage.getValue("MinPriceIncrement") != null) {
            instrument.setTickSize(readMessage.getDouble("MinPriceIncrement"));
            instrument.setMultiplier(readMessage.getDouble("FaceValue") / instrument.getTickSize());
        }

        if (readMessage.getSequence("MarketSegmentGrp") != null && readMessage.getSequence("MarketSegmentGrp").getLength() > 0) {
            if (readMessage.getSequence("MarketSegmentGrp").get(0).getValue("RoundLot") != null)
                instrument.setLotSize(readMessage.getSequence("MarketSegmentGrp").get(0).getInt("RoundLot"));
        }

        GroupValue tradingSession = null;
        if (readMessage.getSequence("MarketSegmentGrp") != null && readMessage.getSequence("MarketSegmentGrp").getLength() > 0) {
            GroupValue marketSegmentGrp = readMessage.getSequence("MarketSegmentGrp").get(0);
            if (marketSegmentGrp.getSequence("TradingSessionRulesGrp") != null && marketSegmentGrp.getSequence("TradingSessionRulesGrp").getLength() > 0) {
                tradingSession = marketSegmentGrp.getSequence("TradingSessionRulesGrp").get(0);
            }
        }

        StringBuilder tradingStatus = new StringBuilder();
        if (tradingSession != null && tradingSession.getValue("TradingSessionSubID") != null)
            tradingStatus.append(tradingSession.getString("TradingSessionSubID")).append("-");
        else
            tradingStatus.append("NA-");

        if (tradingSession != null && tradingSession.getValue("SecurityTradingStatus") != null)
            tradingStatus.append(tradingSession.getInt("SecurityTradingStatus"));
        else
            tradingStatus.append("18");

        instrument.setTradingStatus(tradingStatus.toString());

        return instrument;
    }
}
