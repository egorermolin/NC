package ru.ncapital.gateways.moexfast.connection;

/**
 * Created by egore on 12/9/15.
 */
public class Connection {

    private ConnectionId id;

    private int port;

    private String ip;

    private String source;

    public Connection(ConnectionId id, int port, String ip, String source) {
        this.port = port;
        this.ip = ip;
        this.source = source;
    }

    public ConnectionId getId() {
        return id;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
