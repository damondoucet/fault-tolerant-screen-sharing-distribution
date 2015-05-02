package test.unit.imageviewer;

import main.Snapshot;
import main.imageviewer.Slideshow;
import main.input.ScreenGrabber;
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
        Util.sleepMillis(testDuration);
        Slideshow test = new Slideshow(buffer);
        Util.sleepMillis(testDuration);

        grabber.endCapture();
        test.close();
    }

}
