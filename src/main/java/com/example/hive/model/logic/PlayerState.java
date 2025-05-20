package com.example.hive.model.logic;

import com.example.hive.model.enums.PieceColor;
import com.example.hive.model.enums.PieceType;
import com.example.hive.model.grid.HexCoordinate;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds state specific to a player, such as remaining pieces,
 * whether the Queen Bee has been placed, and the move count.
 * This class tracks the player's progress and game state.
 */
public class PlayerState {
    private final PieceColor color;
    private final Map<PieceType, Integer> remainingPieces;
    private boolean queenPlaced;
    private int moveCount;

    private static final int TOTAL_PIECES_AMOUNT = 11;

    private HexCoordinate queenCoordinate;

    /**
     * Constructs a PlayerState for a given color and initial pieces.
     * Initializes the remaining pieces, the move count, and the queenPlaced for the player.
     *
     * @param color the color of the player (BLACK, WHITE)
     * @param initialPieces a map of initial pieces for the player by type
     */
    public PlayerState(PieceColor color, Map<PieceType, Integer> initialPieces) {
        this.color = color;
        remainingPieces = new HashMap<>();
        remainingPieces.putAll(initialPieces);
        queenPlaced = false;
        moveCount = 0;
    }

    /**
     * Gets the color of the player (BLACK, WHITE).
     *
     * @return the player's color.
     */
    public PieceColor getColor() {
        return color;
    }

    /**
     * Gets the count of pieces that have been placed on the board.
     * This is calculated as the total number of pieces minus the remaining pieces.
     *
     * @return the number of pieces placed by the player.
     */
    public int getPlacedPiecesCount() {
        return TOTAL_PIECES_AMOUNT - remainingPieces.values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * Gets the number of remaining pieces of a specified type that the player can place.
     *
     * @param type the type of piece to check.
     * @return the number of remaining pieces of the specified type.
     */
    public int getRemainingPiecesToPlace(PieceType type) {
        return remainingPieces.getOrDefault(type, 0);
    }

    /**
     * Decrements the number of remaining pieces of a specified type by 1.
     * If the count reaches 0, the piece type is removed from the map of remaining pieces.
     *
     * @param type the type of piece to decrement.
     */
    public void decrementPiece(PieceType type) {
        if (type != null) {
            int currentAmount = getRemainingPiecesToPlace(type);
            remainingPieces.put(type, currentAmount - 1);
            if (currentAmount == 0) {
                remainingPieces.remove(type);
            }
        }
    }

    /**
     * Checks if the Queen Bee has been placed by the player.
     *
     * @return true if the Queen Bee has been placed, false otherwise.
     */
    public boolean isQueenPlaced() {
        return queenPlaced;
    }

    /**
     * Marks the Queen Bee as placed.
     */
    public void setQueenPlaced() {
        queenPlaced = true;
    }

    /**
     * Gets the number of moves made by the player.
     *
     * @return the player's move count.
     */
    public int getMoveCount() {
        return moveCount;
    }

    /**
     * Increments the player's move count by 1.
     */
    public void incrementMoveCount() {
        moveCount++;
    }

    /**
     * Gets a map of the remaining pieces that the player can place, grouped by piece type.
     *
     * @return a map containing the number of remaining pieces for each piece type.
     */
    public Map<PieceType, Integer> getRemainingPiecesToPlace() {
        return new HashMap<>(remainingPieces);
    }

    /**
     * Gets the coordinate of the Queen Bee piece if it has been placed.
     *
     * @return the coordinate of the Queen Bee, or null if it hasn't been placed.
     */
    public HexCoordinate getQueenCoordinate() {
        return queenCoordinate;
    }

    /**
     * Sets the coordinate of the Queen Bee piece when it is placed on the board.
     *
     * @param queenCoordinate the coordinate where the Queen Bee is placed.
     */
    public void setQueenCoordinate(HexCoordinate queenCoordinate) {
        this.queenCoordinate = queenCoordinate;
    }
}
