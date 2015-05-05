package test.unit.network.protocols;

import main.network.protocols.NetworkProtocol;
import main.network.protocols.kary_tree.KaryTreeNetworkProtocol;
import main.network.test.TestConnectionFactory;
import main.network.test.TestConnectionManager;
import main.util.Util;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.List;
import java.util.function.Consumer;

/**
 * Tests for the KaryTreeNetworkProtocol using TestConnections.
 */
public class KaryTreeTests {
    private final static int CONNECTION_DELAY_MILLIS = 200;

    // How long until a client should have received the snapshot.
    private final static int CLIENT_DELAY_MILLIS = 700;

    private void runTest(int numClients, int k, Consumer<TestState> test) {
        KaryTreeNodeFactory factory = new KaryTreeNodeFactory(k);
        TestState.runTest(
                numClients,
                (manager) -> factory.createBroadcaster(manager, TestState.BROADCASTER_KEY),
                (manager, key) -> factory.createClient(manager, TestState.BROADCASTER_KEY, key),
                test);
    }

    private void runUnaryTest(int numClients, Consumer<TestState> test) {
        runTest(numClients, 1, test);
    }

    private void runBinaryTest(int numClients, Consumer<TestState> test) {
        runTest(numClients, 2, test);
    }

    private void assertCorrectParent(String expectedParent, NetworkProtocol node) {
        KaryTreeNetworkProtocol<String> castedNode =
                (KaryTreeNetworkProtocol<String>)node;
        assertEquals(expectedParent, castedNode.getParentKey());
    }

    @Test
    public void testOneClientTwoImages() {
        runUnaryTest(1, (state) -> {
            Util.sleepMillis(CONNECTION_DELAY_MILLIS);
            assertCorrectParent(TestState.BROADCASTER_KEY, state.clients.get(0));

            state.broadcaster.insertSnapshot(state.snapshots[0]);
            state.broadcaster.insertSnapshot(state.snapshots[1]);
            Util.sleepMillis(2 * CLIENT_DELAY_MILLIS);

            assertEquals(state.snapshots[0], state.clientOutputQueues.get(0).poll());
            assertEquals(state.snapshots[1], state.clientOutputQueues.get(0).poll());
            assertTrue(state.clientOutputQueues.get(0).isEmpty());
        });
    }

    // With two clients, K=2 should have both connecting to the broadcaster
    @Test
    public void tesTwoClientsTwoImages() {
        runBinaryTest(2, (state) -> {
            Util.sleepMillis(CONNECTION_DELAY_MILLIS);
            assertCorrectParent(TestState.BROADCASTER_KEY, state.clients.get(0));
            assertCorrectParent(TestState.BROADCASTER_KEY, state.clients.get(1));

            state.broadcaster.insertSnapshot(state.snapshots[0]);
            state.broadcaster.insertSnapshot(state.snapshots[1]);
            Util.sleepMillis(2 * CLIENT_DELAY_MILLIS);

            assertEquals(state.snapshots[0], state.clientOutputQueues.get(0).poll());
            assertEquals(state.snapshots[1], state.clientOutputQueues.get(0).poll());
            assertTrue(state.clientOutputQueues.get(0).isEmpty());

            assertEquals(state.snapshots[0], state.clientOutputQueues.get(1).poll());
            assertEquals(state.snapshots[1], state.clientOutputQueues.get(1).poll());
            assertTrue(state.clientOutputQueues.get(1).isEmpty());
        });
    }

    // Check that exactly one node has the broadcaster as the parent, and the
    // other has the other as its parent.
    // Expects exactly two clients.
    private void checkUnary(List<NetworkProtocol> clients) {
        // This is sort of gross. Sorry. :/
        KaryTreeNetworkProtocol<String> c0 =
                (KaryTreeNetworkProtocol<String>)clients.get(0);

        if (c0.getParentKey().equals(TestState.BROADCASTER_KEY))
            assertCorrectParent(TestState.CLIENT_KEYS[0], clients.get(1));
        else {
            assertCorrectParent(TestState.CLIENT_KEYS[1], clients.get(0));
            assertCorrectParent(TestState.BROADCASTER_KEY, clients.get(1));
        }
    }

    // Test that with K=1 and no failures, only one node is connected to the
    // broadcaster.
    @Test
    public void testUnaryTreeTwoClients() {
        runBinaryTest(2, (state) -> {
            Util.sleepMillis(CONNECTION_DELAY_MILLIS);
            checkUnary(state.clients);

            state.broadcaster.insertSnapshot(state.snapshots[0]);
            state.broadcaster.insertSnapshot(state.snapshots[1]);
            Util.sleepMillis(2 * CLIENT_DELAY_MILLIS);

            assertEquals(state.snapshots[0], state.clientOutputQueues.get(0).poll());
            assertEquals(state.snapshots[1], state.clientOutputQueues.get(0).poll());
            assertTrue(state.clientOutputQueues.get(0).isEmpty());

            assertEquals(state.snapshots[0], state.clientOutputQueues.get(1).poll());
            assertEquals(state.snapshots[1], state.clientOutputQueues.get(1).poll());
            assertTrue(state.clientOutputQueues.get(1).isEmpty());
        });
    }

    @Test
    public void testTwoClientsImageFailImage() {
        runBinaryTest(2, (state) -> {
            Util.sleepMillis(CONNECTION_DELAY_MILLIS);
            checkUnary(state.clients);

            state.broadcaster.insertSnapshot(state.snapshots[0]);
            Util.sleepMillis(CLIENT_DELAY_MILLIS);

            // Now insert failure and wait for reconnection, then insert the
            // next snapshot.
            state.manager.setRateLimit(TestState.BROADCASTER_KEY, TestState.CLIENT_KEYS[0], 0);
            Util.sleepMillis(CONNECTION_DELAY_MILLIS);
            checkUnary(state.clients);

            state.broadcaster.insertSnapshot(state.snapshots[1]);
            Util.sleepMillis(CLIENT_DELAY_MILLIS);

            assertEquals(state.snapshots[0], state.clientOutputQueues.get(0).poll());
            assertEquals(state.snapshots[1], state.clientOutputQueues.get(0).poll());
            assertTrue(state.clientOutputQueues.get(0).isEmpty());

            assertEquals(state.snapshots[0], state.clientOutputQueues.get(1).poll());
            assertEquals(state.snapshots[1], state.clientOutputQueues.get(1).poll());
            assertTrue(state.clientOutputQueues.get(1).isEmpty());
        });
    }

    // Check that the K=1 is only a suggestion. Create a unary tree then
    // destroy the connection between the two nodes and make sure both clients
    // still receive the snapshot.
    @Test
    public void testFailureBetweenNodes() {
        runUnaryTest(2, (state) -> {
            Util.sleepMillis(CONNECTION_DELAY_MILLIS);
            checkUnary(state.clients);

            // Now insert failure and wait for reconnection, then insert the
            // snapshot.
            state.manager.setRateLimit(TestState.CLIENT_KEYS[0], TestState.CLIENT_KEYS[1], 0);
            Util.sleepMillis(CONNECTION_DELAY_MILLIS);

            state.broadcaster.insertSnapshot(state.snapshots[0]);
            Util.sleepMillis(CLIENT_DELAY_MILLIS);

            assertEquals(state.snapshots[0], state.clientOutputQueues.get(0).poll());
            assertTrue(state.clientOutputQueues.get(0).isEmpty());

            assertEquals(state.snapshots[0], state.clientOutputQueues.get(1).poll());
            assertTrue(state.clientOutputQueues.get(1).isEmpty());
        });
    }

    // Create a failure such that client 0 must be client 1's parent. Then
    // change the failures so that client 1 must be client 0's parent.
    @Test
    public void testChildMustBecomeParent() {
        runBinaryTest(2, (state) -> {
            state.manager.setRateLimit(TestState.BROADCASTER_KEY, TestState.CLIENT_KEYS[1], 0);
            Util.sleepMillis(CONNECTION_DELAY_MILLIS);
            assertCorrectParent(TestState.BROADCASTER_KEY, state.clients.get(0));
            assertCorrectParent(TestState.CLIENT_KEYS[0], state.clients.get(1));

            // Now change the failure
            state.manager.setRateLimit(TestState.BROADCASTER_KEY, TestState.CLIENT_KEYS[1], 1000);
            state.manager.setRateLimit(TestState.BROADCASTER_KEY, TestState.CLIENT_KEYS[0], 0);
            Util.sleepMillis(CONNECTION_DELAY_MILLIS);

            // Check that we have the correct setup.
            assertCorrectParent(TestState.BROADCASTER_KEY, state.clients.get(1));
            assertCorrectParent(TestState.CLIENT_KEYS[1], state.clients.get(0));

            // Check that the snapshot goes through.
            state.broadcaster.insertSnapshot(state.snapshots[0]);
            Util.sleepMillis(CLIENT_DELAY_MILLIS);

            assertEquals(state.snapshots[0], state.clientOutputQueues.get(0).poll());
            assertTrue(state.clientOutputQueues.get(0).isEmpty());

            assertEquals(state.snapshots[0], state.clientOutputQueues.get(1).poll());
            assertTrue(state.clientOutputQueues.get(1).isEmpty());
        });
    }
}
