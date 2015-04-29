package test.benchmarks;

import main.network.ConnectionFactory;
import main.network.test.TestConnectionManager;

import java.util.List;

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

    }

    /**
     * Apply the rate limits for the given round. This indexes into the
     * schedule list.
     *
     * @param round The round for which limits should be applied.
     */
    public void applyForRound(int round) {

    }
}
