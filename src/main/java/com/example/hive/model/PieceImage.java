package com.example.hive.model;

import javafx.scene.image.Image;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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


    private static final String BASE_PATH = "file:resources/images/";
    private final Image image;
    private final PieceColor color;

    PieceImage(String fileName, PieceColor color) {
        image = new Image(BASE_PATH + fileName);
        this.color = color;
    }

    public Image getImage() {
        return image;
    }

    public static List<PieceImage> getPiecesByColor(PieceColor color) {
        return Stream.of(values())
                .filter(piece -> piece.color == color)
                .collect(Collectors.toList());
    }
}
