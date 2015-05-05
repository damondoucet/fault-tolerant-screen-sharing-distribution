package test.unit.network;

import main.network.test.TestConnectionFactory;
import main.network.test.TestConnectionManager;
import org.junit.Test;

import static org.junit.Assert.*;

import java.io.IOException;

/**
 * Tests the TestConnection and associated classes for sending and receiving
 * data.
 *
 * Note: if some of these tests are failing, consider upping the timeout. Be
 * careful about that; this stuff shouldn't take very long to complete, so
 * there might be a bigger problem.
 */
public class TestConnectionTests {
    private TestClient<String> createTestClient(TestConnectionManager manager,
                                                String key) {
        manager.onNewClient(key);
        return new TestClient<>(
                new TestConnectionFactory(manager, key),
                key);
    }

    private ConnectionPair<String> createConnectionPair(String source, String dest) {
        TestConnectionManager manager = new TestConnectionManager();

        TestClient<String> sourceClient = createTestClient(manager, source),
                destClient = createTestClient(manager, dest);

        return TestClient.connect(sourceClient, destClient);
    }

    @Test(timeout=100)
    public void testConnectionEstablished() {
        ConnectionPair<String> connections = createConnectionPair("a", "b");

        assertEquals("a", connections.sourceToDest.getSource());
        assertEquals("b", connections.sourceToDest.getDest());

        assertEquals("b", connections.destToSource.getSource());
        assertEquals("a", connections.destToSource.getDest());
    }

    @Test(timeout=100)
    public void testConnectionsCanTransmit() throws IOException {
        ConnectionPair<String> connections = createConnectionPair("a", "b");

        byte[] data1 = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 9, 8, 7, 6, 5};
        byte[] data2 = new byte[]{1, 3, 3, 7, 1, 2, 3, 4, 1, 1, 2, 3, 5, 8, 13};
        connections.testSending(data1);
        connections.testBackwardsSending(data2);
    }

    // Tests that connections can be reopened after closing.
    @Test(timeout=100)
    public void testReopenAfterClose() throws IOException {
        TestConnectionManager manager = new TestConnectionManager();

        TestClient<String> sourceClient = createTestClient(manager, "a"),
                destClient = createTestClient(manager, "b");

        ConnectionPair connections = TestClient.connect(sourceClient, destClient);
        connections.sourceToDest.close();
        connections.destToSource.close();

        connections = TestClient.connect(sourceClient, destClient);
        byte[] data = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 9, 8, 7, 6, 5};
        connections.testSending(data);
    }
}
