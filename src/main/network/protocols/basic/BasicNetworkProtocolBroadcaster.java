package main.network.protocols.basic;

import main.network.Connection;
import main.network.protocols.NetworkProtocolClient;
import main.util.InterruptableThreadSet;
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
public class BasicNetworkProtocolBroadcaster<TKey> extends NetworkProtocolClient<TKey> {
    private final ConcurrentLinkedQueue<Snapshot> snapshotQueue;
    private final InterruptableThreadSet threadSet;
    private final ClientList<TKey> clientList;

    public BasicNetworkProtocolBroadcaster(ConnectionFactory<TKey> connectionFactory) {
        super(connectionFactory);
        clientList = new ClientList<>(connectionFactory.getKey(), null);
        snapshotQueue = new ConcurrentLinkedQueue<>();

        threadSet = new InterruptableThreadSet(
                Arrays.asList(this::acceptConnections, this::sendSnapshot),
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
        if (snapshot != null) {
            onSnapshot(snapshot);
            clientList.sendSnapshot(snapshot);
        }
    }

    @Override
    public String getParentKey() {
        return "HOST";
    }
}
