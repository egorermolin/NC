package ru.ncapital.gateways.moexfast.connection.messageprocessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by egore on 24.02.2016.
 */
public class BaseProcessor {
    private ThreadLocal<Logger> logger = new ThreadLocal<Logger>() {
        @Override
        protected Logger initialValue() {
            return LoggerFactory.getLogger(Thread.currentThread().getName() + "-Processor");
        }
    };

    private ThreadLocal<Long> inTimestamp = new ThreadLocal<Long>() {
        @Override
        public Long initialValue() {
            return 0L;
        }
    };

    private ThreadLocal<Boolean> isPrimary = new ThreadLocal<Boolean>() {
        @Override
        public Boolean initialValue() {
            return Boolean.FALSE;
        }
    };

    private boolean isPrimaryAlive = true;

    public Logger getLogger() {
        return logger.get();
    }

    Long getInTimestamp() {
        return inTimestamp.get();
    }

    boolean isPrimary() {
        return isPrimary.get();
    }

    boolean isPrimaryAlive() { return isPrimaryAlive; }

    public ThreadLocal<Long> getInTimestampHolder() {
        return inTimestamp;
    }

    public void setIsPrimary(boolean isPrimary) {
        this.isPrimary.set(isPrimary);
    }

    public void setPrimaryAlive(boolean alive) {
        isPrimaryAlive = alive;
    }

}
