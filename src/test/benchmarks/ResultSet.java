package test.benchmarks;

import java.util.Map;
import java.util.stream.LongStream;

/**
 * Represents the results of an input run on a specific protocol.
 *
 * This class is just a wrapper around a single data structure (a nested map):
 *      Round Number maps to a map, which is keyed on Client and maps to the
 *      latency for that client on the given round.
 *
 *      Latency here is defined as the number of milliseconds between the
 *      end of inserting the snapshot into the broadcaster, and the end of
 *      receiving the snapshot at that client.
 */
public class ResultSet<T> {
    private final Map<Integer, Map<T, Long>> roundToClientToLatency;

    public ResultSet(Map<Integer, Map<T, Long>> roundToClientToLatency) {
        this.roundToClientToLatency = roundToClientToLatency;
    }

    private static <T> LongStream getLongStream(Map<T, Long> map) {
        return map.values().stream().mapToLong(Long::longValue);
    }

    private static <T> double average(Map<T, Long> map) {
        return getLongStream(map).average().getAsDouble();
    }

    private static <T> long max(Map<T, Long> map) {
        return getLongStream(map).max().getAsLong();
    }

    public double getAverageLatencyForRound(int round) {
        return average(roundToClientToLatency.get(round));
    }

    public long getMaxLatencyForRound(int round) {
        return max(roundToClientToLatency.get(round));
    }

    private LongStream getLongStream() {
        return roundToClientToLatency.values().stream()
                .flatMapToLong(
                        map -> map.values().stream()
                                .mapToLong(Long::longValue));
    }

    public double getAverageLatency() {
        return getLongStream().average().getAsDouble();
    }

    public long getMaxLatency() {
        return getLongStream().max().getAsLong();
    }

    public void print() {
        System.out.println("=============");
        System.out.println(" Result Set: ");
        System.out.println("  avg lat: " + getAverageLatency() + "ms");
        System.out.println("  max lat: " + getMaxLatency());
        System.out.println("=============");
    }
}
