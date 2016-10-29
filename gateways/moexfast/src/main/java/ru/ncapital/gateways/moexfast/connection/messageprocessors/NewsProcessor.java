package ru.ncapital.gateways.moexfast.connection.messageprocessors;

import org.openfast.GroupValue;
import org.openfast.Message;
import org.openfast.SequenceValue;
import ru.ncapital.gateways.moexfast.IMarketDataHandler;
import ru.ncapital.gateways.moexfast.domain.impl.News;

public class NewsProcessor extends Processor {

    private IMarketDataHandler marketDataHandler;

    public NewsProcessor(IMarketDataHandler marketDataHandler) {
        this.marketDataHandler = marketDataHandler;
    }

    @Override
    public void processMessage(Message readMessage) {
        StringBuilder message = new StringBuilder();
        if (readMessage.getValue("Urgency") != null)
            message.append("URGENT: ");

        if (readMessage.getValue("Headline") != null)
            message.append(readMessage.getString("Headline")).append(": ");

        SequenceValue text = readMessage.getSequence("NewsText");
        GroupValue lineOfText = text.get(0);
        if (lineOfText.getValue("Text") != null)
            message.append(lineOfText.getString("Text"));

        if (readMessage.getValue("NewsId") != null)
            message.append(" (").append(readMessage.getString("NewsId")).append(")");

        News news = new News();
        news.setMessage(message.toString());
        marketDataHandler.onNews(news);
    }
}
