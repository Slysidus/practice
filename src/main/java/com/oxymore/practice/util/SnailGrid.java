package com.oxymore.practice.util;

public final class SnailGrid {
    private final int increment;

    private int curr, pos;
    private int x, z;

    public SnailGrid(int increment, int x, int z) {
        this.increment = increment;
        this.curr = 0;
        this.pos = 0;
        this.x = x;
        this.z = z;
    }

    private int getPL() {
        return 3 + ((curr - 1) * 2);
    }

    private int getTotalOnCurr() {
        int var = getPL();
        return var * 2 + (var - 2) * 2;
    }

    private int getRestOnCurr() {
        if (curr == 0)
            return 0;
        return getTotalOnCurr() - pos;
    }

    private boolean hasNext() {
        return getRestOnCurr() > 0;
    }

    public Vector2D next() {
        if (hasNext()) {
            int pl = getPL();
            if (pos < pl) {
                x += increment;
            } else if (pos <= (pl + (pl - 2))) {
                z -= increment;
            } else if (pos < 2 * pl + (pl - 2)) {
                x -= increment;
            } else if (pos < 2 * pl + 2 * (pl - 2)) {
                z += increment;
            }
            pos++;
        } else {
            curr++;
            x -= increment;
            z += increment * (curr == 1 ? 1 : 2);
            pos = 1;
        }
        return new Vector2D(x, z);
    }
}
