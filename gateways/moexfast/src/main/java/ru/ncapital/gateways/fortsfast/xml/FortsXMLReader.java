package ru.ncapital.gateways.fortsfast.xml;

import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import ru.ncapital.gateways.moexfast.Utils;
import ru.ncapital.gateways.moexfast.connection.Connection;
import ru.ncapital.gateways.moexfast.connection.ConnectionId;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by egore on 12/14/15.
 */
public class FortsXMLReader {

    public Map<ConnectionId, Connection> read(String filename) {
        TreeMap<ConnectionId, Connection> connectionsMap = new TreeMap<ConnectionId, Connection>();

        try {
            File inputFile = new File(filename);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();

            NodeList marketDataGroups = doc.getDocumentElement().getElementsByTagName("MarketDataGroup");
            for (int i = 0; i < marketDataGroups.getLength(); ++i) {
                Node marketDataGroup = marketDataGroups.item(i);
                if (marketDataGroup.hasChildNodes()) {
                    String feedType = marketDataGroup.getAttributes().getNamedItem("feedType").getNodeValue();
                    NodeList connections = doc.getDocumentElement().getElementsByTagName("connection");
                    for (int j = 0; j < connections.getLength(); ++j) {
                        Node connection = connections.item(j);
                        if (connection.getParentNode().getParentNode() == marketDataGroup) {
                            char type = 'X';
                            String ip = "0.0.0.0";
                            int port = 0;
                            String sourceIp = "0.0.0.0";
                            char feed = 'X';

                            NodeList connectionProperties = connection.getChildNodes();
                            for (int l = 0; l < connectionProperties.getLength(); ++l) {
                                Node connectionProperty = connectionProperties.item(l);
                                if (connectionProperty.hasChildNodes()) {
                                    if (connectionProperty.getNodeName().equals("src-ip")) {
                                        sourceIp = connectionProperty.getFirstChild().getNodeValue();
                                    } else if (connectionProperty.getNodeName().equals("ip")) {
                                        ip = connectionProperty.getFirstChild().getNodeValue();
                                    } else if (connectionProperty.getNodeName().equals("port")) {
                                        port = Integer.valueOf(connectionProperty.getFirstChild().getNodeValue());
                                    } else if (connectionProperty.getNodeName().equals("type")) {
                                        String typeStr = connectionProperty.getFirstChild().getNodeValue();
                                        if (typeStr.contains("Instrument"))
                                            type = typeStr.charAt(11);
                                        else
                                            type = typeStr.charAt(0);
                                    } else if (connectionProperty.getNodeName().equals("feed")) {
                                        feed = connectionProperty.getFirstChild().getNodeValue().charAt(0);
                                    }
                                }
                            }

                            ConnectionId connectionId = ConnectionId.convert(feedType + "-" + type + "-" + feed);
                            if (connectionId == null)
                                continue;

                            connectionsMap.put(connectionId, new Connection(connectionId, port, ip, sourceIp));
                        }
                    }
                }
            }

        } catch (SAXException e) {
            Utils.printStackTrace(e, LoggerFactory.getLogger("MicexXMLReader"), "Exception occurred while reading XML");
        } catch (ParserConfigurationException e) {
            Utils.printStackTrace(e, LoggerFactory.getLogger("MicexXMLReader"), "Exception occurred while reading XML");
        } catch (IOException e) {
            Utils.printStackTrace(e, LoggerFactory.getLogger("MicexXMLReader"), "Exception occurred while reading XML");
        }

        return connectionsMap;
    }
}
