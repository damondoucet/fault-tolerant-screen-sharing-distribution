package main.network;

import main.Snapshot;
import main.util.Util;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.*;

/**
 * Holds a list of connections to clients requesting snapshots.
 */
public class ClientList<T> {
    private final T key;
    private final List<Connection<T>> connections;

    // If connectionHandler != null, we'll spawn a new thread and run the
    // connectionHandler on it.
    private final Consumer<Connection> connectionHandler;

    public ClientList(T key) {
        this(key, null);
    }

    public ClientList(T key, Consumer<Connection> connectionHandler) {
        this.key = key;
        this.connections = Collections.synchronizedList(new LinkedList<>());
        this.connectionHandler = connectionHandler;
    }

    public void addConnection(Connection<T> connection) {
        checkArgument(connection.getSource().equals(key),
                "Tried to add connection where source (%s) was not us (%s)",
                connection.getSource(), key);

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
        }
    }

    public void removeConnection(T dest) {
        connections.removeIf((connection) -> connection.getDest().equals(dest));
    }

    private byte[] getSnapshotBytes(Snapshot snapshot) {
        try {
            return snapshot.toBytes();
        } catch (IOException e) {
            Util.printException(
                    String.format("Error converting snapshot %d to bytes:\n",
                            snapshot.getFrameIndex()),
                    e);
        }
        return null;
    }

    private void sendBytesToConnections(byte[] bytes) {
        for (Iterator<Connection<T>> it  = connections.iterator(); it.hasNext(); ) {
            Connection<T> connection = it.next();
            try {
                connection.write(bytes);
            } catch (IOException e) {  // skip this client
                // TODO(ddoucet): maybe this is too aggressive?
                Util.printException("Error writing to connection", e);
                connection.close();
                it.remove();
            }
        }
    }

    public void sendSnapshot(Snapshot snapshot) {
        if (connections.size() == 0)
            return;

        byte[] bytes = getSnapshotBytes(snapshot);

        if (bytes == null)
            return;

        sendBytesToConnections(bytes);
    }
}
