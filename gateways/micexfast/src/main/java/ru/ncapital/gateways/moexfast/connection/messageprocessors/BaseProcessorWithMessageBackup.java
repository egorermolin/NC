package ru.ncapital.gateways.moexfast.connection.messageprocessors;

import org.openfast.Message;

import java.util.*;

/**
 * Created by egore on 24.02.2016.
 */
public class BaseProcessorWithMessageBackup extends BaseProcessor {
    private Queue<Integer> backupCacheQueue = new LinkedList<Integer>();

    private Map<Integer, Message> backupCache = Collections.synchronizedMap(new HashMap<Integer, Message>());

    private boolean backupCacheQueueFull = false;

    protected void addBackupMessage(int seqNum, Message readMessage) {
        backupCacheQueue.add(seqNum);
        backupCache.put(seqNum, readMessage);
        if (backupCacheQueueFull)
            backupCache.remove(backupCacheQueue.poll());
        else // keep size at 100
            backupCacheQueueFull = backupCacheQueue.size() > 100;
    }

    protected Message getBackupMessage(int seqNum) {
        return backupCache.get(seqNum);
    }
}
