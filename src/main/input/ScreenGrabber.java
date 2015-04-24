package main.input;

import java.awt.*;
import java.awt.image.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Merry on 4/23/15.
 */
public class ScreenGrabber {

    private Robot myRobot;
    private Rectangle screenRectangle;
    private ConcurrentLinkedQueue<BufferedImage> buffer;
    private long frequency;
    private AtomicBoolean isCapturing;


    /**
     *
     * @param buffer
     * @param frequency in millis
     */
    public ScreenGrabber(ConcurrentLinkedQueue<BufferedImage> buffer, long frequency) {
        try {
            this.myRobot = new Robot();
            this.screenRectangle = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            this.buffer = buffer;
            this.frequency = frequency;
            this.isCapturing = new AtomicBoolean();
        } catch (AWTException e) {
            e.printStackTrace();
        }
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
                this.buffer.add(img);
            }

            long timeElapsed = System.currentTimeMillis() - startTimeMillis;
            long sleepTime = (this.frequency - timeElapsed > 0) ? this.frequency - timeElapsed : 0;
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

}
