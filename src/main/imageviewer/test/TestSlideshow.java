package main.imageviewer.test;

import main.imageviewer.Slideshow;
import main.input.ScreenGrabber;
import org.junit.Test;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

/**
 * Created by anubhav on 4/24/15.
 */
public class TestSlideshow {

    /* @Test
    public void TestBasic() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                // Read the images, named dart0.jpg ... dart<numSlides>.jpg, and store in array.
                BufferedImage[] images = new BufferedImage[3];
                try {
//                    for (int i = 1; i < numSlides; i++) {
//                        images[i] = ImageIO.read(new File("images/image"+i+".png"));
//                    }
                    images[0] = ImageIO.read(new File("images/image1.png"));
                    images[1] = ImageIO.read(new File("images/image2.png"));
                    images[2] = ImageIO.read(new File("images/image3.png"));
                } catch (Exception e) {
                    System.err.println("Couldn't load image");
                }
                System.out.println(images);
                // Fire off the slideshow.
                new Slideshow(images);
            }
        });
//        assert(true);/
    }*/
}
