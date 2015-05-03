package main.network.test;

import main.network.Connection;
import main.util.RateLimitingInputStream;
import main.util.Util;

import static com.google.common.base.Preconditions.*;

import java.io.IOException;
import java.io.InputStream;
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
    // Wraps reading from the readQueue for getInputStream().
    private class TestConnectionInputStream extends InputStream {
        @Override
        public int read() throws IOException {
            return Util.next(readQueue) & 0xff;
        }
    }

    // Used for closing the other end of a connection.
    private final TestConnectionManager manager;

    private final ConcurrentLinkedQueue<Byte> readQueue;
    private final ConcurrentLinkedQueue<Byte> writeQueue;
    private final RateLimitingInputStream inputStream;
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
        this.inputStream = new RateLimitingInputStream(new TestConnectionInputStream());

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
    public InputStream getInputStream() {
        return inputStream;
    }

    @Override
    public int read(byte[] bytes, int numBytes) throws IOException {
        int bytesRead = Util.read(inputStream, bytes, numBytes);

        if (closed.get())
            throw new IOException("Stream closed");

        return bytesRead;
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

    // TODO(ddoucet): @Override
    public void setRateLimit(double kbps) {
        inputStream.setRateLimit(kbps);
    }
}
