package main.imageviewer;
import main.Snapshot;
import main.network.Util;

import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;
import javax.swing.*;

import java.io.File;
import java.util.concurrent.ConcurrentLinkedQueue;
import static com.google.common.base.Preconditions.*;

/**
 * Basic slide show app, adapted from Chris Bailey-Kellogg app from Dartmouth CS
 */
public class Slideshow extends DrawingFrame {
    private ConcurrentLinkedQueue<Snapshot> slides;					// images to display

    public Slideshow(ConcurrentLinkedQueue<Snapshot> images) {
        // TODO: this should be init'd with a black window
        super("Slideshow", Util.next(images).getImage(), 600, 300);
        slides = images;

        new Thread(() -> {
            while (true) {
                Snapshot img = Util.next(slides);
                advance(img.getImage());
            }
        }).start();
    }

    /**
     * Advances to the next slide.
     */
    private void advance(BufferedImage img) {
        image = img;
        // Need to redraw since image is modified
        repaint();
    }

    /**
     * Main method for the application
     * @param args		command-line arguments (ignored)
     */
    /*public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ConcurrentLinkedQueue<Snapshot> queue = new ConcurrentLinkedQueue<>();
            new Thread(() -> {
                Util.sleepMillis(125);

                try {
                    BufferedImage[] images = new BufferedImage[3];
                    images[0] = ImageIO.read(new File("images/image1.png"));
                    images[1] = ImageIO.read(new File("images/image2.png"));
                    images[2] = ImageIO.read(new File("images/image3.png"));
                    checkState(images[0] != null && images[1] != null && images[2] != null);
                    int index = 0;
                    Snapshot next = Snapshot.lossySnapshot(0, images[0]);
                    while (true) {
                        queue.add(next);
                        index = (index + 1) % images.length;
                        next = next.createNext(images[index]);
                        Util.sleepMillis(500);
                    }
                } catch (Exception e) {
                    Util.printException("Error pushing images", e);
                }
            }).start();

            new Slideshow(queue);
        });
    }*/
}