package ru.ncapital.gateways.micexfast.domain;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by egore on 16.02.2016.
 */
public enum ProductType {
    CORPORATE(3, "CORP"),
    CURRENCY(4, "CURR"),
    EQUITY(5, "EQU"),
    GOVERNMENT(6, "GOV"),
    INDEX(7, "INDEX"),
    MORTGAGE(10, "MORT"),
    MUNICIPAL(11, "MUNI"),
    OTHER(12, "OTHER"),
    FINANCING(13, "FIN"),
    UNKNOWN(-1, "UNKNOWN");

    private int product;

    private String description;

    private static Map<Integer, ProductType> typeMap = new HashMap<Integer, ProductType>();

    ProductType(int product, String description) {
        this.product = product;
        this.description = description;
    }

    static {
        {
            for (ProductType type : ProductType.values()) {
                typeMap.put(type.getProduct(), type);
            }
        }
    }

    public int getProduct() {
        return this.product;
    }

    public String getDescription() { return this.description; }

    public static ProductType convert(int type) { return typeMap.get(type); }
}
