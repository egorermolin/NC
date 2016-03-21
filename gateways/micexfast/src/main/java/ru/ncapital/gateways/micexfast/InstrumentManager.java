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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by egore on 24.12.2015.
 */

@Singleton
public class InstrumentManager implements MessageHandler {
    private Map<String, Instrument> instruments = new ConcurrentHashMap<String, Instrument>();

    private Set<String> ignoredSymbols = new HashSet<String>();

    private Logger logger = LoggerFactory.getLogger("InstrumentManager");

    @Inject
    private ConnectionManager connectionManager;

    private int numberOfInstruments;

    private AtomicBoolean instrumentsDownloaded = new AtomicBoolean(false);

    private Set<TradingSessionId> allowedTradingSessionIds = new HashSet<TradingSessionId>();

    private Set<ProductType> allowedProductTypes = new HashSet<ProductType>();

    private Set<String> allowedSymbols = new HashSet<String>();

    @Inject
    private MarketDataManager marketDataManager;

    private IMarketDataHandler marketDataHandler;

    public InstrumentManager configure(IGatewayConfiguration configuration) {
        this.marketDataHandler = configuration.getMarketDataHandler();
        this.allowedTradingSessionIds.addAll(Arrays.asList(configuration.getAllowedTradingSessionIds(configuration.getMarketType())));
        this.allowedProductTypes.addAll(Arrays.asList(configuration.getAllowedProductTypes(configuration.getMarketType())));
        this.allowedSymbols.addAll(Arrays.asList(configuration.getAllowedSymbols(configuration.getMarketType())));
        if (allowedSymbols.contains("*"))
            allowedSymbols.clear();

        return this;
    }

    @Override
    public void handleMessage(Message readMessage, Context context, Coder coder) {
        String symbol;
        String tradingSessionId;
        Instrument instrument;
        final StringBuilder tradingStatus = new StringBuilder();

        switch (readMessage.getString("MessageType").charAt(0)) {
            case 'f':
                if (logger.isTraceEnabled())
                    logger.trace("Instrument Status Message Received " + readMessage.getInt("MsgSeqNum"));

                symbol = readMessage.getString("Symbol");
                tradingSessionId = readMessage.getString("TradingSessionID");

                if (allowedTradingSessionIds.contains(TradingSessionId.convert(tradingSessionId))) {

                } else {
                    if (logger.isTraceEnabled())
                        logger.trace("Instrument Status Ignored " + symbol + ":" + tradingSessionId);

                    break;
                }

                if (logger.isTraceEnabled())
                    logger.trace("Instrument Status Received " + symbol + ":" + tradingSessionId);

                instrument = instruments.get(symbol);
                if (instrument == null)
                    break;

                if (readMessage.getValue("TradingSessionSubID") != null)
                    tradingStatus.append(readMessage.getString("TradingSessionSubID")).append("-");
                else
                    tradingStatus.append("NA-");

                if (readMessage.getValue("SecurityTradingStatus") != null)
                    tradingStatus.append(readMessage.getInt("SecurityTradingStatus"));
                else
                    tradingStatus.append("18");
                instrument.setTradingStatus(tradingStatus.toString());

                // send to client
                marketDataManager.onBBO(new BBO(symbol) {
                    {
                        setTradingStatus(tradingStatus.toString());
                    }
                }, Utils.currentTimeInTicks());
                break;

            case 'd':
                if (logger.isTraceEnabled())
                    logger.trace("Instrument Message Received " + readMessage.getInt("MsgSeqNum"));

                if (numberOfInstruments == 0)
                    numberOfInstruments = readMessage.getInt("TotNumReports");
                else if (numberOfInstruments == instruments.size())
                    break;

                symbol = readMessage.getString("Symbol");
                GroupValue tradingSession = readMessage.getSequence("MarketSegmentGrp").get(0)
                                                       .getSequence("TradingSessionRulesGrp").get(0);

                tradingSessionId = tradingSession.getString("TradingSessionID");

                if (allowedTradingSessionIds.isEmpty() || allowedTradingSessionIds.contains(TradingSessionId.convert(tradingSessionId))) {
                } else {
                    if (logger.isDebugEnabled())
                        logger.debug("Instrument Filtered " + symbol + ":" + tradingSessionId);

                    ignoredSymbols.add(symbol + ":" + tradingSessionId);
                    break;
                }

                if (allowedProductTypes.isEmpty() || allowedProductTypes.contains(ProductType.convert(readMessage.getInt("Product")))) {
                } else {
                    if (logger.isDebugEnabled())
                        logger.debug("Instrument Filtered by ProductType" + symbol + ":" + tradingSessionId + ":" + readMessage.getInt("Product"));

                    ignoredSymbols.add(symbol + ":" + tradingSessionId);
                    break;
                }

                if (allowedSymbols.isEmpty() || allowedSymbols.contains(symbol)) {
                } else {
                    if (logger.isTraceEnabled())
                        logger.trace("Instrument Filtered by Symbol" + symbol);

                    ignoredSymbols.add(symbol + ":" + tradingSessionId);
                    break;
                }

                if (logger.isDebugEnabled())
                    logger.debug("Instrument Received " + symbol + ":" + tradingSessionId + " " + ProductType.convert(readMessage.getInt("Product")));

                instrument = new Instrument(symbol + ":" + tradingSessionId);
                if (instruments.containsKey(instrument.getSymbol()))
                    break;

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
                instruments.put(instrument.getSymbol(), instrument);

                // send to client
                marketDataManager.onBBO(new BBO(symbol) {
                    {
                        setTradingStatus(tradingStatus.toString());
                    }
                }, Utils.currentTimeInTicks());

                if (logger.isTraceEnabled())
                    logger.trace("ADDED INSTRUMENT " + instrument.getSymbol());

                break;
        }

        if (instruments.size() + ignoredSymbols.size() == numberOfInstruments) {
            if (!instrumentsDownloaded.getAndSet(true)) {
                logger.info("FINISHED INSTRUMENTS " + numberOfInstruments);
                connectionManager.stopInstrument();
                marketDataHandler.onInstruments(instruments.values().toArray(new Instrument[instruments.size()]));
            }
        }
    }
}
