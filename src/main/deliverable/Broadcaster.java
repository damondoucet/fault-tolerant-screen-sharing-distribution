package main.deliverable;

import main.Snapshot;
import main.network.SocketConnectionFactory;
import main.network.SocketInformation;
import main.network.protocols.NetworkProtocol;
import main.network.protocols.tree.TreeNetworkProtocol;
import main.util.QueueHandler;
import main.util.Util;

import java.awt.*;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Holds main method for a broadcaster for screen broadcasting.
 */
public class Broadcaster {
    private final static boolean LOCAL_MACHINE_ONLY = false;
    private final static int PORT = 5567;
    private final static long FREQUENCY = 20;

    private final ScreenGrabber grabber;
    private final NetworkProtocol networkBroadcaster;

    private final ConcurrentLinkedQueue<Snapshot> slideshowInput;
    private final QueueHandler<Snapshot> queueHandler;

    private Slideshow slideshow;

    private Broadcaster(ScreenGrabber grabber,
                       ConcurrentLinkedQueue<Snapshot> grabberOutput,
                       NetworkProtocol networkBroadcaster) {
        this.grabber = grabber;
        this.networkBroadcaster = networkBroadcaster;
        this.slideshowInput = new ConcurrentLinkedQueue<>();
        this.queueHandler = new QueueHandler<>(
                grabberOutput,
                (snapshot) -> {
                    slideshowInput.add(snapshot);
                    if (grabberOutput.size() > 10) {
                        grabberOutput.poll();
                        networkBroadcaster.insertSnapshot(snapshot);
                    } else {
                        networkBroadcaster.insertSnapshot(snapshot);
                    }

                });
    }

    public void start() {
        grabber.startCapture();
        queueHandler.start();
        slideshow = new Slideshow(slideshowInput, networkBroadcaster.getParentKeyString());
        networkBroadcaster.start();
    }

    public void stop() {
        grabber.endCapture();
        queueHandler.stop();
        slideshow.close();
        networkBroadcaster.stop();
    }

    public static void main(String[] args) throws AWTException, IOException {
        SocketInformation socketInfo = new SocketInformation(Util.getIP(LOCAL_MACHINE_ONLY), PORT);
        System.out.println(socketInfo);

        // using basic protocol
//        NetworkProtocol netBroadcaster = new BasicNetworkProtocolBroadcaster<>(
//                SocketConnectionFactory.fromSocketInfo(socketInfo));

        // using tree protocol
        NetworkProtocol netBroadcaster = TreeNetworkProtocol.losslessClient(
                SocketConnectionFactory.fromSocketInfo(socketInfo), socketInfo);

        ConcurrentLinkedQueue<Snapshot> snapshots = new ConcurrentLinkedQueue<>();
        ScreenGrabber grabber = ScreenGrabber.fromQueueFrequencyDimension(snapshots, FREQUENCY, new Dimension(600, 300));
        Broadcaster broadcaster = new Broadcaster(grabber, snapshots, netBroadcaster);

        broadcaster.start();

        Runtime.getRuntime().addShutdownHook(new Thread(broadcaster::stop));
    }
}
