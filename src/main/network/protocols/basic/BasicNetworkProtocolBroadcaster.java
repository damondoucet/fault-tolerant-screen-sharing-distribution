package main.network.protocols.basic;

import main.InterruptableThreadSet;
import main.Snapshot;
import main.network.ClientList;
import main.network.ConnectionFactory;

import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Broadcaster for the basic network protocol.
 *
 * This spawns two threads: a thread to accept connections, and a thread to
 * send snapshots to clients. When a connection is accepted, it's inserted into
 * a list, which the other thread iterates through when it receives a snapshot.
 */
public class BasicNetworkProtocolBroadcaster<T> extends BasicNetworkProtocol<T> {
    private final ConcurrentLinkedQueue<Snapshot> snapshotQueue;
    private final InterruptableThreadSet threadSet;
    private final ClientList<T> clientList;

    public BasicNetworkProtocolBroadcaster(ConnectionFactory<T> connectionFactory) {
        super(connectionFactory);
        clientList = new ClientList(connectionFactory.getKey());
        snapshotQueue = new ConcurrentLinkedQueue<>();

        threadSet = new InterruptableThreadSet(
                Arrays.asList(() -> acceptConnections(), () -> sendSnapshot()),
                null);  // TODO(ddoucet): should probably handle errors
    }

    @Override
    public void insertSnapshot(Snapshot image) {
        snapshotQueue.add(image);
    }

    @Override
    public void start() {
        threadSet.start();
    }

    @Override
    public void stop() {
        threadSet.stop();
    }

    private void acceptConnections() {
        try {
            clientList.addConnection(connectionFactory.acceptConnection());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void sendSnapshot() {
        Snapshot snapshot = snapshotQueue.poll();
        if (snapshot != null)
            clientList.sendSnapshot(snapshot);
    }
}
