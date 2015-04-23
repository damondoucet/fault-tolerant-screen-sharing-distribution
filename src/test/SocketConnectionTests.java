package test;

import main.network.SocketConnectionFactory;
import main.network.SocketInformation;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Tests SocketConnection and SocketConnectionFactory.
 */
public class SocketConnectionTests {
    private final static int PORT1 = 5555;
    private final static int PORT2 = 6666;

    private TestClient<SocketInformation> createTestClient(int port)
            throws IOException {
        SocketInformation info = new SocketInformation("127.0.0.1", port);
        SocketConnectionFactory factory = SocketConnectionFactory.fromSocketInfo(info);

        return new TestClient<>(factory, info);
    }

    private ConnectionPair<SocketInformation> createConnectionPair(int port1, int port2)
            throws IOException {
        TestClient<SocketInformation> sourceClient = createTestClient(port1),
                destClient = createTestClient(port2);

        return TestClient.connect(sourceClient, destClient);
    }

    @Test(timeout=100)
    public void testSocketsConnect() throws IOException {
        ConnectionPair<SocketInformation> connections = createConnectionPair(PORT1, PORT2);

        assertEquals(PORT1, connections.sourceToDest.getSource().port);
        assertEquals(PORT2, connections.destToSource.getSource().port);

        connections.source.getConnectionFactory().close();
        connections.dest.getConnectionFactory().close();
    }

    @Test(timeout=100)
    public void testSocketsSendData() throws IOException {
        ConnectionPair<SocketInformation> connections = createConnectionPair(PORT1, PORT2);

        byte[] data1 = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 9, 8, 7, 6, 5};
        byte[] data2 = new byte[]{1, 3, 3, 7, 1, 2, 3, 4, 1, 1, 2, 3, 5, 8, 13};
        connections.testSending(data1);
        connections.testBackwardsSending(data2);

        connections.source.getConnectionFactory().close();
        connections.dest.getConnectionFactory().close();
    }
}
