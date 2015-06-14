package nvlled.memgame;

import java.awt.*;
import java.util.*;
import javax.swing.*;

public class MemGrid {
    int rows;
    int columns;

    int blockSize = 100; // pixel unit
    int blockSpace = 20; // pixel unit
    int offsetX;
    int offsetY;

    MemBlock[] blocks;

    public MemGrid(int rows, int columns, MemBlock[] blocks) {
        this.rows = rows;
        this.columns = columns;
        this.blocks = blocks;
    }

    public static MemGrid createAuto(MemBlock... blockArgs) {
        java.util.List<MemBlock> blockList = new LinkedList<MemBlock>();
        for (MemBlock block: blockArgs) {
            // add twice since pairs of block are needed
            blockList.add(block.copy());
            blockList.add(block.copy());
        }
        Collections.shuffle(blockList);

        int size = blockList.size();
        int columns = (int) Math.sqrt(size);
        if (size % columns != 0)
            columns = 2;
        int rows = size/columns;

        MemBlock[] blocks = blockList.toArray(new MemBlock[size]);
        return new MemGrid(rows, columns, blocks);
    }

    public void paint(Graphics2D g) {
        for (int i = 0; i < blocks.length; i++) {
            int row = i / columns;
            int col = i % columns;

            int z = blockSize + blockSpace;
            int x = offsetX + col * z;
            int y = offsetY + row * z;

            Graphics2D g_ = (Graphics2D) g.create(x, y, blockSize, blockSize);
            blocks[i].paint(g_);
        }
    }

    public void setProps(BlockProps props) {
        blockSize = props.blockSize;
        blockSpace = props.blockSpace;
        offsetX = props.offsetX;
        offsetY = props.offsetY;
    }

    public MemBlock getBlockAt(int x, int y) {
        if (x < offsetX || y < offsetY)
            return null;

        x -= offsetX;
        y -= offsetY;

        int z = blockSize+blockSpace;
        int row = y/z;
        int col = x/z;

        int right = col*z + blockSize;
        int bottom = row*z + blockSize;

        if (x > right || y > bottom)
            return null;
        if (x > z*(rows-1) || y > z*(columns+1))
            return null;
        if (col > columns)
            return null;

        int i = row*columns + col;
        if (i < blocks.length)
            return blocks[i];
        return null;
    }
}
