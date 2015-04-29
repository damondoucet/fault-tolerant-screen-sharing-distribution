package main.input;

import main.Snapshot;
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
    private Robot myRobot;
    private Rectangle screenRectangle;
    private ConcurrentLinkedQueue<Snapshot> buffer;
    private long frequency; // in fps
    private AtomicBoolean isCapturing;
    private Snapshot mySnapshot;
    private long delay; // in millis
    private Dimension dimension;

    private ScreenGrabber(Robot robot,
                          ConcurrentLinkedQueue<Snapshot> buffer,
                          long frequency, Dimension dimension) {
        this.myRobot = robot;
        this.dimension = dimension;
        this.screenRectangle = new Rectangle(this.dimension);
        this.buffer = buffer;
        this.frequency = frequency;
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
        return new ScreenGrabber(new Robot(), buffer, frequency, Toolkit.getDefaultToolkit().getScreenSize());
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
        new Thread(() -> capture()).start(); // lambda function that is coerced to be a Runnable
    }

    public void endCapture() {
        this.isCapturing.set(false);
    }

    public void capture() {
        while (this.myRobot != null && this.isCapturing.get()) {
            long startTimeMillis = System.currentTimeMillis();
            BufferedImage img = this.myRobot.createScreenCapture(this.screenRectangle);
            if(img != null) {
                img = Scalr.resize(img, (int) this.dimension.getWidth(), (int) this.dimension.getHeight());
                if (this.mySnapshot == null) {
                    this.mySnapshot = Snapshot.lossySnapshot(0, img);
                    this.buffer.add(this.mySnapshot);
                } else {
                    this.buffer.add(this.mySnapshot.createNext(img));
                }
            }

            long timeElapsed = System.currentTimeMillis() - startTimeMillis;
            long sleepTime = (this.delay - timeElapsed > 0) ? this.delay - timeElapsed : 0;
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
