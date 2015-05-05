package main.network;

import main.Snapshot;
import main.util.Util;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.*;

/**
 * Holds a list of connections to clients requesting snapshots.
 */
public class ClientList<TKey> {
    private final TKey key;
    private final List<Connection<TKey>> connections;

    // If connectionHandler != null, we'll spawn a new thread and run the
    // connectionHandler on it. When the connectionHandler returns, the
    // connection will be killed.
    private final Consumer<Connection> connectionHandler;

    // When a new client connects, we send this to them if it isn't null.
    private final AtomicReference<Snapshot> mostRecentSnapshot;

    public ClientList(TKey key, /* nullable */ Consumer<Connection> connectionHandler) {
        this.key = key;
        this.connections = Collections.synchronizedList(new LinkedList<>());
        this.connectionHandler = connectionHandler;
        this.mostRecentSnapshot = new AtomicReference<>();
    }

    public void addConnection(Connection<TKey> connection) {
        checkArgument(connection.getSource().equals(key),
                "Tried to add connection where source (%s) was not us (%s)",
                connection.getSource(), key);

        if (mostRecentSnapshot.get() != null) {
            // Send the snapshot before we add it to the connections list,
            // otherwise another thread may call sendSnapshot(), which would
            // write to the connection simultaneously.
            try {
                byte[] bytes = getSnapshotBytes(mostRecentSnapshot.get());
                if (bytes != null)
                    connection.write(bytes);
            } catch (IOException e) {
                Util.printException(
                        "Error writing most recent snapshot to new connection (dest "
                                + connection.getDest() + ")", e);
                return;  // No reason to add this connection if it just crashed
            }
        }

        connections.add(connection);

        if (connectionHandler != null)
            new Thread(new ConnectionHandler<>(connection)).start();
    }

    private class ConnectionHandler<T> implements Runnable {
        private final Connection<T> connection;

        public ConnectionHandler(Connection<T> connection) {
            this.connection = connection;
        }

        @Override
        public void run() {
            connectionHandler.accept(connection);
            connection.close();
            connections.removeIf(conn -> conn.getDest().equals(connection.getDest()));
        }
    }

    private byte[] getSnapshotBytes(Snapshot snapshot) {
        try {
            return snapshot.toBytes();
        } catch (IOException e) {
            Util.printException(
                    String.format("Error converting snapshot %d to bytes:\n",
                            snapshot.getFrameIndex()),
                    e);
            return null;
        }
    }

    public void removeAll() {
        synchronized (connections) {
            for (Iterator<Connection<TKey>> it = connections.iterator(); it.hasNext(); ) {
                it.next().close();
                it.remove();
            }
        }
    }

    public void sendBytesToConnections(byte[] bytes) {
        synchronized (connections) {
            for (Iterator<Connection<TKey>> it  = connections.iterator(); it.hasNext(); ) {
                Connection<TKey> connection = it.next();
                try {
                    Util.threadsafeWrite(connection, bytes);
                } catch (IOException e) {
                    Util.printException("Error writing to connection", e);
                    connection.close();
                    it.remove();
                }
            }
        }
    }

    public void sendSnapshot(Snapshot snapshot) {
        mostRecentSnapshot.set(snapshot);

        if (connections.size() == 0)
            return;

        byte[] bytes = getSnapshotBytes(snapshot);

        if (bytes == null)
            return;

        sendBytesToConnections(bytes);
    }
}
