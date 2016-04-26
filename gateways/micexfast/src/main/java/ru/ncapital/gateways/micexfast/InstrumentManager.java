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

    private AtomicBoolean receivedFirstInstrumentStatus = new AtomicBoolean(false);
    
    public InstrumentManager configure(IGatewayConfiguration configuration) {
        this.marketDataHandler = configuration.getMarketDataHandler();
        this.allowedTradingSessionIds.addAll(Arrays.asList(configuration.getAllowedTradingSessionIds()));
        this.allowedProductTypes.addAll(Arrays.asList(configuration.getAllowedProductTypes()));
        this.allowedSecurityIds.addAll(Arrays.asList(configuration.getAllowedSecurityIds()));
        if (allowedSecurityIds.contains("*"))
            allowedSecurityIds.clear();

        return this;
    }

    @Override
    protected boolean checkSequence(Message readMessage) {
        int seqNum = readMessage.getInt("MsgSeqNum");
        long sendingTime = readMessage.getLong("SendingTime");
        char messageType = readMessage.getString("MessageType").charAt(0);

        switch (messageType) {
            case 'd':
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
                break;

            case 'f':
                if (!receivedFirstInstrumentStatus.getAndSet(true)) {
                    // we received first instrument status
                    reset();
                }

                if (sequenceArray.checkSequence(seqNum) == SequenceArray.Result.DUPLICATE)
                    return false;

                break;
        }

        return true;
    }

    private void reset() {
        sequenceArray.clear();
    }

    private boolean addNewInstrument(Instrument instrument) {
        if (isDuplicateInstrument(instrument))
            return false;

        if (!isAllowedInstrument(instrument)) {
            ignoredInstruments.putIfAbsent(instrument.getSecurityId(), instrument);
            return false;
        }

        if (instruments.putIfAbsent(instrument.getSecurityId(), instrument) != null)
            return false;

        return true;
    }

    private boolean isDuplicateInstrument(Instrument instrument) {
        if (instruments.containsKey(instrument.getSecurityId())) {
            if (getLogger().isTraceEnabled())
                getLogger().trace("Instrument Duplicate [Symbol: " + instrument.getSymbol() + "][TradingSessionId: " + instrument.getTradingSessionId() + "]");

            return true;
        }

        if (ignoredInstruments.containsKey(instrument.getSecurityId())) {
            if (getLogger().isTraceEnabled())
                getLogger().trace("Instrument Duplicate Ignored [Symbol: " + instrument.getSymbol() + "][TradingSessionId: " + instrument.getTradingSessionId() + "]");

            return true;
        }

        return false;
    }

    public boolean isAllowedInstrument(Instrument instrument) {
        if (instruments.contains(instrument.getSecurityId()))
            return true;

        if (ignoredInstruments.contains(instrument.getSecurityId()))
            return false;

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

        return true;
    }

    private String buildTradingStatusForInstrumentStatusMessage(Message readMessage) {
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

    private String buildTradingStatusForInstrumentMessage(Message readMessage) {
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

    private void checkInstrumentFinish() {
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

    private Instrument getInstrument(Message readMessage) {
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

        Instrument instrument = new Instrument(symbol, tradingSessionId);

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

        instrument.setTradingStatus(buildTradingStatusForInstrumentMessage(readMessage));

        return instrument;
    }

    private void sendToClient(String securityId, String tradingStatus) {
        BBO tradingStatusUpdate = new BBO(securityId);
        tradingStatusUpdate.setTradingStatus(tradingStatus);
        marketDataManager.onBBO(tradingStatusUpdate, Utils.currentTimeInTicks());
    }

    @Override
    protected void processMessage(Message readMessage) {
        switch (readMessage.getString("MessageType").charAt(0)) {
            case 'f':
                Instrument instrument = new Instrument(readMessage.getString("Symbol"), readMessage.getString("TradingSessionID"));
                String tradingStatus = buildTradingStatusForInstrumentStatusMessage(readMessage);

                if (isAllowedInstrument(instrument))
                    sendToClient(instrument.getSecurityId(), tradingStatus);

                break;

            case 'd':
                if (numberOfInstruments == 0) {
                    numberOfInstruments = readMessage.getInt("TotNumReports");
                    getLogger().info("EXPECTING INSTRUMENTS " + numberOfInstruments);
                } else if (numberOfInstruments == (instruments.size() + ignoredInstruments.size()))
                    break;


                Instrument newInstrument = getInstrument(readMessage);

                if (addNewInstrument(newInstrument)) {
                    if (getLogger().isDebugEnabled())
                        getLogger().debug("Instrument Received " + newInstrument.getSecurityId() + " " + newInstrument.getProductType());

                    sendToClient(newInstrument.getSecurityId(), newInstrument.getTradingStatus());
                }

                checkInstrumentFinish();
                break;
        }
    }

    public void setMarketDataManager(MarketDataManager marketDataManager) {
        this.marketDataManager = marketDataManager;
    }

    public void setConnectionManager(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }
}
