package main.deliverable;
import main.Snapshot;
import main.util.QueueHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Basic slide show app, very loosely adapted from Chris Bailey-Kellogg app
 * from Dartmouth CS.
 */
public class ImageDisplay extends JFrame {
    private final static int WIDTH = 600;
    private final static int HEIGHT = 300;

    private final QueueHandler<Snapshot> queueHandler;

    private static BufferedImage getStartingImage() {
        return new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
    }

    private static final Font font = new Font("Arial", Font.BOLD, 24);

    private final JComponent canvas;					// handles graphics display
    private BufferedImage image;						// what to draw by default
    private int width;
    private int height;
    private String parentAddress;

    public ImageDisplay(ConcurrentLinkedQueue<Snapshot> images, String source) {
        this.width = WIDTH;
        this.height = HEIGHT;
        this.parentAddress = source;
        canvas = createCanvas();
        finishGUI(width, height);
        setImage(getStartingImage());

        queueHandler = new QueueHandler<>(
                images,
                (snapshot) -> setImage(snapshot.getImage()));
        queueHandler.start();
    }

    /**
     * Creates our graphics-handling component.
     */
    private JComponent createCanvas() {
        JComponent canvas = new JComponent() {
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
        return canvas;
    }

    private void setWidth(int width) {
        this.width = width;
    }

    private void setHeight(int height) {
        this.height = height;
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
     * Sets the default image
     *
     * @param image
     */
    public void setImage(BufferedImage image) {
        this.image = image;
        repaint();
    }

    public void close() {
        queueHandler.stop();
    }

    public void setParentAddress(String ipAddr) {
        this.parentAddress = ipAddr;
    }
}