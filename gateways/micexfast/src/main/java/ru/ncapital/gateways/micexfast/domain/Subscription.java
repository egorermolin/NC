package ru.ncapital.gateways.micexfast.domain;

/**
 * Created by egore on 12/7/15.
 */
public class Subscription {
    String subscriptionKey;

    public Subscription(String subscriptionKey) {
        this.subscriptionKey = subscriptionKey;
    }

    public String getSubscriptionKey() {
        return subscriptionKey;
    }
}
