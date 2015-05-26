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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SocketInformation)) return false;

        SocketInformation that = (SocketInformation) o;
        return port == that.port &&
                ip.equals(that.ip);
    }

    @Override
    public int hashCode() {
        int result = ip != null ? ip.hashCode() : 0;
        result = 31 * result + port;
        return result;
    }

    @Override
    public String toString() {
        return ip + ":" + port;
    }
}
