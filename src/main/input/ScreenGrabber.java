package main.input;

import main.Snapshot;
import main.util.Util;
import org.imgscalr.Scalr;

import java.awt.*;
import java.awt.image.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Takes screenshots at a specific frequency and outputs them to a
 * ConcurrentLinkedQueue<BufferedImage> at a specific resolution.
 */
public class ScreenGrabber {
    private final Robot myRobot;
    private final Rectangle screenRectangle;
    private final ConcurrentLinkedQueue<Snapshot> buffer;
    private final AtomicBoolean isCapturing;
    private Snapshot mySnapshot;
    private final long delay; // in millis
    private final Dimension dimension;

    private ScreenGrabber(Robot robot,
                          ConcurrentLinkedQueue<Snapshot> buffer,
                          long frequency, Dimension dimension) {
        this.myRobot = robot;
        this.dimension = dimension;
        this.screenRectangle = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        this.buffer = buffer;
        this.isCapturing = new AtomicBoolean();
        this.delay = 1000 / frequency;
    }

    /**
     *
     * Public constructor.
     *
     * @param buffer
     * @param frequency
     * @return
     * @throws AWTException
     */
    public static ScreenGrabber fromQueueAndFrequency(ConcurrentLinkedQueue<Snapshot> buffer,
                                                      long frequency)
            throws AWTException {
        return fromQueueFrequencyDimension(buffer, frequency, Toolkit.getDefaultToolkit().getScreenSize());
    }

    public static ScreenGrabber fromQueueFrequencyDimension(ConcurrentLinkedQueue<Snapshot> buffer,
                                                            long frequency,
                                                            Dimension dimension)
            throws AWTException {
        return new ScreenGrabber(new Robot(), buffer, frequency, dimension);
    }

    /**
     *
     * Public constructor with dimension parameter.
     *
     * @param buffer
     * @param frequency
     * @param dimension
     * @return
     * @throws AWTException
     */
    public static ScreenGrabber fromQueueAndFrequency(ConcurrentLinkedQueue<Snapshot> buffer,
                                                      long frequency, Dimension dimension)
            throws AWTException {
        return new ScreenGrabber(new Robot(), buffer, frequency, dimension);
    }


    public void startCapture() {
        this.isCapturing.set(true);
        new Thread(this::capture).start(); // lambda function that is coerced to be a Runnable
    }

    public void endCapture() {
        this.isCapturing.set(false);
    }

    private BufferedImage resize(BufferedImage img) {
        return Scalr.resize(img,
                Scalr.Method.SPEED,
                Scalr.Mode.FIT_EXACT,
                (int) this.dimension.getWidth(),
                (int) this.dimension.getHeight());
    }

    public void capture() {
        while (this.myRobot != null && this.isCapturing.get()) {
            long startTimeMillis = System.currentTimeMillis();
            BufferedImage img = this.myRobot.createScreenCapture(this.screenRectangle);
            if(img != null) {
                img = resize(img);
                if (this.mySnapshot == null)
                    this.mySnapshot = Snapshot.lossySnapshot(0, img);
                else
                    this.mySnapshot = this.mySnapshot.createNext(img);

                this.buffer.add(this.mySnapshot);
            }

            long timeElapsed = System.currentTimeMillis() - startTimeMillis;
            long sleepTime = Math.max(this.delay - timeElapsed, 0);
            Util.sleepMillis(sleepTime);
        }
    }
}
