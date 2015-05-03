package test.benchmarks;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines a specific case to run as a benchmark.
 *
 * Benchmarks are defined by the number of clients, the number of snapshots to
 * be sent (each snapshot represents a round), and the failure schedule.
 */
public class Input {
    public static String BROADCASTER = "broadcaster";

    public final String name;
    public final int numRounds;
    public final List<List<RateLimitSchedule.RateLimitInstance>> schedule;
    public final List<String> clients;

    public Input(String name,
                 int numClients,
                 List<List<RateLimitSchedule.RateLimitInstance>> schedule) {
        this.name = name;
        this.numRounds = schedule.size();
        this.schedule = schedule;
        this.clients = createClients(numClients);
    }

    private static List<String> createClients(int numClients) {
        List<String> clients = new ArrayList<>();
        for (int i = 0; i < numClients; i++)
            clients.add(getClientName(i));
        return clients;
    }

    public static String getClientName(int index) {
        return "client" + index;
    }
}
