package ru.ncapital.gateways.moexfast.messagehandlers;

/**
 * Created by egore on 5/5/16.
 */
public enum MessageHandlerType {
    ORDER_LIST("OrderList"),
    STATISTICS("Statistics"),
    HEARTBEAT("Heartbeat");

    private String description;

    MessageHandlerType(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }

    public boolean equals(String type) {
        return description.equals(type);
    }
}
