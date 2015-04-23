package main.network;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Static utility functions.
 */
public class Util {
    // Disable constructor
    private Util() {}

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
        }
    }

    public static <T> T next(ConcurrentLinkedQueue<T> queue) {
        T t;
        while ((t = queue.poll()) == null)
            sleep(10);
        return t;
    }
}
