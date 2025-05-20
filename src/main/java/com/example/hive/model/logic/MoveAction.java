package com.example.hive.model.logic;

/**
 * Represents a generic move action in the Hive game.
 * <p>
 * This abstract class is extended by specific move types, such as piece placement
 * and movement. It serves as a common interface for all types of actions
 * that can be taken by a player during their turn.
 * </p>
 */
public abstract class MoveAction {

    /**
     * Determines whether this action represents the placement of a new piece
     * onto the board (as opposed to moving an existing piece).
     *
     * @return {@code true} if this move is a placement; {@code false} if it is a movement
     */
    public abstract boolean isPlacement();


}

