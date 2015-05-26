package main.util;

import com.google.common.base.Charsets;
import com.google.common.primitives.Longs;
import main.network.SocketInformation;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.*;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

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

    // These methods suck :/
    public static <T> T deserialize(InputStream stream, Class<T> type) throws IOException {
        if (type.equals(String.class))
            return (T)stringFromStream(stream);
        else if (type.equals(SocketInformation.class))
            return (T)sockInfoFromStream(stream);
        throw new NotImplementedException();
    }

    public static <T> byte[] serialize(T obj) {
        if (obj.getClass().equals(String.class)) {
            return writeObj(obj, Serialization::stringToStream);
        } else if (obj.getClass().equals(SocketInformation.class))
            return writeObj(obj, Serialization::sockInfoToStream);
        throw new NotImplementedException();
    }

    private static <T> byte[] writeObj(T obj, BiConsumer<OutputStream, Object> writer) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writer.accept(baos, obj);
        return baos.toByteArray();
    }

    private static String stringFromStream(InputStream stream)
            throws IOException {
        int length = (int)readLong(stream);
        byte[] bytes = read(stream, length);

        return new String(bytes, Charsets.UTF_8);
    }

    private static void stringToStream(OutputStream stream, Object obj) {
        String value = (String)obj;
        byte[] bytes = value.getBytes(Charsets.UTF_8);

        try {
            writeLong(stream, bytes.length);
            stream.write(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static SocketInformation sockInfoFromStream(InputStream stream)
            throws IOException {
        String ip = stringFromStream(stream);
        int port = (int)readLong(stream);

        return new SocketInformation(ip, port);
    }

    private static void sockInfoToStream(OutputStream stream, Object obj) {
        SocketInformation value = (SocketInformation)obj;

        try {
            stringToStream(stream, value.ip);
            stream.write(Longs.toByteArray(value.port));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
