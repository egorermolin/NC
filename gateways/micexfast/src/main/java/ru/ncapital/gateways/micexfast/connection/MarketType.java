package ru.ncapital.gateways.micexfast.connection;

/**
 * Created by egore on 3/13/16.
 */
public enum MarketType {
    CURR("CURR"),
    FOND("FOND");

    private final String description;

    MarketType(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return this.description;
    }
}
