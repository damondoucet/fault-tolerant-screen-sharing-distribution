package main.network.protocols.basic;

import main.network.protocols.NetworkProtocolClient;
import main.util.InterruptableThreadSet;
import main.Snapshot;
import main.network.Connection;
import main.network.ConnectionFactory;
import main.util.Serialization;
import main.util.Util;
import main.network.protocols.NetworkProtocol;

import java.io.IOException;
import java.util.Arrays;

/**
 * Client for the basic network protocol.
 */
public class BasicNetworkProtocolClient<TKey> extends NetworkProtocolClient<TKey> {
    private final TKey broadcasterKey;
    private final InterruptableThreadSet threadSet;

    private Connection<TKey> connection;

    private BasicNetworkProtocolClient(ConnectionFactory<TKey> connectionFactory,
                                       TKey broadcasterKey,
                                       boolean lossy) {
        super(connectionFactory, lossy);

        this.broadcasterKey = broadcasterKey;
        this.threadSet = new InterruptableThreadSet(
                Arrays.asList(this::receiveSnapshots),
                null);

        this.connection = null;
    }

    public static <T> NetworkProtocol losslessClient(ConnectionFactory<T> connectionFactory,
                                                     T broadcasterKey) {
        return new BasicNetworkProtocolClient<>(connectionFactory, broadcasterKey, false);

    }

    public static <T> NetworkProtocol lossyClient(ConnectionFactory<T> connectionFactory,
                                                  T broadcasterKey) {
        return new BasicNetworkProtocolClient<>(connectionFactory, broadcasterKey, true);
    }

    @Override
    public void insertSnapshot(Snapshot image) {
        throw new RuntimeException(
                "BasicNetworkProtocolClient should not insert snapshot");
    }

    @Override
    public void start() {
        threadSet.start();
    }

    @Override
    public void stop() {
        threadSet.stop();
    }

    private void receiveSnapshots() {
        try {
            if (connection == null) {
                connection = connectionFactory.openConnection(broadcasterKey);
                return;  // give the thread a chance to die
            }

            byte prefix = Serialization.readByteWithTimeout(
                    connection.getInputStream(), 5000);

            if (prefix == Snapshot.SNAPSHOT_PREFIX)
                onSnapshot(readSnapshot(connection, -1));
            else
                System.err.printf("%s read illegal prefix %s from broadcaster\n",
                        connectionFactory.getKey(), prefix);
        } catch (Exception e) {
            if (connection != null)
                connection.close();

            connection = null;
            Util.printException("Error receiving Snapshots", e);
        }
    }
    @Override
    public TKey getParentKey() {
        return broadcasterKey;
    }
}
