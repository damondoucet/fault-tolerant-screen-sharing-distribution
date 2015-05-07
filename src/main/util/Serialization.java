package main.util;

import com.google.common.primitives.Longs;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Utilities for serializing and deserializing keys for connections (as used in
 * protocols).
 */
public class Serialization {
    private Serialization() {}

    public static int read(InputStream inputStream, byte[] bytes, int numBytes) throws IOException {
        checkArgument(bytes.length >= numBytes,
                "Read buffer not big enough. Length given: %s, numBytes: %s",
                bytes.length, numBytes);

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

    public static byte[] readWithTimeout(InputStream stream, int numBytes, long timeoutMillis)
            throws InterruptedException, ExecutionException, TimeoutException {
        return Util.doWithTimeout(() -> read(stream, numBytes), timeoutMillis);
    }

    public static byte readByteWithTimeout(InputStream stream, long timeoutMillis)
            throws InterruptedException, ExecutionException, TimeoutException {
        return Util.doWithTimeout(() -> read(stream, 1)[0], timeoutMillis);
    }

    public static long readLong(InputStream stream) throws IOException {
        return Longs.fromByteArray(read(stream, Long.BYTES));
    }

    public static void writeLong(OutputStream stream, long value) throws IOException {
        stream.write(Longs.toByteArray(value));
    }

    public static <T> T deserialize(InputStream stream) throws IOException {
        throw new NotImplementedException();
    }

    public static <T> byte[] serialize(T obj) {
        throw new NotImplementedException();
    }
}
