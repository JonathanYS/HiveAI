package com.example.hive.model;

import javafx.scene.image.ImageView;

import java.util.*;

import static com.example.hive.model.PieceColor.*;
import static com.example.hive.model.PieceImage.*;
import static com.example.hive.model.PieceImage.SPIDER_BLACK;

/**
 * Combines the grid, movement validation, and player state into one model.
 */
public class GameModel {
    private final Grid grid;
    private final MovementValidator moveValidator;
    private final Map<PieceColor, PlayerState> playerStates;
    // private PieceColor currentTurn; // White starts, for example.

    public GameModel() {
        grid = new Grid();
        Map<PieceType, Integer> initialPieces = grid.getPiecesCount();
        moveValidator = new MovementValidator(grid);
        playerStates = new HashMap<>();
        playerStates.put(WHITE, new PlayerState(WHITE, initialPieces));
        playerStates.put(BLACK, new PlayerState(PieceColor.BLACK, initialPieces));
        // currentTurn = WHITE;
    }

    public PieceColor getTurn() {
        return grid.getTurn();
    }

    public boolean canMovePieces(PieceColor color) {
        return playerStates.get(color).isQueenPlaced();
    }

    public boolean isStillLeftPanelPiece(PieceColor currentTurn) {
        int counter = 0;
        System.out.println(playerStates.get(currentTurn).getRemainingPiecesToPlace());
        for (Map.Entry<PieceType, Integer> entry : playerStates.get(currentTurn).getRemainingPiecesToPlace().entrySet())
            counter += entry.getValue();
        System.out.println("COUNTER = " + counter);
        return counter > 0;
    }

    public int getMoveCount(PieceColor color) {
        return playerStates.get(color).getMoveCount();
    }

    public void incrementMoveCount(PieceColor color) {
        playerStates.get(color).incrementMoveCount();
    }

    public void setQueenPlaced(PieceColor color) {
        playerStates.get(color).setQueenPlaced();
    }

    public int getPlacedPiecesCount(PieceColor color) {
        return playerStates.get(color).getPlacedPiecesCount();
    }

    public int getPlacedPiecesCount() {
        int blackPlacedPiecesAmount = playerStates.get(BLACK).getPlacedPiecesCount();
        int whitePlacedPiecesAmount = playerStates.get(WHITE).getPlacedPiecesCount();
        return blackPlacedPiecesAmount + whitePlacedPiecesAmount;
    }

    public HexCoordinate getQueenCoordinate(PieceColor color) {
        return playerStates.get(color).getQueenCoordinate();
    }

    public void decrementPiece(PieceColor pieceColor, PieceType pieceType) {
        playerStates.get(pieceColor).decrementPiece(pieceType);
        System.out.println("WHITE remaining pieces: " + playerStates.get(WHITE).getRemainingPiecesToPlace());
        System.out.println("BLACK remaining pieces: " + playerStates.get(BLACK).getRemainingPiecesToPlace());
    }

    public void setQueenCoordinate(HexCoordinate queenCoordinate, PieceColor color) {
        playerStates.get(color).setQueenCoordinate(queenCoordinate);
    }

    public Map<HexCoordinate, Deque<PieceWrapper>> getGrid() {
        return grid.getGrid();
    }

    public Map<HexCoordinate, Deque<PieceWrapper>> getGrid(Map<HexCoordinate, Deque<PieceWrapper>> gridCopy) {
        return grid.getGrid(gridCopy);
    }

    public int countNeighbours(Map<HexCoordinate, Deque<PieceWrapper>> gridCopy, HexCoordinate coord) {
        return grid.countNeighbours(gridCopy, coord);
    }

    public PieceWrapper buildPieceWrapper(PieceType pieceType, PieceColor pieceColor, ImageView imageView) {
        return new PieceWrapper(new Piece(pieceType, pieceColor), imageView);
    }

    public boolean isQueenPlaced(PieceColor color) {
        return playerStates.get(color).isQueenPlaced();
    }

    public Map<PieceType, Integer> getRemainingPiecesToPlace(PieceColor color) {
        return playerStates.get(color).getRemainingPiecesToPlace();
    }

    public int getRemainingPiecesToPlace(PieceColor color, PieceType type) {
        return playerStates.get(color).getRemainingPiecesToPlace(type);
    }

    public void movePiece(MovementAction movementAction, boolean isSimulated) {
        grid.movePiece(this, movementAction, isSimulated);
    }

    public Map<HexCoordinate, Deque<PieceWrapper>> movePiece(Map<HexCoordinate, Deque<PieceWrapper>> gridCopy, MovementAction move, boolean isSimulated) {
        return grid.movePiece(this, gridCopy, move, isSimulated);
    }

    public int placePiece(PieceColor currentTurn, PieceWrapper pieceWrapper, PlacementAction placementAction, boolean isSimulated) {
        return grid.placePiece(this, currentTurn, pieceWrapper, placementAction, isSimulated);
    }

    public Pair<Map<HexCoordinate, Deque<PieceWrapper>>, Integer> placePiece(Map<HexCoordinate, Deque<PieceWrapper>> gridCopy,
                                                                             PieceColor currentTurn, PieceWrapper pieceWrapper, PlacementAction placementAction, boolean isSimulated) {
        return grid.placePiece(this, gridCopy, currentTurn, pieceWrapper, placementAction, isSimulated);
    }

    public List<MovementAction> getLegalMoves(PieceColor currentTurn) {
        return moveValidator.getLegalMoves(grid.getGrid(), currentTurn);
    }

    public List<MovementAction> getLegalMoves(Map<HexCoordinate, Deque<PieceWrapper>> gridCopy, PieceColor currentTurn) {
        return moveValidator.getLegalMoves(gridCopy, currentTurn);
    }

    public List<MovementAction> getValidMoves(HexCoordinate from) {
        return moveValidator.getValidMoves(from);
    }

    public List<MovementAction> getValidMoves(Map<HexCoordinate, Deque<PieceWrapper>> gridCopy, HexCoordinate from) {
        return moveValidator.getValidMoves(gridCopy, from);
    }

    public List<PlacementAction> getValidPlacements(PieceColor currentTurn) {
        return moveValidator.getValidPlacements(this, currentTurn);
    }

    public List<PlacementAction> getValidPlacements(Map<HexCoordinate, Deque<PieceWrapper>> gridCopy, PieceColor currentTurn) {
        return moveValidator.getValidPlacements(this, gridCopy, currentTurn);
    }

    public Map<PieceColor, Boolean> checkWin() {
        return grid.checkWin(this);
    }

    public Map<PieceColor, Boolean> checkWin(Map<HexCoordinate, Deque<PieceWrapper>> gridCopy) {
        return grid.checkWin(this, gridCopy);
    }

    public int countTotalLegalMoves(Map<HexCoordinate, Deque<PieceWrapper>> gridCopy, PieceColor currentTurn) {
        return moveValidator.countTotalLegalMoves(this, gridCopy, currentTurn);
    }

    public void advanceTurn() {
        grid.advanceTurn();
    }

    public HexCoordinate getHexCoordinateByPieceWrapperImage(ImageView imageView) {
        return grid.getHexCoordinateByPieceWrapperImage(imageView);
    }
}
