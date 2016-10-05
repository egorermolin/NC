package ru.ncapital.gateways.moexfast.connection.multicast.quickdecoder;

import org.openfast.*;
import org.openfast.error.FastConstants;
import org.openfast.template.Field;
import org.openfast.template.Group;
import org.openfast.template.MessageTemplate;

import java.io.InputStream;

/**
 * Created by egore on 10/3/16.
 */
public class HeartbeatDecoder extends QuickDecoder {
    @Override
    public FieldValue[] decodeFields(InputStream in, BitVectorReader presenceMapReader, Context context) {
        FieldValue[] values = new FieldValue[getMessageTemplate().getFieldCount()];
        // 1. MessageType -> set on return
        // values[0];

        // 2.BeginString -> constant -> skip
        // values[1];

        // 3. SenderCompID -> constant -> skip
        // values[2];

        // 4. MsgSeqNum -> decode uint32 -> skip
        // values[3];
        skipInteger(in);

        // 5. SendingTime -> uint64 -> skip
        // values[4];
        skipInteger(in);

        return values;
    }
}
