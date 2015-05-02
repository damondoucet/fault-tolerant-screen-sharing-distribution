package test.unit.network;

import com.google.common.primitives.Bytes;
import main.network.test.TestConnectionFactory;
import main.network.test.TestConnectionManager;
import main.util.Util;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Tests that rate-limiting TestConnection objects works correctly.
 */
public class TestConnectionRateLimitTests {
    private final static String SOURCE_KEY = "source";
    private final static String DEST_KEY = "dest";
    private final static int KBPS = 10;

    private byte[] createData(int numBytes) {
        byte[] data = new byte[numBytes];
        for (int i = 0; i < numBytes; i++)
            data[i] = (byte)(i * 2);
        return data;
    }

    private TestClient<String> createTestClient(TestConnectionManager manager,
                                                String key) {
        manager.onNewClient(key);
        return new TestClient<>(
                new TestConnectionFactory(manager, key),
                key);
    }

    /*
     * Recall that the TestConnection releases data every 0.1 seconds.
     *
     * This tests that the connection is rate-limited by computing how many
     * bytes can be sent per 0.1s, and sending one more than that, then waiting
     * until all bytes have been received.
     *
     * It ensures that receiving all bytes takes some time.
     *
     * Note that it assumes the connection has ALREADY been rate-limited.
     */
    private void testConnectionIsRateLimited(ConnectionPair connections,
                                             int kbps) throws IOException {
        // (kbps kb/s * 1000 b/kb * 0.1s) + 1 byte
        byte[] data = createData(kbps * 100 + 1);
        connections.sourceToDest.write(data);

        long start = System.nanoTime();
        byte[] received = Util.read(
                connections.destToSource.getInputStream(), data.length);
        long end = System.nanoTime();
        double durationSeconds = (long)((end - start) / 1e9);

        assertEquals(Bytes.asList(data), Bytes.asList(received));

        // The fastest this should run is 0.1s, and the slowest is 0.2s. We
        // give slightly more acceptable bounds by accepting 0.05s through
        // 0.25s.
        /* System.out.println(
                "Should have taken between 0.1s and 0.2s " +
                    "(fudge factor allows 0.05s to 0.25s), took " +
                    durationSeconds + "s"); */
        assertTrue("Took " + durationSeconds + "s",
                durationSeconds >= 0.05 && durationSeconds <= 0.25);
    }

    // Tests that a connection can be rate-limited.
    @Test(timeout=500)
    public void testConnectionLimited() throws IOException {
        TestConnectionManager manager = new TestConnectionManager();

        TestClient<String> sourceClient = createTestClient(manager, SOURCE_KEY),
                destClient = createTestClient(manager, DEST_KEY);

        ConnectionPair connections = TestClient.connect(sourceClient, destClient);

        manager.setRateLimit(SOURCE_KEY, DEST_KEY, KBPS);
        testConnectionIsRateLimited(connections, KBPS);
    }

    // Tests that the rate-limit works both as source->dest and dest->source.
    @Test(timeout=500)
    public void testLimitBidirectional() throws IOException {
        TestConnectionManager manager = new TestConnectionManager();

        TestClient<String> sourceClient = createTestClient(manager, SOURCE_KEY),
                destClient = createTestClient(manager, DEST_KEY);

        ConnectionPair connections = TestClient.connect(sourceClient, destClient);

        manager.setRateLimit(DEST_KEY, SOURCE_KEY, KBPS);
        testConnectionIsRateLimited(connections, KBPS);
    }

    // Setting the rate limit before creating the connection should still
    // rate-limit the connection.
    @Test(timeout=500)
    public void testSettingBeforeCreation() throws IOException {
        TestConnectionManager manager = new TestConnectionManager();

        TestClient<String> sourceClient = createTestClient(manager, SOURCE_KEY),
                destClient = createTestClient(manager, DEST_KEY);

        manager.setRateLimit(SOURCE_KEY, DEST_KEY, KBPS);
        ConnectionPair connections = TestClient.connect(sourceClient, destClient);

        testConnectionIsRateLimited(connections, KBPS);
    }

    // Creating a connection, rate-limiting it, closing the connection, then
    // re-opening the connection should still be rate-limited.
    @Test(timeout=500)
    public void testReopeningConnection() throws IOException  {
        TestConnectionManager manager = new TestConnectionManager();

        TestClient<String> sourceClient = createTestClient(manager, SOURCE_KEY),
                destClient = createTestClient(manager, DEST_KEY);

        ConnectionPair connections = TestClient.connect(sourceClient, destClient);
        manager.setRateLimit(SOURCE_KEY, DEST_KEY, KBPS);

        connections.sourceToDest.close();
        connections.destToSource.close();

        connections = TestClient.connect(sourceClient, destClient);
        testConnectionIsRateLimited(connections, KBPS);
    }

    // Tests that setting the rate limit, creating the connection, then setting
    // the limit to a higher value works correctly.
    @Test(timeout=500)
    public void testUpdatingRateLimit() throws IOException  {
        TestConnectionManager manager = new TestConnectionManager();

        TestClient<String> sourceClient = createTestClient(manager, SOURCE_KEY),
                destClient = createTestClient(manager, DEST_KEY);

        manager.setRateLimit(SOURCE_KEY, DEST_KEY, KBPS);
        ConnectionPair connections = TestClient.connect(sourceClient, destClient);

        manager.setRateLimit(SOURCE_KEY, DEST_KEY, 2 * KBPS);
        testConnectionIsRateLimited(connections, 2 * KBPS);
    }
}