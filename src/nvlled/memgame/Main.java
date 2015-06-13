package nvlled.memgame;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

import nvlled.emit.*;

public class Main {
    public static void main(String[] args) throws Exception {
        final MemGrid grid = MemGrid.createAuto(
            ImageMemBlock.loadFile("images/triangle.png"),
            ImageMemBlock.loadFile("images/square.png"),
            ImageMemBlock.loadFile("images/circle.png"),
            ImageMemBlock.loadFile("images/kitten.png"),
            ImageMemBlock.loadFile("images/spider.png"),
            ImageMemBlock.loadFile("images/ex.png")
        );

        JFrame frame = new JFrame();
        frame.setSize(700, 500);
        frame.setVisible(true);

        final Emitter<GameEvent> events = new Emitter<GameEvent>();

        frame.addMouseListener(new MouseHandler(grid, events));
        final Script script = new MainScript(grid, events);

        new Thread(new Runnable() {
            public void run() {
                try {
                    script.run();
                } catch (InterruptedException e) {
                    System.out.println("script interrupted");
                }
            }
        }).start();

        Dimension winSize = frame.getSize();
        Image dbImage = frame.createImage((int) winSize.getWidth(), (int) winSize.getHeight());
        while (true) {
            events.dispatchEvents();

            grid.paint((Graphics2D) dbImage.getGraphics());
            Graphics2D g = (Graphics2D) frame.getGraphics();
            g.drawImage(dbImage, 0, 0, null);

            Thread.sleep(33);
        }
    }
}

class BlockSelectEvent extends GameEvent {
    public static final int TYPE = 1024;

    MemBlock block;

    BlockSelectEvent(MemBlock block) {
        this.block = block;
    }

    @Override
    int getType() { return TYPE; }

    MemBlock getBlock() { return block; }
}

class MouseHandler extends MouseAdapter {
    MemGrid grid;
    Emitter<GameEvent> events;

    public MouseHandler(MemGrid grid, Emitter<GameEvent> events) {
        this.grid = grid;
        this.events = events;
    }

    public void mouseClicked(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        MemBlock block = grid.getBlockAt(x, y);
        if (block != null) {
            events.emit(new BlockSelectEvent(block));
        }
    }
}

class MainScript implements Script {
    MemGrid grid;
    Emitter<GameEvent> events;

    public MainScript(MemGrid grid, Emitter<GameEvent> events) {
        this.grid = grid;
        this.events = events;
    }

    private MemBlock nextBlock() throws InterruptedException {
        BlockSelectEvent e = (BlockSelectEvent) events.waitEvent(new Predicate<GameEvent>() {
            public boolean test(GameEvent e) {
                if (e.getType() == BlockSelectEvent.TYPE) {
                    return !((BlockSelectEvent) e).getBlock().shown;
                }
                return false;
            }
        });
        return e.getBlock();
    }

    public void run() throws InterruptedException {
        while(true) {
            // TODO: add effects
            MemBlock block1 = nextBlock();
            block1.show();

            MemBlock block2 = block1;
            while (block1 == block2)
                block2 = nextBlock();
            block2.show();

            Thread.sleep(1200);

            if (!block1.equals(block2)) {
                block1.hide();
                block2.hide();
            }
        }
    }
}
