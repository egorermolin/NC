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
public class InstrumentManager implements MessageHandler {
    private Map<String, Instrument> instruments = new ConcurrentHashMap<String, Instrument>();

    private Map<String, Instrument> ignoredInstruments = new ConcurrentHashMap<String, Instrument>();

    private Set<Integer> addedInstruments = new HashSet<Integer>();

    private Logger logger = LoggerFactory.getLogger("InstrumentManager");

    private int numberOfInstruments;

    private AtomicBoolean instrumentsDownloaded = new AtomicBoolean(false);

    private Set<TradingSessionId> allowedTradingSessionIds = new HashSet<TradingSessionId>();

    private Set<ProductType> allowedProductTypes = new HashSet<ProductType>();

    private Set<String> allowedSecurityIds = new HashSet<String>();

    private boolean addBoardToSecurityId;

    @Inject
    private ConnectionManager connectionManager;

    @Inject
    private MarketDataManager marketDataManager;

    private IMarketDataHandler marketDataHandler;

    public InstrumentManager configure(IGatewayConfiguration configuration) {
        this.marketDataHandler = configuration.getMarketDataHandler();
        this.addBoardToSecurityId = configuration.addBoardToSecurityId();
        this.allowedTradingSessionIds.addAll(Arrays.asList(configuration.getAllowedTradingSessionIds()));
        this.allowedProductTypes.addAll(Arrays.asList(configuration.getAllowedProductTypes()));
        this.allowedSecurityIds.addAll(Arrays.asList(configuration.getAllowedSecurityIds()));
        if (allowedSecurityIds.contains("*"))
            allowedSecurityIds.clear();

        return this;
    }

    private boolean checkAndAddInstrument(Instrument instrument) {
        if (instruments.containsKey(instrument.getSecurityId()))
            return false;

        if (ignoredInstruments.containsKey(instrument.getSecurityId()))
            return false;

        if (allowedTradingSessionIds.isEmpty() || allowedTradingSessionIds.contains(TradingSessionId.convert(instrument.getTradingSessionId()))) {
        } else {
            if (logger.isDebugEnabled())
                logger.debug("Instrument Filtered by TradingSessionId [Symbol: " + instrument.getSymbol() + "][TradingSessionId: " + instrument.getTradingSessionId() + "]");

            ignoredInstruments.put(instrument.getSecurityId(), instrument);
            return false;
        }

        if (allowedProductTypes.isEmpty() || allowedProductTypes.contains(instrument.getProductType())) {
        } else {
            if (logger.isDebugEnabled())
                logger.debug("Instrument Filtered by ProductType [SecurityId: " + instrument.getSecurityId() + "][Product: " + instrument.getProductType().getDescription());

            ignoredInstruments.put(instrument.getSecurityId(), instrument);
            return false;
        }

        if (allowedSecurityIds.isEmpty() || allowedSecurityIds.contains(instrument.getSecurityId())) {
        } else {
            if (logger.isTraceEnabled())
                logger.trace("Instrument Filtered by SecurityId [SecurityId: " + instrument.getSecurityId() + "]");

            ignoredInstruments.put(instrument.getSecurityId(), instrument);
            return false;
        }

        if (instruments.putIfAbsent(instrument.getSecurityId(), instrument) == null)
            return true;

        return false;
    }

    @Override
    public void handleMessage(Message readMessage, Context context, Coder coder) {
        String symbol;
        String tradingSessionId;
        String securityId;
        Instrument instrument;
        final StringBuilder tradingStatus = new StringBuilder();

        switch (readMessage.getString("MessageType").charAt(0)) {
            case 'f':
                // TODO not supported
                break;

            case 'd':
                if (logger.isTraceEnabled())
                    logger.trace("Instrument Message Received " + readMessage.getInt("MsgSeqNum"));

                if (numberOfInstruments == 0)
                    numberOfInstruments = readMessage.getInt("TotNumReports");
                else if (numberOfInstruments == instruments.size())
                    break;

                GroupValue tradingSession = readMessage.getSequence("MarketSegmentGrp").get(0)
                                                       .getSequence("TradingSessionRulesGrp").get(0);

                symbol = readMessage.getString("Symbol");
                tradingSessionId = tradingSession.getString("TradingSessionID");

                instrument = new Instrument(symbol, tradingSessionId, addBoardToSecurityId);
                if (!checkAndAddInstrument(instrument))
                    break;

                if (logger.isDebugEnabled())
                    logger.debug("Instrument Received " + symbol + ":" + tradingSessionId + " " + ProductType.convert(readMessage.getInt("Product")));

                if (readMessage.getValue("Currency") != null)
                    instrument.setCurrency(readMessage.getString("Currency"));
                else
                    instrument.setCurrency("RUB");

                instrument.setProductType(readMessage.getInt("Product"));
                instrument.setUnderlying(readMessage.getString("EncodedShortSecurityDesc"));
                instrument.setDescription(readMessage.getString("SecurityDesc"));
                if (readMessage.getValue("MinPriceIncrement") != null) {
                    instrument.setTickSize(readMessage.getDouble("MinPriceIncrement"));
                    instrument.setMultiplier(readMessage.getDouble("FaceValue") / instrument.getTickSize());
                }
                if (readMessage.getSequence("MarketSegmentGrp").get(0).getValue("RoundLot") != null)
                    instrument.setLotSize(readMessage.getSequence("MarketSegmentGrp").get(0).getInt("RoundLot"));
                if (tradingSession.getValue("TradingSessionSubID") != null)
                    tradingStatus.append(tradingSession.getString("TradingSessionSubID")).append("-");
                else
                    tradingStatus.append("NA-");
                if (tradingSession.getValue("SecurityTradingStatus") != null)
                    tradingStatus.append(tradingSession.getInt("SecurityTradingStatus"));
                else
                    tradingStatus.append("18");
                instrument.setTradingStatus(tradingStatus.toString());

                // send to client
                marketDataManager.onBBO(new BBO(instrument.getSecurityId()) {
                    {
                        setTradingStatus(tradingStatus.toString());
                    }
                }, Utils.currentTimeInTicks());
                break;
        }

        if (instruments.size() + ignoredInstruments.size() == numberOfInstruments) {
            if (!instrumentsDownloaded.getAndSet(true)) {
                logger.info("FINISHED INSTRUMENTS " + (numberOfInstruments - ignoredInstruments.size()));
                connectionManager.stopInstrument();
                marketDataHandler.onInstruments(instruments.values().toArray(new Instrument[instruments.size()]));
            }
        } else {
            if (instruments.size() + ignoredInstruments.size() % 1000 == 0) {
                synchronized (this) {
                    if (instruments.size() + ignoredInstruments.size() % 1000 == 0) {
                        int added = (instruments.size() + ignoredInstruments.size()) / 1000;
                        if (addedInstruments.add(added)) {
                            logger.info("ADDED INSTRUMENTS " + added * 1000);
                        }
                    }
                }
            }
        }
    }
}
