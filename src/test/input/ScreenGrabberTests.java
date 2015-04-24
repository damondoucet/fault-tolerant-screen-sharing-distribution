package test.input;

import main.input.ScreenGrabber;

import main.network.Util;
import org.junit.Test;
import static org.junit.Assert.assertTrue;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Tests the ScreenGrabber.
 */
public class ScreenGrabberTests {
    // Tests that the screen grabber takes screenshots at the right frequency,
    // with some margin for error.
    @Test
    public void TestBasic() throws AWTException {
        ConcurrentLinkedQueue<BufferedImage> buffer = new ConcurrentLinkedQueue<BufferedImage>();
        ScreenGrabber grabber = ScreenGrabber.fromQueueAndFrequency(buffer, 100);
        int testDuration = 2000;
        grabber.startCapture();
        Util.sleepMillis(testDuration);
        grabber.endCapture();
        assertTrue(buffer.size() > testDuration/100/2);
    }

}
