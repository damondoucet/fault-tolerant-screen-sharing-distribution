package main.input.test;

import main.input.ScreenGrabber;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by Merry on 4/23/15.
 */
public class TestScreenGrabber {

    @Test
    public void TestBasic() {
        ConcurrentLinkedQueue<BufferedImage> buffer = new ConcurrentLinkedQueue<BufferedImage>();
        ScreenGrabber grabber = new ScreenGrabber(buffer, 100);
        int testDuration = 2000;
        grabber.startCapture();
        try {
            TimeUnit.MILLISECONDS.sleep(testDuration);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        grabber.endCapture();
        assertTrue(buffer.size() > testDuration/100/2);
    }

}
