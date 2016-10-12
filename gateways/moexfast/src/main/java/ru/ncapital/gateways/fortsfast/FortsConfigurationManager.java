package ru.ncapital.gateways.fortsfast;

import com.google.inject.Singleton;
import ru.ncapital.gateways.fortsfast.xml.FortsXMLReader;
import ru.ncapital.gateways.micexfast.xml.MicexXMLReader;
import ru.ncapital.gateways.moexfast.ConfigurationManager;
import ru.ncapital.gateways.moexfast.IGatewayConfiguration;
import ru.ncapital.gateways.moexfast.connection.ConnectionId;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Egor on 03-Oct-16.
 */
@Singleton
public class FortsConfigurationManager extends ConfigurationManager {
    private ConnectionId[] fortsConnectionIds;

    @Override
    public ConfigurationManager configure(IGatewayConfiguration configuration) {
        this.connections = new FortsXMLReader().read(configuration.getConnectionsFile());

        return super.configure(configuration);
    }

    @Override
    public ConnectionId[] getAllConnectionIds() {
        if (fortsConnectionIds == null) {
            List<ConnectionId> fortsConnectionIdsList = new ArrayList<>();
            for (ConnectionId connectionId : ConnectionId.values()) {
                final String connectionIdString = connectionId.toString();
                if (connectionIdString.contains("FUT-") || connectionIdString.contains("ORDERS-"))
                    fortsConnectionIdsList.add(connectionId);
            }

            fortsConnectionIds = new ConnectionId[fortsConnectionIdsList.size()];
            fortsConnectionIdsList.toArray(fortsConnectionIds);
        }
        return fortsConnectionIds;
    }
}
