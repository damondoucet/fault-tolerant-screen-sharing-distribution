package main.util;

import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * Handles a thread that can start and stop.
 */
public class QueueHandler<T> {
    private final ConcurrentLinkedQueue<T> queue;
    private final Consumer<T> consumer;
    private final InterruptableThreadSet threadSet;

    public QueueHandler(ConcurrentLinkedQueue<T> queue,
                        Consumer<T> consumer) {
        this.queue = queue;
        this.consumer = consumer;
        this.threadSet = new InterruptableThreadSet(
                Arrays.asList(this::maybeHandleEntry),
                null);
    }

    private void maybeHandleEntry() {
        T next = queue.poll();
        if (next != null)
            consumer.accept(next);
    }

    public void start() {
        threadSet.start();
    }

    public void stop() {
        threadSet.stop();
    }
}
