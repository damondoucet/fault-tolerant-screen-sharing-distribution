package test.unit.deliverable;

import main.Snapshot;
import main.deliverable.ImageDisplay;
import main.deliverable.ScreenGrabber;
import main.util.Util;
import org.junit.Test;

import java.awt.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Tests the iamge viewer with a screen grabber.
 */
public class ImageViewerTests {
    @Test
    public void TestBasic() throws AWTException {
        ConcurrentLinkedQueue<Snapshot> buffer = new ConcurrentLinkedQueue<>();
        ScreenGrabber grabber = ScreenGrabber.fromQueueAndFrequency(buffer, 30);
        int testDuration = 5000;

        grabber.startCapture();
        ImageDisplay test = new ImageDisplay("Client", buffer, "127.0.0.1");
        Util.sleepMillis(testDuration);

        grabber.endCapture();
        test.close();
    }

}
