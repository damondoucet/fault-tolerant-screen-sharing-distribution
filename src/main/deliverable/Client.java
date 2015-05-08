package main.deliverable;

import main.Snapshot;
import main.imageviewer.Slideshow;
import main.network.SocketConnectionFactory;
import main.network.SocketInformation;
import main.network.protocols.NetworkProtocol;
import main.network.protocols.basic.BasicNetworkProtocolClient;
import main.network.protocols.tree.TreeNetworkProtocol;
import main.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
        System.out.println(networkClient.getParentKeyString());
        slideshow = new Slideshow(slideshowInput, networkClient.getParentKeyString());
        networkClient.start();
        new Runnable() {

            @Override
            public void run() {
                Util.sleepMillis(1000);
                slideshow.setParentIP(networkClient.getParentKeyString());
            }
        }.run();
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

        SocketConnectionFactory socketConnectionFactory = SocketConnectionFactory.fromSocketInfo(getSocketInfo());

        // using basic protocol
//        NetworkProtocol networkClient = BasicNetworkProtocolClient.lossyClient(
//                socketConnectionFactory,
//                broadcasterSocketInfo);

        // using tree protocol
        NetworkProtocol networkClient = TreeNetworkProtocol.losslessClient(
                socketConnectionFactory, getBroadcasterSocketInfo());

        Client client = new Client(networkClient);
        client.start();

        Runtime.getRuntime().addShutdownHook(new Thread(client::stop));

        try{
            BufferedReader br =
                    new BufferedReader(new InputStreamReader(System.in));

            String input;

            while((input=br.readLine())!=null){
                // looking for e.g. "kill 127.0.0.1:5567"
                if (input.startsWith("kill")){
                    String socketInfo = input.substring(("kill ").length());
                    String[] parts = socketInfo.split(":");
                    socketConnectionFactory.kill(new SocketInformation(parts[0], Integer.parseInt(parts[1])));
                }
            }

        }catch(IOException io){
            io.printStackTrace();
        }

    }
}
