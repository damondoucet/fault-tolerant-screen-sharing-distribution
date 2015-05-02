package main.deliverable;

import main.Snapshot;
import main.imageviewer.Slideshow;
import main.network.SocketConnectionFactory;
import main.network.SocketInformation;
import main.network.protocols.NetworkProtocol;
import main.network.protocols.basic.BasicNetworkProtocolClient;
import main.util.Util;

import java.io.IOException;
import java.net.SocketException;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Client for deliverable.
 */
public class Client {
    private final static boolean LOCAL_MACHINE_ONLY = false;

    private final NetworkProtocol networkClient;
    private final ConcurrentLinkedQueue<Snapshot> slideshowInput;
    private Slideshow slideshow;

    private Client(NetworkProtocol networkClient) {
        this.networkClient = networkClient;
        this.slideshowInput = new ConcurrentLinkedQueue<>();

        this.networkClient.registerOutputQueue(this.slideshowInput);
    }

    public void start() {
        slideshow = new Slideshow(slideshowInput);
        networkClient.start();
    }

    public void stop() {
        networkClient.stop();
        slideshow.close();
    }

    private static SocketInformation getBroadcasterSocketInfo() {
        // TODO(ddoucet)
        return new SocketInformation("127.0.0.1", 5567);
    }

    private static SocketInformation getSocketInfo() throws SocketException {
        // TODO(ddoucet): need some way to generate ports
        return new SocketInformation(Util.getIP(LOCAL_MACHINE_ONLY), 11235);
    }

    public static void main(String[] args) throws IOException {
        SocketInformation broadcasterSocketInfo = getBroadcasterSocketInfo();

        NetworkProtocol networkClient = BasicNetworkProtocolClient.lossyClient(
                SocketConnectionFactory.fromSocketInfo(getSocketInfo()),
                broadcasterSocketInfo);

        Client client = new Client(networkClient);
        client.start();
        Runtime.getRuntime().addShutdownHook(new Thread(client::stop));
    }
}
