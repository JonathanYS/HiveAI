package com.example.hive.model.grid;

import com.example.hive.model.enums.EndGameStatus;
import com.example.hive.model.enums.PieceColor;
import com.example.hive.model.enums.PieceType;
import com.example.hive.model.logic.*;
import com.example.hive.model.utils.Pair;
import javafx.scene.image.ImageView;
import org.pcollections.PMap;
import org.pcollections.PStack;

import java.util.*;

import static com.example.hive.model.enums.PieceColor.*;

/**
 * The {@code GameModel} class acts as the central model of the Hive game.
 * It coordinates the mutable and immutable grids, movement and placement validation,
 * and the state of both players. This class provides an API for accessing and modifying
 * the game state, determining valid moves, and managing player turns.
 */
public class GameModel {
    private final Map<PieceColor, PlayerState> playerStates;
    private ImmutableGrid immutableGrid;
    private final MovementValidator movementValidator;

    /**
     * Constructs a new GameModel and initializes game components such as the
     * immutable grid, movement validator, and player states.
     */
    public GameModel() {
        immutableGrid = new ImmutableGrid();
        Map<PieceType, Integer> initialPieces = immutableGrid.getPiecesCount();
        movementValidator = new MovementValidator(immutableGrid);
        playerStates = new HashMap<>();
        playerStates.put(WHITE, new PlayerState(WHITE, initialPieces));
        playerStates.put(BLACK, new PlayerState(PieceColor.BLACK, initialPieces));
    }

    /**
     * Gets and returns the current player's turn as followed by the ImmutableGrid instance of the game grid.
     *
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
        for (Map.Entry<PieceType, Integer> entry : playerStates.get(currentTurn).getRemainingPiecesToPlace().entrySet())
            counter += entry.getValue();
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
     * @return total number of pieces that have been placed on the board by both players.
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
     * Moves a piece in the grid and sets the ImmutableGrid of the movementValidator object to the updated ImmutableGrid object.
     *
     * @param movementAction the move action.
     */
    public void movePiece(MovementAction movementAction) {
        immutableGrid.movePiece(this, movementAction);
        movementValidator.setImmutableGrid(immutableGrid);
    }

    /**
     * Simulates a move on the grid.
     *
     * @param gridCopy the current grid state.
     * @param move the move to perform.
     * @return updated immutable grid.
     */
    public PMap<HexCoordinate, PStack<PieceWrapper>> simulateMovePiece(PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, MovementAction move) {
        return immutableGrid.simulateMovePiece(gridCopy, move);
    }

    /**
     * Places a piece on grid.
     *
     * @param currentTurn the current player's color.
     * @param pieceWrapper the piece to place.
     * @param placementAction the placement action.
     * @return a Pair object containing the updated grid and an Integer response indicating if the player must place its Queen Bee by its next turn.
     */
    public Pair<PMap<HexCoordinate, PStack<PieceWrapper>>, Integer> placePiece(PieceColor currentTurn, PieceWrapper pieceWrapper, PlacementAction placementAction) {
        Pair<PMap<HexCoordinate, PStack<PieceWrapper>>, Integer> response = immutableGrid.placePiece(this, currentTurn, pieceWrapper, placementAction);
        immutableGrid.setGrid(response.getKey());
        return response;
    }

    /**
     * Simulates the placement of a piece on the grid.
     *
     * @param currentTurn the current player's color.
     * @param gridCopy the current state of the grid.
     * @param pieceWrapper the piece to place.
     * @param placementAction the placement action.
     * @return a Pair object containing the updated grid and an Integer response indicating if the player must place its Queen Bee by its next turn.
     */
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
        return getLegalMoves(getGrid(), currentTurn);
    }

    /**
     * Gets valid moves from a custom grid.
     *
     * @param gridCopy the working grid.
     * @param from coordinate of the piece.
     * @return list of valid moves.
     */
    public List<MovementAction> getValidMoves(PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, HexCoordinate from) {
        return movementValidator.getValidMoves(gridCopy, from);
    }

    /**
     * Gets all valid placement options for the current player.
     *
     * @param currentTurn the player's color.
     * @return list of legal placements.
     */
    public List<PlacementAction> getValidPlacements(PieceColor currentTurn) {
        return movementValidator.getValidPlacements(this, getGrid(), currentTurn);
    }

    public List<PlacementAction> getValidPlacements(PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, PieceColor currentTurn) {
        return movementValidator.getValidPlacements(this, gridCopy, currentTurn);
    }

    /**
     * Checks if any player has won the game based on the current grid.
     *
     * @param isSimulated whether this check is for a simulated grid.
     * @return map showing winning status for each player.
     */
    public Pair<Map<PieceColor, Boolean>, EndGameStatus> checkWin(boolean isSimulated) {
        return immutableGrid.checkWin(this, isSimulated);
    }

    /**
     * Determines if the source location of a move is pinning the opponent's Queen's neighbors by keeping them from moving.
     * Pinning is when an adjacent piece is unable to move due to the presence
     * of surrounding pieces.
     *
     * @param gridCopy the current immutable grid state.
     * @param move the proposed move to evaluate.
     * @param queenNeighbors the set of coordinates adjacent to the opponent's Queen.
     * @return true if the source location of the move is pinning the opponent's Queen's neighbors, false otherwise.
     */
    public boolean isPinning(PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, MoveAction move, Set<HexCoordinate> queenNeighbors) {
        return movementValidator.isPinning(gridCopy, move, queenNeighbors);
    }

    /**
     * Switches turn to the next player.
     *
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

    /**
     * Retrieves the piece wrapper at the given coordinate.
     *
     * @param coordinate the grid coordinate.
     * @return the piece wrapper at that coordinate.
     */
    public PieceWrapper getPieceWrapperByHexCoordinate(HexCoordinate coordinate) {
        return immutableGrid.getPieceWrapperByHexCoordinate(coordinate);
    }

    /**
     * Counts the total legal moves available to a player on a given grid.
     *
     * @param gridCopy the grid state.
     * @param currentTurn the player's color.
     * @return count of legal moves.
     */
    public int countTotalLegalMoves(PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, PieceColor currentTurn) {
        return movementValidator.countTotalLegalMoves(this, gridCopy, currentTurn);
    }

    /**
     * Counts occupied neighbors for a given coordinate in a given grid copy.
     *
     * @param gridCopy the grid state.
     * @param coord center coordinate.
     * @return number of neighbors.
     */
    public int countNeighbours(PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, HexCoordinate coord) {
        return immutableGrid.countNeighbours(gridCopy, coord);
    }

    /**
     * Checks win condition on given grid copy.
     *
     * @param gridCopy immutable grid state.
     * @param isSimulated whether simulation mode.
     * @return win state per player.
     */
    public Pair<Map<PieceColor, Boolean>, EndGameStatus> checkWin(PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, boolean isSimulated) {
        return immutableGrid.checkWin(this, gridCopy, isSimulated);
    }

    /**
     * Returns a list of all legal movement actions for a player on a custom grid.
     *
     * @param gridCopy the grid state.
     * @param currentTurn the player's color.
     * @return list of valid moves.
     */
    public List<MovementAction> getLegalMoves(PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, PieceColor currentTurn) {
        return movementValidator.getLegalMoves(gridCopy, currentTurn);
    }

    public ImmutableGrid getImmutableGridCopy() {
        return new ImmutableGrid(immutableGrid.getGrid(), immutableGrid.getPiecesCount(), immutableGrid.getTurn());
    }

    /**
     * @return the current grid map.
     */
    public PMap<HexCoordinate, PStack<PieceWrapper>> getGrid() { return immutableGrid.getGrid(); }

    /**
     * Computes all hypothetical placement-and-move destinations for each piece type at a given coordinate.
     * <p>This method simulates placing a piece of each non-BLANK type on the specified coordinate
     * and then determines all legal moves that originate from that coordinate on the resulting grid.
     * It returns a set of (PieceType, destination) pairs representing where each piece could move.</p>
     *
     * @param coordinate the hex coordinate where the hypothetical piece is placed and moved from
     * @param color the color of the piece being tested
     * @return a set of pairs, each containing a PieceType and a HexCoordinate to which that piece
     *         could legally move if it were placed at the given coordinate
     */
    public Set<Pair<PieceType, HexCoordinate>> getHypotheticDestinationsFrom(HexCoordinate coordinate, PieceColor color) { return movementValidator.getHypotheticDestinationsFrom(this, coordinate, color); }
}
