package main;

import com.google.common.primitives.Longs;
import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;
import main.util.Util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

/**
 * Represents an image captured from the screen. Snapshots are ordered by their
 * frame indexes.
 */
public class Snapshot {
    private final long frameIndex;
    private final BufferedImage image;

    // Use lossy=false for testing, lossy=true for better performance.
    private final boolean lossy;

    private Snapshot(long frameIndex, BufferedImage image, boolean lossy) {
        this.frameIndex = frameIndex;
        this.image = image;
        this.lossy = lossy;
    }

    public static Snapshot lossySnapshot(long frameIndex, BufferedImage image) {
        return new Snapshot(frameIndex, image, true);
    }

    public static Snapshot losslessSnapshot(long frameIndex, BufferedImage image) {
        return new Snapshot(frameIndex, image, false);
    }

    private static String getEncoding(boolean lossy) {
        return lossy ? "JPG" : "PNG";
    }

    private String getEncoding() {
        return getEncoding(lossy);
    }

    public long getFrameIndex() {
        return frameIndex;
    }

    public BufferedImage getImage() {
        return image;
    }

    public Snapshot createNext(BufferedImage newImage) {
        return new Snapshot(frameIndex + 1, newImage, lossy);
    }

    private ByteOutputStream imageToByteOutputStream() throws IOException {
        ByteOutputStream outputStream = new ByteOutputStream();
        ImageIO.write(image, getEncoding(), outputStream);
        return outputStream;
    }

    // ImageIO is a little obnoxious in that it doesn't necessarily read all
    // the bytes it wrote, so we have to write the length of the image and then
    // clean up the buffer after reading.
    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(Longs.toByteArray(frameIndex));

        ByteOutputStream imageOutput = imageToByteOutputStream();
        outputStream.write(Longs.toByteArray(imageOutput.size()));
        imageOutput.writeTo(outputStream);

        return outputStream.toByteArray();
    }

    private static long readLong(InputStream stream) throws IOException {
        return Longs.fromByteArray(Util.read(stream, Long.BYTES));
    }

    private static BufferedImage readImage(InputStream stream, long imageSize)
            throws IOException {
        return ImageIO.read(new ByteArrayInputStream(
                Util.read(stream, (int)imageSize)));
    }

    // See comment above toBytes
    public static Snapshot fromInputStream(InputStream stream, boolean lossy)
            throws IOException {
        long index = readLong(stream);
        long imageSize = readLong(stream);
        BufferedImage image = readImage(stream, imageSize);

        return new Snapshot(index, image, lossy);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass())
            return false;

        Snapshot rhs = (Snapshot)obj;
        return getFrameIndex() == rhs.getFrameIndex() &&
                imagesEqual(getImage(), rhs.getImage());
    }

    public static boolean imagesEqual(BufferedImage lhs, BufferedImage rhs) {
        if ((lhs == null) ^ (rhs == null))
            return false;
        if (lhs == null && rhs == null)
            return true;

        int width = lhs.getWidth(), height = lhs.getHeight();

        if (width != rhs.getWidth() ||
                height != rhs.getHeight())
            return false;

        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
                if (lhs.getRGB(x, y) != rhs.getRGB(x, y))
                    return false;

        return true;
    }
}
