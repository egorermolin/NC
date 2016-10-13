package ru.ncapital.gateways.moexfast;

import org.openfast.Message;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.Processor;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.SequenceArray;
import ru.ncapital.gateways.moexfast.domain.BBO;
import ru.ncapital.gateways.moexfast.domain.IInstrument;
import ru.ncapital.gateways.moexfast.domain.Instrument;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Egor on 03-Oct-16.
 */
public abstract class InstrumentManager<T> extends Processor {
    protected SequenceArray sequenceArrayForInstrumentStatus = new SequenceArray();

    protected ConcurrentHashMap<T, Instrument<T>> instruments = new ConcurrentHashMap<>();

    protected ConcurrentHashMap<T, Instrument<T>> ignoredInstruments = new ConcurrentHashMap<>();

    private Set<Integer> addedInstruments = new HashSet<>();

    private AtomicBoolean instrumentsDownloaded = new AtomicBoolean(false);

    private int numberOfInstruments;

    private MarketDataManager marketDataManager;

    private GatewayManager gatewayManager;

    private long sendingTimeOfInstrumentStart;

    protected IMarketDataHandler marketDataHandler;

    public InstrumentManager configure(IGatewayConfiguration configuration) {
        this.marketDataHandler = configuration.getMarketDataHandler();
        return this;
    }

    public void setMarketDataManager(MarketDataManager marketDataManager) {
        this.marketDataManager = marketDataManager;
    }

    public void setGatewayManager(GatewayManager gatewayManager) {
        this.gatewayManager = gatewayManager;
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
                            sequenceArray.clear();
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

    protected boolean isDuplicateInstrument(Instrument<T> instrument) {
        if (instruments.containsKey(instrument.getExchangeSecurityId())) {
            if (getLogger().isTraceEnabled())
                getLogger().trace(instrument.getName() + " Duplicate " + instrument.getId());

            return true;
        }

        if (ignoredInstruments.containsKey(instrument.getExchangeSecurityId())) {
            if (getLogger().isTraceEnabled())
                getLogger().trace(instrument.getName() + " Duplicate Ignored " + instrument.getId());

            return true;
        }

        return false;
    }

    protected boolean addNewInstrument(Instrument<T> instrument) {
        if (isDuplicateInstrument(instrument))
            return false;

        if (!isAllowedInstrument(instrument)) {
            ignoredInstruments.putIfAbsent(instrument.getExchangeSecurityId(), instrument);
            return false;
        }

        if (instruments.putIfAbsent(instrument.getExchangeSecurityId(), instrument) != null)
            return false;

        return true;
    }

    public boolean isAllowedInstrument(T exchangeSecurityId) {
        return instruments.containsKey(exchangeSecurityId);
    }

    public boolean isAllowedInstrument(Instrument<T> instrument) {
        if (instruments.containsKey(instrument.getExchangeSecurityId()))
            return true;

        if (ignoredInstruments.containsKey(instrument.getExchangeSecurityId()))
            return false;

        return false;
    }

    @Override
    protected void processMessage(Message readMessage) {
        switch (readMessage.getString("MessageType").charAt(0)) {
            case 'f':
                Instrument<T> instrument = createInstrument(readMessage);
                String tradingStatus = createTradingStatusForInstrumentStatus(readMessage);

                if (isAllowedInstrument(instrument.getExchangeSecurityId()))
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


                Instrument newInstrument = createFullInstrument(readMessage);

                if (addNewInstrument(newInstrument)) {
                    if (getLogger().isDebugEnabled())
                        getLogger().debug(newInstrument.getName() + " Received " + newInstrument.getId());

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
                gatewayManager.onInstrumentDownloadFinished();
                marketDataHandler.onInstruments(getInstruments());
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

    private void sendToClient(String securityId, String tradingStatus) {
        BBO tradingStatusUpdate = new BBO(securityId);
        tradingStatusUpdate.setTradingStatus(tradingStatus);
        marketDataManager.onBBO(tradingStatusUpdate);
    }

    protected abstract Instrument<T> createInstrument(Message readMessage);

    protected abstract Instrument<T> createFullInstrument(Message readMessage);

    protected abstract String createTradingStatusForInstrumentStatus(Message readMessage);

    protected IInstrument[] getInstruments() {
        return instruments.values().toArray(new IInstrument[instruments.size()]);
    }
}
