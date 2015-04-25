package main.network.protocols.basic;

import main.InterruptableThreadSet;
import main.Snapshot;
import main.network.Connection;
import main.network.ConnectionFactory;
import main.network.Util;
import main.network.protocols.NetworkProtocol;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Client for the basic network protocol.
 */
public class BasicNetworkProtocolClient<T> extends BasicNetworkProtocol<T> {
    private final T broadcasterKey;
    private final InterruptableThreadSet threadSet;
    private final boolean lossy;

    private Connection<T> connection;

    public BasicNetworkProtocolClient(ConnectionFactory<T> connectionFactory,
                                      T broadcasterKey,
                                      boolean lossy) {
        super(connectionFactory);

        this.broadcasterKey = broadcasterKey;
        this.threadSet = new InterruptableThreadSet(
                Arrays.asList(() -> receiveSnapshots()),
                null);
        this.lossy = lossy;

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

            onSnapshot(Snapshot.fromInputStream(connection.getInputStream(), lossy));
        } catch (IOException e) {
            if (connection != null)
                connection.close();

            connection = null;
            Util.printException("Error receiving Snapshots", e);
        }
    }
}
