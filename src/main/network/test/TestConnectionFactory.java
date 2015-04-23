package main.network.test;

import main.network.Connection;
import main.network.ConnectionFactory;

/**
 * Handles creating connections for a given test client.
 *
 * Simple wrapper around TestConnectionManager, which is threadsafe.
 */
public class TestConnectionFactory implements ConnectionFactory<String> {
    private final TestConnectionManager manager;
    private final String client;

    public TestConnectionFactory(TestConnectionManager manager, String client) {
        this.manager = manager;
        this.client = client;
    }

    @Override
    public Connection<String> acceptConnection() {
        return manager.acceptConnection(client);
    }

    @Override
    public Connection<String> openConnection(String dest) {
        return manager.openConnection(client, dest);
    }
}
