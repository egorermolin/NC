package ru.ncapital.gateways.fortsfast;

import ru.ncapital.gateways.moexfast.ConfigurationManager;
import ru.ncapital.gateways.moexfast.connection.ConnectionId;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Egor on 03-Oct-16.
 */
public class FortsConfigurationManager extends ConfigurationManager {
    private ConnectionId[] fortsConnectionIds;

    @Override
    public ConnectionId[] getAllConnectionIds() {
        if (fortsConnectionIds == null) {
            List<ConnectionId> fortsConnectionIdsList = new ArrayList<>();
            for (ConnectionId connectionId : ConnectionId.values()) {
                final String connectionIdString = connectionId.toString();
                if (connectionIdString.contains("FUT-"))
                    fortsConnectionIdsList.add(connectionId);
            }

            fortsConnectionIds = new ConnectionId[fortsConnectionIdsList.size()];
            fortsConnectionIdsList.toArray(fortsConnectionIds);
        }
        return fortsConnectionIds;
    }
}
