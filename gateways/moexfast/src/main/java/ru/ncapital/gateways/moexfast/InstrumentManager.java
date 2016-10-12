package ru.ncapital.gateways.moexfast;

import org.openfast.Message;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.Processor;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.SequenceArray;
import ru.ncapital.gateways.moexfast.domain.BBO;
import ru.ncapital.gateways.moexfast.domain.Instrument;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Egor on 03-Oct-16.
 */
public abstract class InstrumentManager extends Processor {
    protected SequenceArray sequenceArrayForInstrumentStatus = new SequenceArray();

    protected ConcurrentHashMap<String, Instrument> instruments = new ConcurrentHashMap<>();

    protected ConcurrentHashMap<String, Instrument> ignoredInstruments = new ConcurrentHashMap<>();

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

    protected boolean isDuplicateInstrument(Instrument instrument) {
        if (instruments.containsKey(instrument.getSecurityId())) {
            if (getLogger().isTraceEnabled())
                getLogger().trace(instrument.getName() + " Duplicate " + instrument.getFullname());

            return true;
        }

        if (ignoredInstruments.containsKey(instrument.getSecurityId())) {
            if (getLogger().isTraceEnabled())
                getLogger().trace(instrument.getName() + " Duplicate Ignored " + instrument.getFullname());

            return true;
        }

        return false;
    }

    protected boolean addNewInstrument(Instrument instrument) {
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

    public boolean isAllowedInstrument(Instrument instrument) {
        if (instruments.containsKey(instrument.getSecurityId()))
            return true;

        if (ignoredInstruments.containsKey(instrument.getSecurityId()))
            return false;

        return true;
    }

    public boolean isAllowedInstrument(String securityId) {
        return instruments.containsKey(securityId);
    }

    @Override
    protected void processMessage(Message readMessage) {
        switch (readMessage.getString("MessageType").charAt(0)) {
            case 'f':
                Instrument instrument = createInstrument(readMessage);
                String tradingStatus = createTradingStatusForInstrumentStatus(readMessage);

                if (isAllowedInstrument(instrument.getSecurityId()))
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
                        getLogger().debug(newInstrument.getName() + " Received " + newInstrument.getFullname());

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

    protected abstract Instrument createInstrument(Message readMessage);

    protected abstract Instrument createFullInstrument(Message readMessage);

    protected abstract String createTradingStatusForInstrumentStatus(Message readMessage);

    protected abstract Instrument[] getInstruments();
}
