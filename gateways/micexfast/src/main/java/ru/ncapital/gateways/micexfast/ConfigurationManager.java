package ru.ncapital.gateways.micexfast;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.Assisted;
import org.slf4j.LoggerFactory;
import ru.ncapital.gateways.micexfast.connection.Connection;
import ru.ncapital.gateways.micexfast.connection.ConnectionId;
import ru.ncapital.gateways.micexfast.xml.XMLReader;

import java.net.NetworkInterface;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by egore on 12/14/15.
 */
@Singleton
public class ConfigurationManager {
    private String fastTemplatesFile;

    private String networkInterface;

    private Map<ConnectionId, Connection> connections;

    public ConfigurationManager configure(IGatewayConfiguration configuration) {
        this.fastTemplatesFile = configuration.getFastTemplatesFile();
        this.networkInterface = configuration.getNetworkInterface();
        this.connections = new XMLReader().read(configuration.getConnectionsFile());

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
}
