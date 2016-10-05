package ru.ncapital.gateways.moexfast.connection.multicast.quickdecoder;

import org.openfast.*;
import org.openfast.codec.FastDecoder;
import org.openfast.error.FastException;
import org.openfast.template.MessageTemplate;
import org.openfast.template.type.codec.TypeCodec;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by egore on 10/3/16.
 */
public class QuickDecoderMessageInputStream extends MessageInputStream {
    private InputStream in;

    private Map templateHandlers = Collections.EMPTY_MAP;

    private MessageBlockReader blockReader = MessageBlockReader.NULL;

    private Map<MessageTemplate, QuickDecoder> quickDecoders = Collections.EMPTY_MAP;

    public QuickDecoderMessageInputStream(InputStream inputStream) {
        super(inputStream, new Context());
        this.in = inputStream;
    }

    public Message decodeMessage() {
        try {
            if (!blockReader.readBlock(in))
                return null;

            // decode pmap and templateId
            BitVectorValue bitVectorValue = (BitVectorValue) TypeCodec.BIT_VECTOR.decode(in);
            if (bitVectorValue == null)
                return null;

            BitVector pmap = (bitVectorValue).value;
            BitVectorReader presenceMapReader = new BitVectorReader(pmap);

            // if template id is not present, use previous, else decode template id
            int templateId = (presenceMapReader.read()) ? TypeCodec.UINT.decode(in).toInt() : getContext().getLastTemplateId();
            getContext().setLastTemplateId(templateId);

            MessageTemplate template = getContext().getTemplate(templateId);
            if (template == null)
                return null;

            QuickDecoder decoder = quickDecoders.get(template);
            if (decoder == null)
                return null;

            FieldValue[] fieldValues = decoder.decodeFields(in, presenceMapReader, getContext());
            fieldValues[0] = new IntegerValue(templateId);

            return new Message(template, fieldValues);
        } catch (FastException e) {
            throw new FastException("An error occurred while decoding " + this, e.getCode(), e);
        }
    }

    @Override
    public Message readMessage() {
        Message message = decodeMessage();
        if (message == null)
            return null;

        blockReader.messageRead(in, null);
        if (templateHandlers.containsKey(message.getTemplate())) {
            MessageHandler handler = (MessageHandler) templateHandlers.get(message.getTemplate());
            handler.handleMessage(message, null, null);
            return readMessage();
        }
        return null;
    }

    @Override
    public void addMessageHandler(MessageTemplate template, MessageHandler handler) {
        if (templateHandlers == Collections.EMPTY_MAP)
            templateHandlers = new HashMap();

        if (quickDecoders == Collections.EMPTY_MAP)
            quickDecoders = new HashMap<>();

        if (template.getName().equals("X-OLR-CURR"))
            quickDecoders.put(template, new CurrIncrementalDecoder());

        if (template.getName().equals("X-OLR-FOND"))
            quickDecoders.put(template, new FondIncrementalDecoder());

        if (template.getName().equals("0"))
            quickDecoders.put(template, new HeartbeatDecoder());

        if (quickDecoders.containsKey(template))
            quickDecoders.get(template).setMessageTemplate(template);

        templateHandlers.put(template, handler);
    }

    @Override
    public void setBlockReader(MessageBlockReader messageBlockReader) {
        this.blockReader = messageBlockReader;
    }
}
