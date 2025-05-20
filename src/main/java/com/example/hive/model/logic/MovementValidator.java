package com.example.hive.model.logic;

import static com.example.hive.model.enums.PieceColor.*;
import static com.example.hive.model.enums.PieceType.*;

import com.example.hive.model.grid.Piece;
import com.example.hive.model.grid.PieceWrapper;
import com.example.hive.model.enums.PieceColor;
import com.example.hive.model.enums.PieceType;
import com.example.hive.model.grid.GameModel;
import com.example.hive.model.grid.HexCoordinate;
import com.example.hive.model.grid.ImmutableGrid;
import com.example.hive.model.utils.Pair;
import org.pcollections.ConsPStack;
import org.pcollections.PMap;
import org.pcollections.PStack;

import java.util.*;

/**
 * Validates whether a move or placement in the Hive game adheres to the rules of the hive.
 * This class is used to check whether the hive remains connected after a move or placement.
 */
public class MovementValidator {
    private ImmutableGrid immutableGrid;

    /**
     * Constructs a new MovementValidator with the given immutable grid.
     *
     * @param immutableGrid The immutable grid representing the game state.
     */
    public MovementValidator(ImmutableGrid immutableGrid) {
        this.immutableGrid = immutableGrid;
    }

    /**
     * Sets a new immutable grid for this MovementValidator.
     *
     * @param immutableGrid The new immutable grid.
     */
    public void setImmutableGrid(ImmutableGrid immutableGrid) {
        this.immutableGrid = immutableGrid;
    }

    /**
     * Retrieves a list of valid placement actions for the current player's turn.
     * A placement is valid if the tile is blank and adjacent to another piece of its
     * color but not adjacent to the opponent's pieces.
     *
     * @param gameModel The current game model.
     * @param gridCopy A copy of the game grid.
     * @param currentTurn The current player's turn.
     * @return A list of valid placement actions, or null if no valid placements exist.
     */
    public List<PlacementAction> getValidPlacements(GameModel gameModel, PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, PieceColor currentTurn) {
        boolean stillLeft = gameModel.isStillLeftPanelPiece(currentTurn);
        if (!stillLeft)
            return null;
        List<PlacementAction> placements = new ArrayList<>();
        for (PMap.Entry<HexCoordinate, PStack<PieceWrapper>> entry : gridCopy.entrySet()) {
            HexCoordinate coord = entry.getKey();
            PieceWrapper tile = entry.getValue().get(0);
            assert tile != null;
            if (tile.getPiece().type() == BLANK && isValidPlacement(gameModel, gridCopy, coord, currentTurn)) {
                placements.add(new PlacementAction(coord));
            }
        }

        return placements;
    }

    /**
     * Checks if the placement of a piece at a specific coordinate is valid.
     *
     * @param gameModel The current game model.
     * @param gridCopy A copy of the game grid.
     * @param coord The coordinate of the piece to place.
     * @param currentTurn The current player's turn.
     * @return True if the placement is valid, false otherwise.
     */
    public boolean isValidPlacement(GameModel gameModel, PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, HexCoordinate coord, PieceColor currentTurn) {
        // The coordinate must exist in the grid.
        if (!gridCopy.containsKey(coord)) {
            return false;
        }
        PieceWrapper tile = gridCopy.get(coord).get(0);
        // The tile must be blank.
        if (tile.getPiece().type() != BLANK) {
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
                if (!neighborStack.isEmpty() && neighborStack.get(0).getPiece().type() != BLANK) {
                    hasAdjacentPiece = true;

                    PieceColor neighborColor = neighborStack.get(0).getPiece().color();
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
     * The valid targets depend on the piece's type and movement rules.
     *
     * @param gridCopy A copy of the game grid.
     * @param from The coordinate of the piece to move.
     * @return A list of valid movement actions for the piece at the given coordinate.
     */
    public List<MovementAction> getValidMoves(PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, HexCoordinate from) {
        PieceWrapper wrapper = gridCopy.get(from).get(0);
        if (wrapper == null || wrapper.getPiece() == null) return Collections.emptyList();
        Piece piece = wrapper.getPiece();
        return switch (piece.type()) {
            case QUEEN_BEE -> getValidQueenMoves(gridCopy, from);
            case ANT -> getValidAntMoves(gridCopy, from);
            case SPIDER -> getValidSpiderMoves(gridCopy, from);
            case GRASSHOPPER -> getValidGrasshopperMoves(gridCopy, from);
            case BEETLE -> getValidBeetleMoves(gridCopy, from);
            default -> Collections.emptyList();
        };
    }

    /**
     * Checks whether a piece at a given coordinate can be removed from the grid.
     *
     * @param gridCopy A copy of the game grid.
     * @param paramCoords The coordinates of the piece to check.
     * @return True if the piece can be removed, false otherwise.
     */
    private boolean canRemove(PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, HexCoordinate paramCoords) {
        int numPieces = gridCopy.get(paramCoords).size();
        return numPieces > 1;
    }

    /**
     * Checks whether a piece at a given coordinate has freedom to move according to the game's rules.
     *
     * @param gridCopy A copy of the game grid.
     * @param coord The coordinates of the piece to check.
     * @return True if the piece has freedom to move, false otherwise.
     */
    private boolean hasFreedom(PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, HexCoordinate coord) {
        return switch (gridCopy.get(coord).get(0).getPiece().type()) {
            case BEETLE, GRASSHOPPER -> canRemove(gridCopy, coord);
            case QUEEN_BEE, SPIDER, ANT -> (immutableGrid.countNeighbours(gridCopy, coord) <= 4) && canRemove(gridCopy, coord);
            default -> false;
        };
    }

    /**
     * Checks if a piece can slide to a given target coordinate.
     * The sliding piece must not collide with other pieces and should have a valid path.
     *
     * @param gridCopy A copy of the game grid.
     * @param from The current position of the piece.
     * @param to The target position.
     * @return True if the piece can slide to the target coordinate, false otherwise.
     */
    private boolean canSlide(PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, HexCoordinate from, HexCoordinate to) {
        if (from.equals(to)) return false;  // Can't slide to the same tile.

        Set<HexCoordinate> visited = new HashSet<>();
        Queue<HexCoordinate> queue = new LinkedList<>();

        queue.add(from);
        visited.add(from);

        while (!queue.isEmpty()) {
            HexCoordinate current = queue.poll();

            for (HexCoordinate neighbor : current.getNeighbors()) {
                if (!visited.contains(neighbor) && gridCopy.containsKey(neighbor) && gridCopy.get(neighbor).get(0).getPiece().type() == BLANK) {
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

    /**
     * Checks whether the hive remains connected after a piece is removed or moved.
     *
     * @param gameGrid A copy of the game grid.
     * @param from The current position of the piece.
     * @param to The target position of the piece.
     * @return True if the hive remains connected after the move, false otherwise.
     */
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
     * @param gridCopy A copy of the game grid.
     * @return True if the entire hive is connected, otherwise false.
     */
    // Very similar to Graph Theory, when finding connectivity components (DFS/BFS).
    // The graph of the Hive grid, must be one connectivity component at all time.
    private boolean checkHiveConnectivity(PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy) {
        Queue<PieceWrapper> queue;
        Set<HexCoordinate> visited = new HashSet<>();
        List<HexCoordinate> toVisit = new ArrayList<>();
        HexCoordinate startingCoordinate = null;

        Iterator<PMap.Entry<HexCoordinate, PStack<PieceWrapper>>> iterator = gridCopy.entrySet().iterator();
        while (iterator.hasNext() && startingCoordinate == null) {
            PMap.Entry<HexCoordinate, PStack<PieceWrapper>> entry = iterator.next();
            PieceWrapper pieceWrapper = entry.getValue().get(0);
            if (pieceWrapper != null && pieceWrapper.getPiece().type() != BLANK)
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


                    if (neighborTile != null && neighborTile.getPiece().type() != BLANK) {
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

    /**
     * Checks if a move is valid based on the grid state and movement rules.
     *
     * @param gridCopy A copy of the current game grid.
     * @param from The current position of the piece.
     * @param to The target position to move to.
     * @return True if the move is valid, false otherwise.
     */
    public boolean isValidMove(PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, HexCoordinate from, HexCoordinate to) {
        PieceType pieceType = gridCopy.get(from).get(0).getPiece().type();;
        if (gridCopy.containsKey(to)) {
            if (pieceType != BEETLE && gridCopy.get(to).get(0).getPiece().type() != BLANK) {
                return false;
            }
        }

        PStack<PieceWrapper> fromStack = gridCopy.get(from);
        PStack<PieceWrapper> toStack = gridCopy.get(to);
        if (fromStack == null || fromStack.isEmpty() || toStack == null || toStack.isEmpty()) {
            return false;
        }

        if (pieceType != BEETLE && pieceType != GRASSHOPPER && !canSlide(gridCopy, from, to)) {
            return false;
        }
        if (!hasFreedomToMove(gridCopy, from, to)) {
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
        return validMovements;
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
        return validMovements;
    }

    /**
     * Returns a list of valid Spider moves for a given position.
     *
     * @param gridCopy A copy of the game grid.
     * @param pieceCoordinate The current position of the Spider.
     * @return A list of valid Spider movement actions.
     */
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

    /**
     * Performs a depth-first search to find all possible movement actions for a Spider.
     *
     * @param gridCopy A copy of the current game grid.
     * @param from The current position of the Spider.
     * @param current The current position being explored.
     * @param depth The current depth of the search.
     * @param visited A set of visited coordinates to avoid revisiting.
     * @param results A list to store the found movement actions.
     */
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

    /**
     * Returns the common free tiles around the surrounding neighbors of a coordinate.
     *
     * @param gridCopy A copy of the current game grid.
     * @param coord The coordinate to check surrounding neighbors.
     * @return A set of common free tiles around the neighbors of the coordinate.
     */
    private Set<HexCoordinate> getCommonFreeTiles(PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, HexCoordinate coord) {
        Set<HexCoordinate> neighbors = new HashSet<>();
        Set<HexCoordinate> freeTilesSurroundingCoord = new HashSet<>();
        Set<HexCoordinate> commonFreeTiles = new HashSet<>();

        for (HexCoordinate coordinate : coord.getNeighbors()) {
            if (gridCopy.containsKey(coordinate) && gridCopy.get(coordinate) != null && gridCopy.get(coordinate).get(0).getPiece().type() != BLANK) {
                neighbors.add(coordinate);
            }
        }

        for (HexCoordinate coordinate : coord.getNeighbors()) {
            if (gridCopy.containsKey(coordinate) && gridCopy.get(coordinate) != null && gridCopy.get(coordinate).get(0).getPiece().type() == BLANK) {
                freeTilesSurroundingCoord.add(coordinate);
            }
        }


        for (HexCoordinate neighbor : neighbors) {
            for (HexCoordinate neighborOfNeighbor : neighbor.getNeighbors()) {
                if (gridCopy.containsKey(neighborOfNeighbor) && gridCopy.get(neighborOfNeighbor) != null && gridCopy.get(neighborOfNeighbor).get(0).getPiece().type() == BLANK && freeTilesSurroundingCoord.contains(neighborOfNeighbor)) {
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
                if (gridCopy.get(targetCoordinate) != null && Objects.requireNonNull(gridCopy.get(targetCoordinate).get(0)).getPiece().type() != BLANK) {
                    do targetCoordinate = targetCoordinate.add(direction);
                    while (gridCopy.get(targetCoordinate) != null && Objects.requireNonNull(gridCopy.get(targetCoordinate).get(0)).getPiece().type() != BLANK);
                    if (isValidMove(gridCopy, pieceCoordinate, targetCoordinate))
                        validMovements.add(new MovementAction(pieceCoordinate, targetCoordinate));
                }
            }
        }
        return validMovements;
    }

    /**
     * Checks if the source coordinate from a move is pinning an opponent's piece (restricting its mobility) that
     * is surrounding the opponent's queen.
     *
     * @param gridCopy A copy of the current game grid.
     * @param move The move to check.
     * @param queenNeighbors The neighboring coordinates surrounding the opponent's Queen.
     * @return True if the move is pinning an opponent's piece, false otherwise.
     */
    public boolean isPinning(PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, MoveAction move, Set<HexCoordinate> queenNeighbors) {
        if (!move.isPlacement()) {
            if (queenNeighbors.contains(((MovementAction) move).getFrom()) && gridCopy.get(((MovementAction) move).getFrom()).size() > 1)
                return true;
            for (HexCoordinate neighbor : ((MovementAction) move).getFrom().getNeighbors()) {
                if (gridCopy.containsKey(neighbor)) {
                    // Piece at neighbor is directly surrounding the queen.
                    if (queenNeighbors.contains(neighbor)) {
                        // Connectivity rule: test if moving 'move.getFrom()' disconnects the neighbor.
                        if (gridCopy.get(neighbor).get(0).getPiece().type() != BLANK) {
                            if (!testConnectivityWithout(gridCopy, neighbor)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Tests if the grid remains connected after removing a specific piece.
     *
     * @param gridCopy A copy of the current game grid.
     * @param toRemove The coordinate of the piece to remove.
     * @return True if the grid remains connected after removal, false otherwise.
     */
    private boolean testConnectivityWithout(PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, HexCoordinate toRemove) {
        PMap<HexCoordinate, PStack<PieceWrapper>> modifiedGrid = gridCopy;
        modifiedGrid = modifiedGrid.minus(toRemove);

        Optional<HexCoordinate> start = modifiedGrid.keySet().stream().findFirst();
        if (start.isEmpty()) return true; // empty grid is trivially connected.

        Set<HexCoordinate> visited = new HashSet<>();
        Deque<HexCoordinate> stack = new ArrayDeque<>();
        stack.push(start.get());

        while (!stack.isEmpty()) {
            HexCoordinate current = stack.pop();
            if (visited.add(current)) {
                for (HexCoordinate neighbor : current.getNeighbors()) {
                    if (modifiedGrid.containsKey(neighbor) && !visited.contains(neighbor)) {
                        stack.push(neighbor);
                    }
                }
            }
        }

        return visited.size() == modifiedGrid.size();
    }

    /**
     * Checks if a Beetle has freedom to move to the target position.
     *
     * @param gridCopy A copy of the current game grid.
     * @param from The current position of the Beetle.
     * @param to The target position to check.
     * @return True if the Beetle can move to the target, false otherwise.
     */
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
                if (isFreedomToMoveBeetle(gridCopy, pieceCoordinate, neighbor) && isValidMove(gridCopy, pieceCoordinate, neighbor))
                    validMovements.add(new MovementAction(pieceCoordinate, neighbor));
            }
        }
        return validMovements;
    }

    /**
     * Returns a list of legal moves for the current turn, based on the grid state.
     *
     * @param gridCopy A copy of the current game grid.
     * @param currentTurn The color of the current player (WHITE or BLACK).
     * @return A list of legal movement actions for the current player.
     */
    public List<MovementAction> getLegalMoves(PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, PieceColor currentTurn) {
        List<MovementAction> legalMoves = new ArrayList<>();
        List<MovementAction> possibleMoves;

        for (PMap.Entry<HexCoordinate, PStack<PieceWrapper>> entry : gridCopy.entrySet()) {
            PieceWrapper piece = entry.getValue().get(0);
            if (piece != null && ((currentTurn == WHITE && piece.getPiece().color() == WHITE) || (currentTurn == BLACK && piece.getPiece().color() == BLACK))) {
                possibleMoves = getValidMoves(gridCopy, entry.getKey());
                legalMoves.addAll(possibleMoves);
            }
        }
        return legalMoves;
    }

    /**
     * Counts the total number of legal moves and placements for the current player.
     *
     * @param gameModel The current game model.
     * @param gridCopy A copy of the current game grid.
     * @param currentTurn The color of the current player (WHITE or BLACK).
     * @return The total number of legal moves and placements.
     */
    public int countTotalLegalMoves(GameModel gameModel, PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, PieceColor currentTurn) {
        List<MovementAction> legalMoves = getLegalMoves(gridCopy, currentTurn);
        List<PlacementAction> legalPlacements = getValidPlacements(gameModel, gridCopy, currentTurn);
        int numLegalMoves = legalMoves != null ? legalMoves.size() : 0;
        int numLegalPlacements = legalPlacements != null ? legalPlacements.size() : 0;
        return numLegalMoves + numLegalPlacements;
    }

    /**
     * Computes all hypothetical placement-and-move destinations for each piece type at a given coordinate.
     * <p>This method simulates placing a piece of each non-BLANK type on the specified coordinate
     * and then determines all legal moves that originate from that coordinate on the resulting grid.
     * It returns a set of (PieceType, destination) pairs representing where each piece could move.</p>
     *
     * @param gameModel the current game model providing grid state and move rules
     * @param coordinate the hex coordinate where the hypothetical piece is placed and moved from
     * @param color the color of the piece being tested
     * @return a set of pairs, each containing a PieceType and a HexCoordinate to which that piece
     *         could legally move if it were placed at the given coordinate
     */
    public Set<Pair<PieceType, HexCoordinate>> getHypotheticDestinationsFrom (GameModel gameModel, HexCoordinate coordinate, PieceColor color) {
        Set<Pair<PieceType, HexCoordinate>> hypotheticDestinationsSet = new HashSet<>();
        PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy;
        for (PieceType pieceType : PieceType.values()) {
            if (pieceType != BLANK) {
                gridCopy = gameModel.getGrid();
                PieceWrapper pieceWrapper = new PieceWrapper(new Piece(pieceType, color));
                PStack<PieceWrapper> stack = gridCopy.get(coordinate).plus(pieceWrapper);
                gridCopy = gridCopy.plus(coordinate, stack);

                List<HexCoordinate> neighborsToAdd = new ArrayList<>();
                Set<HexCoordinate> neighboors = coordinate.getNeighbors();
                for (HexCoordinate neighbor : neighboors) {
                    if (!gridCopy.containsKey(neighbor)) {
                        neighborsToAdd.add(neighbor);
                    }
                }

                for (HexCoordinate neighbor : neighborsToAdd) {
                    stack = ConsPStack.empty();
                    stack = stack.plus(new PieceWrapper(new Piece(BLANK, NONE)));
                    gridCopy = gridCopy.plus(neighbor, stack);
                }

                List<MovementAction> legalMoves = getLegalMoves(gridCopy, color);
                for (MovementAction movementAction : legalMoves) {
                    if (movementAction.getFrom() == coordinate) {
                        hypotheticDestinationsSet.add(new Pair<>(pieceType, movementAction.getTo()));
                    }
                }
            }
        }
        return hypotheticDestinationsSet;
    }

}
