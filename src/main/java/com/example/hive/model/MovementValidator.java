package com.example.hive.model;

import static com.example.hive.model.PieceColor.*;
import static com.example.hive.model.PieceImage.*;
import static com.example.hive.model.PieceType.*;
import com.example.hive.model.Pair;

import java.util.*;

/**
 * Encapsulates the movement rules for pieces. This validator uses the Board state to compute valid moves.
 */
public class MovementValidator {
    private final Grid grid;

    public MovementValidator(Grid grid) {
        this.grid = grid;
    }


    /**
     * Returns a list of ImageViews representing valid placements on the grid.
     *
     * @return A list of ImageViews representing valid placements.
     */
    public List<PlacementAction> getValidPlacements(GameModel gameModel, PieceColor currentTurn) {
        return getValidPlacements(gameModel, grid.getGrid(), currentTurn);
    }

    public List<PlacementAction> getValidPlacements(GameModel gameModel, Map<HexCoordinate, Deque<PieceWrapper>> gridCopy, PieceColor currentTurn) {
        boolean stillLeft = gameModel.isStillLeftPanelPiece(currentTurn);
        if (!stillLeft)
            return null;
        List<PlacementAction> placements = new ArrayList<>();
        for (Map.Entry<HexCoordinate, Deque<PieceWrapper>> entry : gridCopy.entrySet()) {
            HexCoordinate coord = entry.getKey();
            PieceWrapper tile = entry.getValue().peek();
            assert tile != null;
            if (tile.getPiece().getType() == BLANK && isValidPlacement(gameModel, gridCopy, coord, currentTurn)) {
                placements.add(new PlacementAction(coord));
            }
        }

        return placements;
    }

    public boolean isValidPlacement(GameModel gameModel, Map<HexCoordinate, Deque<PieceWrapper>> gridCopy, HexCoordinate coord, PieceColor currentTurn) {
        // The coordinate must exist in the grid.
        if (!gridCopy.containsKey(coord)) {
            return false;
        }
        PieceWrapper tile = gridCopy.get(coord).peek();
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
                Deque<PieceWrapper> neighborStack = gridCopy.get(neighbor);
                if (!neighborStack.isEmpty() && neighborStack.peek().getPiece().getType() != BLANK) {
                    hasAdjacentPiece = true;

                    PieceColor neighborColor = neighborStack.peek().getPiece().getColor();
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
    public List<MovementAction> getValidMoves(HexCoordinate from) {
        return getValidMoves(grid.getGrid(), from);
    }


    /**
     * Returns valid target coordinates for the piece at the given coordinate.
     */
    public List<MovementAction> getValidMoves(Map<HexCoordinate, Deque<PieceWrapper>> gridCopy, HexCoordinate from) {
        PieceWrapper wrapper = gridCopy.get(from).peek();
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

    private boolean canRemove(Map<HexCoordinate, Deque<PieceWrapper>> gridCopy, HexCoordinate paramCoords) {
        int numPieces = gridCopy.get(paramCoords).size();
        return numPieces > 1;
    }

    private boolean hasFreedom(Map<HexCoordinate, Deque<PieceWrapper>> gridCopy, HexCoordinate coord) {
        return switch (gridCopy.get(coord).peek().getPiece().getType()) {
            case BEETLE, GRASSHOPPER -> canRemove(gridCopy, coord);
            case QUEEN_BEE, SPIDER, ANT -> (grid.countNeighbours(gridCopy, coord) <= 4) && canRemove(gridCopy, coord);
            default -> false;
        };
    }

    private boolean canSlide(Map<HexCoordinate, Deque<PieceWrapper>> gridCopy, HexCoordinate from, HexCoordinate to) {
        if (from.equals(to)) return false;  // Can't slide to the same tile.

        Set<HexCoordinate> visited = new HashSet<>();
        Queue<HexCoordinate> queue = new LinkedList<>();

        queue.add(from);
        visited.add(from);

        while (!queue.isEmpty()) {
            HexCoordinate current = queue.poll();

            for (HexCoordinate neighbor : current.getNeighbors()) {
                if (!visited.contains(neighbor) && gridCopy.containsKey(neighbor) && gridCopy.get(neighbor).peek().getPiece().getType() == BLANK) {
                    queue.add(neighbor);
                    visited.add(neighbor);

                    if (neighbor.equals(to)) return true;  // Found a valid sliding path
                }
            }
        }
        return false;  // No valid path found
    }

    private boolean hasFreedomToMove(Map<HexCoordinate, Deque<PieceWrapper>> gridCopy, HexCoordinate from, HexCoordinate to) {
        return isConnectedAfterRemoval(gridCopy, from, to);
    }

    private boolean isConnectedAfterRemoval(Map<HexCoordinate, Deque<PieceWrapper>> gameGrid, HexCoordinate from, HexCoordinate to) {
        Map<HexCoordinate, Deque<PieceWrapper>> gridCopy = grid.getGrid(gameGrid);

        Deque<PieceWrapper> stack = gridCopy.get(from);
        PieceWrapper piece = stack.pop();

        boolean pieceRemoved = checkHiveConnectivity(gridCopy);

        stack = gridCopy.get(to);
        stack.push(piece);
        boolean pieceMoved = checkHiveConnectivity(gridCopy);

        boolean pieceHasFreedom = hasFreedom(gridCopy, to);

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
    private boolean checkHiveConnectivity(Map<HexCoordinate, Deque<PieceWrapper>> gridCopy) {
        Queue<PieceWrapper> queue = new LinkedList<>();
        Set<HexCoordinate> visited = new HashSet<>();
        List<HexCoordinate> toVisit = new ArrayList<>();
        HexCoordinate startingCoordinate = null;

        for (Map.Entry<HexCoordinate, Deque<PieceWrapper>> entry : gridCopy.entrySet()) {
            PieceWrapper pieceWrapper = entry.getValue().peek();
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
                    PieceWrapper neighborTile = gridCopy.get(neighbor).peek();
                    queue = new LinkedList<>();


                    if (neighborTile != null && neighborTile.getPiece().getType() != BLANK) {
                        toVisit.add(neighbor);
                        visited.add(neighbor);

                        queue.add(gridCopy.get(neighbor).pop());
                        while (gridCopy.get(neighbor).size() > 1) {
                            visited.add(neighbor);
                            queue.add(gridCopy.get(neighbor).pop());
                        }
                        while (!(queue.isEmpty())) {
                            gridCopy.get(neighbor).push(queue.poll());
                        }
                    }
                }
            }
        }

        // Check if all pieces are visited (meaning they are all connected).
        return visited.size() == grid.getTotalOccupiedTiles(gridCopy);
    }

    public boolean isValidMove(Map<HexCoordinate, Deque<PieceWrapper>> gridCopy, HexCoordinate from, HexCoordinate to) {
        // System.out.println("TO: " + to);
        PieceType pieceType = gridCopy.get(from).peek().getPiece().getType();;
        if (gridCopy.containsKey(to)) {
            if (pieceType != BEETLE && gridCopy.get(to).peek().getPiece().getType() != BLANK) {
                // System.out.println(223);
                return false;
            }
        }

        Deque<PieceWrapper> fromStack = gridCopy.get(from);
        Deque<PieceWrapper> toStack = gridCopy.get(to);
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
    private List<MovementAction> getValidQueenMoves(Map<HexCoordinate, Deque<PieceWrapper>> gridCopy, HexCoordinate pieceCoordinate) {
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

    private List<HexCoordinate> getFreeCoords(Map<HexCoordinate, Deque<PieceWrapper>> gridCopy) {
        List<HexCoordinate> freeCoords = new ArrayList<>();
        for (Map.Entry<HexCoordinate, Deque<PieceWrapper>> entry : gridCopy.entrySet()) {
            PieceWrapper pieceWrapper = entry.getValue().peek();
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
    private List<MovementAction> getValidAntMoves(Map<HexCoordinate, Deque<PieceWrapper>> gridCopy, HexCoordinate pieceCoordinate) {
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

    private List<MovementAction> getValidSpiderMoves(Map<HexCoordinate, Deque<PieceWrapper>> gridCopy, HexCoordinate pieceCoordinate) {
        List<MovementAction> validMovements = new ArrayList<>();

        if (!hasFreedom(gridCopy, pieceCoordinate)) {
            return validMovements;
        }

        Set<HexCoordinate> visited = new HashSet<>();
        visited.add(pieceCoordinate);

        spiderDFS(gridCopy, pieceCoordinate, pieceCoordinate, 0, visited, validMovements);
        return validMovements;
    }

    private void spiderDFS(Map<HexCoordinate, Deque<PieceWrapper>> gridCopy,
                           HexCoordinate from,
                           HexCoordinate current,
                           int depth,
                           Set<HexCoordinate> visited,
                           List<MovementAction> results) {
        if (depth == 3) {
            results.add(new MovementAction(from, current));
            return;
        }

        PieceWrapper pieceWrapper = gridCopy.get(from).pop();
        Set<HexCoordinate> commonFreeTiles = getCommonFreeTiles(gridCopy, current);
        gridCopy.get(from).push(pieceWrapper);

        for (HexCoordinate neighbor : commonFreeTiles) {
            if (!visited.contains(neighbor) && isValidMove(gridCopy, from, neighbor)) {
                visited.add(neighbor);
                spiderDFS(gridCopy, from, neighbor, depth + 1, visited, results);
                visited.remove(neighbor);
            }
        }
    }



    private Set<HexCoordinate> getCommonFreeTiles(Map<HexCoordinate, Deque<PieceWrapper>> gridCopy, HexCoordinate coord) {
        Set<HexCoordinate> neighbors = new HashSet<>();
        Set<HexCoordinate> freeTilesSurroundingCoord = new HashSet<>();
        Set<HexCoordinate> commonFreeTiles = new HashSet<>();

        for (HexCoordinate coordinate : coord.getNeighbors()) {
            if (gridCopy.containsKey(coordinate) && gridCopy.get(coordinate) != null && gridCopy.get(coordinate).peek().getPiece().getType() != BLANK) {
                neighbors.add(coordinate);
            }
            else if (gridCopy.containsKey(coordinate) && gridCopy.get(coordinate) != null && gridCopy.get(coordinate).peek().getPiece().getType() == BLANK) {
                freeTilesSurroundingCoord.add(coordinate);
            }
        }

        for (HexCoordinate neighbor : neighbors) {
            for (HexCoordinate neighborOfNeighbor : neighbor.getNeighbors()) {
                if (gridCopy.containsKey(neighborOfNeighbor) && gridCopy.get(neighborOfNeighbor) != null && gridCopy.get(neighborOfNeighbor).peek().getPiece().getType() == BLANK && freeTilesSurroundingCoord.contains(neighborOfNeighbor)) {
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
    private List<MovementAction> getValidGrasshopperMoves(Map<HexCoordinate, Deque<PieceWrapper>> gridCopy, HexCoordinate pieceCoordinate) {
        List<MovementAction> validMovements = new ArrayList<>();
        // Grasshopper must jump over pieces in a straight line.
        if (hasFreedom(gridCopy, pieceCoordinate)) {
            for (HexCoordinate direction : HexCoordinate.DIRECTIONS) {
                HexCoordinate targetCoordinate = pieceCoordinate.add(direction);
                if (gridCopy.get(targetCoordinate) != null && Objects.requireNonNull(gridCopy.get(targetCoordinate).peek()).getPiece().getType() != BLANK) {
                    do targetCoordinate = targetCoordinate.add(direction);
                    while (gridCopy.get(targetCoordinate) != null && Objects.requireNonNull(gridCopy.get(targetCoordinate).peek()).getPiece().getType() != BLANK);
                    if (isValidMove(gridCopy, pieceCoordinate, targetCoordinate))
                        validMovements.add(new MovementAction(pieceCoordinate, targetCoordinate));
                }
            }
        }
        System.out.println("VALID GRASSHOPPER MOVES: "+ validMovements);
        return validMovements;
    }

    private boolean isFreedomToMoveBeetle(Map<HexCoordinate, Deque<PieceWrapper>> gridCopy, HexCoordinate from, HexCoordinate to) {
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
    private List<MovementAction> getValidBeetleMoves(Map<HexCoordinate, Deque<PieceWrapper>> gridCopy, HexCoordinate pieceCoordinate) {
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


    public List<MovementAction> getLegalMoves(PieceColor currentTurn) {
        return getLegalMoves(grid.getGrid(), currentTurn);
    }

    public List<MovementAction> getLegalMoves(Map<HexCoordinate, Deque<PieceWrapper>> gridCopy, PieceColor currentTurn) {
        List<MovementAction> legalMoves = new ArrayList<>();
        List<MovementAction> possibleMoves;

        // Set<ImageView> placedPieces = currentTurn == WHITE ? placedWhitePieces : placedBlackPieces;

        for (Map.Entry<HexCoordinate, Deque<PieceWrapper>> entry : gridCopy.entrySet()) {
            PieceWrapper piece = entry.getValue().peek();
            if (piece != null && ((currentTurn == WHITE && piece.getPiece().getColor() == WHITE) || (currentTurn == BLACK && piece.getPiece().getColor() == BLACK))) {
                possibleMoves = getValidMoves(gridCopy, entry.getKey());
                legalMoves.addAll(possibleMoves);
            }
        }
        return legalMoves;
    }

    public int countTotalLegalMoves(GameModel gameModel, Map<HexCoordinate, Deque<PieceWrapper>> gridCopy, PieceColor currentTurn) {
        List<MovementAction> legalMoves = getLegalMoves(gridCopy, currentTurn);
        List<PlacementAction> legalPlacements = getValidPlacements(gameModel, gridCopy, currentTurn);
        int numLegalMoves = legalMoves != null ? legalMoves.size() : 0;
        int numLegalPlacements = legalPlacements != null ? legalPlacements.size() : 0;
        return numLegalMoves + numLegalPlacements;
    }

    public int countLegalMovesForPiece(Map<HexCoordinate, Deque<PieceWrapper>> gridCopy, PieceWrapper piece) {
        List<MovementAction> validPieceMovements = getValidMoves(grid.getKeyByValue(gridCopy, piece));
        return validPieceMovements.size();
    }


}
