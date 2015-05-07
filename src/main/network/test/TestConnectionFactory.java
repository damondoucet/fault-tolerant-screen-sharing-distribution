package main.network.test;

import main.network.Connection;
import main.network.ConnectionFactory;

import java.io.IOException;

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

    public String getKey() {
        return client;
    }

    @Override
    public Connection<String> acceptConnection() {
        return manager.acceptConnection(client);
    }

    @Override
    public Connection<String> openConnection(String dest)
            throws IOException {
        return manager.openConnection(client, dest);
    }

    @Override
    public void close() {
    }
}
