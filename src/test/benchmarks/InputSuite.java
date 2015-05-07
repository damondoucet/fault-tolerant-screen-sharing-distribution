package test.benchmarks;

import main.network.protocols.NetworkProtocol;
import main.network.protocols.basic.BasicNetworkProtocolBroadcaster;
import main.network.protocols.basic.BasicNetworkProtocolClient;
import main.network.test.TestConnectionFactory;
import main.network.test.TestConnectionManager;
import org.junit.Before;
import org.junit.Test;
import test.unit.network.protocols.KaryTreeNodeFactory;

import java.util.Arrays;
import java.util.List;

/**
 * Defines the set of benchmarks to be used and the set of protocols to test.
 *
 * Runs all benchmarks and dumps output to stdout.
 */
public class InputSuite {
    // For descriptions of these inputs, see above their initializations in
    // init().

    // TODO(ddoucet): what about large benchmarks? probably make another class?
    private Input oneClientNoFailures;
    private Input oneClientSlowNetwork;
    private Input twoClientsNoFailures;
    private Input twoClientsOneFailure;
    private Input oneIsolatedThenNeeded;
    private Input chain;

    // -1 for broadcaster
    private RateLimitSchedule.RateLimitInstance createRateLimitInstance(int clientOne, int clientTwo, int kbps) {
        String a = clientOne == -1 ? Input.BROADCASTER : Input.getClientName(clientOne);
        String b = clientTwo == -1 ? Input.BROADCASTER : Input.getClientName(clientTwo);

        return new RateLimitSchedule.RateLimitInstance(a, b, kbps);
    }

    @Before
    public void init() {
        oneClientNoFailures =
                new Input("1 client no failures", 1,
                        new RateLimitSchedule.Builder()
                            .setNumRounds(4)
                        .build());

        oneClientSlowNetwork =
                new Input("1 client slow network", 1,
                        new RateLimitSchedule.Builder()
                            .setNumRounds(4)
                            .setRound(0,
                                    createRateLimitInstance(-1, 0, 5))
                        .build());

        twoClientsNoFailures =
                new Input("2 clients no failures", 2,
                        new RateLimitSchedule.Builder()
                            .setNumRounds(4)
                        .build());

        twoClientsOneFailure =
                new Input("2 clients 1 failure", 2,
                        new RateLimitSchedule.Builder()
                            .setNumRounds(4)
                            .setRound(2,
                                    createRateLimitInstance(-1, 0, 0))
                        .build());

        // Initially, one client can only connect to the broadcaster, and
        // everyone else can connect to everyone else. Then, nobody can connect
        // to anyone except the initially isolated one, who can connect to
        // everyone. In this case, client 0 is the initially isolated one.
        oneIsolatedThenNeeded =
                new Input("one isolated then needed", 4,
                        new RateLimitSchedule.Builder()
                            .setNumRounds(4)
                            .setRound(0,
                                    createRateLimitInstance(0, 1, 0),
                                    createRateLimitInstance(0, 2, 0),
                                    createRateLimitInstance(0, 3, 0))
                            .setRound(2,
                                    // Unisolate client 0.
                                    createRateLimitInstance(0, 1, RateLimitSchedule.DEFAULT_KBPS),
                                    createRateLimitInstance(0, 2, RateLimitSchedule.DEFAULT_KBPS),
                                    createRateLimitInstance(0, 3, RateLimitSchedule.DEFAULT_KBPS),

                                    // Isolate everyone else.
                                    createRateLimitInstance(-1, 1, 0),
                                    createRateLimitInstance(-1, 2, 0),
                                    createRateLimitInstance(-1, 3, 0),
                                    createRateLimitInstance(1, 2, 0),
                                    createRateLimitInstance(1, 3, 0),
                                    createRateLimitInstance(2, 3, 0))
                            .build());

        // Nodes can only connect to nodes directly before them or after them
        // in the chain.
        chain =
                new Input("chain", 4,
                        new RateLimitSchedule.Builder()
                            .setNumRounds(4)
                            .setRound(0,
                                    createRateLimitInstance(-1, 1, 0),
                                    createRateLimitInstance(-1, 2, 0),
                                    createRateLimitInstance(-1, 3, 0),
                                    createRateLimitInstance(1, 3, 0))
                        .build());
    }

    private static NetworkProtocol createBasicBroadcaster(TestConnectionManager manager, String key) {
        NetworkProtocol broadcaster = new BasicNetworkProtocolBroadcaster<>(
                new TestConnectionFactory(manager, key));
        manager.onNewClient(key);
        return broadcaster;
    }

    private static NetworkProtocol createBasicClient(TestConnectionManager manager, String key) {
         NetworkProtocol client = BasicNetworkProtocolClient.lossyClient(
                 new TestConnectionFactory(manager, key), Input.BROADCASTER);
        manager.onNewClient(key);
        return client;
    }

    private void printResultSet(ResultSet<String> resultSet) {
        resultSet.print();
    }

    private ResultSet<String> runBasic(Input input) throws Exception {
        TestConnectionManager manager = new TestConnectionManager();
        return new Runner(
                () -> createBasicBroadcaster(manager, Input.BROADCASTER),
                key -> createBasicClient(manager, key),
                manager,
                input).run();
    }

    private ResultSet<String> runKaryTree(int k, Input input) throws Exception {
        TestConnectionManager manager = new TestConnectionManager();
        KaryTreeNodeFactory factory = new KaryTreeNodeFactory(k);
        return new Runner(
                () -> factory.createBroadcaster(manager, Input.BROADCASTER),
                key -> factory.createClient(manager, Input.BROADCASTER, key),
                manager,
                input).run();
    }

    private ResultSet<String> runBinaryTree(Input input) throws Exception {
        return runKaryTree(2, input);
    }

    @Test
    public void benchmarkBasicProtocol() throws Exception {
        List<Input> inputs = Arrays.asList(oneClientNoFailures, oneClientSlowNetwork);
        for (Input input : inputs)
            printResultSet(runBasic(input));
    }

    // We don't test different values of K because these are pretty small
    // benchmarks (no more than two clients). With larger numbers of clients,
    // different values of K should be tested.
    // TODO(ddoucet): uncomment this when it gets fixed
    /* @Test
    public void benchmarkBinaryTreeProtocol() throws Exception {
        List<Input> inputs = Arrays.asList(
                oneClientNoFailures,
                oneClientSlowNetwork,
                twoClientsNoFailures,
                twoClientsOneFailure,
                oneIsolatedThenNeeded,
                chain);

        for (Input input : inputs)
            printResultSet(runBinaryTree(input));
    } */
}
