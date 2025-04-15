package com.example.hive.model;

import static com.example.hive.model.PieceColor.*;
import static com.example.hive.model.PieceImage.*;
import static com.example.hive.model.PieceType.*;
import com.example.hive.model.Pair;
import org.pcollections.PMap;
import org.pcollections.PStack;

import java.util.*;

/**
 * Encapsulates the movement rules for pieces. This validator uses the Board state to compute valid moves.
 */
public class MovementValidatorSimulation {
    private ImmutableGrid immutableGrid;

    public MovementValidatorSimulation(ImmutableGrid immutableGrid) {
        this.immutableGrid = immutableGrid;
    }

    public void setImmutableGrid(ImmutableGrid immutableGrid) {
        this.immutableGrid = immutableGrid;
    }

    public List<PlacementAction> getValidPlacements(GameModel gameModel, PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, PieceColor currentTurn) {
        boolean stillLeft = gameModel.isStillLeftPanelPiece(currentTurn);
        if (!stillLeft)
            return null;
        List<PlacementAction> placements = new ArrayList<>();
        for (PMap.Entry<HexCoordinate, PStack<PieceWrapper>> entry : gridCopy.entrySet()) {
            HexCoordinate coord = entry.getKey();
            PieceWrapper tile = entry.getValue().get(0);
            assert tile != null;
            if (tile.getPiece().getType() == BLANK && isValidPlacement(gameModel, gridCopy, coord, currentTurn)) {
                placements.add(new PlacementAction(coord));
            }
        }

        return placements;
    }

    public boolean isValidPlacement(GameModel gameModel, PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, HexCoordinate coord, PieceColor currentTurn) {
        // The coordinate must exist in the grid.
        if (!gridCopy.containsKey(coord)) {
            return false;
        }
        PieceWrapper tile = gridCopy.get(coord).get(0);
        // The tile must be blank.
        if (tile.getPiece().getType() != BLANK) {
            return false;
        }
        // For the first move, allow placement.
        if (gameModel.getPlacedPiecesCount() < 2) {
            return true;
        }
        boolean hasAdjacentPiece = false;
        boolean isTouchingOpponent = false;
        for (HexCoordinate neighbor : coord.getNeighbors()) {
            if (gridCopy.containsKey(neighbor)) {
                PStack<PieceWrapper> neighborStack = gridCopy.get(neighbor);
                if (!neighborStack.isEmpty() && neighborStack.get(0).getPiece().getType() != BLANK) {
                    hasAdjacentPiece = true;

                    PieceColor neighborColor = neighborStack.get(0).getPiece().getColor();
                    if (neighborColor != currentTurn) {
                        isTouchingOpponent = true;
                    }
                }
            }
        }
        return hasAdjacentPiece && !isTouchingOpponent;
    }
    
    /**
     * Returns valid target coordinates for the piece at the given coordinate.
     */
    public List<MovementAction> getValidMoves(PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, HexCoordinate from) {
        PieceWrapper wrapper = gridCopy.get(from).get(0);
        if (wrapper == null || wrapper.getPiece() == null) return Collections.emptyList();
        Piece piece = wrapper.getPiece();
        return switch (piece.getType()) {
            case QUEEN_BEE -> getValidQueenMoves(gridCopy, from);
            case ANT -> getValidAntMoves(gridCopy, from);
            case SPIDER -> getValidSpiderMoves(gridCopy, from);
            case GRASSHOPPER -> getValidGrasshopperMoves(gridCopy, from);
            case BEETLE -> getValidBeetleMoves(gridCopy, from);
            default -> Collections.emptyList();
        };
    }

    private boolean canRemove(PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, HexCoordinate paramCoords) {
        int numPieces = gridCopy.get(paramCoords).size();
        return numPieces > 1;
    }

    private boolean hasFreedom(PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, HexCoordinate coord) {
        return switch (gridCopy.get(coord).get(0).getPiece().getType()) {
            case BEETLE, GRASSHOPPER -> canRemove(gridCopy, coord);
            case QUEEN_BEE, SPIDER, ANT -> (immutableGrid.countNeighbours(gridCopy, coord) <= 4) && canRemove(gridCopy, coord);
            default -> false;
        };
    }

    private boolean canSlide(PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, HexCoordinate from, HexCoordinate to) {
        if (from.equals(to)) return false;  // Can't slide to the same tile.

        Set<HexCoordinate> visited = new HashSet<>();
        Queue<HexCoordinate> queue = new LinkedList<>();

        queue.add(from);
        visited.add(from);

        while (!queue.isEmpty()) {
            HexCoordinate current = queue.poll();

            for (HexCoordinate neighbor : current.getNeighbors()) {
                if (!visited.contains(neighbor) && gridCopy.containsKey(neighbor) && gridCopy.get(neighbor).get(0).getPiece().getType() == BLANK) {
                    queue.add(neighbor);
                    visited.add(neighbor);

                    if (neighbor.equals(to)) return true;  // Found a valid sliding path
                }
            }
        }
        return false;  // No valid path found
    }

    private boolean hasFreedomToMove(PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, HexCoordinate from, HexCoordinate to) {
        return isConnectedAfterRemoval(gridCopy, from, to);
    }

    private boolean isConnectedAfterRemoval(PMap<HexCoordinate, PStack<PieceWrapper>> gameGrid, HexCoordinate from, HexCoordinate to) {
        PStack<PieceWrapper> stack = gameGrid.get(from);
        PieceWrapper piece = stack.get(0);
        gameGrid = gameGrid.plus(from, stack.minus(piece));

        boolean pieceRemoved = checkHiveConnectivity(gameGrid);

        stack = gameGrid.get(to);
        stack = stack.plus(piece);
        gameGrid = gameGrid.plus(to, stack);
        boolean pieceMoved = checkHiveConnectivity(gameGrid);

        boolean pieceHasFreedom = hasFreedom(gameGrid, to);

        return pieceRemoved && pieceMoved && pieceHasFreedom;
    }

    /**
     * Checks the connectivity of the hive using a flood-fill (DFS/BFS) algorithm.
     *
     * @param gridCopy         A copy of the game grid.
     * @return True if the entire hive is connected, otherwise false.
     */
    // Very similar to Graph Theory, when finding connectivity components (DFS/BFS).
    // The graph of the Hive grid, must be one connectivity component at all time.
    private boolean checkHiveConnectivity(PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy) {
        Queue<PieceWrapper> queue = new LinkedList<>();
        Set<HexCoordinate> visited = new HashSet<>();
        List<HexCoordinate> toVisit = new ArrayList<>();
        HexCoordinate startingCoordinate = null;

        for (PMap.Entry<HexCoordinate, PStack<PieceWrapper>> entry : gridCopy.entrySet()) {
            PieceWrapper pieceWrapper = entry.getValue().get(0);
            if (pieceWrapper != null && pieceWrapper.getPiece().getType() != BLANK)
                startingCoordinate = entry.getKey();
            else if (pieceWrapper == null) {
                System.out.println("NULL COORD: " + entry.getKey());

            }
        }

        toVisit.add(startingCoordinate);
        visited.add(startingCoordinate);

        while (!toVisit.isEmpty()) {
            HexCoordinate current = toVisit.remove(toVisit.size() - 1);

            for (HexCoordinate neighbor : current.getNeighbors()) {
                if (!visited.contains(neighbor) && gridCopy.containsKey(neighbor)) {
                    PieceWrapper neighborTile = gridCopy.get(neighbor).get(0);
                    queue = new LinkedList<>();


                    if (neighborTile != null && neighborTile.getPiece().getType() != BLANK) {
                        toVisit.add(neighbor);
                        visited.add(neighbor);

                        PStack<PieceWrapper> stack = gridCopy.get(neighbor);
                        PieceWrapper pieceWrapper = stack.get(0);
                        gridCopy = gridCopy.plus(neighbor, stack.minus(pieceWrapper));
                        queue.add(pieceWrapper);
                        while (gridCopy.get(neighbor).size() > 1) {
                            visited.add(neighbor);
                            stack = gridCopy.get(neighbor);
                            pieceWrapper = stack.get(0);
                            gridCopy = gridCopy.plus(neighbor, stack.minus(pieceWrapper));
                            queue.add(pieceWrapper);
                        }
                        while (!(queue.isEmpty())) {
                            stack = gridCopy.get(neighbor).plus(queue.poll());
                            gridCopy = gridCopy.plus(neighbor, stack);
                        }
                    }
                }
            }
        }

        // Check if all pieces are visited (meaning they are all connected).
        return visited.size() == immutableGrid.getTotalOccupiedTiles(gridCopy);
    }

    public boolean isValidMove(PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, HexCoordinate from, HexCoordinate to) {
        // System.out.println("TO: " + to);
        PieceType pieceType = gridCopy.get(from).get(0).getPiece().getType();;
        if (gridCopy.containsKey(to)) {
            if (pieceType != BEETLE && gridCopy.get(to).get(0).getPiece().getType() != BLANK) {
                // System.out.println(223);
                return false;
            }
        }

        PStack<PieceWrapper> fromStack = gridCopy.get(from);
        PStack<PieceWrapper> toStack = gridCopy.get(to);
        if (fromStack == null || fromStack.isEmpty() || toStack == null || toStack.isEmpty()) {
            // System.out.println(231);
            return false;
        }

        if (pieceType != BEETLE && pieceType != GRASSHOPPER && !canSlide(gridCopy, from, to)) {
            // System.out.println(236);
            return false;
        }
        if (!hasFreedomToMove(gridCopy, from, to)) {
            // System.out.println(240);
            return false;
        }
        return true;
    }

    /**
     * Returns a list of valid movements for the Bee piece from its current position.
     * The Bee can move one tile in any direction.
     *
     * @param pieceCoordinate The current position of the Bee piece.
     * @return A list of valid movement tiles for the Bee.
     */
    private List<MovementAction> getValidQueenMoves(PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, HexCoordinate pieceCoordinate) {
        List<MovementAction> validMovements = new ArrayList<>();
        // Bee can move one tile in any direction.
        if (hasFreedom(gridCopy, pieceCoordinate)) {
            for (HexCoordinate neighbor : pieceCoordinate.getNeighbors()) {
                if (isValidMove(gridCopy, pieceCoordinate, neighbor)) {
                    validMovements.add(new MovementAction(pieceCoordinate, neighbor));
                }
            }
        }
        // System.out.println("VALID QB MOVES: " + validMovements);
        return validMovements;
    }

    private List<HexCoordinate> getFreeCoords(PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy) {
        List<HexCoordinate> freeCoords = new ArrayList<>();
        for (PMap.Entry<HexCoordinate, PStack<PieceWrapper>> entry : gridCopy.entrySet()) {
            PieceWrapper pieceWrapper = entry.getValue().get(0);
            if (pieceWrapper != null && pieceWrapper.getPiece().getType() == BLANK) {
                freeCoords.add(entry.getKey());
            }
        }
        return freeCoords;
    }

    /**
     * Returns a list of valid movements for the Ant piece from its current position.
     * The Ant can move any number of tiles in a straight line ("sliding").
     *
     * @param pieceCoordinate The current position of the Ant piece.
     * @return A list of valid movement tiles for the Ant.
     */
    private List<MovementAction> getValidAntMoves(PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, HexCoordinate pieceCoordinate) {
        List<MovementAction> validMovements = new ArrayList<>();
        // Ant can move any number of tiles in a straight line ("sliding").
        Queue<HexCoordinate> queue = new LinkedList<>();
        Set<HexCoordinate> visited = new HashSet<>();
        if (hasFreedom(gridCopy, pieceCoordinate)) {

            queue.add(pieceCoordinate);
            visited.add(pieceCoordinate);
            while (!queue.isEmpty()) {
                HexCoordinate coord = queue.poll();
                for (HexCoordinate neighbor : coord.getNeighbors()) {
                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);
                        if (isValidMove(gridCopy, pieceCoordinate, neighbor)) {
                            queue.add(neighbor);
                            validMovements.add(new MovementAction(pieceCoordinate, neighbor));
                        }
                    }
                }

            }


        }
        // System.out.println("VALID ANT MOVES: " + validMovements);
        return validMovements;
    }

    private List<MovementAction> getValidSpiderMoves(PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, HexCoordinate pieceCoordinate) {
        List<MovementAction> validMovements = new ArrayList<>();

        if (!hasFreedom(gridCopy, pieceCoordinate)) {
            return validMovements;
        }

        Set<HexCoordinate> visited = new HashSet<>();
        visited.add(pieceCoordinate);

        spiderDFS(gridCopy, pieceCoordinate, pieceCoordinate, 0, visited, validMovements);
        return validMovements;
    }

    private void spiderDFS(PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy,
                           HexCoordinate from,
                           HexCoordinate current,
                           int depth,
                           Set<HexCoordinate> visited,
                           List<MovementAction> results) {
        if (depth == 3) {
            results.add(new MovementAction(from, current));
            return;
        }

        PieceWrapper pieceWrapper = gridCopy.get(from).get(0);
        gridCopy.plus(from, gridCopy.get(from).minus(pieceWrapper));
        Set<HexCoordinate> commonFreeTiles = getCommonFreeTiles(gridCopy, current);
        gridCopy.plus(from, gridCopy.get(from).plus(pieceWrapper));

        for (HexCoordinate neighbor : commonFreeTiles) {
            if (!visited.contains(neighbor) && isValidMove(gridCopy, from, neighbor)) {
                visited.add(neighbor);
                spiderDFS(gridCopy, from, neighbor, depth + 1, visited, results);
                visited.remove(neighbor);
            }
        }
    }


    private Set<HexCoordinate> getCommonFreeTiles(PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, HexCoordinate coord) {
        Set<HexCoordinate> neighbors = new HashSet<>();
        Set<HexCoordinate> freeTilesSurroundingCoord = new HashSet<>();
        Set<HexCoordinate> commonFreeTiles = new HashSet<>();

        for (HexCoordinate coordinate : coord.getNeighbors()) {
            if (gridCopy.containsKey(coordinate) && gridCopy.get(coordinate) != null && gridCopy.get(coordinate).get(0).getPiece().getType() != BLANK) {
                neighbors.add(coordinate);
            }
        }

        for (HexCoordinate coordinate : coord.getNeighbors()) {
            if (gridCopy.containsKey(coordinate) && gridCopy.get(coordinate) != null && gridCopy.get(coordinate).get(0).getPiece().getType() == BLANK) {
                freeTilesSurroundingCoord.add(coordinate);
            }
        }


        for (HexCoordinate neighbor : neighbors) {
            for (HexCoordinate neighborOfNeighbor : neighbor.getNeighbors()) {
                if (gridCopy.containsKey(neighborOfNeighbor) && gridCopy.get(neighborOfNeighbor) != null && gridCopy.get(neighborOfNeighbor).get(0).getPiece().getType() == BLANK && freeTilesSurroundingCoord.contains(neighborOfNeighbor)) {
                    commonFreeTiles.add(neighborOfNeighbor);
                }
            }
        }

        return commonFreeTiles;

    }

    /**
     * Returns a list of valid movements for the Grasshopper piece from its current position.
     * The Grasshopper must jump over exactly one piece.
     *
     * @param pieceCoordinate The current position of the Grasshopper piece.
     * @return A list of valid movement tiles for the Grasshopper.
     */
    private List<MovementAction> getValidGrasshopperMoves(PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, HexCoordinate pieceCoordinate) {
        List<MovementAction> validMovements = new ArrayList<>();
        // Grasshopper must jump over pieces in a straight line.
        if (hasFreedom(gridCopy, pieceCoordinate)) {
            for (HexCoordinate direction : HexCoordinate.DIRECTIONS) {
                HexCoordinate targetCoordinate = pieceCoordinate.add(direction);
                if (gridCopy.get(targetCoordinate) != null && Objects.requireNonNull(gridCopy.get(targetCoordinate).get(0)).getPiece().getType() != BLANK) {
                    do targetCoordinate = targetCoordinate.add(direction);
                    while (gridCopy.get(targetCoordinate) != null && Objects.requireNonNull(gridCopy.get(targetCoordinate).get(0)).getPiece().getType() != BLANK);
                    if (isValidMove(gridCopy, pieceCoordinate, targetCoordinate))
                        validMovements.add(new MovementAction(pieceCoordinate, targetCoordinate));
                }
            }
        }
        // System.out.println("VALID GRASSHOPPER MOVES: "+ validMovements);
        return validMovements;
    }

    private boolean isFreedomToMoveBeetle(PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, HexCoordinate from, HexCoordinate to) {
        if (gridCopy.get(to).size() == gridCopy.get(from).size() + 1) {
            if (!canSlide(gridCopy, from, to))
                return false;
        }

        return hasFreedomToMove(gridCopy, from, to);
    }

    /**
     * Returns a list of valid movements for the Beetle piece from its current position.
     * The Beetle shares the same movement rules as the Ant piece.
     *
     * @param pieceCoordinate The current position of the Beetle piece.
     * @return A list of valid movement tiles for the Beetle.
     */
    private List<MovementAction> getValidBeetleMoves(PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, HexCoordinate pieceCoordinate) {
        List<MovementAction> validMovements = new ArrayList<>();

        if (hasFreedom(gridCopy, pieceCoordinate)) {
            for (HexCoordinate neighbor : pieceCoordinate.getNeighbors()) {
                // System.out.println("TO: " + neighbor);
                if (isFreedomToMoveBeetle(gridCopy, pieceCoordinate, neighbor) && isValidMove(gridCopy, pieceCoordinate, neighbor))
                    validMovements.add(new MovementAction(pieceCoordinate, neighbor));
            }
        }
        // System.out.println("VALID BEETLE MOVES: "+ validMovements);
        return validMovements;
    }

    public List<MovementAction> getLegalMoves(PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, PieceColor currentTurn) {
        List<MovementAction> legalMoves = new ArrayList<>();
        List<MovementAction> possibleMoves;

        // Set<ImageView> placedPieces = currentTurn == WHITE ? placedWhitePieces : placedBlackPieces;

        for (PMap.Entry<HexCoordinate, PStack<PieceWrapper>> entry : gridCopy.entrySet()) {
            PieceWrapper piece = entry.getValue().get(0);
            if (piece != null && ((currentTurn == WHITE && piece.getPiece().getColor() == WHITE) || (currentTurn == BLACK && piece.getPiece().getColor() == BLACK))) {
                possibleMoves = getValidMoves(gridCopy, entry.getKey());
                legalMoves.addAll(possibleMoves);
            }
        }
        return legalMoves;
    }

    public int countTotalLegalMoves(GameModel gameModel, PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, PieceColor currentTurn) {
        List<MovementAction> legalMoves = getLegalMoves(gridCopy, currentTurn);
        List<PlacementAction> legalPlacements = getValidPlacements(gameModel, gridCopy, currentTurn);
        int numLegalMoves = legalMoves != null ? legalMoves.size() : 0;
        int numLegalPlacements = legalPlacements != null ? legalPlacements.size() : 0;
        return numLegalMoves + numLegalPlacements;
    }

    public int countLegalMovesForPiece(PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, PieceWrapper piece) {
        List<MovementAction> validPieceMovements = getValidMoves(gridCopy, immutableGrid.getKeyByValue(gridCopy, piece));
        return validPieceMovements.size();
    }


}
