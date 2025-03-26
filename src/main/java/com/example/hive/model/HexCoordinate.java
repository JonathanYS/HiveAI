package com.example.hive.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

/**
 * The {@code HexCoordinate} class represents a coordinate in a hexagonal grid.
 * It uses a coordinate system with two axes: {@code q} (the horizontal axis) and {@code r} (the vertical axis).
 * This class provides methods to get neighboring coordinates, add another coordinate, and check equality.
 */
public class HexCoordinate {

    public static final HexCoordinate[] DIRECTIONS = {
            new HexCoordinate(1, -1),  // Top-right
            new HexCoordinate(1, 0),   // Right
            new HexCoordinate(0, 1),   // Bottom-right
            new HexCoordinate(-1, 1),  // Bottom-left
            new HexCoordinate(-1, 0),  // Left
            new HexCoordinate(0, -1)   // Top-left
    };

    private int q;
    private int r;

    /**
     * Creates a new {@code HexCoordinate} with the specified q and r values.
     *
     * @param q The q coordinate (horizontal axis).
     * @param r The r coordinate (vertical axis).
     */
    public HexCoordinate(int q, int r) {
        this.q = q;
        this.r = r;
    }

    /**
     * Returns the q coordinate (horizontal axis).
     *
     * @return The q coordinate.
     */
    public int getQ() {
        return q;
    }

    /**
     * Returns the r coordinate (vertical axis).
     *
     * @return The r coordinate.
     */
    public int getR() {
        return r;
    }

    /**
     * Adds the specified direction to the current coordinate.
     *
     * @param direction The direction to add to the current coordinate.
     * @return A new {@code HexCoordinate} that is the sum of this coordinate and the direction.
     */
    public HexCoordinate add(HexCoordinate direction) {
        return new HexCoordinate(this.q + direction.q, this.r + direction.r);
    }

    /**
     * Returns the 6 neighboring coordinates in a hexagonal grid.
     * These are the coordinates that are directly adjacent to this coordinate.
     *
     * @return An array of 6 neighboring {@code HexCoordinate} objects.
     */
    public ArrayList<HexCoordinate> getNeighbors() {
        return new ArrayList<HexCoordinate>(Arrays.asList(
                new HexCoordinate(q + 1, r -1),
                new HexCoordinate(q + 1, r),
                new HexCoordinate(q, r + 1),
                new HexCoordinate(q - 1, r + 1),
                new HexCoordinate(q - 1, r),
                new HexCoordinate(q, r - 1)
        ));
    }

    /**
     * Compares this {@code HexCoordinate} to another object.
     *
     * @param obj The object to compare to.
     * @return {@code true} if the object is a {@code HexCoordinate} and its q and r values are equal to this coordinate's q and r values, otherwise {@code false}.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        HexCoordinate o = (HexCoordinate) obj;
        return q == o.getQ() && r == o.getR();
    }

    /**
     * Returns a hash code value for this {@code HexCoordinate}.
     *
     * @return A hash code value for this coordinate.
     */
    @Override
    public int hashCode() {
        return Objects.hash(getQ(), getR());
    }

    /**
     * Returns a string representation of this {@code HexCoordinate} in the format "(q = {q}, r = {r})".
     *
     * @return A string representation of this coordinate.
     */
    @Override
    public String toString() {
        return String.format("(q = %d , r = %d)", q, r);
    }
}
