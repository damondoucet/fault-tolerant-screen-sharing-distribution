package main.network;

/**
 * Responsible for creating connections for a specific client. T is the
 * key-type of the connection.
 */
public interface ConnectionFactory<T> {
    /**
     * @return Null if the connection factory has died, otherwise a new
     *      connection. This method blocks until a new connection has opened.
     *      A null value indicates the caller should stop listening for
     *      connections.
     */
    public Connection<T> acceptConnection();

    /**
     * Creates a connection to the given key.
     *
     * @param key The client to open the connection with.
     * @return An opened connection.
     */
    public Connection<T> openConnection(T key);

    // TODO(ddoucet): Add some methods here for rate-limiting. Latency and/or
    // throughput.
}
