package test.input;

import main.Snapshot;
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
    // with some margin for error. May not pass every time depending on
    // what you are doing with your computer, so run it a couple of times.
    @Test
    public void TestBasic() throws AWTException {
        ConcurrentLinkedQueue<Snapshot> buffer = new ConcurrentLinkedQueue<Snapshot>();
        int[] frequencies = {200, 100, 50, 30};
        int testDuration = 2000;
        for (int i=0; i < frequencies.length; i++) {
            ScreenGrabber grabber = ScreenGrabber.fromQueueAndFrequency(buffer, frequencies[i]);
            grabber.startCapture();
            Util.sleepMillis(testDuration);
            grabber.endCapture();
            System.out.println(buffer.size() + "  " + testDuration/frequencies[i]);
            assertTrue(buffer.size() > testDuration / frequencies[i] / 1.5);
            assertTrue(buffer.size() < testDuration / frequencies[i] + 1);
            buffer.clear();
        }
    }

}
