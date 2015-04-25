package test;

import main.Snapshot;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 * Utilities for using buffered images in testing.
 */
public class ImageUtil {
    private final static int WIDTH = 256;
    private final static int HEIGHT = 128;

    // Yellow rectangle with a diagonal red line
    public static BufferedImage createImage1() {
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = img.createGraphics();

        graphics.setBackground(Color.YELLOW);
        graphics.clearRect(0, 0, WIDTH, HEIGHT);

        graphics.setColor(Color.RED);
        graphics.drawLine(20, 50, 200, 10);

        graphics.dispose();
        return img;
    }

    // Orange rectangle with a black circle
    public static BufferedImage createImage2() {
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = img.createGraphics();

        graphics.setBackground(Color.ORANGE);
        graphics.clearRect(0, 0, WIDTH, HEIGHT);

        graphics.setColor(Color.BLACK);
        Ellipse2D circle = new Ellipse2D.Double(65, 10, 60, 60);
        graphics.draw(circle);

        graphics.dispose();
        return img;
    }

    // Uncomment this to write the images to the root directory to make sure
    // the images look correct.
    @Test
    public void writeImages() throws IOException {
        BufferedImage im1 = createImage1();
        ImageIO.write(im1, "PNG", new File("image1.PNG"));
        BufferedImage im2 = ImageIO.read(new File("image1.png"));
        ImageIO.write(im2, "PNG", new File("image1prime.png"));
        assertTrue(Snapshot.imagesEqual(im1, im2));
        ImageIO.write(createImage2(), "JPG", new File("image2.jpg"));
    }

    @Test
    public void testImageEquality() {
        BufferedImage image = createImage1();

        assertTrue(Snapshot.imagesEqual(image, image));
        assertTrue(Snapshot.imagesEqual(image, createImage1()));
    }
}
