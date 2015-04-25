package main.network;

import main.Snapshot;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static com.google.common.base.Preconditions.*;

/**
 * Holds a list of connections to clients requesting snapshots.
 */
public class ClientList<T> {
    private final T key;
    private final List<Connection<T>> connections;

    public ClientList(T key) {
        this.key = key;
        connections = Collections.synchronizedList(new LinkedList<>());
    }

    public void addConnection(Connection<T> connection) {
        checkArgument(connection.getSource().equals(key),
                "Tried to add connection where source (%s) was not us (%s)",
                connection.getSource(), key);

        connections.add(connection);
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

    // TODO(ddoucet): this doesn't remove closed clients
    private void sendBytesToConnections(byte[] bytes) {
        for (Connection<T> connection : connections) {
            try {
                connection.write(bytes);
            } catch (IOException e) {  // skip this client
                Util.printException("Error writing to connection", e);
            }
        }
    }

    public void sendSnapshot(Snapshot snapshot) {
        byte[] bytes = getSnapshotBytes(snapshot);

        if (bytes == null)
            return;

        sendBytesToConnections(bytes);
    }
}
