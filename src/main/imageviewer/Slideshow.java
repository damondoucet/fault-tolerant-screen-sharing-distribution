package main.imageviewer;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;
import javax.swing.*;

import java.io.File;
import java.util.ArrayList;

/**
 * Basic slide show app, adapted from Chris Bailey-Kellogg app from Dartmouth CS
 */
public class Slideshow extends DrawingFrame {
    private static final int numSlides = 4;			// number of slides

    private BufferedImage[] slides;					// images to display
    private int curr = 0;							// current slide number

    public Slideshow(BufferedImage[] images) {
        super("Slideshow", images[0]);
        slides = images;

        canvas.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent event) {
                advance();
            }
        });
    }

    /**
     * Advances to the next slide.
     */
    private void advance() {
        curr = (curr + 1) % numSlides; // use modular arithmetic to wrap around to 0
        System.out.println("slide "+curr);
        image = slides[curr];
        // Need to redraw since image is modified
        repaint();
    }

    /**
     * Main method for the application
     * @param args		command-line arguments (ignored)
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                // Read the images, named dart0.jpg ... dart<numSlides>.jpg, and store in array.
                BufferedImage[] images = new BufferedImage[numSlides];
                try {
//                    for (int i = 1; i < numSlides; i++) {
//                        images[i] = ImageIO.read(new File("images/image"+i+".png"));
//                    }
                    images[0] = ImageIO.read(new File("images/image1.png"));
                    images[1] = ImageIO.read(new File("images/image2.png"));
                    images[2] = ImageIO.read(new File("images/image3.png"));
                }
                catch (Exception e) {
                    System.err.println("Couldn't load image");
                }
                System.out.println(images);
                // Fire off the slideshow.
                new Slideshow(images);
            }
        });
    }
}