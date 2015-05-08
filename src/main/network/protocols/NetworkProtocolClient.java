package main.network.protocols;

import main.Snapshot;
import main.network.Connection;
import main.network.ConnectionFactory;
import main.util.Util;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents a node in a network protocol that can receive snapshots.
 *
 * It implements the registerOutputQueue() method and provides a protected
 * onSnapshot method for inserting a received snapshot into the queue if it
 * exists.
 */
public abstract class NetworkProtocolClient<TKey> implements NetworkProtocol {
    protected final ConnectionFactory<TKey> connectionFactory;
    private final boolean lossy;
    private final AtomicReference<Snapshot> mostRecentSnapshot;

    protected ConcurrentLinkedQueue<Snapshot> queue;

    protected NetworkProtocolClient(ConnectionFactory<TKey> connectionFactory) {
        // Defaults to lossy snapshots, but this should probably only be used
        // if the implementer doesn't read snapshots.
        this(connectionFactory, true);
    }

    protected NetworkProtocolClient(ConnectionFactory<TKey> connectionFactory,
                                    boolean lossy) {
        this.connectionFactory = connectionFactory;
        this.lossy = lossy;
        this.mostRecentSnapshot = new AtomicReference<>(null);

        this.queue = null;
    }

    @Override
    public void registerOutputQueue(ConcurrentLinkedQueue<Snapshot> queue) {
        this.queue = queue;

        if (mostRecentSnapshot.get() != null)
            queue.add(mostRecentSnapshot.get());
    }

    // TODO(ddoucet): timeout should be pushed down to the snapshot layer so
    // that it can only abort when reading the first 8 bytes rather than the
    // entire snapshot.
    protected Snapshot readSnapshot(Connection<TKey> connection, long timeoutMillis)
            throws Exception {
        Callable<Snapshot> callable = () -> Snapshot.fromInputStream(connection.getInputStream(), lossy);

        if (timeoutMillis == -1)
            return callable.call();
        return Util.doWithTimeout(callable, timeoutMillis);
    }

    protected void onSnapshot(Snapshot snapshot) {
        mostRecentSnapshot.getAndUpdate(
                snap -> {
                    if (snap != null && snapshot.getFrameIndex() <= snap.getFrameIndex())
                        return snap;
                    if (queue != null)
                        queue.add(snapshot);
                    return snapshot;
                });
    }

    public abstract void insertSnapshot(Snapshot image);

    @Override
    public abstract void start();

    @Override
    public abstract void stop();
}
