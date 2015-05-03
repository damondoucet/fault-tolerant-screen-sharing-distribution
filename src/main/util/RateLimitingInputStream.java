package main.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An InputStream that implements rate-limiting.
 */
public class RateLimitingInputStream extends InputStream {
    // length of an epoch in nanoseconds; 0.1s = 1E8ns
    private final static long EPOCH_NANO = 100000000;

    private final InputStream inputStream;

    private final AtomicInteger maxBytesPerEpoch;
    private long startNano;
    private int bytesReadSinceStart;

    public RateLimitingInputStream(InputStream inputStream) {
        this(inputStream, -1);
    }

    public RateLimitingInputStream(InputStream inputStream,
                                   double kbps) {
        this.inputStream = inputStream;
        this.maxBytesPerEpoch = new AtomicInteger();
        setRateLimit(kbps);

        this.startNano = System.nanoTime() - EPOCH_NANO;
        this.bytesReadSinceStart = 0;
    }

    public void setRateLimit(double kbps) {
        if (Math.abs(kbps + 1) <= 1e-6)  // kbps == -1
            maxBytesPerEpoch.set(-1);
        else
            maxBytesPerEpoch.set((int)(kbps * 100));
    }

    private void sleepUntilNextEpoch() {
        long sleepMillis = (startNano + EPOCH_NANO - System.nanoTime()) / 1000000;
        if (sleepMillis > 0)
            Util.sleepMillis(sleepMillis);
    }

    private int readByte() throws IOException {
        bytesReadSinceStart++;
        return inputStream.read();
    }

    @Override
    public int read() throws IOException {
        long nowNano = System.nanoTime();
        int maxBytesPerEpoch = this.maxBytesPerEpoch.get();

        if (nowNano - startNano > EPOCH_NANO) {
            startNano = nowNano;
            bytesReadSinceStart = 0;
        } else if (maxBytesPerEpoch != -1 && bytesReadSinceStart >= maxBytesPerEpoch) {
            sleepUntilNextEpoch();
            startNano = System.nanoTime();
            bytesReadSinceStart = 0;
        }

        return readByte();
    }
}
