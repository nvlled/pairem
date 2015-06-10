package nvlled.memgame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class Main {
    public static void main(String[] args) throws Exception {
        final MemGrid grid = MemGrid.createAuto(
            ImageMemBlock.loadFile("images/triangle.png"),
            ImageMemBlock.loadFile("images/square.png"),
            ImageMemBlock.loadFile("images/circle.png"),
            ImageMemBlock.loadFile("images/kitten.png"),
            ImageMemBlock.loadFile("images/ex.png"),
            ImageMemBlock.loadFile("images/spider.png")
        );

        JFrame frame = new JFrame();
        frame.setSize(700, 500);
        frame.setVisible(true);
        frame.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int x = e.getX();
                int y = e.getY();
                MemBlock block = grid.getBlockAt(x, y);
                if (block != null)
                    block.shown = !block.shown;
            }
        });

        //InputTranslator translator;

        while (true) {
            //translator.emitEvents();

            Graphics2D g = (Graphics2D) frame.getGraphics();
            grid.paint(g);
            Thread.sleep(33);
        }
    }

    public static void draw(Graphics2D g, MemGrid grid) { }
}
