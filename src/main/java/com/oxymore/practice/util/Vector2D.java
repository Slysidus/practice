package com.oxymore.practice.util;

import lombok.Data;

@Data
public final class Vector2D implements Comparable<Vector2D> {
    private final int x;
    private final int z;

    @Override
    public int compareTo(Vector2D other) {
        final int p1 = (int) Math.sqrt(x * x + z * z);
        final int p2 = (int) Math.sqrt(other.x * other.x + other.z * other.z);
        return Integer.compare(p1, p2);
    }
}
