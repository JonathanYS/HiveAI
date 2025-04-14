package com.example.hive.model;

import java.util.*;
import static com.example.hive.model.PieceType.*;
import static com.example.hive.model.PieceColor.*;

/**
 * The Grid class represents the game board for Hive.
 * It manages the placement and movement of pieces, and determining the
 * game state (such as turns, and win conditions).
 *
 * <p>
 * <b>Time Complexity:</b> Most grid operations are O(n) in the worst-case (n = number of cells), but many work on fixed-size neighbor sets (O(1)).
 * <br>
 * <b>Space Complexity:</b> O(n) for grid storage and copies.
 * </p>
 */
public class Grid {

    private final Map<HexCoordinate, Deque<PieceWrapper>> grid = new HashMap<>();

    private Map<PieceColor, Integer> moveCount;
    private Map<PieceColor, Boolean> queenPlaced;

    private Map<PieceType, Integer> piecesCount;

    private PieceColor currentTurn = WHITE; // White's turn.

    /**
     * Constructs a Grid instance and initializes the game board with starting pieces.
     * Time Complexity: O(1)
     * Space Complexity: O(n) where n is the number of initialized grid cells.
     */
    public Grid() {
        // Initialize the starting tile at (0,0).
        Deque<PieceWrapper> stack = new ArrayDeque<>();
        Piece piece = new Piece(BLANK, NONE);
        stack.push(new PieceWrapper(piece));
        grid.put(new HexCoordinate(0, 0), stack);
        piecesCount = new HashMap<>();
        piecesCount.put(GRASSHOPPER, 3);
        piecesCount.put(BEETLE, 2);
        piecesCount.put(QUEEN_BEE, 1);
        piecesCount.put(SPIDER, 2);
        piecesCount.put(ANT, 3);

        moveCount = new HashMap<>();
        queenPlaced = new HashMap<>();
        moveCount.put(WHITE, 0);
        moveCount.put(BLACK, 0);
        queenPlaced.put(WHITE, false);
        queenPlaced.put(BLACK, false);
    }

    /**
     * Returns a deep copy of the current grid.
     * Time Complexity: O(n) where n is the number of grid cells.
     * Space Complexity: O(n)
     *
     * @return a new map representing a deep copy of the grid.
     */
    public Map<HexCoordinate, Deque<PieceWrapper>> getGrid() {
        Map<HexCoordinate, Deque<PieceWrapper>> gridCopy = new HashMap<>(grid);
        for (Map.Entry<HexCoordinate, Deque<PieceWrapper>> entry : grid.entrySet()) {
            gridCopy.put(entry.getKey(), new ArrayDeque<>(entry.getValue())); // Ensures a deep copy of stacks.
        }
        return gridCopy;
    }

    /**
     * Returns a deep copy of the provided grid.
     * Time Complexity: O(n)
     * Space Complexity: O(n)
     *
     * @param gameGrid the grid to copy.
     * @return a deep copy of the gameGrid.
     */
    public Map<HexCoordinate, Deque<PieceWrapper>> getGrid(Map<HexCoordinate, Deque<PieceWrapper>> gameGrid) {
        Map<HexCoordinate, Deque<PieceWrapper>> gridCopy = new HashMap<>(gameGrid);
        for (Map.Entry<HexCoordinate, Deque<PieceWrapper>> entry : gameGrid.entrySet()) {
            gridCopy.put(entry.getKey(), new ArrayDeque<>(entry.getValue())); // Ensures a deep copy of stacks.
        }
        return gridCopy;
    }

    /**
     * Returns a copy of the pieces count mapping.
     * Time Complexity: O(1)
     * Space Complexity: O(n) for the copy.
     *
     * @return a map of PieceType to remaining count.
     */
    public Map<PieceType, Integer> getPiecesCount() {
        return new HashMap<>(piecesCount);
    }

    /**
     * Helper method to retrieve a key by its value from a map.
     * <p>
     * Note: For frequent lookups, consider maintaining a reverse map.
     * </p>
     *
     * Time Complexity: O(n) where n is the number of entries in the map.
     * Space Complexity: O(1)
     *
     * @param map the map to search.
     * @param value the value to find.
     * @return the associated key or null if not found.
     */
    public <K, V> K getKeyByValue(Map<K, Deque<V>> map, V value) {
        for (Map.Entry<K, Deque<V>> entry : map.entrySet()) {
            assert entry.getValue().peek() != null;
            if (entry.getValue().peek().equals(value)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Counts the number of non-blank neighboring pieces around the given coordinate.
     * Time Complexity: O(1) since the neighbor set size is constant (typically 6).
     * Space Complexity: O(1)
     *
     * @param gridCopy the grid to check.
     * @param coord the central coordinate.
     * @return the count of neighbors.
     */
    public int countNeighbours(Map<HexCoordinate, Deque<PieceWrapper>> gridCopy, HexCoordinate coord) {
        int counter = 0;
        for (HexCoordinate neighbor : coord.getNeighbors()) {
            if (gridCopy.containsKey(neighbor) && gridCopy.get(neighbor) != null && Objects.requireNonNull(gridCopy.get(neighbor).peek()).getPiece().getType() != BLANK)
                counter++;
        }
        return counter;
    }

    /**
     * Counts the total number of occupied (non-blank) tiles in the grid.
     * Time Complexity: O(n) where n is the number of grid cells.
     * Space Complexity: O(1)
     *
     * @param gridCopy the grid to evaluate.
     * @return the total count of occupied tiles.
     */
    public int getTotalOccupiedTiles(Map<HexCoordinate, Deque<PieceWrapper>> gridCopy) {
        int counter = 0;
        for (Map.Entry<HexCoordinate, Deque<PieceWrapper>> entry : gridCopy.entrySet()) {
            PieceWrapper pieceWrapper = entry.getValue().peek();
            if (pieceWrapper.getPiece().getType() != BLANK)
                counter++;
        }
        return counter;
    }

    /**
     * Moves a piece on the main grid based on the given movement action.
     * Also advances the turn.
     * Time Complexity: O(1) for piece move, plus O(1) for neighbor additions.
     * Space Complexity: O(1)
     *
     * @param movementAction the move to perform.
     */
    public void movePiece(GameModel gameModel, MovementAction movementAction, boolean isSimulated) {
        movePiece(gameModel, grid, movementAction, isSimulated);
        advanceTurn();
    }


    /**
     * Moves a piece on a provided grid copy.
     * <p>
     * This method pops the piece from the source stack and pushes it onto the target stack.
     * It also ensures that all neighboring coordinates of the target exist in the grid.
     * </p>
     *
     * Time Complexity: O(1) for the move and neighbor insertion (neighbors are constant in number).
     * Space Complexity: O(1)
     *
     * @param gridCopy the grid on which to perform the move.
     * @param move the movement action.
     * @return the updated grid copy.
     */
    public Map<HexCoordinate, Deque<PieceWrapper>> movePiece(GameModel gameModel, Map<HexCoordinate, Deque<PieceWrapper>> gridCopy, MovementAction move, boolean isSimulated) {
        HexCoordinate targetCoord = move.getTo();
        HexCoordinate sourceCoord = move.getFrom();

        System.out.println("TARGET COORD: "+ targetCoord);
        Deque<PieceWrapper> targetStack = gridCopy.get(targetCoord);
        Deque<PieceWrapper> sourceStack = gridCopy.get(sourceCoord);

        if (sourceStack.peek().getPiece().getType() == QUEEN_BEE && !isSimulated)
            gameModel.setQueenCoordinate(targetCoord, sourceStack.peek().getPiece().getColor());

        targetStack.push(sourceStack.pop());

        if (sourceStack.isEmpty()) {
            sourceStack.push(new PieceWrapper(new Piece(BLANK, NONE)));
        }

        for (HexCoordinate neighbor : targetCoord.getNeighbors()) {
            if (!gridCopy.containsKey(neighbor)) {
                Deque<PieceWrapper> stack = new ArrayDeque<>();
                stack.push(new PieceWrapper(new Piece(BLANK, NONE)));
                gridCopy.put(neighbor, stack);
            }
        }

        /*
        List<HexCoordinate> sourceCoordNeighbors = new ArrayList<>(sourceCoord.getNeighbors());
        Iterator<HexCoordinate> iterator = sourceCoordNeighbors.iterator();
        while (iterator.hasNext()) {
            HexCoordinate neighbor = iterator.next();
            if (gridCopy.get(neighbor).peek().getPiece().getType() != BLANK) {
                for (HexCoordinate neighborOfNeighbor : neighbor.getNeighbors()) {
                    if (!neighborOfNeighbor.equals(targetCoord)) {
                        // Remove neighborOfNeighbor if present.
                        sourceCoordNeighbors.remove(neighborOfNeighbor);
                    }
                }
                iterator.remove();
            }
        }
         */

        /*
        System.out.println("Neighbors: " + sourceCoordNeighbors);
        for (HexCoordinate tile : sourceCoordNeighbors)
            grid.get(tile).pop();
         */


        // clearNullValues();
        return gridCopy;
    }

    /**
     * Helper method to clear any null values in the grid.
     * Time Complexity: O(n)
     * Space Complexity: O(1)
     */
    private void clearNullValues() {
        Iterator<Map.Entry<HexCoordinate, Deque<PieceWrapper>>> iterator = grid.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<HexCoordinate, Deque<PieceWrapper>> tile = iterator.next();
            if (tile.getValue().peek() == null)
                iterator.remove();
        }
    }

    /**
     * Places a piece on the main grid.
     * Advances the turn afterward.
     * Time Complexity: O(n) due to iteration over grid entries.
     * Space Complexity: O(n)
     *
     * @param gameModel the game model.
     * @param currentTurn the current turn color.
     * @param pieceWrapper the piece to place.
     * @param placementAction the placement action.
     * @param isSimulated true if this is a simulation.
     * @return an integer response indicating if the player needs to place its Queen Bee by its next turn.
     */
    public int placePiece(GameModel gameModel, PieceColor currentTurn, PieceWrapper pieceWrapper, PlacementAction placementAction, boolean isSimulated) {
        Pair<Map<HexCoordinate, Deque<PieceWrapper>>, Integer> pair = placePiece(gameModel, grid, currentTurn, pieceWrapper, placementAction, isSimulated);
        advanceTurn();
        return pair.getValue();
    }

    /**
     * Places a piece on a provided grid copy.
     * Time Complexity: O(n) in worst-case due to iteration over grid entries.
     * Space Complexity: O(n)
     *
     * @param gameModel the game model.
     * @param gridCopy the grid copy.
     * @param currentTurn the current turn color.
     * @param pieceWrapper the piece to place.
     * @param placementAction the placement action.
     * @param isSimulated true if simulation.
     * @return a Pair containing the updated grid and an integer response (stating if the player needs to place its Queen Bee by its next turn).
     */
    public Pair<Map<HexCoordinate, Deque<PieceWrapper>>, Integer> placePiece(GameModel gameModel, Map<HexCoordinate, Deque<PieceWrapper>> gridCopy,
                                                                             PieceColor currentTurn, PieceWrapper pieceWrapper, PlacementAction placementAction, boolean isSimulated) {
        PieceWrapper newPiece = pieceWrapper;
        PieceWrapper oldPiece = gridCopy.get(placementAction.getDestination()).peek();
        Pair<Map<HexCoordinate, Deque<PieceWrapper>>, Integer> returnedPair;
        List<HexCoordinate> neighborsToAdd = new ArrayList<>();
        PieceWrapper placedPiece = new PieceWrapper(new Piece(newPiece.getPiece().getType(), newPiece.getPiece().getColor()));

        for (Map.Entry<HexCoordinate, Deque<PieceWrapper>> entry : gridCopy.entrySet()) {
            Deque<PieceWrapper> stack = entry.getValue();
            PieceWrapper piece = entry.getValue().peek();
            HexCoordinate pieceImageCoord = entry.getKey();

            if (piece == oldPiece) {
                System.out.println(newPiece.getPiece().getType() + "\n" + newPiece.getPiece().getColor());
                stack.push(placedPiece);
                if (newPiece.getPiece().getType() == QUEEN_BEE) {
                    if (!isSimulated) {
                    gameModel.setQueenPlaced(currentTurn);
                    gameModel.setQueenCoordinate(pieceImageCoord, currentTurn);
                    }
                }
                ArrayList<HexCoordinate> neighboors = pieceImageCoord.getNeighbors();
                for (HexCoordinate neighbor : neighboors) {
                    if (!gridCopy.containsKey(neighbor)) {
                        neighborsToAdd.add(neighbor);
                    }
                }
            }
        }

        /*
        if (isWhite)
            placedWhitePieces.add(placedImageView);
        else
            placedBlackPieces.add(placedImageView);
         */

        for (HexCoordinate neighbor : neighborsToAdd) {
            Deque<PieceWrapper> stack = new ArrayDeque<>();
            stack.push(new PieceWrapper(new Piece(BLANK, NONE)));
            gridCopy.put(neighbor, stack);
        }

        if (!isSimulated)
            gameModel.decrementPiece(currentTurn, newPiece.getPiece().getType());

        gameModel.incrementMoveCount(currentTurn);

        int moves = gameModel.getMoveCount(currentTurn);
        boolean queenIsPlaced = gameModel.isQueenPlaced(currentTurn);
        if (moves == 3 && !queenIsPlaced) {
            returnedPair = new Pair<>(gridCopy, 1);
            return returnedPair;
        }
        returnedPair = new Pair<>(gridCopy, 0);
        return returnedPair;
    }

    /**
     * Checks if there is a winner by determining if a queen is completely surrounded.
     * Time Complexity: O(n * d) where n is the number of grid cells and d is the number of neighbors per cell.
     * Space Complexity: O(1)
     *
     * @param gameModel the game model.
     * @return a map with win flags for WHITE and BLACK.
     */
    public Map<PieceColor, Boolean> checkWin(GameModel gameModel) {
        return checkWin(gameModel, grid);
    }

    /**
     * Checks for a win condition on a grid copy.
     * Time Complexity: O(n * d) where n is the number of grid cells and d is the number of neighbors per cell.
     * Space Complexity: O(1)
     *
     * @param gameModel the game model.
     * @param gridCopy the grid copy.
     * @return a map with win flags for WHITE and BLACK.
     */
    public Map<PieceColor, Boolean> checkWin(GameModel gameModel, Map<HexCoordinate, Deque<PieceWrapper>> gridCopy) {
        Map<PieceColor, Boolean> winner = new HashMap<>();
        winner.put(WHITE, false);
        winner.put(BLACK, false);
        for (Map.Entry<HexCoordinate, Deque<PieceWrapper>> entry : gridCopy.entrySet()) {
            PieceWrapper pieceWrapper = entry.getValue().peek();
            if (pieceWrapper.getPiece().getType() == QUEEN_BEE && pieceWrapper.getPiece().getColor() == BLACK) {
                winner.put(WHITE, true);
                for (HexCoordinate neighbor : getKeyByValue(gridCopy, pieceWrapper).getNeighbors()) {
                    if (gridCopy.get(neighbor).peek().getPiece().getColor() == NONE)
                        winner.put(WHITE, false);
                }
            }
        }
        for (Map.Entry<HexCoordinate, Deque<PieceWrapper>> entry : gridCopy.entrySet()) {
            PieceWrapper pieceWrapper = entry.getValue().peek();
            if (pieceWrapper.getPiece().getType() == QUEEN_BEE && pieceWrapper.getPiece().getColor() == WHITE) {
                winner.put(BLACK, true);
                for (HexCoordinate neighbor : getKeyByValue(gridCopy, pieceWrapper).getNeighbors()) {
                    if (gridCopy.get(neighbor).peek().getPiece().getColor() == NONE)
                        winner.put(BLACK, false);
                }
            }
        }

        if (!winner.get(WHITE) && !winner.get(BLACK)) {
            if (gameModel.getLegalMoves(currentTurn).isEmpty() && gameModel.getValidPlacements(currentTurn).isEmpty()) {
                advanceTurn();
                if (gameModel.getLegalMoves(currentTurn).isEmpty() && gameModel.getValidPlacements(currentTurn).isEmpty()) {
                    winner.put(WHITE, true);
                    winner.put(BLACK, true);
                }
            }
        }
        return winner;
    }

    /**
     * Returns the current player's turn.
     * Time Complexity: O(1)
     * Space Complexity: O(1)
     *
     * @return WHITE if it is white's turn; BLACK otherwise.
     */
    public PieceColor getTurn() {
        return currentTurn;
    }

    /**
     * Advances the turn to the next player.
     * Time Complexity: O(1)
     * Space Complexity: O(1)
     */
    public void advanceTurn() {
        currentTurn = (currentTurn == WHITE) ? BLACK : WHITE;
    }


    /**
     * Retrieves the HexCoordinate corresponding to the given ImageView.
     * <p>
     * Note: For frequent lookups, consider maintaining a reverse map.
     * </p>
     * Time Complexity: O(n) where n is the number of grid cells.
     * Space Complexity: O(1)
     *
     * @param imageObject the imageObject to search for.
     * @return the associated HexCoordinate, or null if not found.
     */
    public <T> HexCoordinate getHexCoordinateByPieceWrapperImage(T imageObject) {
        for (Map.Entry<HexCoordinate, Deque<PieceWrapper>> entry : grid.entrySet()) {
            if (entry.getValue().peek().getImageView().equals(imageObject))
                return entry.getKey();
        }
        return null;
    }
}
