// Copyright 2016 Orc Software AB All rights reserved.
// Reproduction in whole or in part in any form or medium without express
// written permission of Orc Software AB is strictly prohibited.
package ru.ncapital.gateways.micexfast.connection.messageprocessors;

import org.openfast.GroupValue;

/**
 * Created by egore on 5/30/16.
 */
public class StoredMdEntry {
    private GroupValue mdEntry;

    private long sendingTime;

    public StoredMdEntry(GroupValue mdEntry, long sendingTime) {
        this.mdEntry = mdEntry;
        this.sendingTime = sendingTime;
    }

    public long getSendingTime() {
        return sendingTime;
    }

    public GroupValue getMdEntry() {
        return mdEntry;
    }
}
