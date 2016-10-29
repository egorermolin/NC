package ru.ncapital.gateways.moexfast.connection.messageprocessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    Long getInTimestamp() {
        return inTimestamp.get();
    }

    boolean isPrimary() {
        return isPrimary.get();
    }

    public ThreadLocal<Long> getInTimestampHolder() {
        return inTimestamp;
    }

    public void setIsPrimary(boolean isPrimary) {
        this.isPrimary.set(isPrimary);
    }

    public Logger getLogger() {
        return logger.get();
    }
}
