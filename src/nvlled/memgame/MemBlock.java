package nvlled.memgame;

import java.awt.*;
import javax.swing.*;
import javax.imageio.*;
import java.io.*;

public abstract class MemBlock {
    Renderer renderer;
    boolean shown;
    boolean solving = false;

    private static final Color BORDER_COLOR = new Color(40, 40, 40);
    private static final Color SOLVING_BORDER_COLOR = new Color(200, 180, 200);
    private static final int BORDER_SIZE = 8;

    public abstract void paintBlock(Graphics2D g);

    public abstract MemBlock copy();

    public boolean equals(MemBlock block) {
        return this == block;
    }

    public void paintBackground(Graphics2D g) {
        Rectangle rect = g.getClipBounds();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, (int) rect.getWidth(), (int) rect.getHeight());
    }

    public void paint(Graphics2D g) {
        Rectangle rect = g.getClipBounds();
        int w = (int) rect.getWidth();
        int h = (int) rect.getHeight();
        int size = BORDER_SIZE;

        Color borderColor = BORDER_COLOR;
        if (solving)
            borderColor = SOLVING_BORDER_COLOR;
        g.setColor(borderColor);

        g.fillRoundRect(0, 0, w, h, 20, 20);
        g.clipRect(size, size, w-size, h-size);

        Renderer r = renderer;
        if (r != null) {
            if (renderer.isOverlay()) {
                if (shown) {
                    paintBlock(g);
                } else {
                    paintBackground(g);
                }
            }
            r.paint(g);
        } else {
            paintBackground(g);
            if (shown) {
                paintBlock(g);
            }
        }
    }

    public void setSolving(boolean t) { solving = t; }
    public void show() { shown = true; }
    public void hide() { shown = false; }

    public void setRenderer(Renderer r) {
        renderer = r;
    }

    public interface Renderer {
        public void paint(Graphics2D g);
        public boolean isOverlay();
    }
}

class ImageMemBlock extends MemBlock {
    Image image;

    public ImageMemBlock(Image image) {
        this.image = image;
    }

    public MemBlock copy() {
        return new ImageMemBlock(image);
    }

    public static ImageMemBlock loadFile(String filename) throws IOException {
        Image image = ImageIO.read(new File(filename));
        return new ImageMemBlock(image);
    }

    @Override
    public boolean equals(MemBlock block) {
        if (block instanceof ImageMemBlock) {
            ImageMemBlock block_ = (ImageMemBlock) block;
            return image == block_.image;
        }
        return false;
    }

    @Override
    public void paintBlock(Graphics2D g) {
        // TODO: adjust image size to current graphics size
        Rectangle rect = g.getClipBounds();
        g.drawImage(image, 0, 0, (int) rect.getWidth(), (int) rect.getHeight(), null);
    }
}
