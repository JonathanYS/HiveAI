package com.example.hive.model.logic;

import com.example.hive.model.grid.HexCoordinate;

import java.util.Objects;

/**
 * Represents an action where a piece is placed at a specific location on the board.
 * Extends the MoveAction class to represent a move action that involves placing a piece.
 */
public class PlacementAction extends MoveAction {
    private final HexCoordinate destination;

    /**
     * Constructs a PlacementAction with the specified destination.
     * This represents the action of placing a piece at the given destination on the board.
     *
     * @param destination the coordinate on the board where the piece is placed.
     */
    public PlacementAction(HexCoordinate destination) {
        this.destination = destination;
    }

    /**
     * Gets the destination coordinate of this PlacementAction.
     *
     * @return the destination coordinate where the piece is placed.
     */
    public HexCoordinate getDestination() {
        return destination;
    }

    /**
     * Compares this PlacementAction to another object for equality.
     * Two PlacementActions are considered equal if they have the same destination.
     *
     * @param object the object to compare to.
     * @return true if the objects are equal, false otherwise.
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        PlacementAction that = (PlacementAction) object;
        return Objects.equals(getDestination(), that.getDestination());
    }

    /**
     * Returns the hash code for this PlacementAction, calculated from the destination.
     *
     * @return the hash code of this PlacementAction.
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(getDestination());
    }

    /**
     * Determines whether this action is a placement action.
     *
     * @return true, as this is a placement action.
     */
    @Override
    public boolean isPlacement() {
        return true;
    }

    /**
     * Returns a string representation of this PlacementAction, showing the destination.
     *
     * @return a string representation of the PlacementAction.
     */

    @Override
    public String toString() {
        return "PlacementAction{destination=" + destination + "}";
    }
}
