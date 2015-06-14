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

        final JFrame frame = new JFrame();
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame.setSize(500, 700);
                frame.setVisible(true);
            }
        });

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
        final Option<Image> dbImage = new Option<>(frame.createImage(
                    (int) winSize.getWidth(),
                    (int) winSize.getHeight()));


        frame.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                Dimension winSize = frame.getSize();
                grid.setProps(BlockProps.compute(
                        grid.rows,
                        grid.columns,
                        (int) winSize.getWidth(),
                        (int) winSize.getHeight()
                ));
                dbImage.set(frame.createImage(
                        (int) winSize.getWidth(),
                        (int) winSize.getHeight()));
            }
        });


        NextFrameEvent nextFrame = new NextFrameEvent();

        while (true) {
            events.dispatchEvents();

            Graphics2D dbg = (Graphics2D) dbImage.get().getGraphics();
            Graphics2D g = (Graphics2D) frame.getGraphics();

            winSize = frame.getSize();
            clearGraphics(dbg, (int) winSize.getWidth(), (int) winSize.getHeight());

            grid.paint(dbg);
            g.drawImage(dbImage.get(), 0, 0, null);

            Thread.sleep(33);
            events.emit(nextFrame);
        }
    }

    public static void clearGraphics(Graphics g, int w, int h) {
        g.setColor(Color.ORANGE);
        g.fillRect(0, 0, w, h);
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
    float fadeStep = 0.08f;

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

            Thread.sleep(800);

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

class BlockProps {
    int blockSize;
    int blockSpace;
    int offsetX;
    int offsetY;
    public static double bsRatio = 0.10;

    private BlockProps() {  }

    public static BlockProps compute(int rows, int cols, int frameW, int frameH) {
        double n = Math.min(frameW/cols, frameH/rows);

        BlockProps props = new BlockProps();
        props.blockSize  = (int) (n * (1-bsRatio));
        props.blockSpace = (int) (n * bsRatio);
        props.offsetX = (int) Math.abs(frameW - n*cols + props.blockSpace)/2;
        props.offsetY = (int) Math.abs(frameH - n*rows + props.blockSpace)/2;

        return props;
    }

    @Override
    public String toString() {
        return String.format("Props(x=%d, y=%d, size=%d, space=%d)",
                offsetX, offsetY, blockSize, blockSpace);
    }
}
