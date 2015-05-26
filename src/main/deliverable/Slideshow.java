package main.deliverable;
import main.Snapshot;
import main.deliverable.DrawingFrame;
import main.util.QueueHandler;

import java.awt.image.BufferedImage;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Basic slide show app, very loosely adapted from Chris Bailey-Kellogg app
 * from Dartmouth CS.
 */
public class Slideshow extends DrawingFrame {
    private final QueueHandler<Snapshot> queueHandler;

    private static BufferedImage getStartingImage() {
        return new BufferedImage(600, 300, BufferedImage.TYPE_INT_ARGB);
    }

    public Slideshow(ConcurrentLinkedQueue<Snapshot> images, String source) {
        super("Slideshow", getStartingImage(), 600, 300, source);

        queueHandler = new QueueHandler<>(
                images,
                (snapshot) -> advance(snapshot.getImage()));
        queueHandler.start();
    }

    /**
     * Advances to the next slide.
     */
    private void advance(BufferedImage img) {
        image = img;
        // Need to redraw since image is modified
        repaint();
    }

    public void close() {
        queueHandler.stop();
    }

    public void setParentAddress(String ipAddr) {
        super.setParentAddress(ipAddr);
    }
}