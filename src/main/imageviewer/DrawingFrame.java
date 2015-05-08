package main.imageviewer;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * A simple JFrame containing a single component for drawing in (via the draw method)
 * Allows setting of a default image to be drawn if the draw method is not overridden.
 *
 * @author Chris Bailey-Kellogg, Dartmouth CS 10, Winter 2014
 */
public class DrawingFrame extends JFrame {
    public JComponent canvas;						// handles graphics display
    public BufferedImage image;						// what to draw by default
    public int width;
    public int height;
    public String parentIP;

    /**
     * Just the shell -- need to call finishGUI once the size is known.
     *
     * @param title		displayed in window title bar
     */
    public DrawingFrame(String title) {
        super(title);
        createCanvas();
    }

    /**
     * A canvas for the specified image file
     *
     * @param title		displayed in window title bar
     * @param filename	for the image
     */
    public DrawingFrame(String title, String filename, int width, int height) {
        super(title);
        try {
            image = ImageIO.read(new File(filename));
        }
        catch (Exception e) {
            System.err.println("Couldn't load image");
            System.exit(-1);
        }
        createCanvas();
        this.width = width;
        this.height = height;
        finishGUI(width, height);
    }

    /**
     * An image-ful canvas, with a preloaded image
     *
     * @param title		displayed in window title bar
     * @param image
     */
    public DrawingFrame(String title, BufferedImage image, int width, int height, String parentIP) {
        super(title);
        this.width = width;
        this.height = height;
        this.parentIP = parentIP;
        createCanvas();
        finishGUI(width, height);
        setImage(image);
    }

    /**
     * An image-less canvas of the specified size.
     *
     * @param title		displayed in window title bar
     * @param width		window size
     * @param height	window size
     */
    public DrawingFrame(String title, int width, int height) {
        super(title);
        createCanvas();
        this.width = width;
        this.height = height;
        finishGUI(width, height);
    }

    /**
     * Creates our graphics-handling component.
     */
    protected void createCanvas() {
        System.out.println("Calling create canvas");
        canvas = new JComponent() {
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                draw(g);
            }
        };
        canvas.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                setWidth(e.getComponent().getWidth());
                setHeight(e.getComponent().getHeight());

            }
        });
    }

    private void setWidth(int width) {
        this.width = width;
    }

    private void setHeight(int height) {
        this.height = height;
    }

    protected void setParentIP(String ip) {
        this.parentIP = ip;
    }

    /**
     * Boilerplate to finish initializing the GUI to the specified size.
     *
     * @param width
     * @param height
     */
    public void finishGUI(int width, int height) {
        setSize(width, height);
        canvas.setPreferredSize(new Dimension(width, height));
        getContentPane().add(canvas);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        pack();
        setVisible(true);
    }

    /**
     * Displays the default image (or anything else -- subclasses can override) using the graphics object
     *
     * @param g
     */
    public void draw(Graphics g) {
        if (image != null) {
            g.drawImage(image, 0, 0, width, height, null);
            Font font = new Font("Arial", Font.BOLD, 24);
            g.setFont(font);
            g.drawString(this.parentIP, width - 170, height - 20);
            canvas.setForeground(Color.RED);

        }
    }

    /**
     * Gets the default image
     */
    public BufferedImage getImage() {
        return image;
    }

    /**
     * Sets the default image
     *
     * @param image
     */
    public void setImage(BufferedImage image) {
        this.image = image;
        repaint();
    }
}