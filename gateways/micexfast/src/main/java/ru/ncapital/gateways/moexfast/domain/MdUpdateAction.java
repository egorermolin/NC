package ru.ncapital.gateways.moexfast.domain;

import java.util.HashMap;

/**
 * Created by egore on 12/15/15.
 */
public enum MdUpdateAction {
    INSERT('0', "INSERT"),
    UPDATE('1', "UPDATE"),
    DELETE('2', "DELETE"),
    SNAPSHOT('J', "SNAPSHOT");
    
    private char action;

    private String description;
    
    private static HashMap<Character, MdUpdateAction> actionMap = new HashMap<Character, MdUpdateAction>();

    MdUpdateAction(char action, String description) {
        this.action = action;
        this.description = description;
    }

    static {
        {
            for (MdUpdateAction action : MdUpdateAction.values()) {
                actionMap.put(action.getAction(), action);
            }
        }
    }
    
    public char getAction() { return action; }
    
    public static MdUpdateAction convert(char action) { return actionMap.get(action); }

    public String description() { return description; }

    @Override
    public String toString() {
        return "MdUpdateAction{" +
                "description='" + description + '\'' +
                '}';
    }
}
