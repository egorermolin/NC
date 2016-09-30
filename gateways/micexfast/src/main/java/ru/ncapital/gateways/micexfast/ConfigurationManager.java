package ru.ncapital.gateways.micexfast;

import com.google.inject.Singleton;
import org.slf4j.LoggerFactory;
import ru.ncapital.gateways.moexfast.connection.Connection;
import ru.ncapital.gateways.moexfast.connection.ConnectionId;
import ru.ncapital.gateways.moexfast.xml.XMLReader;

import java.net.NetworkInterface;
import java.util.Map;

/**
 * Created by egore on 12/14/15.
 */
@Singleton
public class ConfigurationManager {
    private String fastTemplatesFile;

    private String networkInterface;

    private Map<ConnectionId, Connection> connections;

    private boolean asynchChannelReader;

    private long feedDownTimeout;

    private boolean restartOnAllFeedDown;

    public ConfigurationManager configure(IGatewayConfiguration configuration) {
        this.fastTemplatesFile = configuration.getFastTemplatesFile();
        this.networkInterface = configuration.getNetworkInterface();
        this.connections = new XMLReader().read(configuration.getConnectionsFile());
        this.asynchChannelReader = configuration.isAsynchChannelReader();
        this.feedDownTimeout = configuration.getFeedDownTimeout();
        this.restartOnAllFeedDown = configuration.restartOnAllFeedDown();

        return this;
    }

    public String getFastTemplatesFile() {
        return fastTemplatesFile;
    }

    public Connection getConnection(ConnectionId connectionId) {
        return connections.get(connectionId);
    }

    public String getPrimaryNetworkInterface() {
        return networkInterface.split(";")[0];
    }

    public String getSecondaryNetworkInterface() {
        return networkInterface.contains(";") ? networkInterface.split(";")[1] : networkInterface;
    }

    public boolean checkInterfaces() {
        try {
            if (NetworkInterface.getByName(getPrimaryNetworkInterface()) == null)
                throw new RuntimeException("Invalid Primary Interface " + getPrimaryNetworkInterface());
        } catch (Exception e) {
            LoggerFactory.getLogger("ConfigurationManager").error(e.toString(), e);
            return false;
        }

        try {
            if (NetworkInterface.getByName(getSecondaryNetworkInterface()) == null)
                throw new RuntimeException("Invalid Secondary Interface " + getSecondaryNetworkInterface());
        } catch (Exception e) {
            LoggerFactory.getLogger("ConfigurationManager").error(e.toString(), e);
            return false;
        }

        return true;
    }

    public boolean isAsynchChannelReader() {
        return asynchChannelReader;
    }

    public long getFeedDownTimeout() {
        return feedDownTimeout;
    }

    public boolean restartOnAllFeedDown() {
        return restartOnAllFeedDown;
    }
}
