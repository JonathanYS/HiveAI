package com.example.hive.model;

public enum PieceColor {
    BLACK, WHITE, NONE, SIMULATION;

    public PieceColor getOpposite() {
        return this == WHITE ? BLACK : WHITE;
    }
}

