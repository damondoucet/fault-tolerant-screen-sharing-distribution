package test.imageviewer;

import main.Snapshot;
import main.imageviewer.Slideshow;
import main.input.ScreenGrabber;
import main.network.Util;
import org.junit.Test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.junit.Assert.assertTrue;

/**
 * Created by anubhav on 4/26/15.
 */
public class ImageViewerTests {

    @Test
    public void TestBasic() throws AWTException {
        ConcurrentLinkedQueue<Snapshot> buffer = new ConcurrentLinkedQueue<Snapshot>();
        ScreenGrabber grabber = ScreenGrabber.fromQueueAndFrequency(buffer, 30);
        int testDuration = 5000;
        grabber.startCapture();
        Util.sleepMillis(testDuration);
        Slideshow test = new Slideshow(buffer);
        Util.sleepMillis(testDuration);
        grabber.endCapture();
//        assertTrue(buffer.size() > testDuration/100/2);


    }

}
