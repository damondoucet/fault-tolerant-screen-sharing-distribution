package main.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
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
        return localMachineOnly ? "127.0.0.1" : Util.getMITIP();
    }

    public static int read(InputStream inputStream, byte[] bytes, int numBytes) throws IOException {
        int bytesRead = 0;
        while (bytesRead < numBytes) {
            int n = inputStream.read(bytes, bytesRead, numBytes - bytesRead);
            if (n < 0)
                break;
            bytesRead += n;
        }
        return bytesRead;
    }

    public static byte[] read(InputStream inputStream, int numBytes) throws IOException {
        byte[] bytes = new byte[numBytes];
        int bytesRead;
        if ((bytesRead = read(inputStream, bytes, numBytes)) < numBytes)
            throw new IOException("Unable to read all bytes from input stream. Read "
                    + bytesRead + "; wanted " + numBytes);
        return bytes;
    }
}
