package nvlled.memgame;

import java.awt.*;
import javax.swing.*;
import javax.imageio.*;
import java.io.*;

public abstract class MemBlock {
    Renderer renderer;
    boolean shown;

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
