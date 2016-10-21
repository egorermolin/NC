package ru.ncapital.gateways.moexfast.connection.multicast.quickdecoder;

import org.openfast.BitVectorReader;
import org.openfast.Context;
import org.openfast.FieldValue;
import org.openfast.Global;
import org.openfast.error.FastConstants;
import org.openfast.template.MessageTemplate;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by egore on 10/3/16.
 */
public abstract class QuickDecoder {

    private MessageTemplate template;

    public void setMessageTemplate(MessageTemplate template) {
        this.template = template;
    }

    public MessageTemplate getMessageTemplate() {
        return template;
    }

    protected void skipInteger(InputStream in) {
        int byt;
        try {
            do {
                byt = in.read();
                if (byt < 0)
                    Global.handleError(FastConstants.END_OF_STREAM, "The end of the input stream has been reached.");

            } while ((byt & 0x80) == 0);
        } catch (IOException e) {
            Global.handleError(FastConstants.IO_ERROR, "A IO error has been encountered while decoding.", e);
        }
    }

    protected void skipChar(InputStream in) {
        int byt;
        try {
            byt = in.read();
            if (byt < 0)
                Global.handleError(FastConstants.END_OF_STREAM, "The end of the input stream has been reached.");
        } catch (IOException e) {
            Global.handleError(FastConstants.IO_ERROR, "A IO error has been encountered while decoding.", e);
        }
    }

    protected void skipString(InputStream in) {
        int byt;
        try {
            do {
                byt = in.read();
                if (byt < 0)
                    Global.handleError(FastConstants.END_OF_STREAM, "The end of the input stream has been reached.");
            } while ((byt & 0x80) == 0);
        } catch (IOException e) {
            Global.handleError(FastConstants.IO_ERROR, "A IO error has been encountered while decoding.", e);
        }
    }


    protected String decodeChar(InputStream in, boolean nullable) {
        byte[] value = new byte[1];
        int byt;
        try {
            byt = in.read();
            if (byt < 0)
                Global.handleError(FastConstants.END_OF_STREAM, "The end of the input stream has been reached.");

            value[0] = (byte) byt;
        } catch (IOException e) {
            Global.handleError(FastConstants.IO_ERROR, "A IO error has been encountered while decoding.", e);
        }
        value[0] &= 0x7f;
        if (value[0] == 0 && nullable)
            return null;

        return new String(value);
    }

    public abstract FieldValue[] decodeFields(InputStream in, BitVectorReader presenceMapReader, Context context);
}
