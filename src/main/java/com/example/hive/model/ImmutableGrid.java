package com.example.hive.model;

import com.example.hive.model.*;
import javafx.scene.image.ImageView;
import org.pcollections.HashTreePMap;
import org.pcollections.PMap;
import org.pcollections.ConsPStack;
import org.pcollections.PStack;

import java.util.*;

import static com.example.hive.model.PieceColor.*;
import static com.example.hive.model.PieceColor.BLACK;
import static com.example.hive.model.PieceType.*;
import static com.example.hive.model.PieceType.ANT;

// Note: Adjust your imports for PieceWrapper, MovementAction, PlacementAction, HexCoordinate, Piece, PieceColor, PieceImage, etc.

public class ImmutableGrid {
    // Persistent map storing grid state: each cell is a persistent stack of PieceWrapper.
    private PMap<HexCoordinate, PStack<PieceWrapper>> grid;

    private final Map<PieceType, Integer> PIECES_COUNT = new HashMap<>(){{
        put(GRASSHOPPER, 3);
        put(BEETLE, 2);
        put(QUEEN_BEE, 1);
        put(SPIDER, 2);
        put(ANT, 3);
    }};

    private PieceColor currentTurn = WHITE; // White's turn.

    /**
     * Constructs an ImmutableGrid with an empty persistent map, then initializes the starting cell.
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

    public int countNeighbours(PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, HexCoordinate coord) {
        int counter = 0;
        for (HexCoordinate neighbor : coord.getNeighbors()) {
            if (gridCopy.containsKey(neighbor) && gridCopy.get(neighbor) != null && Objects.requireNonNull(gridCopy.get(neighbor).get(0)).getPiece().getType() != BLANK)
                counter++;
        }
        return counter;
    }

    public int getTotalOccupiedTiles(PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy) {
        int counter = 0;
        for (PMap.Entry<HexCoordinate, PStack<PieceWrapper>> entry : gridCopy.entrySet()) {
            PieceWrapper pieceWrapper = entry.getValue().get(0);
            if (pieceWrapper.getPiece().getType() != BLANK)
                counter++;
        }
        return counter;
    }

    public void movePiece(GameModel gameModel, MovementAction move) {
        HexCoordinate targetCoord = move.getTo();
        HexCoordinate sourceCoord = move.getFrom();
        PStack<PieceWrapper> sourceStack = grid.get(sourceCoord);

        if (sourceStack.get(0).getPiece().getType() == QUEEN_BEE)
            gameModel.setQueenCoordinate(targetCoord, sourceStack.get(0).getPiece().getColor());
        System.out.println(80);
        advanceTurn();

        setGrid(simulateMovePiece(grid, move));
    }

    /**
     * Updates the grid with a move. This method "moves" the piece from the source to the target and returns a new ImmutableGrid.
     *
     * @param move the movement action
     * @return a new ImmutableGrid with the move applied.
     */
    public PMap<HexCoordinate, PStack<PieceWrapper>> simulateMovePiece(PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, MovementAction move) {
        PMap<HexCoordinate, PStack<PieceWrapper>> newGrid = gridCopy;
        HexCoordinate targetCoord = move.getTo();
        HexCoordinate sourceCoord = move.getFrom();

        System.out.println("TARGET COORD: "+ targetCoord);
        PStack<PieceWrapper> targetStack = gridCopy.get(targetCoord);
        PStack<PieceWrapper> sourceStack = gridCopy.get(sourceCoord);

        // System.out.println("Source Stack Before Removal: " + sourceStack);

        PieceWrapper piece = sourceStack.get(0);
        sourceStack = sourceStack.minus(piece);

        // System.out.println("Source Stack After Removal: " + sourceStack);

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
        return newGrid;
    }

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

    public Pair<PMap<HexCoordinate, PStack<PieceWrapper>>, Integer> placePiece(GameModel gameModel, PieceColor color, PieceWrapper pieceWrapper,
                                    PlacementAction placementAction) {
        gameModel.decrementPiece(color, pieceWrapper.getPiece().getType());
        gameModel.incrementMoveCount(color);
        HexCoordinate pieceImageCoord = placementAction.getDestination();
        if (pieceWrapper.getPiece().getType() == QUEEN_BEE) {
            gameModel.setQueenPlaced(currentTurn);
            gameModel.setQueenCoordinate(pieceImageCoord, currentTurn);
        }
        Pair<PMap<HexCoordinate, PStack<PieceWrapper>>, Integer> response = simulatePlacePiece(gameModel, color, grid, pieceWrapper, placementAction);
        advanceTurn();
        System.out.println("placePiece: " + currentTurn);
        return response;
    }

    /**
     * Places a piece on the grid at the destination specified by the placement action.
     * Returns a new ImmutableGrid with the piece placed.
     *
     * @param pieceWrapper the piece to place
     * @param placementAction the placement action (destination coordinate)
     * @return a new ImmutableGrid with the placement applied.
     */
    public Pair<PMap<HexCoordinate, PStack<PieceWrapper>>, Integer> simulatePlacePiece(GameModel gameModel, PieceColor currentTurn, PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, PieceWrapper pieceWrapper,
                                    PlacementAction placementAction) {
        // System.out.println(grid);
        // System.out.println(placementAction.getDestination());
        Pair<PMap<HexCoordinate, PStack<PieceWrapper>>, Integer> returnedPair;
        PMap<HexCoordinate, PStack<PieceWrapper>> newGrid;
        PieceWrapper newPiece = pieceWrapper;
        List<HexCoordinate> neighborsToAdd = new ArrayList<>();
        PieceWrapper placedPiece = new PieceWrapper(new Piece(newPiece.getPiece().getType(), newPiece.getPiece().getColor()));

        newGrid = gridCopy;


        HexCoordinate pieceImageCoord = placementAction.getDestination();
        PStack<PieceWrapper> stack = gridCopy.get(pieceImageCoord);

        // System.out.println(newPiece.getPiece().getType() + "\n" + newPiece.getPiece().getColor());
        stack = stack.plus(placedPiece);
        newGrid = newGrid.plus(pieceImageCoord, stack);
        ArrayList<HexCoordinate> neighboors = pieceImageCoord.getNeighbors();
        // System.out.println(pieceImageCoord.getNeighbors());
        for (HexCoordinate neighbor : neighboors) {
            if (!newGrid.containsKey(neighbor)) {
                neighborsToAdd.add(neighbor);
            }
        }

        /*
        if (isWhite)
            placedWhitePieces.add(placedImageView);
        else
            placedBlackPieces.add(placedImageView);
         */
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
     * Returns the persistent grid.
     *
     * @return the persistent map representing the grid.
     */
    public PMap<HexCoordinate, PStack<PieceWrapper>> getGrid() {
        return grid;
    }

    public Map<PieceColor, Boolean> checkWin(GameModel gameModel) {
        return checkWin(gameModel, grid);
    }

    public Map<PieceColor, Boolean> checkWin(GameModel gameModel, PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy) {
        // System.out.println(198);
        // System.out.println(gridCopy);
        PieceWrapper pieceWrapper;
        Map<PieceColor, Boolean> winner = new HashMap<>();
        winner.put(WHITE, false);
        winner.put(BLACK, false);

        boolean isBlackQBPlaced = gameModel.isQueenPlaced(BLACK), isWhiteQBPlaced = gameModel.isQueenPlaced(WHITE);

        if (isBlackQBPlaced || isWhiteQBPlaced) {
            if (isBlackQBPlaced) {
                HexCoordinate blackQBCoord = gameModel.getQueenCoordinate(BLACK);
                pieceWrapper = gridCopy.get(blackQBCoord).get(0);
                if (pieceWrapper.getPiece().getType() == QUEEN_BEE && pieceWrapper.getPiece().getColor() == BLACK) {
                    winner.put(WHITE, true);
                    for (HexCoordinate neighbor : getKeyByValue(gridCopy, pieceWrapper).getNeighbors()) {
                        if (gridCopy.get(neighbor).get(0).getPiece().getColor() == NONE)
                            winner.put(WHITE, false);
                    }
                }
            }
            if (isWhiteQBPlaced) {
                HexCoordinate whiteQBCoord = gameModel.getQueenCoordinate(WHITE);
                pieceWrapper = gridCopy.get(whiteQBCoord).get(0);
                if (pieceWrapper.getPiece().getType() == QUEEN_BEE && pieceWrapper.getPiece().getColor() == WHITE) {
                    winner.put(BLACK, true);
                    for (HexCoordinate neighbor : getKeyByValue(gridCopy, pieceWrapper).getNeighbors()) {
                        if (gridCopy.get(neighbor).get(0).getPiece().getColor() == NONE)
                            winner.put(BLACK, false);
                    }
                }
            }

            if (!winner.get(WHITE) && !winner.get(BLACK)) {
                if (gameModel.getLegalMoves(gridCopy, currentTurn).isEmpty() && gameModel.getValidPlacements(currentTurn).isEmpty()) {
                    // color = color == WHITE ? BLACK : WHITE;
                    System.out.println(282);
                    advanceTurn();
                    if (gameModel.getLegalMoves(currentTurn).isEmpty() && gameModel.getValidPlacements(currentTurn).isEmpty()) {
                        winner.put(WHITE, true);
                        winner.put(BLACK, true);
                    }
                }
            }
        }

        return winner;
    }


    public <T> HexCoordinate getHexCoordinateByPieceWrapperImage(T imageObject) {
        for (PMap.Entry<HexCoordinate, PStack<PieceWrapper>> entry : grid.entrySet()) {
            for (PieceWrapper pieceWrapper : entry.getValue()) {
                if (pieceWrapper.getImageView().equals(imageObject)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    public void advanceTurn() {
        currentTurn = (currentTurn == WHITE) ? BLACK : WHITE;
    }

    public PieceColor getTurn() {
        return currentTurn;
    }

    public Map<PieceType, Integer> getPiecesCount() {
        return new HashMap<>(PIECES_COUNT);
    }

    public void setGrid(PMap<HexCoordinate, PStack<PieceWrapper>> grid) {
        this.grid = grid;
    }
}
