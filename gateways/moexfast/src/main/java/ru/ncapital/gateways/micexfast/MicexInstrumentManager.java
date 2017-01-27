package ru.ncapital.gateways.micexfast;

import com.google.inject.Singleton;
import org.openfast.GroupValue;
import org.openfast.Message;
import ru.ncapital.gateways.micexfast.domain.MicexInstrument;
import ru.ncapital.gateways.micexfast.domain.ProductType;
import ru.ncapital.gateways.micexfast.domain.TradingSessionId;
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
public class MicexInstrumentManager extends InstrumentManager<String> {
    private Set<TradingSessionId> allowedTradingSessionIds = new HashSet<>();

    private Set<ProductType> allowedProductTypes = new HashSet<>();

    private Set<String> allowedSecurityIds = new HashSet<>();

    @Override
    public InstrumentManager<String> configure(IGatewayConfiguration configuration) {
        IMicexGatewayConfiguration micexConfiguration = (IMicexGatewayConfiguration) configuration;

        this.allowedTradingSessionIds.addAll(Arrays.asList(micexConfiguration.getAllowedTradingSessionIds()));
        this.allowedProductTypes.addAll(Arrays.asList(micexConfiguration.getAllowedProductTypes()));
        this.allowedSecurityIds.addAll(Arrays.asList(micexConfiguration.getAllowedSecurityIds()));
        if (allowedSecurityIds.contains("*"))
            allowedSecurityIds.clear();

        return super.configure(configuration);
    }

    @Override
    public String getExchangeSecurityId(String securityId) {
        return securityId;
    }

    @Override
    public String getSecurityId(String exchangeSecurityId) {
        return exchangeSecurityId;
    }

    @Override
    public boolean isAllowedInstrument(Instrument<String> instrument) {
        if (super.isAllowedInstrument(instrument))
            return true;

        if (allowedSecurityIds.isEmpty() || allowedSecurityIds.contains(instrument.getSecurityId())) {
        } else {
            if (getLogger().isTraceEnabled())
                getLogger().trace("Ignored by SecurityId: " + instrument.getId());

            addInstrumentToIgnored(instrument);
            return false;
        }

        MicexInstrument micexInstrument = (MicexInstrument) instrument;
        if (allowedTradingSessionIds.isEmpty() || allowedTradingSessionIds.contains(TradingSessionId.convert(micexInstrument.getTradingSessionId()))) {
        } else {
            if (getLogger().isTraceEnabled())
                getLogger().trace("Ignored by TradingSessionId: " + instrument.getId() + ", " + micexInstrument.getTradingSessionId());

            addInstrumentToIgnored(instrument);
            return false;
        }

        if (micexInstrument.getProductType() == null)
            return false;

        if (allowedProductTypes.isEmpty() || allowedProductTypes.contains(micexInstrument.getProductType())) {
        } else {
            if (getLogger().isTraceEnabled())
                getLogger().trace("Ignored by ProductType: " + instrument.getId() + ", " + micexInstrument.getProductType());

            addInstrumentToIgnored(instrument);
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
    protected Instrument<String> createInstrument(Message readMessage) {
        return new MicexInstrument(readMessage.getString("Symbol"), readMessage.getString("TradingSessionID"), -1);
    }

    private String getTradingStatus(Message readMessage) {
        StringBuilder tradingStatus = new StringBuilder();
        GroupValue tradingSession = null;
        if (readMessage.getSequence("MarketSegmentGrp") != null && readMessage.getSequence("MarketSegmentGrp").getLength() > 0) {
            GroupValue marketSegmentGrp = readMessage.getSequence("MarketSegmentGrp").get(0);
            if (marketSegmentGrp.getSequence("TradingSessionRulesGrp") != null && marketSegmentGrp.getSequence("TradingSessionRulesGrp").getLength() > 0) {
                tradingSession = marketSegmentGrp.getSequence("TradingSessionRulesGrp").get(0);
            }
        }
        if (tradingSession != null && tradingSession.getValue("TradingSessionSubID") != null)
            tradingStatus.append(tradingSession.getString("TradingSessionSubID")).append("-");
        else
            tradingStatus.append("NA-");
        if (tradingSession != null && tradingSession.getValue("SecurityTradingStatus") != null)
            tradingStatus.append(tradingSession.getInt("SecurityTradingStatus"));
        else
            tradingStatus.append("18");
        return tradingStatus.toString();
    }

    @Override
    protected Instrument<String> createFullInstrument(Message readMessage) {
        String tradingSessionId = "UNKNOWN";
        if (readMessage.getSequence("MarketSegmentGrp") != null && readMessage.getSequence("MarketSegmentGrp").getLength() > 0) {
            GroupValue marketSegmentGrp = readMessage.getSequence("MarketSegmentGrp").get(0);
            if (marketSegmentGrp.getSequence("TradingSessionRulesGrp") != null && marketSegmentGrp.getSequence("TradingSessionRulesGrp").getLength() > 0) {
                GroupValue tradingSessionRulesGrp = marketSegmentGrp.getSequence("TradingSessionRulesGrp").get(0);
                if (tradingSessionRulesGrp.getValue("TradingSessionID") != null)
                    tradingSessionId = tradingSessionRulesGrp.getString("TradingSessionID");
            }
        }

        Instrument<String> instrument = new MicexInstrument(
                readMessage.getString("Symbol"),
                tradingSessionId,
                readMessage.getValue("Product") != null ? readMessage.getInt("Product") : -1
        );

        if (readMessage.getValue("Currency") != null)
            instrument.setCurrency(readMessage.getString("Currency"));
        else
            instrument.setCurrency("RUB");

        if (readMessage.getValue("EncodedShortSecurityDesc") != null)
            instrument.setUnderlying(readMessage.getString("EncodedShortSecurityDesc"));

        if (readMessage.getValue("SecurityDesc") != null)
            instrument.setDescription(readMessage.getString("SecurityDesc"));

        if (readMessage.getValue("MinPriceIncrement") != null) {
            instrument.setTickSize(readMessage.getBigDecimal("MinPriceIncrement").doubleValue());
            instrument.setMultiplier(readMessage.getBigDecimal("FaceValue").doubleValue() / instrument.getTickSize());
        }

        if (readMessage.getSequence("MarketSegmentGrp") != null && readMessage.getSequence("MarketSegmentGrp").getLength() > 0) {
            if (readMessage.getSequence("MarketSegmentGrp").get(0).getValue("RoundLot") != null)
                instrument.setLotSize(readMessage.getSequence("MarketSegmentGrp").get(0).getInt("RoundLot"));
        }

        instrument.setTradingStatus(getTradingStatus(readMessage));

        return instrument;
    }
}
