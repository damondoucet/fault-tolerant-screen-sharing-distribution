package test.unit.network.protocols;

import main.network.protocols.NetworkProtocol;
import main.network.protocols.tree.TreeNetworkProtocol;
import main.util.Util;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.List;
import java.util.function.Consumer;

/**
 * Tests for the KaryTreeNetworkProtocol using TestConnections.
 */
public class TreeTests {
    private final static int CONNECTION_DELAY_MILLIS = 1500;

    // How long until a client should have received the snapshot.
    private final static int CLIENT_DELAY_MILLIS = 1500;

    private void runTest(int numClients, Consumer<TestState> test) {
        ProtocolFactory factory = new ProtocolFactory(TestState.BROADCASTER_KEY);
        TestState.runTest(
                numClients,
                factory::createTreeBroadcaster,
                factory::createTreeClient,
                test);
    }

    private void assertCorrectParent(String expectedParent, NetworkProtocol node) {
        TreeNetworkProtocol<String> castedNode =
                (TreeNetworkProtocol<String>)node;
        assertEquals(expectedParent, castedNode.getParentKey());
    }

    @Test
    public void testOneClientTwoImages() {
        runTest(1, (state) -> {
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

    @Test
    public void testTwoClientsTwoImages() {
        runTest(2, (state) -> {
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
        TreeNetworkProtocol<String> c0 =
                (TreeNetworkProtocol<String>)clients.get(0);

        if (c0.getParentKey().equals(TestState.BROADCASTER_KEY))
            assertCorrectParent(TestState.CLIENT_KEYS[0], clients.get(1));
        else {
            assertCorrectParent(TestState.CLIENT_KEYS[1], clients.get(0));
            assertCorrectParent(TestState.BROADCASTER_KEY, clients.get(1));
        }
    }

    @Test
    public void testTwoClientsImageFailImage() {
        runTest(2, (state) -> {
            Util.sleepMillis(CONNECTION_DELAY_MILLIS);

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

    // Create a failure such that client 0 must be client 1's parent. Then
    // change the failures so that client 1 must be client 0's parent.
    @Test
    public void testChildMustBecomeParent() {
        runTest(2, (state) -> {
            // First give time for topology information to propagate.
            Util.sleepMillis(CONNECTION_DELAY_MILLIS);

            // Now kill the connection and make sure it connects correctly.
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

    // Test setting 2's parent to be 1, 1's parent to be 3, 3's parent to be 2
    @Test
    public void testCycleException() {
        runTest(4, (state) -> {
            Util.sleepMillis(CONNECTION_DELAY_MILLIS);
            assertCorrectParent(TestState.BROADCASTER_KEY, state.clients.get(0));
            assertCorrectParent(TestState.BROADCASTER_KEY, state.clients.get(1));
            assertCorrectParent(TestState.BROADCASTER_KEY, state.clients.get(2));
            assertCorrectParent(TestState.BROADCASTER_KEY, state.clients.get(3));

            TreeNetworkProtocol<String> twoP = (TreeNetworkProtocol<String>) state.clients.get(2);
            twoP.setParent(TestState.CLIENT_KEYS[1]);
            state.broadcaster.insertSnapshot(state.snapshots[0]);
            state.broadcaster.insertSnapshot(state.snapshots[1]);
            Util.sleepMillis(2 * CLIENT_DELAY_MILLIS);

            assertEquals(state.snapshots[0], state.clientOutputQueues.get(0).poll());
            assertEquals(state.snapshots[1], state.clientOutputQueues.get(0).poll());
            assertTrue(state.clientOutputQueues.get(0).isEmpty());
        });
    }
}
