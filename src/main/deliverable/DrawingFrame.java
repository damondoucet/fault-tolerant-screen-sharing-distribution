package main.deliverable;
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
 * Adapted from Chris Bailey-Kellogg, Dartmouth CS 10, Winter 2014
 */
public class DrawingFrame extends JFrame {
    private static final Font font = new Font("Arial", Font.BOLD, 24);

    public JComponent canvas;						// handles graphics display
    public BufferedImage image;						// what to draw by default
    public int width;
    public int height;
    public String parentAddress;

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
        this.parentAddress = parentIP;
        createCanvas();
        finishGUI(width, height);
        setImage(image);
    }

    /**
     * Creates our graphics-handling component.
     */
    protected void createCanvas() {
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

    protected void setParentAddress(String ip) {
        this.parentAddress = ip;
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
            g.setFont(font);
            int messageWidth = g.getFontMetrics().stringWidth(this.parentAddress);
            g.drawString(this.parentAddress, width - messageWidth - 20, height - 20);
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