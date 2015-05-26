package main.deliverable;

import main.Snapshot;
import main.network.SocketConnectionFactory;
import main.network.SocketInformation;
import main.network.protocols.NetworkProtocol;
import main.network.protocols.tree.TreeNetworkProtocol;
import main.util.Util;

import java.io.IOException;
import java.net.SocketException;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Client for deliverable.
 */
public class Client {
    private final static boolean LOCAL_MACHINE_ONLY = false;

    private final NetworkProtocol networkClient;
    private final ConcurrentLinkedQueue<Snapshot> slideshowInput;
    private ImageDisplay imageDisplay;

    private Client(NetworkProtocol networkClient) {
        this.networkClient = networkClient;
        this.slideshowInput = new ConcurrentLinkedQueue<>();
        this.networkClient.registerOutputQueue(this.slideshowInput);
    }

    private static int getRandomFiveDigitNumber() {
        Random r = new Random( System.currentTimeMillis() );
        return (1 + r.nextInt(2)) * 10000 + r.nextInt(10000);
    }

    // turns e.g. "127.0.0.1:5567" into ["127.0.0.1", 5567]
    private static String[] parseKeyString(String socketInfo) {
        return socketInfo.split(":");
    }

    public void start() {
        System.out.println(networkClient.getParentKeyString());
        imageDisplay = new ImageDisplay(slideshowInput, networkClient.getParentKeyString());
        networkClient.start();
        (new Thread(() -> {
            while (true) {
                Util.sleepMillis(3000);
                imageDisplay.setParentAddress(networkClient.getParentKeyString());
            }
        })).start();
    }

    public void stop() {
        networkClient.stop();
        imageDisplay.close();
    }

    private static SocketInformation getBroadcasterSocketInfo(String ip, int port) {
        return new SocketInformation(ip, port);
    }

    private static SocketInformation getSocketInfo(int port) throws SocketException {
        return new SocketInformation(Util.getIP(LOCAL_MACHINE_ONLY), port);
    }

    public static void main(String[] args) throws IOException {
        int port = getRandomFiveDigitNumber();

        // create a scanner so we can read the command-line input
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter broadcaster IP and port (e.g. : '127.0.0.1:5567'): ");
        String bInfo = scanner.next();
        String[] bParts = parseKeyString(bInfo);
        SocketInformation broadcasterSocketInfo = getBroadcasterSocketInfo(bParts[0], Integer.parseInt(bParts[1]));
        System.out.println(String.format("Connecting from me %s to parent %s", getSocketInfo(port), bInfo));

        SocketConnectionFactory socketConnectionFactory = SocketConnectionFactory.fromSocketInfo(getSocketInfo(port));

        // using basic protocol
//        NetworkProtocol networkClient = BasicNetworkProtocolClient.lossyClient(
//                socketConnectionFactory,
//                broadcasterSocketInfo);

        // using tree protocol
        NetworkProtocol networkClient = TreeNetworkProtocol.losslessClient(
                socketConnectionFactory, broadcasterSocketInfo);

        Client client = new Client(networkClient);
        client.start();

        Runtime.getRuntime().addShutdownHook(new Thread(client::stop));


        String input;

        while ((input=scanner.nextLine())!=null) {
            // looking for e.g. "kill 127.0.0.1:5567"
            try {
                if (input.startsWith("kill")) {
                    System.out.println(networkClient.getParentKeyString());
                    String dInfo = input.substring(("kill ").length());
                    String[] dParts = parseKeyString(dInfo);
                    System.out.println(String.format("Killing connection to %s", dInfo));
                    socketConnectionFactory.kill(new SocketInformation(dParts[0], Integer.parseInt(dParts[1])));
                }
            } catch (Exception e) {
                Util.printException("Kill not successful", e);
            }
        }

    }


}
