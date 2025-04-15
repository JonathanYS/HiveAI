package com.example.hive.model;

import javafx.scene.image.ImageView;
import org.pcollections.ConsPStack;
import org.pcollections.HashTreePMap;
import org.pcollections.PMap;
import org.pcollections.PStack;

import java.util.*;

import static com.example.hive.model.PieceColor.*;
import static com.example.hive.model.PieceImage.*;
import static com.example.hive.model.PieceImage.SPIDER_BLACK;

/**
 * The {@code GameModel} class acts as the central model of the Hive game.
 * It coordinates the mutable and immutable grids, movement and placement validation,
 * and the state of both players. This class provides an API for accessing and modifying
 * the game state, determining valid moves, and managing player turns.
 */
public class GameModel {
    private final Map<PieceColor, PlayerState> playerStates;
    private ImmutableGrid immutableGrid;
    private final MovementValidatorSimulation movementValidatorSimulation;
    // private PieceColor currentTurn; // White starts, for example.

    /**
     * Constructs a new GameModel and initializes game components such as the grid,
     * immutable grid, movement validators, and player states.
     */
    public GameModel() {
        immutableGrid = new ImmutableGrid();
        Map<PieceType, Integer> initialPieces = immutableGrid.getPiecesCount();
        movementValidatorSimulation = new MovementValidatorSimulation(immutableGrid);
        playerStates = new HashMap<>();
        playerStates.put(WHITE, new PlayerState(WHITE, initialPieces));
        playerStates.put(BLACK, new PlayerState(PieceColor.BLACK, initialPieces));
        // currentTurn = WHITE;
    }

    /**
     * @return the current player's turn.
     */
    public PieceColor getTurn() {
        return immutableGrid.getTurn();
    }

    /**
     * Checks if the specified color is allowed to move pieces (after placing their queen).
     *
     * @param color the color of the player.
     * @return true if the queen is placed, false otherwise.
     */
    public boolean canMovePieces(PieceColor color) {
        return playerStates.get(color).isQueenPlaced();
    }

    /**
     * Checks if the current player has any unplaced pieces left in their hand.
     *
     * @param currentTurn the player whose hand is checked.
     * @return true if any pieces are left to place, false otherwise.
     */
    public boolean isStillLeftPanelPiece(PieceColor currentTurn) {
        int counter = 0;
        // System.out.println(playerStates.get(currentTurn).getRemainingPiecesToPlace());
        for (Map.Entry<PieceType, Integer> entry : playerStates.get(currentTurn).getRemainingPiecesToPlace().entrySet())
            counter += entry.getValue();
        // System.out.println("COUNTER = " + counter);
        return counter > 0;
    }

    /**
     * @param color the player's color.
     * @return the number of moves the player has made.
     */
    public int getMoveCount(PieceColor color) {
        return playerStates.get(color).getMoveCount();
    }

    /**
     * Increments the move count for the specified player.
     *
     * @param color the player's color.
     */
    public void incrementMoveCount(PieceColor color) {
        playerStates.get(color).incrementMoveCount();
    }

    /**
     * Marks the queen as placed for the specified player.
     *
     * @param color the player's color.
     */
    public void setQueenPlaced(PieceColor color) {
        playerStates.get(color).setQueenPlaced();
    }

    /**
     * @param color the player's color.
     * @return number of pieces that the player has placed on the board.
     */
    public int getPlacedPiecesCount(PieceColor color) {
        return playerStates.get(color).getPlacedPiecesCount();
    }

    /**
     * @return number of pieces that the player has placed on the board.
     */
    public int getPlacedPiecesCount() {
        int blackPlacedPiecesAmount = playerStates.get(BLACK).getPlacedPiecesCount();
        int whitePlacedPiecesAmount = playerStates.get(WHITE).getPlacedPiecesCount();
        return blackPlacedPiecesAmount + whitePlacedPiecesAmount;
    }

    /**
     * @param color the player's color.
     * @return the coordinate of the queen piece.
     */
    public HexCoordinate getQueenCoordinate(PieceColor color) {
        return playerStates.get(color).getQueenCoordinate();
    }

    /**
     * Decrements the count of a piece type from the player's remaining pieces.
     *
     * @param pieceColor the color of the piece.
     * @param pieceType the type of piece being placed.
     */
    public void decrementPiece(PieceColor pieceColor, PieceType pieceType) {
        playerStates.get(pieceColor).decrementPiece(pieceType);
        // System.out.println("WHITE remaining pieces: " + playerStates.get(WHITE).getRemainingPiecesToPlace());
        // System.out.println("BLACK remaining pieces: " + playerStates.get(BLACK).getRemainingPiecesToPlace());
    }

    /**
     * Sets the coordinate where the queen is placed for the given player.
     *
     * @param queenCoordinate the coordinate to assign.
     * @param color the color of the player.
     */
    public void setQueenCoordinate(HexCoordinate queenCoordinate, PieceColor color) {
        playerStates.get(color).setQueenCoordinate(queenCoordinate);
    }

    /**
     * Constructs a new PieceWrapper.
     *
     * @param pieceType the type of the piece.
     * @param pieceColor the color of the piece.
     * @param imageView the visual representation of the piece.
     * @return a wrapped piece instance.
     */
    public PieceWrapper buildPieceWrapper(PieceType pieceType, PieceColor pieceColor, ImageView imageView) {
        return new PieceWrapper(new Piece(pieceType, pieceColor), imageView);
    }

    /**
     * Checks if the queen of the specified color is already placed.
     *
     * @param color the player's color.
     * @return true if queen is placed.
     */
    public boolean isQueenPlaced(PieceColor color) {
        return playerStates.get(color).isQueenPlaced();
    }

    /**
     * @param color the player's color.
     * @return a map of unplaced pieces and their remaining counts.
     */
    public Map<PieceType, Integer> getRemainingPiecesToPlace(PieceColor color) {
        return playerStates.get(color).getRemainingPiecesToPlace();
    }

    /**
     * @param color the player's color.
     * @param type the piece type.
     * @return remaining count of that piece type.
     */
    public int getRemainingPiecesToPlace(PieceColor color, PieceType type) {
        return playerStates.get(color).getRemainingPiecesToPlace(type);
    }

    /**
     * Moves a piece in the mutable and immutable grids.
     *
     * @param movementAction the move action.
     */
    public void movePiece(MovementAction movementAction) {
        // grid.movePiece(this, movementAction);
        immutableGrid.movePiece(this, movementAction);
        movementValidatorSimulation.setImmutableGrid(immutableGrid);

    }

    /**
     * Simulates a move on the immutable grid.
     *
     * @param move the move to perform.
     * @return updated immutable grid.
     */
    public PMap<HexCoordinate, PStack<PieceWrapper>> simulateMovePiece(PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, MovementAction move) {
        return immutableGrid.simulateMovePiece(gridCopy, move);
    }

    /**
     * Places a piece on both mutable and immutable grids.
     *
     * @param currentTurn the current player's color.
     * @param pieceWrapper the piece to place.
     * @param placementAction the placement action.
     * @return status code or effect of placement.
     */
    public Pair<PMap<HexCoordinate, PStack<PieceWrapper>>, Integer> placePiece(PieceColor currentTurn, PieceWrapper pieceWrapper, PlacementAction placementAction) {
        Pair<PMap<HexCoordinate, PStack<PieceWrapper>>, Integer> response = immutableGrid.placePiece(this, currentTurn, pieceWrapper, placementAction);
        immutableGrid.setGrid(response.getKey());
        return response;
    }

    public Pair<PMap<HexCoordinate, PStack<PieceWrapper>>, Integer> simulatePlacePiece(PieceColor currentTurn, PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, PieceWrapper pieceWrapper, PlacementAction placementAction) {
        return immutableGrid.simulatePlacePiece(this, currentTurn, gridCopy, pieceWrapper, placementAction);
    }

    /**
     * Returns a list of all legal movement actions for a player.
     *
     * @param currentTurn the player to check.
     * @return list of valid moves.
     */
    public List<MovementAction> getLegalMoves(PieceColor currentTurn) {
        return getLegalMoves(getImmutableGrid(), currentTurn);
    }

    /**
     * Gets valid moves from a custom grid.
     *
     * @param gridCopy the working grid.
     * @param from coordinate of the piece.
     * @return list of valid moves.
     */
    public List<MovementAction> getValidMoves(PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, HexCoordinate from) {
        return movementValidatorSimulation.getValidMoves(gridCopy, from);
    }

    /**
     * Gets all valid placement options for the current player.
     *
     * @param currentTurn the player's color.
     * @return list of legal placements.
     */
    public List<PlacementAction> getValidPlacements(PieceColor currentTurn) {
        return movementValidatorSimulation.getValidPlacements(this, getImmutableGrid(), currentTurn);
    }

    /**
     * Checks if any player has won the game based on the current grid.
     *
     * @return map showing winner status for each player.
     */
    public Map<PieceColor, Boolean> checkWin() {
        return immutableGrid.checkWin(this);
    }

    /**
     * Switches turn to the next player.
     */
    public void advanceTurn() {
        immutableGrid.advanceTurn();
    }

    /**
     * Finds the coordinate associated with an imageView.
     *
     * @param imageView the visual representation of a piece.
     * @return coordinate on the grid.
     */
    public HexCoordinate getHexCoordinateByPieceWrapperImage(ImageView imageView) {
        return immutableGrid.getHexCoordinateByPieceWrapperImage(imageView);
    }

    public int countTotalLegalMoves(PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, PieceColor currentTurn) {
        return movementValidatorSimulation.countTotalLegalMoves(this, gridCopy, currentTurn);
    }

    /**
     * Counts neighbors using an immutable grid.
     *
     * @param gridCopy the immutable grid.
     * @param coord center coordinate.
     * @return number of neighbors.
     */
    public int countNeighbours(PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, HexCoordinate coord) {
        return immutableGrid.countNeighbours(gridCopy, coord);
    }

    /**
     * Checks win condition using immutable grid.
     *
     * @param gridCopy immutable grid state.
     * @return win state per player.
     */
    public Map<PieceColor, Boolean> checkWin(PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, PieceColor color) {
        return immutableGrid.checkWin(this, gridCopy);
    }


    public List<MovementAction> getLegalMoves(PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, PieceColor currentTurn) {
        return movementValidatorSimulation.getLegalMoves(gridCopy, currentTurn);
    }

    /**
     * @return the current immutable grid.
     */
    public PMap<HexCoordinate, PStack<PieceWrapper>> getImmutableGrid() {
        return immutableGrid.getGrid();
    }
}
