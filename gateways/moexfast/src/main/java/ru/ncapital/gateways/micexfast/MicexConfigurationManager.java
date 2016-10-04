package ru.ncapital.gateways.micexfast;

import com.google.inject.Singleton;
import ru.ncapital.gateways.micexfast.xml.MicexXMLReader;
import ru.ncapital.gateways.moexfast.ConfigurationManager;
import ru.ncapital.gateways.moexfast.IGatewayConfiguration;
import ru.ncapital.gateways.moexfast.connection.ConnectionId;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Egor on 30-Sep-16.
 */
@Singleton
public class MicexConfigurationManager extends ConfigurationManager {
    private ConnectionId[] micexConnectionIds;

    @Override
    public ConfigurationManager configure(IGatewayConfiguration configuration) {
        this.connections = new MicexXMLReader().read(configuration.getConnectionsFile());

        return super.configure(configuration);
    }

    @Override
    public ConnectionId[] getAllConnectionIds() {
        if (micexConnectionIds == null) {
            List<ConnectionId> micexConnectionIdsList = new ArrayList<>();
            for (ConnectionId connectionId : ConnectionId.values()) {
                final String connectionIdString = connectionId.toString();
                if (connectionIdString.contains("CURR-") || connectionIdString.contains("FOND-"))
                    micexConnectionIdsList.add(connectionId);
            }

            micexConnectionIds = new ConnectionId[micexConnectionIdsList.size()];
            micexConnectionIdsList.toArray(micexConnectionIds);
        }
        return micexConnectionIds;
    }
}
