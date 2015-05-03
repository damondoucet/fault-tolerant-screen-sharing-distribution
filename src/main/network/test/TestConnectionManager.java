package main.network.test;

import main.network.Connection;
import main.util.Util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.google.common.base.Preconditions.*;

/**
 * Handles TestConnections in a threadsafe manner.
 *
 * The management revolves around two maps: connectionData and
 * awaitingConnections. connectionData handles all of the threadsafe queues
 * of data between clients. awaitingConnections is the queue of clients
 * attempting to connect to a specific client. The final map (connections) is
 * just to implement close() functionality.
 *
 * TestConnectionFactory is a wrapper around this class.
 */
public class TestConnectionManager {
    // The maps below must be threadsafe because clients may be created at the
    // same time (concurrently) as others are accessing their maps and queues.

    // Maps source to a map which maps destination to the queue of bytes.
    private final Map<String, Map<String, ConcurrentLinkedQueue<Byte>>> connectionData;

    // Maps destination to a list of clients trying to connect.
    private final Map<String, ConcurrentLinkedQueue<String>> awaitingConnections;

    // Maps source to a map which maps destination to a connection. Used for
    // closing and rate-limiting connections.
    private final Map<String, Map<String, TestConnection>> connections;

    // Note that rate limits are bidirectional. This maps the lexicographically
    // lower key to a map of lexicographically higher key to rate limit.
    // Units of rate limits are kbps.
    private final Map<String, Map<String, Double>> rateLimits;

    public TestConnectionManager() {
        connectionData = Collections.synchronizedMap(new HashMap<>());
        awaitingConnections = Collections.synchronizedMap(new HashMap<>());
        connections = Collections.synchronizedMap(new HashMap<>());
        rateLimits = Collections.synchronizedMap(new HashMap<>());
    }

    public void onNewClient(String key) {
        checkState(!connectionData.containsKey(key),
                "Already have client " + key);
        connectionData.put(key, Collections.synchronizedMap(new HashMap<>()));
        awaitingConnections.put(key, new ConcurrentLinkedQueue<>());
        connections.put(key, new ConcurrentHashMap<>());
        rateLimits.put(key, new ConcurrentHashMap<>());
    }

    private double getRateLimit(String a, String b) {
        if (a.compareTo(b) > 0)
            return getRateLimit(b, a);

        Map<String, Double> map = rateLimits.get(a);
        if (map.containsKey(b))
            return map.get(b);
        else
            return -1;
    }

    /**
     * Sets a bi-directional limit on the rate at which data can be
     * transferred.
     */
    public void setRateLimit(String a, String b, double kbps) {
        if (a.compareTo(b) > 0) {
            setRateLimit(b, a, kbps);
            return;
        }

        rateLimits.get(a).put(b, kbps);

        // TODO(ddoucet): is there a race condition with creating at the same
        // time this is happening?
        TestConnection conn = connections.get(a).get(b);
        if (conn != null) {
            conn.setRateLimit(kbps);
            connections.get(b).get(a).setRateLimit(kbps);
        }
    }

    private TestConnection createConnection(ConcurrentLinkedQueue<Byte> readQueue,
                                            ConcurrentLinkedQueue<Byte> writeQueue,
                                            String source, String dest) {
        TestConnection connection = new TestConnection(
                this, readQueue, writeQueue, source, dest);
        connection.setRateLimit(getRateLimit(source, dest));
        connections.get(source).put(dest, connection);
        return connection;
    }

    // Creates and inserts a queue both from source to dest and from dest to
    // source into the connectionData map. Returns a connection from the source
    // to the dest.
    private TestConnection createConnection(String source, String dest) {
        ConcurrentLinkedQueue<Byte> sourceToDest = new ConcurrentLinkedQueue<>(),
                destToSource = new ConcurrentLinkedQueue<>();

        // Don't reorder these two lines. waitForConnection expects the
        // sourceToDest queue to be inserted before the destToSource queue
        // (although in waitForConnection, it's reversed--their source is our
        // dest and vice versa).
        connectionData.get(source).put(dest, sourceToDest);
        connectionData.get(dest).put(source, destToSource);

        return createConnection(
                destToSource /* read queue */,
                sourceToDest /* write queue */,
                source, dest);
    }

    public Connection<String> acceptConnection(String forKey) {
        String source = Util.next(awaitingConnections.get(forKey));

        // We want to return a connection so that the connecting client is our
        // destination and we are the source.
         return createConnection(forKey, source);
    }

    private TestConnection waitForConnection(String source, String dest) {
        Map<String, ConcurrentLinkedQueue<Byte>> connections = connectionData.get(source);

        while (!connections.containsKey(dest))
            Util.sleepMillis(10);

        // Now that we have our queue to the dest, we're guaranteed there's a
        // queue from the dest to us (see createConnection).
        ConcurrentLinkedQueue<Byte> sourceToDest = connections.get(dest),
                destToSource = connectionData.get(dest).get(source);

        return createConnection(
                destToSource /* read queue */,
                sourceToDest /* write queue */,
                source, dest);
    }

    public Connection<String> openConnection(String source, String dest) {
        // Insert ourselves into the destination's awaitingConnections list.
        com.google.common.base.Verify.verify(awaitingConnections.get(dest) != null);
        awaitingConnections.get(dest).add(source);

        // Wait until we have a queue in connectionData and return that.
        return waitForConnection(source, dest);
    }

    public void closeConnection(String source, String dest) {
        TestConnection sourceToDest = connections.get(source).get(dest);
        TestConnection destToSource = connections.get(dest).get(source);

        if (sourceToDest == null) {
            // Closing a connection that's already closed.
            return;
        }

        // Note we remove from the map first in order to avoid infinite
        // recursion.
        connections.get(source).remove(dest);
        connectionData.get(source).remove(dest);
        if (destToSource != null) {
            connections.get(dest).remove(source);
            connectionData.get(dest).remove(source);

            // Notify the connection object that it's closed.
            destToSource.close();
        }
    }
}
