package test.benchmarks;

import main.network.connections.test.TestConnectionManager;

import java.util.ArrayList;
import java.util.List;
import static com.google.common.base.Verify.*;

/**
 * Represents how connection speeds should be modified for an input case.
 *
 * This is used to test how a protocol responds to certain failures in the
 * network. One specific rate-limit instance is two nodes and a maximum kbytes
 * per tenth-second allowed in transfer. The unit of measurement used here is
 * kbytes/second for simplicity.
 *
 * The schedule then is a mapping of round number to the list of rate-limit
 * instances that should occur just before the round starts.
 */
public class RateLimitSchedule {
    public static class RateLimitInstance {
        public final String a;
        public final String b;
        public final int kbps;

        public RateLimitInstance(String a, String b, int kbps) {
            this.a = a;
            this.b = b;
            this.kbps = kbps;
        }
    }

    public static class Builder {
        private final List<List<RateLimitInstance>> schedule;

        public Builder() {
            schedule = new ArrayList<>();
        }

        public Builder setNumRounds(int numRounds) {
            for (int i = 0; i < numRounds; i++)
                schedule.add(new ArrayList<>());
            return this;
        }

        // Sets the failures that will occur right before the given round.
        public Builder setRound(int round, RateLimitInstance... instances) {
            verify(schedule.size() != 0, "setNumRounds not called yet.");
            verify(round >= 0 && round < schedule.size(), "Illegal round " + round);
            verify(schedule.get(round).isEmpty(),
                    "Already added round " + round);

            for (RateLimitInstance instance : instances)
                schedule.get(round).add(instance);

            return this;
        }

        public List<List<RateLimitInstance>> build() {
            return schedule;
        }
    }

    public final static int DEFAULT_KBPS = 100;
    private final TestConnectionManager manager;
    private final List<List<RateLimitInstance>> schedule;

    public RateLimitSchedule(TestConnectionManager manager,
                             List<List<RateLimitInstance>> schedule) {
        this.manager = manager;
        this.schedule = schedule;
    }

    /**
     * Initialize the rate limit between any pair of the given clients to the
     * default rate.
     *
     * @param clients The list of clients to initialize.
     */
    public void initialize(List<String> clients) {
        for (int i = 0; i < clients.size(); i++)
            for (int j = i + 1; j < clients.size(); j++)
                manager.setRateLimit(clients.get(i), clients.get(j), DEFAULT_KBPS);
    }

    /**
     * Apply the rate limits for the given round. This indexes into the
     * schedule list.
     *
     * @param round The round for which limits should be applied.
     */
    public void applyForRound(int round) {
        for (RateLimitInstance rateLimitInstance : schedule.get(round))
            manager.setRateLimit(
                    rateLimitInstance.a,
                    rateLimitInstance.b,
                    rateLimitInstance.kbps);
    }
}
