package com.example.hive.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds state specific to a player, such as remaining pieces,
 * whether the Queen Bee has been placed, and the move count.
 */
public class PlayerState {
    private final PieceColor color;
    private final Map<PieceType, Integer> remainingPieces;
    private boolean queenPlaced;
    private int moveCount;

    private final int TOTAL_PIECES_AMOUNT = 11;

    private HexCoordinate queenCoordinate;

    public PlayerState(PieceColor color, Map<PieceType, Integer> initialPieces) {
        this.color = color;
        remainingPieces = new HashMap<>();
        for (Map.Entry<PieceType, Integer> entry : initialPieces.entrySet())
            remainingPieces.put(entry.getKey(), entry.getValue());
        System.out.println(remainingPieces);
        // this.remainingPieces.putAll(initialPieces);
        this.queenPlaced = false;
        this.moveCount = 0;
    }

    public PieceColor getColor() {
        return color;
    }

    public int getPlacedPiecesCount() {
        return TOTAL_PIECES_AMOUNT - remainingPieces.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int getRemainingPiecesToPlace(PieceType type) {
        return remainingPieces.getOrDefault(type, 0);
    }

    public void decrementPiece(PieceType type) {
        if (type != null)
            remainingPieces.put(type, getRemainingPiecesToPlace(type) - 1);
    }

    public boolean isQueenPlaced() {
        return queenPlaced;
    }

    public void setQueenPlaced() {
        queenPlaced = true;
    }

    public int getMoveCount() {
        return moveCount;
    }

    public void incrementMoveCount() {
        moveCount++;
    }

    public Map<PieceType, Integer> getRemainingPiecesToPlace() {
        return new HashMap<>(remainingPieces);
    }

    public HexCoordinate getQueenCoordinate() {
        return queenCoordinate;
    }

    public void setQueenCoordinate(HexCoordinate queenCoordinate) {
        this.queenCoordinate = queenCoordinate;
    }
}
