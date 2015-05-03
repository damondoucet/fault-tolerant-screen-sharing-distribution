package main.network.protocols.basic;

import main.Snapshot;
import main.network.ConnectionFactory;
import main.network.protocols.NetworkProtocol;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A simple protocol in which all clients connect to the broadcaster.
 */
public abstract class BasicNetworkProtocol<T> implements NetworkProtocol {
    // A client sends this byte to the broadcaster to request the most recent
    // snapshot.
    protected final static byte REQUEST_SNAPSHOT = 0x11;

    protected final ConnectionFactory<T> connectionFactory;
    protected ConcurrentLinkedQueue<Snapshot> queue;
    protected final AtomicReference<Snapshot> mostRecentSnapshot;

    protected BasicNetworkProtocol(ConnectionFactory<T> connectionFactory) {
        this.connectionFactory = connectionFactory;
        queue = null;
        mostRecentSnapshot = new AtomicReference<>(null);
    }

    @Override
    public void registerOutputQueue(ConcurrentLinkedQueue<Snapshot> queue) {
        this.queue = queue;

        if (mostRecentSnapshot.get() != null)
            queue.add(mostRecentSnapshot.get());
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
