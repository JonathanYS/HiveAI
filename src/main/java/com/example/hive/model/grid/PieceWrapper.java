package com.example.hive.model.grid;

import com.example.hive.model.enums.PieceColor;
import com.example.hive.model.enums.PieceImage;
import com.example.hive.model.enums.PieceType;
import javafx.scene.image.ImageView;

import java.util.Objects;

import static com.example.hive.model.enums.PieceColor.*;
import static com.example.hive.model.enums.PieceImage.*;
import static com.example.hive.model.enums.PieceImage.SPIDER_BLACK;

/**
 * A wrapper class that associates a domain Piece with its corresponding JavaFX ImageView.
 * This allows for combining the piece's data (type and color) with its visual representation.
 */
public class PieceWrapper {
    private final Piece piece;
    private final ImageView imageView;

    /**
     * Constructs a PieceWrapper with the specified Piece.
     * The ImageView is initialized based on the piece's type and color.
     *
     * @param piece the Piece to wrap.
     */
    public PieceWrapper(Piece piece) {
        this.piece = piece;
        imageView = new ImageView(this.getPieceImageFromPieceWrapper().getImage());
    }

    /**
     * Constructs a PieceWrapper with the specified Piece and ImageView.
     * This allows for custom ImageViews to be used if necessary.
     *
     * @param piece the Piece to wrap.
     * @param imageView the custom ImageView associated with the piece.
     */
    public PieceWrapper(Piece piece, ImageView imageView) {
        this.piece = piece;
        this.imageView = imageView;
    }

    /**
     * Gets the Piece associated with this PieceWrapper.
     *
     * @return the Piece wrapped by this wrapper.
     */
    public Piece getPiece() {
        return piece;
    }

    /**
     * Gets the ImageView associated with this PieceWrapper.
     *
     * @return the ImageView corresponding to the piece's image.
     */
    public ImageView getImageView() {
        return imageView;
    }

    /**
     * Determines the appropriate PieceImage based on the piece's color and type.
     *
     * @return the PieceImage that corresponds to the color and type of the piece.
     */
    public PieceImage getPieceImageFromPieceWrapper() {
        PieceColor pieceColor = this.getPiece().color();
        PieceType pieceType = this.getPiece().type();
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

    /**
     * Compares this PieceWrapper to another object for equality.
     * Two PieceWrappers are considered equal if they wrap the same Piece.
     *
     * @param object the object to compare to.
     * @return true if the objects are equal, false otherwise.
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        PieceWrapper that = (PieceWrapper) object;
        return Objects.equals(getPiece(), that.getPiece());
    }

    /**
     * Returns the hash code for this PieceWrapper, calculated from the piece and its ImageView.
     *
     * @return the hash code of this PieceWrapper.
     */
    @Override
    public int hashCode() {
        return Objects.hash(getPiece(), getImageView());
    }
}
