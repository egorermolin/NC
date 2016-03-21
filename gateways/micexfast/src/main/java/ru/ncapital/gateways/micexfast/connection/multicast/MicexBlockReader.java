package ru.ncapital.gateways.micexfast.connection.multicast;

import org.openfast.Message;
import org.openfast.MessageBlockReader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.AsynchronousCloseException;

/**
 * Created by egore on 04.02.2016.
 */
public class MicexBlockReader implements MessageBlockReader {
        private volatile boolean isFinished = false;

        @Override
        public boolean readBlock(InputStream in) {
            if (isFinished) {
                isFinished = false;
                return false;
            }

            try {
                in.read(new byte[4]);
            } catch (AsynchronousCloseException e) {
                return false;
            } catch (IOException e) {
                e.printStackTrace();
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
                e.printStackTrace();
            }
        }
}
