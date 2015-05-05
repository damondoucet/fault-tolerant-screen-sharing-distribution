package main.network;

import java.io.IOException;

/**
 * Responsible for creating connections for a specific client. T is the
 * key-type of the connection.
 */
public interface ConnectionFactory<TKey> {
    public TKey getKey();

    /**
     * @return A new connection. This method blocks until a new connection
     *      has opened or an Exception is thrown.
     */
    public Connection<TKey> acceptConnection() throws IOException;

    /**
     * Creates a connection to the given key.
     *
     * @param key The client to open the connection with.
     * @return An opened connection.
     */
    public Connection<TKey> openConnection(TKey key) throws IOException;

    public void close();

    // TODO(ddoucet): Add some methods here for rate-limiting
}
