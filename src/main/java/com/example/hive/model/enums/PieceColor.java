package com.example.hive.model.enums;

/**
 * Enum representing the color of a game piece in Hive.
 * The colors available are BLACK, WHITE, and NONE.
 */
public enum PieceColor {
    BLACK, WHITE, NONE;

    /**
     * Returns the opposite color of the current color.
     * If the color is WHITE, the opposite is BLACK, and vice versa.
     * If the color is NONE, the method will return NONE.
     *
     * @return the opposite color.
     */
    public PieceColor getOpposite() {
        return this == WHITE ? BLACK : WHITE;
    }
}

