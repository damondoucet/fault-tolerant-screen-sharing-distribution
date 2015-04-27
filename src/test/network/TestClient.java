package test.network;

import main.network.Connection;
import main.network.ConnectionFactory;
import main.network.Util;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents a client for a Connection<T> used for testing.
 */
public class TestClient<T> {
    private final ConnectionFactory<T> connectionFactory;
    private final T key;

    public TestClient(ConnectionFactory<T> connectionFactory, T key) {
        this.connectionFactory = connectionFactory;
        this.key = key;
    }

    public ConnectionFactory<T> getConnectionFactory() {
        return connectionFactory;
    }

    public T getKey() {
        return key;
    }

    /**
     * Spawns a thread that attemps to create a connection with the given
     * destination. It returns immediately and sets conn when the thread
     * completes (i.e., when the connection is created).
     *
     * @param dest The key to connect to
     * @param conn The connection to eventually be set when created
     * @param err Set to not null if an exception occurs.
     */
    public void spawnConnection(T dest,
                                AtomicReference<Connection<T>> conn,
                                AtomicReference<Exception> err) {
        new Thread(() -> {
            try {
                conn.set(connectionFactory.openConnection(dest));
            } catch (RuntimeException e) {
                err.set(e);
            } catch (IOException e) {
                err.set(e);
            }
        }).start();
    }

    /**
     * Spawns a thread that waits until a connection has been made. It returns
     * immediately and sets conn when the thread completes (i.e., when the
     * connection is created).
     * @param conn The connectino to eventually be set when created
     * @param err Set to not null if an exception occurs.
     */
    public void spawnAcceptConnection(AtomicReference<Connection<T>> conn,
                                      AtomicReference<Exception> err) {
        new Thread(() -> {
            try {
                conn.set(connectionFactory.acceptConnection());
            } catch (RuntimeException e) {
                err.set(e);
            } catch (IOException e) {
                err.set(e);
            }
        }).start();
    }

    private static void checkExceptions(AtomicReference<Exception> atSource,
                                        AtomicReference<Exception> atDest) {
        if (atSource.get() != null)
            throw new RuntimeException("Exception when connecting from source", atSource.get());
        if (atDest.get() != null)
            throw new RuntimeException("Exception when accepting at dest", atDest.get());
    }

    private static <T> void wait(AtomicReference<Connection<T>> source,
                                 AtomicReference<Connection<T>> dest,
                                 AtomicReference<Exception> sourceErr,
                                 AtomicReference<Exception> destErr) {
        while ((source.get() == null || dest.get() == null) &&
                sourceErr.get() == null && destErr.get() == null)
            Util.sleepMillis(10);

        checkExceptions(sourceErr, destErr);
    }

    // Connects two TestClients
    public static <T> ConnectionPair connect(TestClient<T> source,
                                             TestClient<T> dest) {
        AtomicReference<Connection<T>> sourceToDestRef = new AtomicReference<>(),
                destToSourceRef = new AtomicReference<>();
        AtomicReference<Exception> err1 = new AtomicReference<>(),
                err2 = new AtomicReference<>();

        source.spawnConnection(dest.getKey(), sourceToDestRef, err1);
        dest.spawnAcceptConnection(destToSourceRef, err2);

        wait(sourceToDestRef, destToSourceRef, err1, err2);

        return new ConnectionPair<>(
                source,
                dest,
                sourceToDestRef.get(),
                destToSourceRef.get());
    }
}
