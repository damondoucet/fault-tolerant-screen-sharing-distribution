package test.network.protocols;

import main.network.Util;
import main.network.protocols.NetworkProtocol;
import main.network.protocols.basic.BasicNetworkProtocolBroadcaster;
import main.network.protocols.basic.BasicNetworkProtocolClient;
import main.network.test.TestConnectionFactory;
import main.network.test.TestConnectionManager;
import org.junit.Before;
import org.junit.Test;
import test.ImageUtil;
import main.Snapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.*;
import static org.junit.Assert.*;

/**
 * Tests for the BasicNetworkProtocol using TestConnections.
 */
public class BasicNetworkProtocolTests {
    // How long until a client should have received the snapshot.
    private final static int CLIENT_DELAY_MILLIS = 500;

    // How long until starting/stopping a thread should have kicked in.
    private final static int THREAD_DELAY_MILLIS = 100;

    private final static String BROADCASTER_KEY = "broadcaster";
    private final static String[] CLIENT_KEYS = {"client1", "client2"};

    private Snapshot[] snapshots;

    @Before
    public void init() {
        snapshots = new Snapshot[2];
        snapshots[0] = Snapshot.losslessSnapshot(0, ImageUtil.createImage1());
        snapshots[1] = snapshots[0].createNext(ImageUtil.createImage2());
    }

    private static class TestState {
        public final TestConnectionManager manager;
        public final NetworkProtocol broadcaster;
        public final List<NetworkProtocol> clients;
        public final List<ConcurrentLinkedQueue<Snapshot>> clientOutputQueues;

        public TestState(int numClients) {
            checkArgument(numClients <= CLIENT_KEYS.length && numClients >= 0,
                    "Unexpected number of clients: %s", numClients);

            manager = new TestConnectionManager();
            broadcaster = createBroadcaster(manager);

            clients = new ArrayList<>();
            clientOutputQueues = new ArrayList<>();
            for (int i = 0; i < numClients; i++) {
                NetworkProtocol client = createClient(manager, CLIENT_KEYS[i]);
                clients.add(client);

                ConcurrentLinkedQueue<Snapshot> queue = new ConcurrentLinkedQueue<>();
                clientOutputQueues.add(queue);

                client.registerOutputQueue(queue);
            }
        }

        private static NetworkProtocol createBroadcaster(TestConnectionManager manager) {
            manager.onNewClient(BROADCASTER_KEY);
            return new BasicNetworkProtocolBroadcaster<>(
                    new TestConnectionFactory(manager, BROADCASTER_KEY));
        }

        private static NetworkProtocol createClient(TestConnectionManager manager,
                                                    String clientKey) {
            manager.onNewClient(clientKey);
            return BasicNetworkProtocolClient.losslessClient(
                    new TestConnectionFactory(manager, clientKey), BROADCASTER_KEY);
        }
    }

    // Creates the state, starts the broadcaster and clients, and waits for
    // them to set up. Then it calls the test, and finally stops the
    // broadcaster and clients.
    private void runTest(int numClients, Consumer<TestState> test) {
        TestState state = new TestState(numClients);

        state.broadcaster.start();
        for (int i = 0; i < numClients; i++)
            state.clients.get(i).start();
        Util.sleepMillis(THREAD_DELAY_MILLIS);  // give time to setup

        test.accept(state);

        for (int i = 0; i < numClients; i++)
            state.clients.get(i).stop();
        state.broadcaster.stop();
    }

    @Test
    public void testOneClientOneImage() {
        runTest(1, (state) -> {
            state.broadcaster.insertSnapshot(snapshots[0]);
            Util.sleepMillis(CLIENT_DELAY_MILLIS);  // give time to propagate

            assertEquals(snapshots[0], state.clientOutputQueues.get(0).poll());
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
            Util.sleepMillis(THREAD_DELAY_MILLIS);

            state.broadcaster.insertSnapshot(snapshots[0]);
            Util.sleepMillis(CLIENT_DELAY_MILLIS);

            assertTrue(state.clientOutputQueues.get(0).isEmpty());

            state.broadcaster.start();
            Util.sleepMillis(CLIENT_DELAY_MILLIS);

            assertEquals(snapshots[0], state.clientOutputQueues.get(0).poll());
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
            Util.sleepMillis(THREAD_DELAY_MILLIS);

            state.broadcaster.insertSnapshot(snapshots[0]);
            Util.sleepMillis(CLIENT_DELAY_MILLIS);

            state.clients.get(0).start();
            Util.sleepMillis(THREAD_DELAY_MILLIS + CLIENT_DELAY_MILLIS);
            assertTrue(state.clientOutputQueues.get(0).isEmpty());

            // Now insert the next snapshot and ensure it propagates.
            state.broadcaster.insertSnapshot(snapshots[1]);
            Util.sleepMillis(CLIENT_DELAY_MILLIS);

            assertEquals(snapshots[1], state.clientOutputQueues.get(0).poll());
            assertTrue(state.clientOutputQueues.get(0).isEmpty());
        });
    } */

    @Test
    public void testOneClientTwoImages() {
        runTest(1, (state) -> {
            state.broadcaster.insertSnapshot(snapshots[0]);
            state.broadcaster.insertSnapshot(snapshots[1]);
            Util.sleepMillis(2 * CLIENT_DELAY_MILLIS);

            assertEquals(snapshots[0], state.clientOutputQueues.get(0).poll());
            assertEquals(snapshots[1], state.clientOutputQueues.get(0).poll());
            assertTrue(state.clientOutputQueues.get(0).isEmpty());
        });
    }


    @Test
    public void testTwoClientsOneImage() {
        runTest(2, (state) -> {
            state.broadcaster.insertSnapshot(snapshots[0]);
            Util.sleepMillis(CLIENT_DELAY_MILLIS);

            assertEquals(snapshots[0], state.clientOutputQueues.get(0).poll());
            assertTrue(state.clientOutputQueues.get(0).isEmpty());

            assertEquals(snapshots[0], state.clientOutputQueues.get(1).poll());
            assertTrue(state.clientOutputQueues.get(1).isEmpty());
        });
    }

    @Test
    public void testTwoClientsTwoImages() {
        runTest(2, (state) -> {
            state.broadcaster.insertSnapshot(snapshots[0]);
            state.broadcaster.insertSnapshot(snapshots[1]);
            Util.sleepMillis(CLIENT_DELAY_MILLIS);

            assertEquals(snapshots[0], state.clientOutputQueues.get(0).poll());
            assertEquals(snapshots[1], state.clientOutputQueues.get(0).poll());
            assertTrue(state.clientOutputQueues.get(0).isEmpty());

            assertEquals(snapshots[0], state.clientOutputQueues.get(1).poll());
            assertEquals(snapshots[1], state.clientOutputQueues.get(1).poll());
            assertTrue(state.clientOutputQueues.get(1).isEmpty());
        });
    }
}
