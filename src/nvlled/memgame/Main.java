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

        NextFrameEvent nextFrame = new NextFrameEvent();

        new Thread(new Runnable() {
            public void run() {
                events.emit(new BlockSelectEvent(grid.blocks[0]));
                try { Thread.sleep(1000); } catch (Exception fuckyou) {}
                events.emit(new BlockSelectEvent(grid.blocks[1]));
            }
        }).start();

        while (true) {
            events.dispatchEvents();

            grid.paint((Graphics2D) dbImage.getGraphics());
            Graphics2D g = (Graphics2D) frame.getGraphics();
            g.drawImage(dbImage, 0, 0, null);

            Thread.sleep(33);
            events.emit(nextFrame);
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

class NextFrameEvent extends GameEvent {
    public static final int TYPE = 512;

    @Override
    int getType() { return TYPE; }
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

class FadeBlock implements MemBlock.Renderer, Runnable {
    MemBlock block;
    Emitter<GameEvent> events;
    float alpha = 0.0f;
    float fadeStep = 0.05f;

    private FadeBlock(MemBlock block, Emitter<GameEvent> events) {
        this.block = block;
        this.events = events;
    }

    public static FadeBlock show(MemBlock block, Emitter<GameEvent> events) {
        return new FadeBlock(block, events);
    }

    public static FadeBlock hide(MemBlock block, Emitter<GameEvent> events) {
        FadeBlock fd = new FadeBlock(block, events);
        fd.alpha = 1.0f;
        fd.fadeStep *= -1f;
        return fd;
    }

    @Override
    public boolean isOverlay() { return false; }

    @Override
    public void paint(Graphics2D g) {
        block.paintBackground(g);
        if (alpha >= 0f && alpha <= 1f)
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        block.paintBlock(g);
    }

    public void run() {
        block.setRenderer(this);
        int type = NextFrameEvent.TYPE;
        Emitter.Iterator<GameEvent> frames = events.iterator(new TypePredicate<GameEvent>(type));

        while (alpha >= 0f && alpha <= 1f) {
            alpha += fadeStep;
            frames.next();
        }
        frames.close();

        if (alpha < 0.5f) {
            block.hide();
        } else {
            block.show();
        }
        block.setRenderer(null);
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
            MemBlock block1 = nextBlock();
            FadeBlock.show(block1, events).run();

            MemBlock block2 = block1;
            while (block1 == block2)
                block2 = nextBlock();
            FadeBlock.show(block2, events).run();

            Thread.sleep(1200);

            if (!block1.equals(block2)) {
                // TODO: use executors
                new Thread(FadeBlock.hide(block1, events)).start();
                FadeBlock.hide(block2, events).run();
            }
        }
    }
}

class TypePredicate<T extends GameEvent> implements Predicate<T> {
    private int type;

    public TypePredicate(int type) { this.type = type; }

    @Override
    public boolean test(T e) {
        return e.getType() == type;
    }
}
