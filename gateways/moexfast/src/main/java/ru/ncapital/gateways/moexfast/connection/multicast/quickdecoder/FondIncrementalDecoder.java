package ru.ncapital.gateways.moexfast.connection.multicast.quickdecoder;

import org.openfast.BitVectorReader;
import org.openfast.Context;
import org.openfast.FieldValue;

import java.io.InputStream;

/**
 * Created by egore on 10/3/16.
 */
public class FondIncrementalDecoder extends QuickDecoder {
    @Override
    public FieldValue[] decodeFields(InputStream in, BitVectorReader presenceMapReader, Context context) {
        return new FieldValue[0];
    }
}
