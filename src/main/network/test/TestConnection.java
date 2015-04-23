package main.network.test;

import main.network.Connection;
import main.network.Util;

import static com.google.common.base.Preconditions.*;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A threadsafe connection intended to be used locally for testing
 * NetworkProtocol implementations.
 *
 * Reads and writes go through ConcurrentLinkedQueues. These are created and
 * managed in the TestConnectionManager. (TestConnectionFactory is just a
 * wrapper around the manager.)
 */
public class TestConnection implements Connection<String> {
    // Used for closing the other end of a connection.
    private final TestConnectionManager manager;

    private final ConcurrentLinkedQueue<Byte> readQueue;
    private final ConcurrentLinkedQueue<Byte> writeQueue;
    private final String source;
    private final String dest;

    private final AtomicBoolean closed;

    public TestConnection(TestConnectionManager manager,
                          ConcurrentLinkedQueue<Byte> readQueue,
                          ConcurrentLinkedQueue<Byte> writeQueue,
                          String source,
                          String dest) {
        this.manager = manager;
        this.readQueue = readQueue;
        this.writeQueue = writeQueue;
        this.source = source;
        this.dest = dest;
        closed = new AtomicBoolean(false);
    }

    public String getSource() {
        return source;
    }

    public String getDest() {
        return dest;
    }

    @Override
    public int read(byte[] bytes, int numBytes) throws IOException {
        checkArgument(bytes.length >= numBytes,
                "Read buffer not big enough. Length given: %s, numBytes: %s",
                bytes.length, numBytes);

        for (int i = 0; i < numBytes; i++)
            bytes[i] = Util.next(readQueue);

        if (closed.get())
            throw new IOException("Stream closed");

        return numBytes;
    }

    @Override
    public void write(byte[] bytes) throws IOException {
        for (byte b : bytes)
            writeQueue.add(b);

        if (closed.get())
            throw new IOException("Stream closed");
    }

    @Override
     public void close() {
        closed.set(true);
        manager.closeConnection(source, dest);
    }
}
