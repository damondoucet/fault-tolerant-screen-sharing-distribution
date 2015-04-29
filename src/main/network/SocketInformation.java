package main.network;

/**
 * Immutable structure that holds IP and port. Represents the key for a
 * socket-based Connection.
 */
public class SocketInformation {
    public final String ip;
    public final int port;

    public SocketInformation(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    @Override
    public String toString() {
        return "SocketInformation{" +
                "ip='" + ip + '\'' +
                ", port=" + port +
                '}';
    }
}
