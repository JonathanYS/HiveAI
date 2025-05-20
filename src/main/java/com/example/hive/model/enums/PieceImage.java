package com.example.hive.model.enums;

import javafx.scene.image.Image;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Enum representing the images associated with the different types of game pieces in Hive.
 * Each enum constant corresponds to a specific image file for a game piece and its associated color.
 */
public enum PieceImage {
    QUEEN_BEE_BLACK("black_bee.gif", PieceColor.BLACK),
    ANT_BLACK("black_ant.gif", PieceColor.BLACK),
    SPIDER_BLACK("black_spider.gif", PieceColor.BLACK),
    GRASSHOPPER_BLACK("black_hopper.gif", PieceColor.BLACK),
    BEETLE_BLACK("black_beetle.gif", PieceColor.BLACK),
    QUEEN_BEE_WHITE("white_bee.gif", PieceColor.WHITE),
    ANT_WHITE("white_ant.gif", PieceColor.WHITE),
    SPIDER_WHITE("white_spider.gif", PieceColor.WHITE),
    GRASSHOPPER_WHITE("white_hopper.gif", PieceColor.WHITE),
    BEETLE_WHITE("white_beetle.gif", PieceColor.WHITE),
    BLANK_TILE("blank_tile.gif", PieceColor.NONE);


    private final Image image;
    private final PieceColor color;

    /**
     * Constructs a PieceImage enum constant with the specified image file name and color.
     * The image is loaded from the "resources" directory.
     *
     * @param fileName the name of the image file (should be located in the /images directory).
     * @param color the color of the piece associated with this image.
     */
    PieceImage(String fileName, PieceColor color) {
        image = new Image(Objects.requireNonNull(getClass().getResource("/images/" + fileName)).toExternalForm());
        this.color = color;
    }

    /**
     * Gets the image associated with this PieceImage.
     *
     * @return the image corresponding to the piece.
     */
    public Image getImage() {
        return image;
    }

    /**
     * Returns a list of PieceImage constants that correspond to the specified color.
     *
     * @param color the color of the pieces to filter by.
     * @return a list of PieceImage constants that match the specified color.
     */
    public static List<PieceImage> getPiecesByColor(PieceColor color) {
        return Stream.of(values())
                .filter(piece -> piece.color == color)
                .collect(Collectors.toList());
    }
}
