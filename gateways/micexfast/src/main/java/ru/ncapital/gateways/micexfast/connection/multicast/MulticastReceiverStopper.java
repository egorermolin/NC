package ru.ncapital.gateways.micexfast.connection.multicast;

import org.slf4j.Logger;

/**
 * Created by egore on 12/15/15.
 */
public class MulticastReceiverStopper implements Runnable {
    private MulticastReceiver mcr;

    public MulticastReceiverStopper(Logger logger, MulticastReceiver mcr) {
        this.mcr = mcr;

        logger.info("STOP " + mcr);
    }

    @Override
    public void run() {
        mcr.stop();
    }
}
