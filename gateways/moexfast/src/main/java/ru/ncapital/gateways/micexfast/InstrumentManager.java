package ru.ncapital.gateways.micexfast;

import com.google.inject.Singleton;
import org.openfast.GroupValue;
import org.openfast.Message;
import ru.ncapital.gateways.moexfast.GatewayManager;
import ru.ncapital.gateways.moexfast.IGatewayConfiguration;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.Processor;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.SequenceArray;
import ru.ncapital.gateways.micexfast.domain.*;
import ru.ncapital.gateways.moexfast.IMarketDataHandler;
import ru.ncapital.gateways.moexfast.domain.BBO;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by egore on 24.12.2015.
 */

@Singleton
public class InstrumentManager extends Processor {
    private ConcurrentHashMap<String, MicexInstrument> instruments = new ConcurrentHashMap<String, MicexInstrument>();

    private ConcurrentHashMap<String, MicexInstrument> ignoredInstruments = new ConcurrentHashMap<String, MicexInstrument>();

    private Set<Integer> addedInstruments = new HashSet<Integer>();

    private int numberOfInstruments;

    private AtomicBoolean instrumentsDownloaded = new AtomicBoolean(false);

    private Set<TradingSessionId> allowedTradingSessionIds = new HashSet<TradingSessionId>();

    private Set<ProductType> allowedProductTypes = new HashSet<ProductType>();

    private Set<String> allowedSecurityIds = new HashSet<String>();

    private MarketDataManager marketDataManager;

    private GatewayManager gatewayManager;

    private IMarketDataHandler marketDataHandler;

    private long sendingTimeOfInstrumentStart;

    protected SequenceArray sequenceArrayForInstrumentStatus = new SequenceArray();

    public InstrumentManager configure(IMicexGatewayConfiguration configuration) {
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
                if (sequenceArrayForInstrumentStatus.checkSequence(seqNum) == SequenceArray.Result.DUPLICATE)
                    return false;

                break;
        }

        return true;
    }

    private void reset() {
        sequenceArray.clear();
    }

    private boolean addNewInstrument(MicexInstrument instrument) {
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

    private boolean isDuplicateInstrument(MicexInstrument instrument) {
        if (instruments.containsKey(instrument.getSecurityId())) {
            if (getLogger().isTraceEnabled())
                getLogger().trace("MicexInstrument Duplicate [Symbol: " + instrument.getSymbol() + "][TradingSessionId: " + instrument.getTradingSessionId() + "]");

            return true;
        }

        if (ignoredInstruments.containsKey(instrument.getSecurityId())) {
            if (getLogger().isTraceEnabled())
                getLogger().trace("MicexInstrument Duplicate Ignored [Symbol: " + instrument.getSymbol() + "][TradingSessionId: " + instrument.getTradingSessionId() + "]");

            return true;
        }

        return false;
    }

    public boolean isAllowedInstrument(String securityId) {
        if (instruments.containsKey(securityId))
            return true;

        return false;
    }

    public boolean isAllowedInstrument(MicexInstrument instrument) {
        if (instruments.containsKey(instrument.getSecurityId()))
            return true;

        if (ignoredInstruments.containsKey(instrument.getSecurityId()))
            return false;

        if (instrument.getProductType() == null)
            return false;

        if (allowedTradingSessionIds.isEmpty() || allowedTradingSessionIds.contains(TradingSessionId.convert(instrument.getTradingSessionId()))) {
        } else {
            if (getLogger().isTraceEnabled())
                getLogger().trace("MicexInstrument Ignored by TradingSessionId [Symbol: " + instrument.getSymbol() + "][TradingSessionId: " + instrument.getTradingSessionId() + "]");

            ignoredInstruments.put(instrument.getSecurityId(), instrument);
            return false;
        }

        if (allowedProductTypes.isEmpty() || allowedProductTypes.contains(instrument.getProductType())) {
        } else {
            if (getLogger().isTraceEnabled())
                getLogger().trace("MicexInstrument Ignored by ProductType [SecurityId: " + instrument.getSecurityId() + "][Product: " + instrument.getProductType() + "]");

            ignoredInstruments.put(instrument.getSecurityId(), instrument);
            return false;
        }

        if (allowedSecurityIds.isEmpty() || allowedSecurityIds.contains(instrument.getSecurityId())) {
        } else {
            if (getLogger().isTraceEnabled())
                getLogger().trace("MicexInstrument Ignored by SecurityId [SecurityId: " + instrument.getSecurityId() + "]");

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

    private MicexInstrument getInstrument(Message readMessage) {
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

        instrument.setTradingStatus(buildTradingStatusForInstrumentMessage(readMessage));

        return instrument;
    }

    private void sendToClient(String securityId, String tradingStatus) {
        BBO tradingStatusUpdate = new BBO(securityId);
        tradingStatusUpdate.setTradingStatus(tradingStatus);
        marketDataManager.onBBO(tradingStatusUpdate);
    }

    @Override
    protected void processMessage(Message readMessage) {
        switch (readMessage.getString("MessageType").charAt(0)) {
            case 'f':
                MicexInstrument instrument = new MicexInstrument(readMessage.getString("Symbol"), readMessage.getString("TradingSessionID"));
                String tradingStatus = buildTradingStatusForInstrumentStatusMessage(readMessage);

                if (isAllowedInstrument(instrument))
                    sendToClient(instrument.getSecurityId(), tradingStatus);

                break;

            case 'd':
                if (numberOfInstruments == 0) {
                    synchronized (this) {
                        if (numberOfInstruments == 0) {
                            numberOfInstruments = readMessage.getInt("TotNumReports");
                            getLogger().info("EXPECTING INSTRUMENTS " + numberOfInstruments);
                        }
                    }
                } else if (numberOfInstruments == (instruments.size() + ignoredInstruments.size()))
                    break;


                MicexInstrument newInstrument = getInstrument(readMessage);

                if (addNewInstrument(newInstrument)) {
                    if (getLogger().isDebugEnabled())
                        getLogger().debug("MicexInstrument Received " + newInstrument.getSecurityId() + " " + newInstrument.getProductType());

                    sendToClient(newInstrument.getSecurityId(), newInstrument.getTradingStatus());
                }

                checkInstrumentFinish();
                break;
        }
    }

    private void checkInstrumentFinish() {
        if (instruments.size() + ignoredInstruments.size() == numberOfInstruments) {
            if (!instrumentsDownloaded.getAndSet(true)) {
                getLogger().info("FINISHED INSTRUMENTS " + instruments.size());
                gatewayManager.onInstrumentDownloadFinished(instruments.values());
                marketDataHandler.onInstruments(instruments.values().toArray(new MicexInstrument[instruments.size()]));
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

    public void setMarketDataManager(MarketDataManager marketDataManager) {
        this.marketDataManager = marketDataManager;
    }

    public void setGatewayManager(GatewayManager gatewayManager) {
        this.gatewayManager = gatewayManager;
    }
}
