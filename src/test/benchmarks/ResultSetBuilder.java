package test.benchmarks;

import main.util.Util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import static com.google.common.base.Preconditions.*;

/**
 * Builder for a ResultSet.
 */
public class ResultSetBuilder<T> {
    private final Map<Integer, Map<T, Long>> roundToClientToDurationMilli;
    private final int numClients;
    private int currentRound;
    private long roundStartNano;

    public ResultSetBuilder(int numClients) {
        roundToClientToDurationMilli = new HashMap<>();
        this.numClients = numClients;
        currentRound = -1;
    }

    public void startRound(int round) {
        checkArgument(round == currentRound + 1);
        currentRound = round;
        roundToClientToDurationMilli.put(round,
                Collections.synchronizedMap(new HashMap<>()));
        roundStartNano = System.nanoTime();
    }

    public void markClientFinished(int round, T client, long endedNano) {
        checkArgument(round == currentRound);
        roundToClientToDurationMilli.get(round).put(
                client, (endedNano - roundStartNano)/1000000);
    }

    public void waitUntilRoundEnded() {
        while (roundToClientToDurationMilli.get(currentRound).size() < numClients)
            Util.sleepMillis(10);
    }

    public ResultSet<T> createResultSet() {
        // copy to remove the synchronization around it
        return new ResultSet<>(new HashMap<>(roundToClientToDurationMilli));
    }
}
