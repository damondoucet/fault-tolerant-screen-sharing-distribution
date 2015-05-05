package main.network.protocols.basic;

import main.network.protocols.NetworkProtocolClient;
import main.util.InterruptableThreadSet;
import main.Snapshot;
import main.network.Connection;
import main.network.ConnectionFactory;
import main.util.Util;
import main.network.protocols.NetworkProtocol;

import java.io.IOException;
import java.util.Arrays;

/**
 * Client for the basic network protocol.
 */
public class BasicNetworkProtocolClient<T> extends NetworkProtocolClient<T> {
    private final T broadcasterKey;
    private final InterruptableThreadSet threadSet;

    private Connection<T> connection;

    private BasicNetworkProtocolClient(ConnectionFactory<T> connectionFactory,
                                       T broadcasterKey,
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

            onSnapshot(readSnapshot(connection));
        } catch (IOException e) {
            if (connection != null)
                connection.close();

            connection = null;
            Util.printException("Error receiving Snapshots", e);
        }
    }
}
