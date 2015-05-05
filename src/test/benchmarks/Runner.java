package test.benchmarks;

import main.Snapshot;
import main.network.protocols.NetworkProtocol;
import main.network.test.TestConnectionManager;
import main.util.Util;
import test.unit.ImageUtil;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Runs a specified Input on a specified network protocol.
 *
 * The protocol is defined by two functions: createBroadcaster() and createClient(index).
 */
public class Runner {
    private final Callable<NetworkProtocol> createBroadcaster;
    private final Function<String, NetworkProtocol> createClient;
    private final Input input;
    private final RateLimitSchedule schedule;
    private final ResultSetBuilder<String> builder;
    private final BufferedImage image;

    private final AtomicInteger currentRound;
    private NetworkProtocol broadcaster;
    private List<NetworkProtocol> clients;
    private Snapshot currentSnapshot;

    public Runner(Callable<NetworkProtocol> createBroadcaster,
                  Function<String, NetworkProtocol> createClient,
                  TestConnectionManager manager,
                  Input input) {
        this.createBroadcaster = createBroadcaster;
        this.createClient = createClient;
        this.input = input;
        this.schedule = new RateLimitSchedule(manager, input.schedule);
        this.builder = new ResultSetBuilder(input.clients.size());
        this.image = ImageUtil.createImage1();
        this.currentRound = new AtomicInteger();
    }

    private void initialize() throws Exception {
        schedule.initialize(input.clients);
        broadcaster = createBroadcaster.call();
        broadcaster.start();
        currentSnapshot = Snapshot.lossySnapshot(0, image);

        clients = new ArrayList<>();
        for (int i = 0; i < input.clients.size(); i++) {
            clients.add(createClient.apply(input.clients.get(i)));
            clients.get(i).start();
            new Thread(new ClientWatcher(i)).start();
        }
    }

    private class ClientWatcher implements Runnable {
        private final int clientIndex;
        private final ConcurrentLinkedQueue<Snapshot> queue;

        public ClientWatcher(int clientIndex) {
            this.clientIndex = clientIndex;
            this.queue = new ConcurrentLinkedQueue<>();
        }

        @Override
        public void run() {
            clients.get(clientIndex).registerOutputQueue(queue);

            int round;
            while ((round = currentRound.get()) < input.numRounds) {
                Snapshot snapshot;
                if ((snapshot = queue.poll()) == null)
                    Util.sleepMillis(1);
                else
                    handleSnapshot(round, snapshot);
            }
        }

        private void handleSnapshot(int round, Snapshot snapshot) {
            long nowNano = System.nanoTime();

            if (snapshot.getFrameIndex() != round) {
                System.out.printf(
                        "%s ignoring snapshot for round %d while in round %d\n",
                        input.clients.get(clientIndex),
                        snapshot.getFrameIndex(),
                        round);
                return;
            }

            builder.markClientFinished(
                    round, input.clients.get(clientIndex), nowNano);
        }
    }

    private void run(int round) {
        System.out.println(input.name + " round " + round);

        currentRound.set(round);
        builder.startRound(round);
        broadcaster.insertSnapshot(currentSnapshot);
        currentSnapshot = currentSnapshot.createNext(image);
        builder.waitUntilRoundEnded();
    }

    private void stop() {
        currentRound.set(input.numRounds);  // kill ClientWatcher threads
        broadcaster.stop();
        for (NetworkProtocol client : clients)
            client.stop();
    }

    public ResultSet<String> run() throws Exception {
        initialize();
        for (int i = 0; i < input.numRounds; i++) {
            schedule.applyForRound(i);
            run(i);
        }

        stop();
        return builder.createResultSet();
    }
}
