package com.example.hive.model;

import javafx.scene.image.ImageView;

import static com.example.hive.model.PieceColor.*;
import static com.example.hive.model.PieceImage.*;
import static com.example.hive.model.PieceImage.SPIDER_BLACK;

/**
 * A wrapper that associates a domain Piece with its JavaFX ImageView.
 */
public class PieceWrapper {
    private final Piece piece;
    private ImageView imageView;

    public PieceWrapper(Piece piece) {
        this.piece = piece;
        imageView = new ImageView(this.getPieceImageFromPieceWrapper().getImage());
    }

    public PieceWrapper(Piece piece, ImageView imageView) {
        this.piece = piece;
        this.imageView = imageView;
    }

    public Piece getPiece() {
        return piece;
    }

    public ImageView getImageView() {
        return imageView;
    }

    public void setImageView (ImageView imageView) {
        this.imageView = imageView;
    }

    public PieceImage getPieceImageFromPieceWrapper() {
        PieceColor pieceColor = this.getPiece().getColor();
        PieceType pieceType = this.getPiece().getType();
        System.out.println(pieceColor);
        System.out.println(pieceType);
        if (pieceColor == WHITE) {
            switch (pieceType) {
                case QUEEN_BEE -> {
                    return QUEEN_BEE_WHITE;
                }
                case ANT -> {
                    return ANT_WHITE;
                }
                case BEETLE -> {
                    return BEETLE_WHITE;
                }
                case GRASSHOPPER -> {
                    return GRASSHOPPER_WHITE;
                }
                case SPIDER -> {
                    return SPIDER_WHITE;
                }
            }
        }
        else if (pieceColor == BLACK) {
            switch (pieceType) {
                case QUEEN_BEE -> {
                    return QUEEN_BEE_BLACK;
                }
                case ANT -> {
                    return ANT_BLACK;
                }
                case BEETLE -> {
                    return BEETLE_BLACK;
                }
                case GRASSHOPPER -> {
                    return GRASSHOPPER_BLACK;
                }
                case SPIDER -> {
                    return SPIDER_BLACK;
                }
            }
        }
        else if (pieceColor == NONE) {
            return BLANK_TILE;
        }
        return null;
    }
}
