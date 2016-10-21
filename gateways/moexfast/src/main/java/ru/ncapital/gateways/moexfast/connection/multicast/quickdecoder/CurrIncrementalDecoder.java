package ru.ncapital.gateways.moexfast.connection.multicast.quickdecoder;

import org.openfast.*;
import org.openfast.template.LongValue;
import org.openfast.template.Sequence;
import org.openfast.template.type.codec.TypeCodec;

import java.io.InputStream;

/**
 * Created by egore on 10/3/16.
 */
public class CurrIncrementalDecoder extends QuickDecoder {

    @Override
    public FieldValue[] decodeFields(InputStream in, BitVectorReader presenceMapReader, Context context) {
        FieldValue[] values = new FieldValue[getMessageTemplate().getFieldCount()];
        boolean skip = false;

        // 1. MessageType -> set on return
        // values[1];

        // 2. ApplVerID -> constant -> skip
        // values[2];

        // 3.BeginString -> constant -> skip
        // values[3];

        // 4. SenderCompID -> constant -> skip
        // values[4];

        // 5. MsgSeqNum -> uint32 -> decode
        values[5] = new IntegerValue(TypeCodec.UINT.decode(in).toInt());

        // TODO skip base on duplicate

        // 6. SendingTime -> uint64 -> decode
        if (skip)
            skipInteger(in);
        else
            values[6] = new LongValue(TypeCodec.INTEGER.decode(in).toLong());

        // 7. GroupMDEntries -> sequence -> decode
        Sequence sequence = skip ? null : (Sequence) getMessageTemplate().getField(7);
        SequenceValue sequenceValue = skip ? null : new SequenceValue(sequence);

        int len = TypeCodec.INTEGER.decode(in).toInt();
        for (int i = 0; i < len; ++i) {
            // group pmap
            BitVector pmap = ((BitVectorValue) TypeCodec.BIT_VECTOR.decode(in)).value;
            BitVectorReader groupPmap = new BitVectorReader(pmap);

            FieldValue[] groupFields = skip ? new FieldValue[0] : new FieldValue[sequence.getGroup().getFieldCount()];
            // 1. MDUpdateAction -> uint32 -> decode
            if (skip)
                skipInteger(in);
            else {
                ScalarValue value = TypeCodec.NULLABLE_UNSIGNED_INTEGER.decode(in);
                if (value != null)
                    groupFields[0] = new IntegerValue(value.toInt());
            }

            // 2. MDEntryType -> string -> decode
            if (skip)
                skipChar(in);
            else {
                String value = decodeChar(in, true);
                if (value != null)
                    groupFields[1] =  new StringValue(value);
            }

            // 3. MDEntryID -> string -> decode
            if (skip)
                skipString(in);
            else {
                ScalarValue value = TypeCodec.NULLABLE_ASCII.decode(in);
                if (value != null)
                    groupFields[2] = new StringValue(value.toString());
            }

            // 4. Symbol -> string -> decode
            if (skip)
                skipString(in);
            else {
                ScalarValue value = TypeCodec.NULLABLE_ASCII.decode(in);
                if (value != null)
                    groupFields[3] = new StringValue(value.toString());
            }

            // TODO skip based on allowed symbol

            // 5. RptSeq -> int32 -> decode
            if (skip)
                skipInteger(in);
            else {
                ScalarValue value = TypeCodec.NULLABLE_INTEGER.decode(in);
                if (value != null)
                    groupFields[4] = new IntegerValue(value.toInt());
            }

            // 6. MDEntryDate -> uint32 -> skip
            skipInteger(in);

            // 7. MDEntryTime -> uint32 -> decode
            if (skip)
                skipInteger(in);
            else {
                ScalarValue value = TypeCodec.NULLABLE_UNSIGNED_INTEGER.decode(in);
                if (value != null)
                    groupFields[6] = new IntegerValue(value.toInt());
            }

            // 8. OrigTime -> uint32 -> decode
            if (skip)
                skipInteger(in);
            else {
                ScalarValue value = TypeCodec.NULLABLE_UNSIGNED_INTEGER.decode(in);
                if (value != null)
                    groupFields[7] = new IntegerValue(value.toInt());
            }

            // 9. MDEntryPx -> decimal -> decode
            if (skip) {
                skipInteger(in);
                skipInteger(in);
            } else {
                ScalarValue value = TypeCodec.NULLABLE_INTEGER.decode(in);
                if (value != null) {
                    int exponent = value.toInt();
                    long mantissa = TypeCodec.INTEGER.decode(in).toLong();
                    groupFields[8] = new DecimalValue(mantissa, exponent);
                }
            }

            // 10. MDEntrySize -> decimal -> decode
            if (skip) {
                skipInteger(in);
                skipInteger(in);
            } else {
                ScalarValue value = TypeCodec.NULLABLE_INTEGER.decode(in);
                if (value != null) {
                    int exponent = value.toInt();
                    long mantissa = TypeCodec.INTEGER.decode(in).toLong();
                    groupFields[9] = new DecimalValue(mantissa, exponent);
                }
            }

            // 11. DealNumber -> string -> decode
            if (groupPmap.read()) {
                if (skip)
                    skipString(in);
                else {
                    ScalarValue value = TypeCodec.NULLABLE_ASCII.decode(in);
                    if (value != null)
                        groupFields[10] = new StringValue(value.toString());
                }
            }

            // 12. OrderStatus -> string -> skip
            skipChar(in);

            // 13. TradingSessionID -> string -> decode
            if (skip)
                skipString(in);
            else {
                ScalarValue value = TypeCodec.NULLABLE_ASCII.decode(in);
                if (value != null)
                    groupFields[12] = new StringValue(value.toString());
            }

            // 14. TradingSessionSubID -> string -> decode
            if (skip)
                skipString(in);
            else {
                ScalarValue value = TypeCodec.NULLABLE_ASCII.decode(in);
                if (value != null)
                    groupFields[13] = new StringValue(value.toString());
            }

            if (!skip)
                sequenceValue.add(groupFields);
        }

        if (!skip)
            values[7] = sequenceValue;

        return values;
    }
}
