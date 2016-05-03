package ru.ncapital.gateways.micexfast.xml;

import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import ru.ncapital.gateways.micexfast.Utils;
import ru.ncapital.gateways.micexfast.connection.Connection;
import ru.ncapital.gateways.micexfast.connection.ConnectionId;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by egore on 12/14/15.
 */
public class XMLReader {

    public Map<ConnectionId, Connection> read(String filename) {
        TreeMap<ConnectionId, Connection> connectionsMap = new TreeMap<ConnectionId, Connection>();

        try {
            File inputFile = new File(filename);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();

            NodeList channels = doc.getDocumentElement().getElementsByTagName("channel");
            for (int i = 0; i < channels.getLength(); ++i) {
                Node channel = channels.item(i);
                if (channel.hasChildNodes()) {
                    String channelId = channel.getAttributes().getNamedItem("id").getNodeValue();
                    NodeList connections = doc.getDocumentElement().getElementsByTagName("connection");
                    for (int j = 0; j < connections.getLength(); ++j) {
                        Node connection = connections.item(j);
                        if (connection.getParentNode().getParentNode() == channel) {
                            String connectionId = connection.getAttributes().getNamedItem("id").getNodeValue();
                            NodeList feeds = doc.getDocumentElement().getElementsByTagName("feed");
                            for (int k = 0; k < feeds.getLength(); ++k) {
                                Node feed = feeds.item(k);
                                if (feed.getParentNode() == connection) {
                                    String feedId = feed.getAttributes().getNamedItem("id").getNodeValue();

                                    ConnectionId mconnectionId = ConnectionId.convert(channelId + '-' + connectionId + "-" + feedId);
                                    if (mconnectionId == null)
                                        continue;

                                    String ip = "";
                                    int port = 0;
                                    String sourceIp = "";

                                    NodeList feedProperties = feed.getChildNodes();
                                    for (int l = 0; l < feedProperties.getLength(); ++l) {
                                        Node feedProperty = feedProperties.item(l);
                                        if (feedProperty.hasChildNodes()) {
                                            if (feedProperty.getNodeName().equals("src-ip")) {
                                                sourceIp = feedProperty.getFirstChild().getNodeValue();
                                            } else if (feedProperty.getNodeName().equals("ip")) {
                                                ip = feedProperty.getFirstChild().getNodeValue();
                                            } else if (feedProperty.getNodeName().equals("port")) {
                                                port = Integer.valueOf(feedProperty.getFirstChild().getNodeValue());
                                            }
                                        }
                                    }

                                    connectionsMap.put(mconnectionId, new Connection(mconnectionId, port, ip, sourceIp));
                                }
                            }
                        }
                    }
                }
            }

        } catch (SAXException e) {
            Utils.printStackTrace(e, LoggerFactory.getLogger("XMLReader"));
        } catch (ParserConfigurationException e) {
            Utils.printStackTrace(e, LoggerFactory.getLogger("XMLReader"));
        } catch (IOException e) {
            Utils.printStackTrace(e, LoggerFactory.getLogger("XMLReader"));
        }

        return connectionsMap;
    }
}
