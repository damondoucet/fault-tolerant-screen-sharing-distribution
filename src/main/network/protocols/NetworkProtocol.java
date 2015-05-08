package main.network.protocols;

import main.Snapshot;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Represents a protocol for distributing snapshots across the peer-to-peer
 * network.
 */
public interface NetworkProtocol {
    /**
     * Sets the queue that the network protocol will insert received images
     * into.
     *
     * @param queue The queue images will be inserted into.
     */
    public void registerOutputQueue(ConcurrentLinkedQueue<Snapshot> queue);

    /**
     * Send an image to receivers. Note that this is only really used for
     * testing; the protocol should handle sending any images it received.
     *
     * @param image The image to send to receivers.
     */
    public void insertSnapshot(Snapshot image);

    /**
     * Spawn threads to handle network traffic.
     */
    public void start();

    /**
     * Kill the threads spawned via start().
     */
    public void stop();

    /**
     * Get the address of our current parent.
     */
    public String getParentKeyString();
}
