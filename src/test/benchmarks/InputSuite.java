package test.benchmarks;

import main.network.protocols.NetworkProtocol;
import main.network.protocols.basic.BasicNetworkProtocolBroadcaster;
import main.network.protocols.basic.BasicNetworkProtocolClient;
import main.network.test.TestConnectionFactory;
import main.network.test.TestConnectionManager;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Defines the set of benchmarks to be used and the set of protocols to test.
 *
 * Runs all benchmarks and dumps output to stdout.
 */
public class InputSuite {
    private List<Input> inputs;

    @Before
    public void init() {
        inputs = Arrays.asList(
                new Input("1 client no failures", 1,
                        new RateLimitSchedule.Builder()
                            .setNumRounds(4)
                            .build()),

                new Input("1 client slow network", 1,
                        new RateLimitSchedule.Builder()
                            .setNumRounds(4)
                            .setRound(0,
                                    new RateLimitSchedule.RateLimitInstance(
                                            Input.BROADCASTER,
                                            Input.getClientName(0),
                                            5))
                        .build()),

                new Input("2 clients no failures", 2,
                        new RateLimitSchedule.Builder()
                            .setNumRounds(4)
                            .build()),

                new Input("2 clients 1 failure", 2,
                        new RateLimitSchedule.Builder()
                            .setNumRounds(4)
                            .setRound(2,
                                new RateLimitSchedule.RateLimitInstance(
                                        Input.BROADCASTER,
                                        Input.getClientName(0),
                                        0
                                ))
                            .build())
        );
    }

    private static NetworkProtocol createBroadcaster(TestConnectionManager manager, String key) {
        NetworkProtocol broadcaster = new BasicNetworkProtocolBroadcaster<>(
                new TestConnectionFactory(manager, key));
        manager.onNewClient(key);
        return broadcaster;
    }

    private static NetworkProtocol createClient(TestConnectionManager manager, String key) {
         NetworkProtocol client = BasicNetworkProtocolClient.lossyClient(
                 new TestConnectionFactory(manager, key), Input.BROADCASTER);
        manager.onNewClient(key);
        return client;
    }

    private ResultSet<String> runBasic(Input input) {
        TestConnectionManager manager = new TestConnectionManager();
        return new Runner(
                (key) -> createBroadcaster(manager, key),
                (key) -> createClient(manager, key),
                manager,
                input).run();
    }

    private void printResultSet(ResultSet<String> resultSet) {
        resultSet.print();
    }

    @Test
    public void benchmarkBasicProtocol() {
        printResultSet(runBasic(inputs.get(0)));
        printResultSet(runBasic(inputs.get(1)));
    }
}
