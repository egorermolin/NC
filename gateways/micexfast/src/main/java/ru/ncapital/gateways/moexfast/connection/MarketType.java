package ru.ncapital.gateways.moexfast.connection;

/**
 * Created by egore on 3/13/16.
 */
public enum MarketType {
    CURR("CURR"),
    FOND("FOND"),
    FUT("FUT");

    private final String description;

    MarketType(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return this.description;
    }
}
