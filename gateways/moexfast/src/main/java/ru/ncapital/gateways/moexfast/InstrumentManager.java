package ru.ncapital.gateways.moexfast;

import org.openfast.Message;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.Processor;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.SequenceArray;
import ru.ncapital.gateways.moexfast.domain.impl.BBO;
import ru.ncapital.gateways.moexfast.domain.intf.IInstrument;
import ru.ncapital.gateways.moexfast.domain.impl.Instrument;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Egor on 03-Oct-16.
 */
public abstract class InstrumentManager<T> extends Processor implements IInstrumentManager {
    private SequenceArray sequenceArrayForSecurityStatus = new SequenceArray();

    private ConcurrentHashMap<T, Instrument<T>> instruments = new ConcurrentHashMap<>();

    private ConcurrentHashMap<T, Instrument<T>> instrumentsBySecurityId = new ConcurrentHashMap<>();

    private ConcurrentHashMap<T, Instrument<T>> ignoredInstruments = new ConcurrentHashMap<>();

    private Set<Integer> addedInstruments = new HashSet<>();

    private AtomicBoolean instrumentsDownloaded = new AtomicBoolean(false);

    private int numberOfInstruments;

    private MarketDataManager<T> marketDataManager;

    private GatewayManager gatewayManager;

    private long timeOfLastSequenceReset;

    private IMarketDataHandler marketDataHandler;

    public InstrumentManager<T> configure(IGatewayConfiguration configuration) {
        this.marketDataHandler = configuration.getMarketDataHandler();
        return this;
    }

    public void setMarketDataManager(MarketDataManager<T> marketDataManager) {
        this.marketDataManager = marketDataManager;
    }

    public void setGatewayManager(GatewayManager gatewayManager) {
        this.gatewayManager = gatewayManager;
    }

    private synchronized boolean resetSequence(long sendingTime) {
        if (timeOfLastSequenceReset < sendingTime) {
            // new snapshot cycle
            timeOfLastSequenceReset = sendingTime;
            sequenceArray.clear();
        } else
            return false;

        return true;
    }

    @Override
    protected boolean checkSequence(Message readMessage) {
        int seqNum = readMessage.getInt("MsgSeqNum");
        long sendingTime = readMessage.getLong("SendingTime");
        char messageType = readMessage.getString("MessageType").charAt(0);

        switch (messageType) {
            case 'd':
                if (seqNum == 1)
                    if (!resetSequence(sendingTime))
                        return false;
                else
                    if (sequenceArray.checkSequence(seqNum) == SequenceArray.Result.DUPLICATE)
                        return false;

                break;

            case '4': // SequenceReset
                if (!resetSequence(sendingTime))
                    return false;

                break;

            case 'f': // SecurityStatus
                if (sequenceArrayForSecurityStatus.checkSequence(seqNum) == SequenceArray.Result.DUPLICATE)
                    return false;

                break;
        }

        return true;
    }

    protected boolean isDuplicateInstrument(Instrument<T> instrument) {
        if (instruments.containsKey(instrument.getExchangeSecurityId())) {
            if (getLogger().isTraceEnabled())
                getLogger().trace("Duplicate " + instrument.getId());

            return true;
        }

        if (ignoredInstruments.containsKey(instrument.getExchangeSecurityId())) {
            if (getLogger().isTraceEnabled())
                getLogger().trace("Duplicate Ignored " + instrument.getId());

            return true;
        }

        return false;
    }

    protected boolean addNewInstrument(Instrument<T> instrument) {
        if (isDuplicateInstrument(instrument))
            return false;

        if (!isAllowedInstrument(instrument)) {
            addInstrumentToIgnored(instrument);
            return false;
        }

        return addInstrument(instrument);
    }

    private boolean addInstrument(Instrument<T> instrument) {
        return instruments.putIfAbsent(instrument.getExchangeSecurityId(), instrument) == null
                   &&
               instrumentsBySecurityId.putIfAbsent(instrument.getExchangeSecurityId(), instrument) == null;
    }

    protected void addInstrumentToIgnored(Instrument<T> instrument) {
        ignoredInstruments.putIfAbsent(instrument.getExchangeSecurityId(), instrument);
    }

    public boolean isAllowedInstrument(T exchangeSecurityId) {
        return instruments.containsKey(exchangeSecurityId);
    }

    public boolean isAllowedInstrument(Instrument<T> instrument) {
        return instruments.containsKey(instrument.getExchangeSecurityId());
    }

    public Instrument<T> getInstrumentByExchangeSecurityId(T exchangeSecurityId) {
        return instruments.get(exchangeSecurityId);
    }

    public Instrument<T> getInstrumentBySecurityId(String securityId) {
        return instrumentsBySecurityId.get(securityId);
    }

    @Override
    protected void processMessage(Message readMessage) {
        switch (readMessage.getString("MessageType").charAt(0)) {
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


                Instrument<T> newInstrument = createFullInstrument(readMessage);
                if (addNewInstrument(newInstrument)) {
                    if (getLogger().isDebugEnabled())
                        getLogger().debug("Received " + newInstrument.getId());

                    sendToClient(newInstrument, newInstrument.getTradingStatus());
                }
                checkInstrumentFinish();
                break;

            case 'f':
                Instrument<T> instrument = createInstrument(readMessage);
                String tradingStatus = createTradingStatusForInstrumentStatus(readMessage);
                if (isAllowedInstrument(instrument))
                    sendToClient(instrument, tradingStatus);

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

    private void sendToClient(Instrument<T> instrument, String tradingStatus) {
        BBO<T> tradingStatusUpdate = marketDataManager.createBBO(instrument.getExchangeSecurityId());
        tradingStatusUpdate.setTradingStatus(tradingStatus);
        marketDataManager.onBBO(tradingStatusUpdate);
    }

    protected abstract Instrument<T> createInstrument(Message readMessage);

    protected abstract Instrument<T> createFullInstrument(Message readMessage);

    protected abstract String createTradingStatusForInstrumentStatus(Message readMessage);

    private IInstrument[] getInstruments() {
        return instruments.values().toArray(new IInstrument[instruments.size()]);
    }

    public T getExchangeSecurityId(String securityId) {
        return getInstrumentBySecurityId(securityId).getExchangeSecurityId();
    }

    public String getSecurityId(T exchangeSecurityId) {
        return getInstrumentByExchangeSecurityId(exchangeSecurityId).getSecurityId();
    }
}
