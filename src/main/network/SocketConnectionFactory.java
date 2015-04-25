package main.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Factory for creating socket-based Connections.
 *
 * Must be threadsafe.
 */
public class SocketConnectionFactory implements ConnectionFactory<SocketInformation> {
    private final SocketInformation info;
    private final ServerSocket serverSocket;

    // Avoid throwing exceptions in constructors
    private SocketConnectionFactory(SocketInformation info,
                                   ServerSocket serverSocket) {
        this.info = info;
        this.serverSocket = serverSocket;
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

        return SocketConnection.fromSocket(socket, info, dest);
    }

    @Override
    public Connection<SocketInformation> openConnection(SocketInformation dest)
            throws IOException {
        return SocketConnection.fromSocket(
                new Socket(dest.ip, dest.port),
                info,
                dest);
    }

    @Override
    public void close() {
        try {
            serverSocket.close();
        } catch (IOException e) {
        }
    }
}
