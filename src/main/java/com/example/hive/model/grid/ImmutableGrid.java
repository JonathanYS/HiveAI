package com.example.hive.model.grid;

import com.example.hive.model.enums.EndGameStatus;
import com.example.hive.model.enums.PieceColor;
import com.example.hive.model.enums.PieceImage;
import com.example.hive.model.enums.PieceType;
import com.example.hive.model.logic.MovementAction;
import com.example.hive.model.logic.PlacementAction;
import com.example.hive.model.utils.Pair;
import javafx.scene.image.ImageView;
import org.pcollections.HashTreePMap;
import org.pcollections.PMap;
import org.pcollections.ConsPStack;
import org.pcollections.PStack;

import java.util.*;

import static com.example.hive.model.enums.PieceColor.*;
import static com.example.hive.model.enums.PieceColor.BLACK;
import static com.example.hive.model.enums.PieceType.*;
import static com.example.hive.model.enums.PieceType.ANT;

/**
 * Represents an immutable, persistent hexagonal grid for the Hive game.
 * <p>
 * Internally uses persistent (immutable) collections to efficiently simulate
 * moves and placements without mutating the original state.
 * Tracks piece counts, current player's turn, and repetition detection for draws.
 * </p>
 */
public class ImmutableGrid {
    // Persistent map storing grid state: each cell is a persistent stack of PieceWrapper.
    private PMap<HexCoordinate, PStack<PieceWrapper>> grid;

    private Map<PieceType, Integer> piecesCount = new HashMap<>(){{
        put(GRASSHOPPER, 3);
        put(BEETLE, 2);
        put(QUEEN_BEE, 1);
        put(SPIDER, 2);
        put(ANT, 3);
    }};

    private PieceColor currentTurn = WHITE; // White's turn.

    private final Map<ImmutableGrid, Integer> repetitionCount = new HashMap<>();
    private static final int REPETITION_THRESHOLD = 3;

    /**
     * Constructs a new empty grid with a single blank tile at origin (0,0).
     */
    public ImmutableGrid() {
        PMap<HexCoordinate, PStack<PieceWrapper>> initialGrid = HashTreePMap.empty();
        // Initialize the starting cell (0,0) with a blank piece.
        PStack<PieceWrapper> initialStack = ConsPStack.<PieceWrapper>empty()
                .plus(new PieceWrapper(new Piece(PieceType.BLANK, PieceColor.NONE),
                        new ImageView(PieceImage.BLANK_TILE.getImage())));
        initialGrid = initialGrid.plus(new HexCoordinate(0, 0), initialStack);
        this.grid = initialGrid;
    }

    /**
     * Internal constructor used for simulating new grid states.
     *
     * @param gridCopy      the grid map to initialize
     * @param piecesCount   the piece count mapping to carry over
     * @param currentTurn   the turn color to set
     */
    public ImmutableGrid(PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, Map<PieceType, Integer> piecesCount, PieceColor currentTurn) {
        this.piecesCount = piecesCount;
        this.currentTurn = currentTurn;
        grid = gridCopy;
    }

    /**
     * Records the given grid state to detect repetitions.
     *
     * @param gridCopy      the grid to record
     * @param piecesCount   the piece counts at this state
     * @param currentTurn   whose turn it is
     * @return the number of times this state has occurred
     */
    private int recordGridForRepetition(PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, Map<PieceType, Integer> piecesCount, PieceColor currentTurn) {
        ImmutableGrid immutableGrid = new ImmutableGrid(gridCopy, piecesCount, currentTurn);
        int count = repetitionCount.getOrDefault(immutableGrid, 0) + 1;
        repetitionCount.put(immutableGrid, count);
        return count;
    }

    /**
     * Counts non-blank neighbour tiles around a given coordinate.
     *
     * @param gridCopy the grid state to inspect
     * @param coord    the coordinate whose neighbours to count
     * @return number of occupied neighbours
     */
    public int countNeighbours(PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, HexCoordinate coord) {
        int counter = 0;
        for (HexCoordinate neighbor : coord.getNeighbors()) {
            if (gridCopy.containsKey(neighbor) && gridCopy.get(neighbor) != null && Objects.requireNonNull(gridCopy.get(neighbor).get(0)).getPiece().type() != BLANK)
                counter++;
        }
        return counter;
    }

    /**
     * Calculates the total number of occupied tiles (non-blank) in the grid.
     *
     * @param gridCopy the grid state to inspect
     * @return number of occupied tiles
     */
    public int getTotalOccupiedTiles(PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy) {
        int counter = 0;
        for (PMap.Entry<HexCoordinate, PStack<PieceWrapper>> entry : gridCopy.entrySet()) {
            PieceWrapper pieceWrapper = entry.getValue().get(0);
            if (pieceWrapper.getPiece().type() != BLANK)
                counter++;
        }
        return counter;
    }

    /**
     * Moves a piece according to the given MovementAction, updates gameModel if Queen moves,
     * advances turn, and sets the new grid state.
     *
     * @param gameModel game state tracker
     * @param move      action containing source and target coordinates
     */
    public void movePiece(GameModel gameModel, MovementAction move) {
        HexCoordinate targetCoord = move.getTo();
        HexCoordinate sourceCoord = move.getFrom();
        PStack<PieceWrapper> sourceStack = grid.get(sourceCoord);

        if (sourceStack.get(0).getPiece().type() == QUEEN_BEE)
            gameModel.setQueenCoordinate(targetCoord, sourceStack.get(0).getPiece().color());
        advanceTurn();

        PMap<HexCoordinate, PStack<PieceWrapper>> simulatedGridState = simulateMovePiece(grid, move);
        setGrid(simulatedGridState);
    }

    /**
     * Simulates moving a piece on a copy of the grid without mutating original.
     *
     * @param gridCopy the grid map to simulate
     * @param move     action describing from/to coordinates
     * @return new grid state after movement
     */
    public PMap<HexCoordinate, PStack<PieceWrapper>> simulateMovePiece(PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, MovementAction move) {
        PMap<HexCoordinate, PStack<PieceWrapper>> newGrid = gridCopy;
        HexCoordinate targetCoord = move.getTo();
        HexCoordinate sourceCoord = move.getFrom();

        PStack<PieceWrapper> targetStack = gridCopy.get(targetCoord);
        PStack<PieceWrapper> sourceStack = gridCopy.get(sourceCoord);

        PieceWrapper piece = sourceStack.get(0);
        sourceStack = sourceStack.minus(piece);

        targetStack = targetStack.plus(piece);
        newGrid = newGrid.plus(targetCoord, targetStack);

        if (sourceStack.isEmpty()) {
            piece = new PieceWrapper(new Piece(BLANK, NONE));
            sourceStack = sourceStack.plus(piece);

        }

        newGrid = newGrid.plus(sourceCoord, sourceStack);

        for (HexCoordinate neighbor : targetCoord.getNeighbors()) {
            if (!gridCopy.containsKey(neighbor)) {
                PStack<PieceWrapper> stack = ConsPStack.empty();
                stack = stack.plus(new PieceWrapper(new Piece(BLANK, NONE)));
                newGrid = newGrid.plus(neighbor, stack);
            }
        }
        return newGrid;
    }

    /**
     * Retrieves the key associated with a given PieceWrapper value.
     *
     * @param map   grid map to search
     * @param value the piece wrapper to find
     * @param <K>   key type
     * @param <V>   piece wrapper type
     * @return coordinate key if found, otherwise null
     */
    public <K, V> K getKeyByValue(PMap<K, PStack<V>> map, V value) {
        for (PMap.Entry<K, PStack<V>> entry : map.entrySet()) {
            for (V v : entry.getValue()) {
                if (v.equals(value)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    /**
     * Places a piece onto the grid, updates gameModel counts, advances turn,
     * and returns simulation result pair to enforces queen placement rule.
     *
     * @param gameModel       game state tracker
     * @param color           color of piece being placed
     * @param pieceWrapper    wrapper containing piece and image
     * @param placementAction destination coordinate info
     * @return pair of new grid and queen placement warning flag
     */
    public Pair<PMap<HexCoordinate, PStack<PieceWrapper>>, Integer> placePiece(GameModel gameModel, PieceColor color, PieceWrapper pieceWrapper,
                                                                               PlacementAction placementAction) {
        gameModel.decrementPiece(color, pieceWrapper.getPiece().type());
        gameModel.incrementMoveCount(color);
        HexCoordinate pieceImageCoord = placementAction.getDestination();
        if (pieceWrapper.getPiece().type() == QUEEN_BEE) {
            gameModel.setQueenPlaced(currentTurn);
            gameModel.setQueenCoordinate(pieceImageCoord, currentTurn);
        }
        Pair<PMap<HexCoordinate, PStack<PieceWrapper>>, Integer> response = simulatePlacePiece(gameModel, color, grid, pieceWrapper, placementAction);
        advanceTurn();
        return response;
    }

    /**
     * Simulates placing a piece on a grid copy without mutating original.
     * Adds blank neighbour tiles as needed.
     *
     * @param gameModel       game state tracker
     * @param currentTurn     current player's color
     * @param gridCopy        grid to simulate on
     * @param newPiece        wrapper for piece to place
     * @param placementAction destination coordinate info
     * @return pair of new grid and queen rule violation indicator
     */
    public Pair<PMap<HexCoordinate, PStack<PieceWrapper>>, Integer> simulatePlacePiece(GameModel gameModel, PieceColor currentTurn, PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, PieceWrapper newPiece,
                                    PlacementAction placementAction) {
        Pair<PMap<HexCoordinate, PStack<PieceWrapper>>, Integer> returnedPair;
        PMap<HexCoordinate, PStack<PieceWrapper>> newGrid;
        List<HexCoordinate> neighborsToAdd = new ArrayList<>();
        PieceWrapper placedPiece = new PieceWrapper(new Piece(newPiece.getPiece().type(), newPiece.getPiece().color()));

        newGrid = gridCopy;


        HexCoordinate pieceImageCoord = placementAction.getDestination();
        PStack<PieceWrapper> stack = gridCopy.get(pieceImageCoord);

        stack = stack.plus(placedPiece);
        newGrid = newGrid.plus(pieceImageCoord, stack);
        Set<HexCoordinate> neighboors = pieceImageCoord.getNeighbors();
        for (HexCoordinate neighbor : neighboors) {
            if (!newGrid.containsKey(neighbor)) {
                neighborsToAdd.add(neighbor);
            }
        }

        for (HexCoordinate neighbor : neighborsToAdd) {
            stack = ConsPStack.empty();
            stack = stack.plus(new PieceWrapper(new Piece(BLANK, NONE)));
            newGrid = newGrid.plus(neighbor, stack);
        }

        int moves = gameModel.getMoveCount(currentTurn);
        boolean queenIsPlaced = gameModel.isQueenPlaced(currentTurn);
        if (moves == 3 && !queenIsPlaced) {
            returnedPair = new Pair<>(newGrid , 1);
            return returnedPair;
        }
        returnedPair = new Pair<>(newGrid, 0);
        return returnedPair;
    }

    /**
     * @return current persistent grid state
     */
    public PMap<HexCoordinate, PStack<PieceWrapper>> getGrid() {
        return grid;
    }

    /**
     * Checks for end game conditions including surrounds, draws, and repetitions.
     *
     * @param gameModel   game state tracker
     * @param isSimulated true if checking on simulated grid
     * @return pair of winner map and endgame status
     */
    public Pair<Map<PieceColor, Boolean>, EndGameStatus> checkWin(GameModel gameModel, boolean isSimulated) {
        return checkWin(gameModel, grid, isSimulated);
    }

    /**
     * Win-check using provided grid state.
     *
     * @param gameModel   game state tracker
     * @param gridCopy    grid state to evaluate
     * @param isSimulated true if evaluating a simulated grid
     * @return pair of winner map and endgame status
     */
    public Pair<Map<PieceColor, Boolean>, EndGameStatus> checkWin(GameModel gameModel, PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, boolean isSimulated) {
        Map<PieceColor, Boolean> winner = new HashMap<>();
        winner.put(WHITE, false);
        winner.put(BLACK, false);
        Pair<Map<PieceColor, Boolean>, EndGameStatus> returnedPair = new Pair<>(winner, EndGameStatus.NONE);

        boolean isBlackQBPlaced = gameModel.isQueenPlaced(BLACK), isWhiteQBPlaced = gameModel.isQueenPlaced(WHITE);

        if (isBlackQBPlaced || isWhiteQBPlaced) {
            if (isBlackQBPlaced) {
                HexCoordinate blackQBCoord = gameModel.getQueenCoordinate(BLACK);
                winner.put(WHITE, true);
                for (HexCoordinate neighbor : blackQBCoord.getNeighbors()) {
                    if (gridCopy.get(neighbor).get(0).getPiece().color() == NONE)
                        winner.put(WHITE, false);
                }
            }
            if (isWhiteQBPlaced) {
                HexCoordinate whiteQBCoord = gameModel.getQueenCoordinate(WHITE);
                winner.put(BLACK, true);
                for (HexCoordinate neighbor : whiteQBCoord.getNeighbors()) {
                    if (gridCopy.get(neighbor).get(0).getPiece().color() == NONE)
                        winner.put(BLACK, false);
                }
            }

            int count = 0;
            if (!isSimulated)
                count = recordGridForRepetition(removeBlankTiles(gridCopy), piecesCount, currentTurn);

            if (!winner.get(WHITE) && !winner.get(BLACK)) {
                if (gameModel.getLegalMoves(gridCopy, currentTurn).isEmpty() && gameModel.getValidPlacements(currentTurn).isEmpty()) {
                    advanceTurn();
                    if (gameModel.getLegalMoves(currentTurn).isEmpty() && gameModel.getValidPlacements(currentTurn).isEmpty()) {
                        winner.put(WHITE, true);
                        winner.put(BLACK, true);
                        returnedPair = new Pair<>(winner, EndGameStatus.DRAW_BY_NO_MOVES);
                    }
                } else if (count >= REPETITION_THRESHOLD) {
                    winner.put(WHITE, true);
                    winner.put(BLACK, true);
                    returnedPair = new Pair<>(winner, EndGameStatus.DRAW_BY_REPETITION);
                }
            } else if (winner.get(WHITE) && winner.get(BLACK)) {
                returnedPair = new Pair<>(winner, EndGameStatus.STALEMATE);
            } else {
                returnedPair = new Pair<>(winner, EndGameStatus.WIN);
            }
        }
        return returnedPair;
    }

    /**
     * Removes all blank-tile entries from the grid.
     *
     * @param gridCopy grid state to filter
     * @return new grid without blank stacks
     */
    public static PMap<HexCoordinate, PStack<PieceWrapper>> removeBlankTiles(PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy) {
        PMap<HexCoordinate, PStack<PieceWrapper>> newGridCopy = HashTreePMap.empty();
        for (PMap.Entry<HexCoordinate, PStack<PieceWrapper>> entry : gridCopy.entrySet()) {
            PStack<PieceWrapper> stack = entry.getValue();
            if (!stack.isEmpty() && entry.getValue().get(0).getPiece().type() != BLANK)
                newGridCopy = newGridCopy.plus(entry.getKey(), entry.getValue());
        }
        return newGridCopy;
    }

    /**
     * Finds the coordinate of a given piece's image view.
     *
     * @param imageView image view to locate
     * @return coordinate if found, otherwise null
     */
    public HexCoordinate getHexCoordinateByPieceWrapperImage(ImageView imageView) {
        for (PMap.Entry<HexCoordinate, PStack<PieceWrapper>> entry : grid.entrySet()) {
            for (PieceWrapper pieceWrapper : entry.getValue()) {
                if (pieceWrapper.getImageView().equals(imageView)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    /**
     * Retrieves the top piece wrapper at a given coordinate.
     *
     * @param coordinate hex coordinate to query
     * @return top PieceWrapper or null if coordinate is absent
     */
    public PieceWrapper getPieceWrapperByHexCoordinate(HexCoordinate coordinate) {
        if (!grid.containsKey(coordinate)) return null;
        return grid.get(coordinate).get(0);
    }

    /**
     * Advances the turn to the next player.
     */
    public void advanceTurn() {
        currentTurn = (currentTurn == WHITE) ? BLACK : WHITE;
    }

    /**
     * @return the color whose turn it is
     */
    public PieceColor getTurn() {
        return currentTurn;
    }

    /**
     * @return copy of remaining pieces count map
     */
    public Map<PieceType, Integer> getPiecesCount() {
        return new HashMap<>(piecesCount);
    }

    /**
     * Sets the grid to a new persistent state.
     *
     * @param grid new grid state map
     */
    public void setGrid(PMap<HexCoordinate, PStack<PieceWrapper>> grid) {
        this.grid = grid;
    }

    /**
     * Compares this {@code ImmutableGrid} to another object.
     *
     * @param object The object to compare to.
     * @return {@code true} if the object is a {@code ImmutableGrid} and its grid, piecesCount and currentTurn values are equal to this ImmutableGrids's grid, piecesCount and currentTurn, otherwise {@code false}.
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        ImmutableGrid that = (ImmutableGrid) object;
        return Objects.equals(getGrid(), that.getGrid()) && Objects.equals(piecesCount, that.piecesCount) && currentTurn == that.currentTurn;
    }

    /**
     * Returns a hash code value for this {@code ImmutableGrid}.
     *
     * @return A hash code value for this ImmutableGrid.
     */
    @Override
    public int hashCode() {
        return Objects.hash(getGrid(), piecesCount, currentTurn);
    }
}
