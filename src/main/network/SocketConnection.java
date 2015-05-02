package main.network;

import main.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * A socket-based Connection.
 */
public class SocketConnection implements Connection<SocketInformation> {
    private final Socket socket;
    private final InputStream inputStream;
    private final OutputStream outputStream;
    private final SocketInformation source;
    private final SocketInformation dest;

    // To avoid throwing exceptions in the constructor, we make it private.
    private SocketConnection(Socket socket,
                             InputStream inputStream,
                             OutputStream outputStream,
                             SocketInformation source,
                             SocketInformation dest) {
        this.socket = socket;
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.source = source;
        this.dest = dest;
    }

    public static SocketConnection fromSocket(Socket socket,
                                              SocketInformation source,
                                              SocketInformation dest)
            throws IOException {
        return new SocketConnection(
                socket,
                socket.getInputStream(),
                socket.getOutputStream(),
                source,
                dest);
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    @Override
    public SocketInformation getSource() {
        return source;
    }

    @Override
    public SocketInformation getDest() {
        return dest;
    }

    @Override
    public InputStream getInputStream() {
        return inputStream;
    }

    @Override
    public int read(byte[] bytes, int numBytes) throws IOException {
        return Util.read(inputStream, bytes, numBytes);
    }

    @Override
    public void write(byte[] bytes) throws IOException {
        outputStream.write(bytes);
    }

    @Override
    public void close() {
        try {
            inputStream.close();
        } catch (IOException e) {
        }

        try {
            outputStream.close();
        } catch (IOException e) {
        }

        try {
            socket.close();
        } catch (IOException e) {
        }
    }
}
