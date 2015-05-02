package test.unit.input;

import main.Snapshot;
import main.input.ScreenGrabber;

import main.util.Util;
import org.junit.Test;
import static org.junit.Assert.*;

import java.awt.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Tests the ScreenGrabber.
 */
public class ScreenGrabberTests {

    // Tests that the screen grabber takes screenshots at the right frequency,
    // with some margin for error. May not pass every time depending on
    // what you are doing with your computer, so run it a couple of times.
    @Test
    public void TestFPS() throws AWTException {
        ConcurrentLinkedQueue<Snapshot> buffer = new ConcurrentLinkedQueue<>();
        int[] fpsValues = {1, 10, 20, 30};
        int testDurationSecs = 2;
        for (int fps : fpsValues) {
            ScreenGrabber grabber = ScreenGrabber.fromQueueAndFrequency(buffer, fps);
            grabber.startCapture();
            Util.sleepMillis(testDurationSecs*1000);
            grabber.endCapture();
            System.out.println("actual: " + buffer.size() + ", ideal: " + testDurationSecs * fps);
            assertTrue(buffer.size() > testDurationSecs * fps / 1.5);
            assertTrue(buffer.size() < testDurationSecs * fps + 1);
            buffer.clear();
        }
    }

    // Tests that the screen grabber takes screenshots at the right resolution.
    @Test
    public void TestResolution() throws AWTException {
        ConcurrentLinkedQueue<Snapshot> buffer = new ConcurrentLinkedQueue<>();
        int fps = 20;
        Dimension dimension = new Dimension(300, 500);
        int testDurationSecs = 1;

        ScreenGrabber grabber = ScreenGrabber.fromQueueAndFrequency(buffer, fps, dimension);
        grabber.startCapture();
        Util.sleepMillis(testDurationSecs*1000);
        grabber.endCapture();

        int previousFrame = -1;
        while (!buffer.isEmpty()) {
            Snapshot thisSnapshot = buffer.poll();

            assertEquals(previousFrame + 1, thisSnapshot.getFrameIndex());
            previousFrame++;

            assertTrue(thisSnapshot.getImage().getWidth() == dimension.getWidth());
            assertTrue(thisSnapshot.getImage().getHeight() == dimension.getHeight());
        }
    }
}
