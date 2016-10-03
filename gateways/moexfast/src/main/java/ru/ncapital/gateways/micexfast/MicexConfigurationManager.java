// Copyright 2016 Itiviti Group All rights reserved.
// Reproduction in whole or in part in any form or medium without express
// written permission of Orc Software AB is strictly prohibited.
package ru.ncapital.gateways.micexfast;

import com.google.inject.Singleton;
import ru.ncapital.gateways.moexfast.ConfigurationManager;
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
    public ConnectionId[] getAllConnectionIds() {
        if (micexConnectionIds == null) {
            List<ConnectionId> micexConnectionIdsList = new ArrayList<>();
            for (ConnectionId connectionId : ConnectionId.values()) {
                final String connectionIdString = connectionId.toString();
                if (connectionIdString.contains("CURR") || connectionIdString.contains("FOND"))
                    micexConnectionIdsList.add(connectionId);
            }

            micexConnectionIds = new ConnectionId[micexConnectionIdsList.size()];
            micexConnectionIdsList.toArray(micexConnectionIds);
        }
        return micexConnectionIds;
    }
}
