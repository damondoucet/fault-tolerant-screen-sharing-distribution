package test.unit.network.protocols;

import main.util.Util;
import main.network.protocols.NetworkProtocol;
import main.network.protocols.basic.BasicNetworkProtocolBroadcaster;
import main.network.protocols.basic.BasicNetworkProtocolClient;
import main.network.test.TestConnectionFactory;
import main.network.test.TestConnectionManager;
import org.junit.Test;

import java.util.function.Consumer;

import static org.junit.Assert.*;

/**
 * Tests for the BasicNetworkProtocol using TestConnections.
 */
public class BasicTests {
    // How long until a client should have received the snapshot.
    private final static int CLIENT_DELAY_MILLIS = 700;

    private void runTest(int numClients, Consumer<TestState> test) {
        ProtocolFactory factory = new ProtocolFactory(TestState.BROADCASTER_KEY);
        TestState.runTest(
                numClients,
                factory::createBasicBroadcaster,
                factory::createBasicClient,
                test);
    }

    @Test
    public void testOneClientOneImage() {
        runTest(1, (state) -> {
            state.broadcaster.insertSnapshot(state.snapshots[0]);
            Util.sleepMillis(CLIENT_DELAY_MILLIS);  // give time to propagate

            assertEquals(state.snapshots[0], state.clientOutputQueues.get(0).poll());
            assertTrue(state.clientOutputQueues.get(0).isEmpty());
        });
    }

    // Stops the broadcaster, inserts a snapshot, sleeps, and ensures the
    // client doesn't receive it. Finally, starts the broadcaster again and
    // ensures the client receives it.
    @Test
    public void testBroadcasterStartStop() {
        runTest(1, (state) -> {
            state.broadcaster.stop();
            Util.sleepMillis(TestState.THREAD_DELAY_MILLIS);

            state.broadcaster.insertSnapshot(state.snapshots[0]);
            Util.sleepMillis(CLIENT_DELAY_MILLIS);

            assertTrue(state.clientOutputQueues.get(0).isEmpty());

            state.broadcaster.start();
            Util.sleepMillis(CLIENT_DELAY_MILLIS);

            assertEquals(state.snapshots[0], state.clientOutputQueues.get(0).poll());
            assertTrue(state.clientOutputQueues.get(0).isEmpty());
        });
    }

    // TODO(ddoucet): fix this
    // Stop the client, insert a snapshot, sleep. Start the client and sleep,
    // then ensure the snapshot is NOT received. Finally, insert another
    // snapshot and ensure it is received.
    /* @Test
    public void testClientStartStop() {
        runTest(1, (state) -> {
            state.clients.get(0).stop();
            Util.sleepMillis(TestState.THREAD_DELAY_MILLIS);

            state.broadcaster.insertSnapshot(state.snapshots[0]);
            Util.sleepMillis(CLIENT_DELAY_MILLIS);

            state.clients.get(0).start();
            Util.sleepMillis(TestState.THREAD_DELAY_MILLIS + CLIENT_DELAY_MILLIS);
            assertTrue(state.clientOutputQueues.get(0).isEmpty());

            // Now insert the next snapshot and ensure it propagates.
            state.broadcaster.insertSnapshot(state.snapshots[1]);
            Util.sleepMillis(CLIENT_DELAY_MILLIS);

            assertEquals(state.snapshots[1], state.clientOutputQueues.get(0).poll());
            assertTrue(state.clientOutputQueues.get(0).isEmpty());
        });
    } */

    @Test
    public void testOneClientTwoImages() {
        runTest(1, (state) -> {
            state.broadcaster.insertSnapshot(state.snapshots[0]);
            state.broadcaster.insertSnapshot(state.snapshots[1]);
            Util.sleepMillis(2 * CLIENT_DELAY_MILLIS);

            assertEquals(state.snapshots[0], state.clientOutputQueues.get(0).poll());
            assertEquals(state.snapshots[1], state.clientOutputQueues.get(0).poll());
            assertTrue(state.clientOutputQueues.get(0).isEmpty());
        });
    }


    @Test
    public void testTwoClientsOneImage() {
        runTest(2, (state) -> {
            state.broadcaster.insertSnapshot(state.snapshots[0]);
            Util.sleepMillis(CLIENT_DELAY_MILLIS);

            assertEquals(state.snapshots[0], state.clientOutputQueues.get(0).poll());
            assertTrue(state.clientOutputQueues.get(0).isEmpty());

            assertEquals(state.snapshots[0], state.clientOutputQueues.get(1).poll());
            assertTrue(state.clientOutputQueues.get(1).isEmpty());
        });
    }

    @Test
    public void testTwoClientsTwoImages() {
        runTest(2, (state) -> {
            state.broadcaster.insertSnapshot(state.snapshots[0]);
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
}
