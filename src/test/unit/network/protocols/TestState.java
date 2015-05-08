package test.unit.network.protocols;

import main.Snapshot;
import main.network.protocols.NetworkProtocol;
import main.network.test.TestConnectionManager;
import main.util.Util;
import test.unit.ImageUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * A helper class for running protocol tests.
 */
public class TestState {
    // How long until starting/stopping a thread should have kicked in.
    public final static int THREAD_DELAY_MILLIS = 100;

    public final static String BROADCASTER_KEY = "broadcaster";
    public final static String[] CLIENT_KEYS = {"client0", "client1", "client2", "client3"};

    public final Snapshot[] snapshots;

    public final TestConnectionManager manager;
    public final NetworkProtocol broadcaster;
    public final List<NetworkProtocol> clients;
    public final List<ConcurrentLinkedQueue<Snapshot>> clientOutputQueues;

    public TestState(int numClients,
                     Function<TestConnectionManager, NetworkProtocol> createBroadcaster,
                     BiFunction<TestConnectionManager, String, NetworkProtocol> createClient) {
        checkArgument(numClients <= CLIENT_KEYS.length && numClients >= 0,
                "Unexpected number of clients: %s", numClients);

        snapshots = new Snapshot[2];
        snapshots[0] = Snapshot.losslessSnapshot(0, ImageUtil.createImage1());
        snapshots[1] = snapshots[0].createNext(ImageUtil.createImage2());

        manager = new TestConnectionManager();
        broadcaster = createBroadcaster.apply(manager);

        clients = new ArrayList<>();
        clientOutputQueues = new ArrayList<>();
        for (int i = 0; i < numClients; i++) {
            NetworkProtocol client = createClient.apply(manager, CLIENT_KEYS[i]);
            clients.add(client);

            ConcurrentLinkedQueue<Snapshot> queue = new ConcurrentLinkedQueue<>();
            clientOutputQueues.add(queue);

            client.registerOutputQueue(queue);
        }
    }

    // Creates the state, starts the broadcaster and clients, and waits for
    // them to set up. Then it calls the test, and finally stops the
    // broadcaster and clients.
    public static void runTest(int numClients,
                               Function<TestConnectionManager, NetworkProtocol> createBroadcaster,
                               BiFunction<TestConnectionManager, String, NetworkProtocol> createClient,
                               Consumer<TestState> test) {
        TestState state = new TestState(numClients, createBroadcaster, createClient);

        state.broadcaster.start();
        for (int i = 0; i < numClients; i++)
            state.clients.get(i).start();
        Util.sleepMillis(THREAD_DELAY_MILLIS);  // give time to setup

        test.accept(state);

        for (int i = 0; i < numClients; i++)
            state.clients.get(i).stop();
        state.broadcaster.stop();
    }
}
