package ru.ncapital.gateways.moexfast.domain;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by egore on 12/19/15.
 */
public enum MdEntryType {
    BID('0', "BID"),
    OFFER('1', "OFFER"),
    LAST('2', "LAST"),
    TRADE('z', "TRADE"),
    EMPTY('J', "EMPTY"),

    OPENING('4', "OPENING"),
    CLOSING('5', "CLOSING"),
    HIGH('7', "HIGH"),
    LOW('8', "LOW"),

    UNKNOWN('X', "UNKNOWN");

    private char type;

    private String description;

    private static Map<Character, MdEntryType> typeMap = new HashMap<Character, MdEntryType>();

    MdEntryType(char type, String description) {
        this.type = type;
        this.description = description;
    }

    static {
        {
            for (MdEntryType type : MdEntryType.values()) {
                typeMap.put(type.getType(), type);
            }
        }
    }

    public char getType() { return type; }

    public static MdEntryType convert(char type) {
        MdEntryType mdEntryType = typeMap.get(type);
        return mdEntryType == null ? UNKNOWN : mdEntryType;
    }

    @Override
    public String toString() {
        return "MdEntryType{" +
                "description='" + description + '\'' +
                '}';
    }
}
