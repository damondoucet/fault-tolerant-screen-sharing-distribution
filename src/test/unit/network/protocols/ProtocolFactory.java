package test.unit.network.protocols;

import main.network.protocols.NetworkProtocol;
import main.network.protocols.basic.BasicNetworkProtocolBroadcaster;
import main.network.protocols.basic.BasicNetworkProtocolClient;
import main.network.protocols.tree.TreeNetworkProtocol;
import main.network.connections.test.TestConnectionFactory;
import main.network.connections.test.TestConnectionManager;

/**
 * Creates network protocol clients for testing.
 */
public class ProtocolFactory {
    private final String broadcasterKey;

    public ProtocolFactory(String broadcasterKey) {
        this.broadcasterKey = broadcasterKey;
    }

    public NetworkProtocol createBasicClient(TestConnectionManager manager, String key) {
        NetworkProtocol client = BasicNetworkProtocolClient.lossyClient(
                new TestConnectionFactory(manager, key), broadcasterKey);
        manager.onNewClient(key);
        return client;
    }

    public NetworkProtocol createBasicBroadcaster(TestConnectionManager manager) {
        NetworkProtocol broadcaster = new BasicNetworkProtocolBroadcaster<>(
                new TestConnectionFactory(manager, broadcasterKey));
        manager.onNewClient(broadcasterKey);
        return broadcaster;
    }

    public NetworkProtocol createTreeBroadcaster(TestConnectionManager manager) {
        return createTreeClient(manager, broadcasterKey);
    }

    public NetworkProtocol createTreeClient(TestConnectionManager manager,
                                            String clientKey) {
        manager.onNewClient(clientKey);
        return TreeNetworkProtocol.losslessClient(
                new TestConnectionFactory(manager, clientKey),
                broadcasterKey);
    }
}
