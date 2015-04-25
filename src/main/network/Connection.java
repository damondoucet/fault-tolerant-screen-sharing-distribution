package main.network;

import java.io.IOException;
import java.io.InputStream;

/**
 * Interface wrapping connections. This allows us to break the dependency on
 * sockets, which makes testing a lot easier.
 */
public interface Connection<T> {
    public T getSource();
    public T getDest();

    /**
     * Used for reading Snapshots.
     *
     * @return An InputStream that can read bytes from the connection.
     */
    public InputStream getInputStream();

    /**
     * @param bytes The array to read into.
     * @param numBytes The maximum number of bytes to read.
     * @return The number of bytes read.
     */
    public int read(byte[] bytes, int numBytes) throws IOException;

    /**
     * @param bytes The bytes to write
     */
    public void write(byte[] bytes) throws IOException;

    /**
     * Closes the socket.
     */
    public void close();
}
