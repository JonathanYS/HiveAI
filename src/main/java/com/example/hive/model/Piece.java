package com.example.hive.model;

/**
 * Domain model for a game piece.
 */
public class Piece {
    private final PieceType type;
    private final PieceColor color;

    public Piece(PieceType type, PieceColor color) {
        this.type = type;
        this.color = color;
    }

    public PieceType getType() {
        return type;
    }

    public PieceColor getColor() {
        return color;
    }

    @Override
    public String toString() {
        return color + " " + type;
    }
}
