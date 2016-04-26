package ru.ncapital.gateways.micexfast;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.openfast.Context;
import org.openfast.GroupValue;
import org.openfast.Message;
import org.openfast.MessageHandler;
import org.openfast.codec.Coder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ncapital.gateways.micexfast.connection.ConnectionManager;
import ru.ncapital.gateways.micexfast.connection.messageprocessors.Processor;
import ru.ncapital.gateways.micexfast.connection.messageprocessors.SequenceArray;
import ru.ncapital.gateways.micexfast.domain.BBO;
import ru.ncapital.gateways.micexfast.domain.Instrument;
import ru.ncapital.gateways.micexfast.domain.ProductType;
import ru.ncapital.gateways.micexfast.domain.TradingSessionId;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by egore on 24.12.2015.
 */

@Singleton
public class InstrumentManager extends Processor {
    private ConcurrentHashMap<String, Instrument> instruments = new ConcurrentHashMap<String, Instrument>();

    private ConcurrentHashMap<String, Instrument> ignoredInstruments = new ConcurrentHashMap<String, Instrument>();

    private Set<Integer> addedInstruments = new HashSet<Integer>();

    private int numberOfInstruments;

    private AtomicBoolean instrumentsDownloaded = new AtomicBoolean(false);

    private Set<TradingSessionId> allowedTradingSessionIds = new HashSet<TradingSessionId>();

    private Set<ProductType> allowedProductTypes = new HashSet<ProductType>();

    private Set<String> allowedSecurityIds = new HashSet<String>();

    @Inject
    private ConnectionManager connectionManager;

    @Inject
    private MarketDataManager marketDataManager;

    private IMarketDataHandler marketDataHandler;

    private long sendingTimeOfInstrumentStart;
    
    public InstrumentManager configure(IGatewayConfiguration configuration) {
        this.marketDataHandler = configuration.getMarketDataHandler();
        this.allowedTradingSessionIds.addAll(Arrays.asList(configuration.getAllowedTradingSessionIds()));
        this.allowedProductTypes.addAll(Arrays.asList(configuration.getAllowedProductTypes()));
        this.allowedSecurityIds.addAll(Arrays.asList(configuration.getAllowedSecurityIds()));
        if (allowedSecurityIds.contains("*"))
            allowedSecurityIds.clear();

        return this;
    }

    private boolean isAllowedInstrument(Instrument instrument) {
        if (instruments.containsKey(instrument.getSecurityId())) {
            if (getLogger().isTraceEnabled())
                getLogger().trace("Instrument Duplicate [Symbol: " + instrument.getSymbol() + "][TradingSessionId: " + instrument.getTradingSessionId() + "]");

            return false;
        }

        if (ignoredInstruments.containsKey(instrument.getSecurityId())) {
            if (getLogger().isTraceEnabled())
                getLogger().trace("Instrument Duplicate Ignored [Symbol: " + instrument.getSymbol() + "][TradingSessionId: " + instrument.getTradingSessionId() + "]");

            return false;
        }

        if (allowedTradingSessionIds.isEmpty() || allowedTradingSessionIds.contains(TradingSessionId.convert(instrument.getTradingSessionId()))) {
        } else {
            if (getLogger().isTraceEnabled())
                getLogger().trace("Instrument Ignored by TradingSessionId [Symbol: " + instrument.getSymbol() + "][TradingSessionId: " + instrument.getTradingSessionId() + "]");

            ignoredInstruments.put(instrument.getSecurityId(), instrument);
            return false;
        }

        if (allowedProductTypes.isEmpty() || allowedProductTypes.contains(instrument.getProductType())) {
        } else {
            if (getLogger().isTraceEnabled())
                getLogger().trace("Instrument Ignored by ProductType [SecurityId: " + instrument.getSecurityId() + "][Product: " + instrument.getProductType() + "]");

            ignoredInstruments.put(instrument.getSecurityId(), instrument);
            return false;
        }

        if (allowedSecurityIds.isEmpty() || allowedSecurityIds.contains(instrument.getSecurityId())) {
        } else {
            if (getLogger().isTraceEnabled())
                getLogger().trace("Instrument Ignored by SecurityId [SecurityId: " + instrument.getSecurityId() + "]");

            ignoredInstruments.put(instrument.getSecurityId(), instrument);
            return false;
        }

        if (instruments.putIfAbsent(instrument.getSecurityId(), instrument) == null)
            return true;

        return false;
    }

    @Override
    protected boolean checkSequence(Message readMessage) {
        int seqNum = readMessage.getInt("MsgSeqNum");
        long sendingTime = readMessage.getLong("SendingTime");

        if (seqNum == 1) {
            synchronized (this) {
                if (sendingTimeOfInstrumentStart < sendingTime) {
                    // new snapshot cycle
                    sendingTimeOfInstrumentStart = sendingTime;
                    reset();
                } else
                    return false;
            }
        } else {
            if (sequenceArray.checkSequence(seqNum) == SequenceArray.Result.DUPLICATE)
                return false;
        }

        return true;
    }

    private void reset() {
        sequenceArray.clear();
    }

    private String buildTradingStatus(Message readMessage) {
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

        return tradingStatus.toString();
    }

    @Override
    protected void processMessage(Message readMessage) {
        String symbol;
        String tradingSessionId = "";
        final Instrument instrument;

        switch (readMessage.getString("MessageType").charAt(0)) {
            case 'f':
                if (instruments.contains(readMessage.getString("Symbol") + Instrument.BOARD_SEPARATOR + readMessage.getString("TradingSessionID")))
                    getLogger().info("Received Instrument Status UPDATE " + readMessage.toString());
                break;

            case 'd':
                if (numberOfInstruments == 0) {
                    numberOfInstruments = readMessage.getInt("TotNumReports");
                    getLogger().info("EXPECTING INSTRUMENTS " + numberOfInstruments);
                } else
                    if (numberOfInstruments == (instruments.size() + ignoredInstruments.size()))
                        break;

                symbol = readMessage.getString("Symbol");
                if (readMessage.getSequence("MarketSegmentGrp") != null && readMessage.getSequence("MarketSegmentGrp").getLength() > 0) {
                    GroupValue marketSegmentGrp = readMessage.getSequence("MarketSegmentGrp").get(0);
                    if (marketSegmentGrp.getSequence("TradingSessionRulesGrp") != null && marketSegmentGrp.getSequence("TradingSessionRulesGrp").getLength() > 0) {
                        GroupValue tradingSessionRulesGrp = marketSegmentGrp.getSequence("TradingSessionRulesGrp").get(0);
                        if (tradingSessionRulesGrp.getValue("TradingSessionID") != null)
                            tradingSessionId = tradingSessionRulesGrp.getString("TradingSessionID");
                    }
                }

                instrument = new Instrument(symbol, tradingSessionId);

                if (readMessage.getValue("Product") != null) {
                    instrument.setProductType(readMessage.getInt("Product"));
                } else
                    instrument.setProductType(-1);

                if (!isAllowedInstrument(instrument))
                    break;

                if (getLogger().isDebugEnabled())
                    getLogger().debug("Instrument Received " + symbol + Instrument.BOARD_SEPARATOR + tradingSessionId + " " + ProductType.convert(readMessage.getInt("Product")));

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

                if (readMessage.getSequence("MarketSegmentGrp").get(0).getValue("RoundLot") != null)
                    instrument.setLotSize(readMessage.getSequence("MarketSegmentGrp").get(0).getInt("RoundLot"));

                instrument.setTradingStatus(buildTradingStatus(readMessage));

                // send to client
                marketDataManager.onBBO(new BBO(instrument.getSecurityId()) {
                    {
                        setTradingStatus(instrument.getTradingStatus());
                    }
                }, Utils.currentTimeInTicks());
                break;
        }

        if (instruments.size() + ignoredInstruments.size() == numberOfInstruments) {
            if (!instrumentsDownloaded.getAndSet(true)) {
                getLogger().info("FINISHED INSTRUMENTS " + instruments.size());
                connectionManager.stopInstrument();
                marketDataHandler.onInstruments(instruments.values().toArray(new Instrument[instruments.size()]));
            }
        } else {
            if ((instruments.size() + ignoredInstruments.size()) % 1000 == 0) {
                synchronized (this) {
                    if ((instruments.size() + ignoredInstruments.size()) % 1000 == 0) {
                        int added = (instruments.size() + ignoredInstruments.size()) / 1000;
                        if (addedInstruments.add(added)) {
                            getLogger().info("PROCESSED INSTRUMENTS " + (instruments.size() + ignoredInstruments.size()));
                        }
                    }
                }
            }
        }
    }
}
