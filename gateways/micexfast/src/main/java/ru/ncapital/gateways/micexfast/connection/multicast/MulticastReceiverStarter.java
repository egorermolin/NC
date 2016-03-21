package ru.ncapital.gateways.micexfast.connection.multicast;

import org.slf4j.Logger;

/**
 * Created by egore on 12/15/15.
 */
public class MulticastReceiverStarter implements Runnable {
    private MulticastReceiver mcr;

    public MulticastReceiverStarter(Logger logger, MulticastReceiver mcr) {
        this.mcr = mcr;

        logger.info("START " + mcr);
    }

    @Override
    public void run() {
        mcr.start();
    }
}
