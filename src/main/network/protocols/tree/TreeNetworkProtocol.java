package main.network.protocols.tree;

import main.Snapshot;
import main.network.ClientList;
import main.network.Connection;
import main.network.ConnectionFactory;
import main.network.protocols.NetworkProtocol;
import main.network.protocols.NetworkProtocolClient;
import main.util.InterruptableThreadSet;
import main.util.Serialization;
import main.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents a non-broadcaster node in the tree protocol.
 *
 * A node searches for a parent and stays with this parent as long as possible
 * (it only searches for a new parent on disconnect or after timing out a
 * read).
 *
 * The main difference between this protocol and the basic protocol is that
 * nodes in this protocol can have and act as parents other than the
 * broadcaster, and that information about the state of this tree is propagated
 * throughout the tree (see the Topology class).
 *
 * TODO(ddoucet): should probably document usage of the scanner
 *
 * TODO(ddoucet): this class is pretty bulky. I wonder how much of it I can
 * strip out and move to utility/helper methods/classes?
 */
public class TreeNetworkProtocol<TKey> extends NetworkProtocolClient<TKey> {
    private final static byte STATE_PREFIX = 0x77;
    private final static byte STATE_ACK = 0x78;  // acknowledge receipt of state

    // How long to wait between sending states to the parent in ns.
    private final static long NANO_SEND_STATE_DELAY = 100000000;  // 100ms

    private final static long TIMEOUT_MILLIS = 1000;

    private final boolean isBroadcaster;
    private final Topology<TKey> topology;
    private final ConcurrentLinkedQueue<Snapshot> snapshotQueue;
    private final InterruptableThreadSet threadSet;
    private final ClientList<TKey> clientList;

    private final AtomicReference<Connection<TKey>> parentConnection;
    private long previousSendStateNano;

    private ParentCandidateScanner<TKey> scanner;

    private TreeNetworkProtocol(ConnectionFactory<TKey> connectionFactory,
                                TKey broadcasterKey,
                                boolean lossy) {
        super(connectionFactory, lossy);

        this.isBroadcaster = connectionFactory.getKey().equals(broadcasterKey);

        this.topology = new Topology<>(broadcasterKey, connectionFactory.getKey());
        this.snapshotQueue = new ConcurrentLinkedQueue<>();
        this.threadSet = new InterruptableThreadSet(
                getThreadFuncs(isBroadcaster),
                null);
        this.clientList = new ClientList<>(
                connectionFactory.getKey(),
                this::handleChild);

        if (isBroadcaster)
            parentConnection = null;
        else
            this.parentConnection = new AtomicReference<>();
    }

    private List<Runnable> getThreadFuncs(boolean isBroadcaster) {
        if (isBroadcaster)
            return Arrays.asList(
                    this::acceptConnections,
                    this::sendSnapshot);
        else
            return Arrays.asList(
                    this::acceptConnections,
                    this::sendSnapshot,
                    this::readFromParent,
                    this::maybeSendState);
    }

    // Used for testing
    public TKey getParentKey() {
        Connection<TKey> connection = parentConnection.get();
        if (connection == null)
            return null;
        return connection.getDest();
    }

    public static <T> NetworkProtocol losslessClient(ConnectionFactory<T> connectionFactory,
                                                     T broadcasterKey) {
        return new TreeNetworkProtocol<>(
                connectionFactory, broadcasterKey, false);
    }

    public static <T> NetworkProtocol lossyClient(ConnectionFactory<T> connectionFactory,
                                                  T broadcasterKey) {
        return new TreeNetworkProtocol<>(
                connectionFactory, broadcasterKey, true);
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

    private void handleChild(Connection<TKey> child) {
        try {
            // TODO(ddoucet): automatically send current state of world

            InputStream stream = child.getInputStream();
            AtomicBoolean shouldExecute = threadSet.getShouldExecute();
            while (shouldExecute != null && shouldExecute.get()) {
                byte prefix = Serialization.readByteWithTimeout(stream, TIMEOUT_MILLIS);

                if (prefix == STATE_PREFIX) {
                    topology.updateChildInfo(child.getDest(), stream);
                    Util.threadsafeWrite(child, new byte[] { STATE_ACK });
                    Util.threadsafeWrite(child, topology.serializeExceptChild(STATE_PREFIX, child.getDest()));
                } else
                    System.err.printf("Illegal prefix %s when reading from %s\n",
                            Byte.toString(prefix), child.getDest().toString());

                Util.sleepMillis(10);
            }
        } catch (Exception e) {
            String message = String.format("%s error handling child %s\n",
                    connectionFactory.getKey(), child.getDest());
            Util.printException(message, e);
        } finally {
            // We don't need to close the connection here; the client list will
            // handle that when we return. We just need to remove the child
            // from our topology.
            topology.removeChild(child.getDest());
        }
    }

    private void closeParent() {
        Connection<TKey> connection = parentConnection.getAndSet(null);
        if (connection != null)
            connection.close();
    }

    private void disconnect() {
        closeParent();
        clientList.removeAll();
    }

    private void sendStateToParent() throws Exception {
        Connection<TKey> parent = parentConnection.get();
        if (parent != null) {
            Util.threadsafeWrite(
                    parent,
                    topology.serializeDescendantInfo(STATE_PREFIX));
        }
        previousSendStateNano = System.nanoTime();
    }

    // Sends the state to the parent every NANO_SEND_STATE_DELAY ns.
    private void maybeSendState() {
        try {
            if (System.nanoTime() - previousSendStateNano >= NANO_SEND_STATE_DELAY)
                sendStateToParent();
        } catch (Exception e) {
            closeParent();
            System.out.printf("%s error sending state to parent %s\n",
                    connectionFactory.getKey(), getParentKey());
        }
    }

    private void attemptConnection() throws Exception {
        if (scanner == null)
            scanner = topology.createParentCandidateScanner();

        TKey parent = scanner.findNewParent();
        if (parent == null) {
            disconnect();
            Util.sleepMillis(500);

            scanner.disconnect();
            parent = scanner.findNewParent();

            if (parent == null) {
                Util.sleepMillis(500);
                scanner = topology.createParentCandidateScanner();
            }
        }

        Connection<TKey> connection = connectionFactory.openConnection(parent);
        parentConnection.set(connection);
        topology.setParent(connection.getDest());
    }

    private void readFromParent() {
        TKey parentKey = getParentKey();
        try {
            // Normally, this would be unsafe, but we're the only thread that
            // would set parentConnection to a non-null value so it's safe in
            // the sense that two threads won't open connections and only one
            // will be saved.
            Connection<TKey> connection = parentConnection.get();
            if (connection == null) {
                attemptConnection();
                return;  // give the thread a chance to die
            }

            InputStream stream = connection.getInputStream();
            byte prefix = Serialization.readByteWithTimeout(stream, TIMEOUT_MILLIS);
            scanner = null;  // after we successfully read a byte

            if (prefix == Snapshot.SNAPSHOT_PREFIX)
                snapshotQueue.add(readSnapshot(connection, -1));
            else if (prefix == STATE_PREFIX)
                topology.updateNonDescendantInfo(stream);
            else if (prefix != STATE_ACK)
                System.err.printf(
                    "%s read unrecognized prefix (%s) from parent %s\n",
                        connectionFactory.getKey(),
                        prefix,
                        connection.getDest());
        } catch (Exception e) {
            closeParent();
            System.out.printf("%s error reading from parent %s\n",
                    connectionFactory.getKey(), parentKey);
        }
    }

    private void sendSnapshot() {
        Snapshot snapshot = snapshotQueue.poll();
        if (snapshot != null) {
            onSnapshot(snapshot);
            clientList.sendSnapshot(snapshot);
        }
    }
}
