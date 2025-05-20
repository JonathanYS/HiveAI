package com.example.hive.model.grid;

import com.example.hive.model.enums.PieceColor;
import com.example.hive.model.enums.PieceType;

import java.util.Objects;

/**
 * Domain model for a game piece, representing a specific type and color of a piece in the game.
 * The Piece class is immutable and uses a record to store its data.
 */
public record Piece(PieceType type, PieceColor color) {

    /**
     * Returns a string representation of this Piece, combining its color and type.
     * For example, "Black Beetle" or "White Queen Bee".
     *
     * @return a string representation of the Piece.
     */
    @Override
    public String toString() {
        return color + " " + type;
    }

    /**
     * Compares this Piece to another object for equality.
     * Two pieces are considered equal if they have the same type and color.
     *
     * @param object the object to compare to.
     * @return true if the objects are equal, false otherwise.
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        Piece piece = (Piece) object;
        return type() == piece.type() && color() == piece.color();
    }

    /**
     * Returns the hash code for this Piece, calculated from its type and color.
     *
     * @return the hash code of this Piece.
     */
    @Override
    public int hashCode() {
        return Objects.hash(type(), color());
    }
}
