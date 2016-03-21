package ru.ncapital.gateways.micexfast.connection.tcp;

/**
 * Created by egore on 1/20/16.
 */
public class TcpConnection {
    private String id;

    private String host;

    private int port;

    private String user;

    private String password;

    public TcpConnection(String id, String host, int port) {
        this.id = id;
        this.host = host;
        this.port = port;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getId() {
        return id;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
