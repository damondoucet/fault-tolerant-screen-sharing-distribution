package test.unit.network.protocols;

import main.network.protocols.NetworkProtocol;
import main.network.protocols.tree.TreeNetworkProtocol;
import main.network.test.TestConnectionFactory;
import main.network.test.TestConnectionManager;

/**
 * Used in tests for creating K-ary tree nodes.
 */
public class KaryTreeNodeFactory {
    private final int k;

    public KaryTreeNodeFactory(int k) {
        this.k = k;
    }

    public NetworkProtocol createBroadcaster(TestConnectionManager manager, String key) {
        return createClient(manager, key, key);
    }

    public NetworkProtocol createClient(TestConnectionManager manager,
                                        String broadcasterKey,
                                        String clientKey) {
        manager.onNewClient(clientKey);
        return TreeNetworkProtocol.losslessClient(
                new TestConnectionFactory(manager, clientKey),
                broadcasterKey);
    }
}

