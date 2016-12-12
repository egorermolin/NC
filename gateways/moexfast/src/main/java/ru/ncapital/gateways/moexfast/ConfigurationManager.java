package ru.ncapital.gateways.moexfast;

import org.slf4j.LoggerFactory;
import ru.ncapital.gateways.moexfast.connection.Connection;
import ru.ncapital.gateways.moexfast.connection.ConnectionId;
import ru.ncapital.gateways.moexfast.connection.multicast.utils.ListNetIntf;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Map;

/**
 * Created by egore on 12/14/15.
 */
public abstract class ConfigurationManager {
    private String fastTemplatesFile;

    private String networkInterface;

    protected Map<ConnectionId, Connection> connections;

    private boolean asynchChannelReader;

    private long feedDownTimeout;

    private boolean restartOnAllFeedDown;

    public ConfigurationManager configure(IGatewayConfiguration configuration) {
        this.fastTemplatesFile = configuration.getFastTemplatesFile();
        this.networkInterface = configuration.getNetworkInterface();
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

    public static NetworkInterface convertNetworkInterface(String name) throws SocketException {
        if (name.contains("."))
            return NetworkInterface.getByName(ListNetIntf.getNetworkInterfaceName(name));
        else
            return NetworkInterface.getByName(name);
    }

    public String getPrimaryNetworkInterface(boolean orderList) {
        if (networkInterface.contains("|")) {
            String networkInterfacePart = networkInterface.split("\\|")[orderList ? 1 : 0];
            if (networkInterfacePart.contains(";"))
                if (!networkInterfacePart.split(";")[0].isEmpty())
                    return networkInterfacePart.split(";")[0];

            return networkInterfacePart.replace(";", "");
        }

        if (orderList)
            return null;

        if (networkInterface.contains(";"))
            if (!networkInterface.split(";")[0].isEmpty())
                return networkInterface.split(";")[0];

        return networkInterface.replace(";", "");
    }

    public String getSecondaryNetworkInterface(boolean orderList) {
        if (networkInterface.contains("|")) {
            String networkInterfacePart = networkInterface.split("\\|")[orderList ? 1 : 0];
            if (networkInterfacePart.contains(";"))
                if (!networkInterfacePart.split(";")[1].isEmpty())
                    return networkInterfacePart.split(";")[1];

            return networkInterfacePart.replace(";", "");
        }

        if (orderList)
            return null;

        if (networkInterface.contains(";"))
            if (!networkInterface.split(";")[1].isEmpty())
                return networkInterface.split(";")[1];

        return networkInterface.replace(";", "");
    }

    public boolean checkInterfaces() {
        try {
            if (convertNetworkInterface(getPrimaryNetworkInterface(false)) == null)
                throw new RuntimeException("Invalid Primary Interface " + getPrimaryNetworkInterface(false));
        } catch (Exception e) {
            LoggerFactory.getLogger("ConfigurationManager").error(e.toString(), e);
            return false;
        }

        try {
            if (convertNetworkInterface(getSecondaryNetworkInterface(false)) == null)
                throw new RuntimeException("Invalid Secondary Interface " + getSecondaryNetworkInterface(false));
        } catch (Exception e) {
            LoggerFactory.getLogger("ConfigurationManager").error(e.toString(), e);
            return false;
        }

        try {
            if (getPrimaryNetworkInterface(true) != null && convertNetworkInterface(getPrimaryNetworkInterface(true)) == null)
                throw new RuntimeException("Invalid Primary Interface For Order List" + getPrimaryNetworkInterface(true));
        } catch (Exception e) {
            LoggerFactory.getLogger("ConfigurationManager").error(e.toString(), e);
            return false;
        }

        try {
            if (getSecondaryNetworkInterface(true) != null && convertNetworkInterface(getSecondaryNetworkInterface(true)) == null)
                throw new RuntimeException("Invalid Secondary Interface For Order List" + getSecondaryNetworkInterface(true));
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

    public abstract ConnectionId[] getAllConnectionIds();
}
