package test.unit;

import main.util.RateLimitingInputStream;
import main.util.Util;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Tests ensuring that an InputStream can be rate-limited.
 */
public class RateLimitingInputStreamTests {
    private RateLimitingInputStream createStream(int numBytes, double kbps) {
        ByteArrayInputStream bais = new ByteArrayInputStream(
                new byte[numBytes]);
        return new RateLimitingInputStream(bais, kbps);
    }

    private void testTimedRead(InputStream stream,
                               int numBytes,
                               double minSeconds,
                               double maxSeconds) throws IOException {
        long nanoStart = System.nanoTime();
        Util.read(stream, numBytes);
        long nanoEnd = System.nanoTime();
        double durationSeconds = (nanoEnd - nanoStart) / 1e9;

        assertTrue("Took " + durationSeconds + "s",
                durationSeconds >= minSeconds && durationSeconds <= maxSeconds);
    }

    private void testNonblockingRead(InputStream stream,
                                     int numBytes) throws IOException {
        testTimedRead(stream, numBytes, -1, 0.05);
    }

    // Tests that the read blocks for a single epoch.
    private void testBlockingRead(InputStream stream,
                                  int numBytes) throws IOException {
        testTimedRead(stream, numBytes, 0.05, 0.15);
    }

    @Test
    public void testNoLimit() throws IOException {
        RateLimitingInputStream stream = createStream(100 /* bytes */, -1);
        testNonblockingRead(stream, 100);
    }

    @Test
    public void testSmallReadDoesntBlock() throws IOException {
        RateLimitingInputStream stream = createStream(100 /* bytes */, 1 /* kbps */);
        testNonblockingRead(stream, 90);
    }

    @Test
    public void testLargeReadBlocks() throws IOException {
        RateLimitingInputStream stream = createStream(2000 /* bytes */, 1 /* kbps */);
        testBlockingRead(stream, 105);
    }

    @Test
    public void testSmallReadWaitSmallReadDoesntBlock() throws IOException {
        RateLimitingInputStream stream = createStream(3000 /* bytes */, 1 /* kbps */);
        testNonblockingRead(stream, 90);
        Util.sleepMillis(150);
        testNonblockingRead(stream, 90);
    }

    @Test
    public void testReadChangeLimitReadBlocks() throws IOException {
        RateLimitingInputStream stream = createStream(2000 /* bytes */, 1 /* kbps */);
        testNonblockingRead(stream, 90);
        stream.setRateLimit(0.5);
        testBlockingRead(stream, 1);
    }

    @Test
    public void testReadChangeLimitReadDoesntBlock() throws IOException {
        RateLimitingInputStream stream = createStream(2000 /* bytes */, 1 /* kbps */);
        testNonblockingRead(stream, 90);
        stream.setRateLimit(2);
        testNonblockingRead(stream, 90);
    }
}
