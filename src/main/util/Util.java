package main.util;

import main.network.connections.Connection;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.concurrent.*;

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
        // System.out.println(e);
        // e.printStackTrace();
    }

    // TODO(ddoucet): This only works on MIT networks LOL
    public static String getMITIP() throws SocketException {
        Enumeration e = NetworkInterface.getNetworkInterfaces();
        while(e.hasMoreElements())
        {
            NetworkInterface n = (NetworkInterface) e.nextElement();
            Enumeration ee = n.getInetAddresses();
            while (ee.hasMoreElements())
            {
                InetAddress i = (InetAddress) ee.nextElement();
                if (i.getHostAddress().startsWith("18."))
                    return i.getHostAddress();
            }
        }

        return null;
    }

    public static String getIP(boolean localMachineOnly) throws SocketException {
        if (localMachineOnly)
            return "127.0.0.1";
        String ip = getMITIP();
        return ip == null ? "127.0.0.1" : ip;
    }

    public static <T> T doWithTimeout(Callable<T> callable, long timeoutMillis)
            throws InterruptedException, ExecutionException, TimeoutException {
        // http://stackoverflow.com/questions/804951/is-it-possible-to-read-from-a-inputstream-with-a-timeout
        ExecutorService executor = null;
        try {
            executor = Executors.newFixedThreadPool(2);
            Future<T> future = executor.submit(callable);
            return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } finally {
            // Clean up thread resources
            if (executor != null)
                executor.shutdownNow();
        }
    }

    public static <T> void threadsafeWrite(Connection<T> connection, byte[] bytes)
            throws IOException {
        synchronized (connection) {
            connection.write(bytes);
        }
    }
}
