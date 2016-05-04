package ru.ncapital.gateways.micexfast.connection.multicast;

import org.openfast.Message;
import org.openfast.MessageBlockReader;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by egore on 04.02.2016.
 */
public class MicexFastMessageBlockReader implements MessageBlockReader {
    private volatile boolean isFinished = false;

    private final IMulticastEventListener eventListener;

    MicexFastMessageBlockReader(IMulticastEventListener eventListener) {
        this.eventListener = eventListener;
    }

    @Override
    public boolean readBlock(InputStream in) {
        if (isFinished) {
            isFinished = false;
            return false;
        }
        try {
            in.read(new byte[4]);
        } catch (IOException e) {
            eventListener.onException(e);
            return false;
        }
        return true;
    }

    @Override
    public void messageRead(InputStream in, Message message) {
        try {
            if (in.available() == 0)
                isFinished = true;
        } catch (IOException e) {
            eventListener.onException(e);
        }
    }
}
