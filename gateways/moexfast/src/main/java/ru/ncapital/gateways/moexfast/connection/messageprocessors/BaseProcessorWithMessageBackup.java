package ru.ncapital.gateways.moexfast.connection.messageprocessors;

import org.openfast.Message;

import java.util.*;

/**
 * Created by egore on 24.02.2016.
 */
public class BaseProcessorWithMessageBackup extends BaseProcessor {
    private Queue<Integer> backupCacheQueue = new LinkedList<Integer>();

    private Map<Integer, Message> backupCache = Collections.synchronizedMap(new HashMap<Integer, Message>());

    void addBackupMessage(int seqNum, Message readMessage) {
        if (backupCacheQueue.size() == 100)
            backupCache.remove(backupCacheQueue.poll());

        backupCacheQueue.add(seqNum);
        backupCache.put(seqNum, readMessage);
   }

    Message getBackupMessage(int seqNum) {
        return backupCache.get(seqNum);
    }
}
