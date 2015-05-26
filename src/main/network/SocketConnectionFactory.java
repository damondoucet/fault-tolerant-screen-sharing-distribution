package main.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for creating socket-based Connections.
 *
 * Must be threadsafe.
 */
public class SocketConnectionFactory implements ConnectionFactory<SocketInformation> {
    private final SocketInformation info;
    private final ServerSocket serverSocket;

    // you can once a SocketInfo is added, it is never removed. aka, you can only connect with a given SocketInfo once
    private final ConcurrentHashMap<SocketInformation, SocketConnection> connections;

    // Avoid throwing exceptions in constructors
    private SocketConnectionFactory(SocketInformation info,
                                   ServerSocket serverSocket) {
        this.info = info;
        this.serverSocket = serverSocket;
        this.connections = new ConcurrentHashMap<>();
    }

    public static SocketConnectionFactory fromSocketInfo(SocketInformation info)
            throws IOException {
        return new SocketConnectionFactory(info, new ServerSocket(info.port));
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    public SocketInformation getKey() {
        return info;
    }

    @Override
    public Connection<SocketInformation> acceptConnection() throws IOException {
        Socket socket = serverSocket.accept();
        SocketInformation dest = new SocketInformation(
                socket.getInetAddress().getHostAddress(),
                socket.getPort());

        if (this.connections.get(dest) != null) {
            throw new IOException();
        } else {
            this.connections.putIfAbsent(dest, SocketConnection.fromSocket(socket, info, dest));
            return this.connections.get(dest);
        }
    }

    @Override
    public Connection<SocketInformation> openConnection(SocketInformation dest)
            throws IOException {
        if (this.connections.get(dest) != null) {
            throw new IOException();
        } else {
            this.connections.putIfAbsent(dest, SocketConnection.fromSocket(
                    new Socket(dest.ip, dest.port),
                    info,
                    dest));
            return this.connections.get(dest);
        }
    }

    public void kill(SocketInformation dest) {
        this.connections.get(dest).close();
        // do not remove dest from this.connections, so that it can not be connected to again
    }

    @Override
    public void close() {
        try {
            serverSocket.close();
        } catch (IOException e) {
        }
    }
}
