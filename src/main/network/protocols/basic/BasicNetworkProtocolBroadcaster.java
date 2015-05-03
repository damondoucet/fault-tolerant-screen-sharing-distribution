package main.network.protocols.basic;

import main.network.Connection;
import main.util.InterruptableThreadSet;
import main.Snapshot;
import main.network.ClientList;
import main.network.ConnectionFactory;
import main.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

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
    private final AtomicBoolean shouldExit;

    private final AtomicReference<Snapshot> mostRecentSnapshot;

    public BasicNetworkProtocolBroadcaster(ConnectionFactory<T> connectionFactory) {
        super(connectionFactory);
        clientList = new ClientList<>(
                connectionFactory.getKey(),
                this::handleConnection);
        snapshotQueue = new ConcurrentLinkedQueue<>();
        shouldExit = new AtomicBoolean(true);

        threadSet = new InterruptableThreadSet(
                Arrays.asList(this::acceptConnections, this::sendSnapshot),
                null);  // TODO(ddoucet): should probably handle errors

        mostRecentSnapshot = new AtomicReference<>();
    }

    private void writeMostRecentSnapshot() throws IOException {
        while (mostRecentSnapshot.get() == null)
            Util.sleepMillis(1);  // should not be null for long

        // Hack. We don't want to write to the connection that asked because
        // then we'd have two threads writing to the same output stream which
        // is a bad idea.
        snapshotQueue.add(mostRecentSnapshot.get());
    }

    private void handleConnection(Connection<T> connection) {
        try {
            InputStream stream = connection.getInputStream();
            while (true) {
                int b = stream.read();
                if (b == REQUEST_SNAPSHOT)
                    writeMostRecentSnapshot();
                else
                    System.err.println(
                            "Broadcaster received unrecognized byte " + b + " from client");
            }
        } catch (Exception e) {
            Util.printException("Error while handling connection in broadcaster", e);
        }
    }

    @Override
    public void insertSnapshot(Snapshot image) {
        snapshotQueue.add(image);
    }

    @Override
    public void start() {
        shouldExit.set(true);
        threadSet.start();
    }

    @Override
    public void stop() {
        shouldExit.set(false);
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
            mostRecentSnapshot.set(snapshot);
            clientList.sendSnapshot(snapshot);
        }
    }
}
