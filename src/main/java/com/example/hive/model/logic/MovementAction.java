package com.example.hive.model.logic;

import com.example.hive.model.grid.HexCoordinate;

import java.util.Objects;

/**
 * Represents a movement action in the Hive game, where a piece moves
 * from one coordinate to another on the board.
 */
public class MovementAction extends MoveAction {
    private final HexCoordinate from;
    private final HexCoordinate to;

    /**
     * Constructs a new MovementAction with the specified starting and ending coordinates.
     *
     * @param from the starting coordinate of the piece.
     * @param to the destination coordinate of the piece.
     */
    public MovementAction(HexCoordinate from, HexCoordinate to) {
        this.from = from;
        this.to = to;
    }

    /**
     * Gets the starting coordinate of the movement.
     *
     * @return the starting coordinate.
     */
    public HexCoordinate getFrom() {
        return from;
    }

    /**
     * Gets the destination coordinate of the movement.
     *
     * @return the destination coordinate.
     */
    public HexCoordinate getTo() {
        return to;
    }

    /**
     * Compares this MovementAction to another object for equality.
     * Two MovementActions are considered equal if they have the same starting and ending coordinates.
     *
     * @param object the object to compare to.
     * @return true if the objects are equal, false otherwise.
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        MovementAction that = (MovementAction) object;
        return Objects.equals(getFrom(), that.getFrom()) && Objects.equals(getTo(), that.getTo());
    }

    /**
     * Returns the hash code for this MovementAction, based on its starting and ending coordinates.
     *
     * @return the hash code of this MovementAction.
     */
    @Override
    public int hashCode() {
        return Objects.hash(getFrom(), getTo());
    }

    /**
     * Determines if this action is a placement action. In the case of MovementAction,
     * this method always returns false because the action is a movement, not a placement.
     *
     * @return false, as this is not a placement action.
     */
    @Override
    public boolean isPlacement() {
        return false;
    }

    /**
     * Returns a string representation of this MovementAction, including the starting and
     * destination coordinates.
     *
     * @return a string representation of the movement action.
     */
    @Override
    public String toString() {
        return "MovementAction{from=" + from + ", to=" + to + "}";
    }
}
