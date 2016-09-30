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
            return new Long(0);
        }
    };

    private ThreadLocal<Boolean> isPrimary = new ThreadLocal<Boolean>() {
        @Override
        public Boolean initialValue() {
            return new Boolean(false);
        }
    };

    public Long getInTimestamp() {
        return inTimestamp.get();
    }

    public ThreadLocal<Long> getInTimestampHolder() {
        return inTimestamp;
    }

    public void setIsPrimary(boolean isPrimary) {
        this.isPrimary.set(isPrimary);
    }

    public boolean isPrimary() {
        return isPrimary.get();
    }

    public Logger getLogger() {
        return logger.get();
    }
}
