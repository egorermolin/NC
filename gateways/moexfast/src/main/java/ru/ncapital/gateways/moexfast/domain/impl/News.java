package ru.ncapital.gateways.moexfast.domain.impl;

import ru.ncapital.gateways.moexfast.domain.intf.INews;

/**
 * Created by egore on 10/29/16.
 */
public class News implements INews {
    private String message;

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    @Override
    public String toString() {
        return "News{" +
                "message='" + message + '\'' +
                '}';
    }
}
