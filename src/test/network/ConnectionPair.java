package test.network;

import main.network.Connection;

import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

// Represents the result of a connection being established.
public class ConnectionPair<T> {
    public final TestClient<T> source;
    public final TestClient<T> dest;

    public final Connection<T> sourceToDest;
    public final Connection<T> destToSource;

    public ConnectionPair(TestClient<T> source,
                          TestClient<T> dest,
                          Connection<T> sourceToDest,
                          Connection<T> destToSource) {
        this.source = source;
        this.dest = dest;
        this.sourceToDest = sourceToDest;
        this.destToSource = destToSource;
    }

    private void testSending(Connection<T> from, Connection<T> to,
                             byte[] data) throws IOException {
        from.write(data);

        byte[] read = new byte[data.length];
        assertEquals(data.length, to.read(read, data.length));
        assertArrayEquals(data, read);
    }

    public void testSending(byte[] data) throws IOException {
        testSending(sourceToDest, destToSource, data);
    }

    public void testBackwardsSending(byte[] data) throws IOException {
        testSending(destToSource, sourceToDest, data);
    }
}
