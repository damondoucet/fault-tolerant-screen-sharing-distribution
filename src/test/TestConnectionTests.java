package test;

import main.network.Connection;
import main.network.Util;
import main.network.test.TestConnectionFactory;
import main.network.test.TestConnectionManager;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tests the TestConnection and associated classes for sending and receiving
 * data.
 *
 * Note: if some of these tests are failing, consider upping the timeout. Be
 * careful about that; this stuff shouldn't take very long to complete, so
 * there might be a bigger problem.
 */
public class TestConnectionTests {
    private static class TestClient {
        private final TestConnectionFactory connectionFactory;
        private final String key;

        public TestClient(TestConnectionManager manager, String key) {
            this.connectionFactory = new TestConnectionFactory(manager, key);
            this.key = key;

            manager.onNewClient(key);
        }

        public String getKey() {
            return key;
        }

        /**
         * Spawns a thread that attemps to create a connection with the given
         * destination. It returns immediately and sets conn when the thread
         * completes (i.e., when the connection is created).
         *
         * @param dest The key to connect to
         * @param conn The connection to eventually be set when created
         */
        public void spawnConnection(String dest,
                                    AtomicReference<Connection<String>> conn) {
            new Thread(() -> {
                conn.set(connectionFactory.openConnection(dest));
            }).start();
        }

        public void spawnAcceptConnection(AtomicReference<Connection<String>> conn) {
            new Thread(() -> {
                conn.set(connectionFactory.acceptConnection());
            }).start();
        }
    }

    // Represents the result of a connection being established.
    private static class ConnectionPair {
        public final Connection<String> sourceToDest;
        public final Connection<String> destToSource;

        public ConnectionPair(Connection<String> sourceToDest,
                              Connection<String> destToSource) {
            this.sourceToDest = sourceToDest;
            this.destToSource = destToSource;
        }
    }

    // Connects two TestClients
    private ConnectionPair connect(TestClient source,
                                   TestClient dest) {
        AtomicReference<Connection<String>> aToBRef = new AtomicReference<>(),
                bToARef = new AtomicReference<>();

        source.spawnConnection(dest.getKey(), aToBRef);
        dest.spawnAcceptConnection(bToARef);

        while (aToBRef.get() == null || bToARef.get() == null)
            Util.sleep(10);

        Connection<String> aToB = aToBRef.get(),
                bToA = bToARef.get();

        return new ConnectionPair(aToB, bToA);
    }

    // Tests that the data is correctly sent from source to dest.
    private void testSending(Connection<String> sourceToDest,
                             Connection<String> destToSource,
                             byte[] data) throws IOException {
        sourceToDest.write(data);

        byte[] read = new byte[data.length];
        assertEquals(data.length, destToSource.read(read, data.length));
        assertArrayEquals(data, read);
    }

    private ConnectionPair createConnectionPair(String source, String dest) {
        TestConnectionManager manager = new TestConnectionManager();

        TestClient sourceClient = new TestClient(manager, source),
                destClient = new TestClient(manager, dest);

        return connect(sourceClient, destClient);
    }

    @Test(timeout=100)
    public void testConnectionEstablished() {
        ConnectionPair connections = createConnectionPair("a", "b");

        assertEquals("a", connections.sourceToDest.getSource());
        assertEquals("b", connections.sourceToDest.getDest());

        assertEquals("b", connections.destToSource.getSource());
        assertEquals("a", connections.destToSource.getDest());
    }

    @Test(timeout=100)
    public void testConnectionsCanTransmit() throws IOException {
        ConnectionPair connections = createConnectionPair("a", "b");

        byte[] data1 = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 9, 8, 7, 6, 5};
        byte[] data2 = new byte[]{1, 3, 3, 7, 1, 2, 3, 4, 1, 1, 2, 3, 5, 8, 13};
        testSending(connections.sourceToDest, connections.destToSource, data1);
        testSending(connections.destToSource, connections.sourceToDest, data2);
    }

    // TODO(ddoucet): test multiple connections
}
