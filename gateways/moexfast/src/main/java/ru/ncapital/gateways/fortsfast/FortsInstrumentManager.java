package ru.ncapital.gateways.fortsfast;

import com.google.inject.Singleton;
import org.openfast.Message;
import ru.ncapital.gateways.moexfast.InstrumentManager;
import ru.ncapital.gateways.moexfast.domain.Instrument;

/**
 * Created by egore on 24.12.2015.
 */

@Singleton
public class FortsInstrumentManager extends InstrumentManager {
    @Override
    protected Instrument createInstrument(Message readMessage) {
        return null;
    }

    @Override
    protected Instrument createFullInstrument(Message readMessage) {
        return null;
    }

    @Override
    protected String createTradingStatusForInstrumentStatus(Message readMessage) {
        return null;
    }

    @Override
    protected Instrument[] getInstruments() {
        return new Instrument[0];
    }
}
