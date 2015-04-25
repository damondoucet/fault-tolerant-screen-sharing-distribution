package main.network;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Static utility functions.
 */
public class Util {
    // Disable constructor
    private Util() {}

    public static void sleepMillis(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
        }
    }

    public static <T> T next(ConcurrentLinkedQueue<T> queue) {
        T t;
        while ((t = queue.poll()) == null)
            sleepMillis(10);
        return t;
    }

    public static void printException(String message, Exception e) {
        System.out.println(message);
        System.out.println(e);
        e.printStackTrace();
    }
}
