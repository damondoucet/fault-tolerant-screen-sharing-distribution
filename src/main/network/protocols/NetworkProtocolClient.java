package main.network.protocols;

import main.Snapshot;
import main.network.Connection;
import main.network.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents a node in a network protocol that can receive snapshots.
 *
 * It implements the registerOutputQueue() method and provides a protected
 * onSnapshot method for inserting a received snapshot into the queue if it
 * exists.
 */
public abstract class NetworkProtocolClient<T> implements NetworkProtocol {
    protected final ConnectionFactory<T> connectionFactory;
    private final boolean lossy;
    protected final AtomicReference<Snapshot> mostRecentSnapshot;

    protected ConcurrentLinkedQueue<Snapshot> queue;

    protected NetworkProtocolClient(ConnectionFactory<T> connectionFactory) {
        // Defaults to lossy snapshots, but this should probably only be used
        // if the implementer doesn't read snapshots.
        this(connectionFactory, true);
    }

    protected NetworkProtocolClient(ConnectionFactory<T> connectionFactory,
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

    protected Snapshot readSnapshot(Connection<T> connection)
            throws IOException {
        return Snapshot.fromInputStream(connection.getInputStream(), lossy);
    }

    protected void onSnapshot(Snapshot snapshot) {
        mostRecentSnapshot.set(snapshot);
        if (queue != null)
            queue.add(snapshot);
    }

    public abstract void insertSnapshot(Snapshot image);

    @Override
    public abstract void start();

    @Override
    public abstract void stop();
}
