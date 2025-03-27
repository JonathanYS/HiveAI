package com.example.hive.model;

import javafx.geometry.Point2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.text.DecimalFormat;
import java.util.*;

/**
 * The Grid class represents the game board for Hive. It manages the placement and movement of pieces,
 * checking for connectivity, and determining the game's state (such as turns, valid moves, and win conditions).
 */
public class Grid {

    private final Map<HexCoordinate, Deque<ImageView>> grid = new HashMap<>();
    private Map<Boolean, Integer> moveCount;
    private Map<Boolean, Boolean> queenPlaced;
    private Map<PieceImage, Integer> piecesCount;  // Track counts of each piece type.

    private boolean isWhiteTurn = true; // White's turn.

    /**
     * Constructs a Grid instance and initializes the game board with starting pieces.
     */
    public Grid() {
        Deque<ImageView> stack = new ArrayDeque<>();
        stack.push(new ImageView(PieceImage.BLANK_TILE.getImage()));
        grid.put(new HexCoordinate(0, 0), stack);
        piecesCount = new HashMap<>();
        piecesCount.put(PieceImage.GRASSHOPPER_BLACK, 3);
        piecesCount.put(PieceImage.GRASSHOPPER_WHITE, 3);
        piecesCount.put(PieceImage.BEETLE_BLACK, 2);
        piecesCount.put(PieceImage.BEETLE_WHITE, 2);
        piecesCount.put(PieceImage.QUEEN_BEE_BLACK, 1);
        piecesCount.put(PieceImage.QUEEN_BEE_WHITE, 1);
        piecesCount.put(PieceImage.SPIDER_BLACK, 2);
        piecesCount.put(PieceImage.SPIDER_WHITE, 2);
        piecesCount.put(PieceImage.ANT_BLACK, 3);
        piecesCount.put(PieceImage.ANT_WHITE, 3);

        moveCount = new HashMap<>();
        queenPlaced = new HashMap<>();
        moveCount.put(true, 0);
        moveCount.put(false, 0);
        queenPlaced.put(true, false);
        queenPlaced.put(false, false);
    }

    /**
     * Returns the current grid of the game.
     *
     * @return A map of HexCoordinates to ImageViews representing the grid.
     */
    public Map<HexCoordinate, Deque<ImageView>> getGrid() {
        return grid;
    }

    /**
     * Returns a list of ImageViews representing valid placements on the grid.
     *
     * @return A list of ImageViews representing valid placements.
     */
    public List<ImageView> getValidPlacements() {
        List<ImageView> placements = new ArrayList<>();
        for (Map.Entry<HexCoordinate, Deque<ImageView>> entry : grid.entrySet()) {
            HexCoordinate coord = entry.getKey();
            ImageView tile = entry.getValue().peek();
            assert tile != null;
            if (tile.getImage().equals(PieceImage.BLANK_TILE.getImage()) && isValidPlacement(coord, isWhiteTurn)) {
                placements.add(tile);
            }
        }
        return placements;
    }

    /**
     * Helper method to get the key (HexCoordinate) associated with a given value (ImageView) from a map.
     *
     * @param map   The map to search through.
     * @param value The value to search for.
     * @return The key associated with the value, or null if not found.
     */
    private <K, V> K getKeyByValue(Map<K, Deque<V>> map, V value) {
        for (Map.Entry<K, Deque<V>> entry : map.entrySet()) {
            assert entry.getValue().peek() != null;
            if (entry.getValue().peek().equals(value)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Returns a list of valid movements for a given piece on the grid.
     *
     * @param piece The ImageView representing the piece.
     * @return A list of valid movements as ImageViews.
     */
    public List<ImageView> getValidMovements(ImageView piece) {
        List<ImageView> validMovements = new ArrayList<>();
        HexCoordinate pieceCoordinate = getKeyByValue(grid, piece);
        PieceImage pieceImage = getPieceTypeFromImageView(piece);

        switch (Objects.requireNonNull(pieceImage)) {
            case QUEEN_BEE_BLACK:
            case QUEEN_BEE_WHITE:
                validMovements.addAll(getValidBeeMovements(pieceCoordinate));
                break;
            case SPIDER_BLACK:
            case SPIDER_WHITE:
                validMovements.addAll(getValidSpiderMovements(pieceCoordinate));
                break;
            case ANT_BLACK:
            case ANT_WHITE:
                validMovements.addAll(getValidAntMovements(pieceCoordinate));
                break;
            case GRASSHOPPER_BLACK:
            case GRASSHOPPER_WHITE:
                validMovements.addAll(getValidGrasshopperMovements(pieceCoordinate));
                break;
            case BEETLE_BLACK:
            case BEETLE_WHITE:
                validMovements.addAll(getValidBeetleMovements(pieceCoordinate));
                break;
            default:
                break;
        }
        return validMovements;
    }

    public boolean isValidMove(HexCoordinate from, HexCoordinate to) {
        if (grid.containsKey(to) && grid.get(to).peek().getImage() != PieceImage.BLANK_TILE.getImage()) {
            System.out.println(131);
            return false;
        }

        Deque<ImageView> fromStack = grid.get(from);
        Deque<ImageView> toStack = grid.get(to);
        if (fromStack == null || fromStack.isEmpty() || toStack == null || toStack.isEmpty()) {
            System.out.println(138);
            return false;
        }

        // ImageView movingPiece = fromStack.peek();
        // boolean isWhite = movingPiece.getId().contains("white");

        if (!canSlide(from, to)) {
            System.out.println(to);
            System.out.println(146);
            return false;
        }

        if (!hasFreedomToMove(from, to)) {
            System.out.println(151);
            return false;
        }

        return true;
    }

    private boolean canSlide(HexCoordinate from, HexCoordinate to) {
        if (from.equals(to)) return false;  // Can't slide to the same tile.

        Set<HexCoordinate> visited = new HashSet<>();
        Queue<HexCoordinate> queue = new LinkedList<>();

        queue.add(from);
        visited.add(from);

        while (!queue.isEmpty()) {
            HexCoordinate current = queue.poll();

            for (HexCoordinate neighbor : current.getNeighbors()) {
                if (!visited.contains(neighbor) && grid.containsKey(neighbor) && grid.get(neighbor).peek().getImage() == PieceImage.BLANK_TILE.getImage()) {
                    queue.add(neighbor);
                    visited.add(neighbor);

                    if (neighbor.equals(to)) return true;  // Found a valid sliding path
                }
            }
        }
        return false;  // No valid path found
    }


    private boolean hasFreedomToMove(HexCoordinate from, HexCoordinate to) {
        return isConnectedAfterRemoval(from, to);
    }

    private boolean isFreedomToMoveBeetle(HexCoordinate from, HexCoordinate to) {
        if (grid.get(to).size() == grid.get(from).size() + 1) {
            if (!canSlide(from, to))
                return false;
        }

        return hasFreedomToMove(from, to);
    }

    private boolean isConnectedAfterRemoval(HexCoordinate from, HexCoordinate to) {
        Map<HexCoordinate, Deque<ImageView>> gridCopy = new HashMap<>(grid);
        for (Map.Entry<HexCoordinate, Deque<ImageView>> entry : grid.entrySet()) {
            gridCopy.put(entry.getKey(), new ArrayDeque<>(entry.getValue())); // Ensures a deep copy of stacks.
        }

        // System.out.println(PieceImage.BLANK_TILE.getImage());
        // System.out.println("STACK BEFORE POP: " + gridCopy.get(from).peek().getImage());
        Deque<ImageView> stack = gridCopy.get(from);
        ImageView piece = stack.pop();
        // System.out.println("STACK AFTER POP: " + gridCopy.get(from));

        boolean pieceRemoved = checkHiveConnectivity(gridCopy);

        stack = gridCopy.get(to);
        stack.push(piece);
        boolean pieceMoved = checkHiveConnectivity(gridCopy);

        boolean pieceHasFreedom = hasFreedom(gridCopy, to);

        return pieceRemoved && pieceMoved && pieceHasFreedom;
    }


    private boolean canRemove(Map<HexCoordinate, Deque<ImageView>> gridCopy, HexCoordinate paramCoords) {
        int numPieces = gridCopy.get(paramCoords).size();
        return numPieces > 1;
        // if (!mustCheckIntegrity(paramCoords))
        //    return true;
        // return checkIntegrity(paramCoords);
    }

    private int countNeighbours(Map<HexCoordinate, Deque<ImageView>> gridCopy, HexCoordinate coord) {
        int counter = 0;
        for (HexCoordinate neighbor : coord.getNeighbors()) {
            if (gridCopy.containsKey(neighbor) && gridCopy.get(neighbor) != null && Objects.requireNonNull(gridCopy.get(neighbor).peek()).getImage() != PieceImage.BLANK_TILE.getImage())
                counter++;
        }
        return counter;
    }

    private boolean hasFreedom(Map<HexCoordinate, Deque<ImageView>> gridCopy, HexCoordinate coord) {
        switch (getPieceTypeFromImageView(gridCopy.get(coord).peek())) {
            case BEETLE_BLACK:
            case BEETLE_WHITE:
            case GRASSHOPPER_BLACK:
            case GRASSHOPPER_WHITE:
                return canRemove(gridCopy, coord);
            case QUEEN_BEE_BLACK:
            case QUEEN_BEE_WHITE:
            case SPIDER_BLACK:
            case SPIDER_WHITE:
            case ANT_BLACK:
            case ANT_WHITE:
                return (countNeighbours(gridCopy, coord) <= 4) && canRemove(gridCopy, coord);
            default:
                System.out.println("of course!");
        }
        return false;
    }

    public boolean isValidPlacement(HexCoordinate coord, boolean isWhite) {
        // The coordinate must exist in the grid.
        if (!grid.containsKey(coord)) {
            return false;
        }
        ImageView tile = grid.get(coord).peek();
        // The tile must be blank.
        if (!tile.getImage().equals(PieceImage.BLANK_TILE.getImage())) {
            return false;
        }
        // For the first move, allow placement.
        if (getPlacedPiecesCount() < 2) {
            return true;
        }
        boolean hasAdjacentPiece = false;
        boolean isTouchingOpponent = false;
        for (HexCoordinate neighbor : coord.getNeighbors()) {
            if (grid.containsKey(neighbor)) {
                Deque<ImageView> neighborStack = grid.get(neighbor);
                if (!neighborStack.isEmpty() && !neighborStack.peek().getImage().equals(PieceImage.BLANK_TILE.getImage())) {
                    hasAdjacentPiece = true;

                    PieceColor neighborColor = getPieceColor(neighborStack.peek());
                    boolean neighborIsWhite = (neighborColor == PieceColor.WHITE);
                    if (neighborIsWhite != isWhite) {
                        isTouchingOpponent = true;
                    }
                }
            }
        }
       //  System.out.println("hasAdjacentPiece: " + hasAdjacentPiece +"\nisTouchingOpponent: " + isTouchingOpponent);
        return hasAdjacentPiece && !isTouchingOpponent;
    }

    /**
     * Checks the connectivity of the hive using a flood-fill (DFS/BFS) algorithm.
     *
     * @param gridCopy         A copy of the game grid.
     * @return True if the entire hive is connected, otherwise false.
     */
    // Very similar to Graph Theory, when finding connectivity components (DFS/BFS).
    // The graph of the Hive grid, must be one connectivity component at all time.
    private boolean checkHiveConnectivity(Map<HexCoordinate, Deque<ImageView>> gridCopy) {
        Queue<ImageView> queue = new LinkedList<>();
        Set<HexCoordinate> visited = new HashSet<>();
        List<HexCoordinate> toVisit = new ArrayList<>();
        HexCoordinate startingCoordinate = null;

        for (Map.Entry<HexCoordinate, Deque<ImageView>> entry : gridCopy.entrySet()) {
            ImageView imageView = entry.getValue().peek();
            if (imageView != null && imageView.getImage() != PieceImage.BLANK_TILE.getImage())
                startingCoordinate = entry.getKey();
            else if (imageView == null) {
                System.out.println("NULL COORD: " + entry.getKey());

            }
        }

        toVisit.add(startingCoordinate);
        visited.add(startingCoordinate);

        while (!toVisit.isEmpty()) {
            HexCoordinate current = toVisit.remove(toVisit.size() - 1);

            for (HexCoordinate neighbor : current.getNeighbors()) {
                if (!visited.contains(neighbor) && gridCopy.containsKey(neighbor)) {
                    ImageView neighborTile = gridCopy.get(neighbor).peek();
                    ImageView tempPiece;
                    queue = new LinkedList<>();


                    if (neighborTile != null && !neighborTile.getImage().equals(PieceImage.BLANK_TILE.getImage())) {
                        // System.out.println("Whohoo!");
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

        // Check if all pieces are visited (meaning they are all connected)
        // System.out.println("VISITED_SIZE: " + visited.size() + "\nTOTAL_OCCUPIED_SIZE: " + getTotalOccupiedTilesCopy(gridCopy) + "\nTARGET_TILE: " + startingCoordinate);
        return visited.size() == getTotalOccupiedTiles(gridCopy);
    }

    private int getTotalOccupiedTiles(Map<HexCoordinate, Deque<ImageView>> gridCopy) {
        int counter = 0;
        for (Map.Entry<HexCoordinate, Deque<ImageView>> entry : gridCopy.entrySet()) {
            ImageView imageView = entry.getValue().peek();
            if (imageView.getImage() != PieceImage.BLANK_TILE.getImage())
                counter++;
        }
        return counter;
    }

    /**
     * Returns a list of valid movements for the Bee piece from its current position.
     * The Bee can move one tile in any direction.
     *
     * @param pieceCoordinate The current position of the Bee piece.
     * @return A list of valid movement tiles for the Bee.
     */
    private List<ImageView> getValidBeeMovements(HexCoordinate pieceCoordinate) {
        List<ImageView> validMovements = new ArrayList<>();
        // Bee can move one tile in any direction.
        if (hasFreedom(grid, pieceCoordinate)) {
            for (HexCoordinate neighbor : pieceCoordinate.getNeighbors()) {
                ImageView targetTile = grid.get(neighbor).peek();
                if (isValidMove(pieceCoordinate, neighbor)) {
                    validMovements.add(targetTile);
                }
            }
            System.out.println("validMovements: " + validMovements);
        }
        return validMovements;
    }

    /**
     * Returns a list of valid movements for the Spider piece from its current position.
     * The Spider moves exactly three tiles in a straight line.
     *
     * @param pieceCoordinate The current position of the Spider piece.
     * @return A list of valid movement tiles for the Spider.
     */
    private List<ImageView> getValidSpiderMovements(HexCoordinate pieceCoordinate) {
        List<ImageView> validMovements = new ArrayList<>();
        // Ant can move any number of tiles in a straight line ("sliding").
        Queue<HexCoordinate> queue = new LinkedList<>();
        Set<HexCoordinate> visited = new HashSet<>();
        if (hasFreedom(grid, pieceCoordinate)) {

            queue.add(pieceCoordinate);
            visited.add(pieceCoordinate);

            int counter = 0;
            System.out.println(queue);
            HexCoordinate coord = queue.poll();


            for (HexCoordinate neighbor1 : getCommonFreeTiles(coord)) {
                visited.add(neighbor1);
                if (isValidMove(pieceCoordinate, neighbor1)) {
                    System.out.println("HERE +++++++++++++++++++++++++++++++");
                    System.out.println(neighbor1);
                    System.out.println(getCommonFreeTiles(neighbor1));
                    for (HexCoordinate neighbor2 : getCommonFreeTiles(neighbor1)) {
                        visited.add(neighbor2);
                        if (isValidMove(pieceCoordinate, neighbor2)) {
                            for (HexCoordinate neighbor3 : getCommonFreeTiles(neighbor2)) {
                                if (!visited.contains(neighbor3)) {
                                    visited.add(neighbor3);
                                    if (isValidMove(pieceCoordinate, neighbor3)) {
                                        validMovements.add(grid.get(neighbor3).peek());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return validMovements;
    }

    private Set<HexCoordinate> getCommonFreeTiles(HexCoordinate coord) {
        Set<HexCoordinate> neighbors = new HashSet<>();
        Set<HexCoordinate> freeTilesSurroundingCoord = new HashSet<>();
        Set<HexCoordinate> commonFreeTiles = new HashSet<>();

        for (HexCoordinate coordinate : coord.getNeighbors()) {
            if (grid.containsKey(coordinate) && grid.get(coordinate) != null && grid.get(coordinate).peek().getImage() != PieceImage.BLANK_TILE.getImage()) {
                neighbors.add(coordinate);
            }
        }

        for (HexCoordinate coordinate : coord.getNeighbors()) {
            if (grid.containsKey(coordinate) && grid.get(coordinate) != null && grid.get(coordinate).peek().getImage() == PieceImage.BLANK_TILE.getImage()) {
                freeTilesSurroundingCoord.add(coordinate);
            }
        }


        for (HexCoordinate neighbor : neighbors) {
            for (HexCoordinate neighborOfNeighbor : neighbor.getNeighbors()) {
                if (grid.containsKey(neighborOfNeighbor) && grid.get(neighborOfNeighbor) != null && grid.get(neighborOfNeighbor).peek().getImage() == PieceImage.BLANK_TILE.getImage() && freeTilesSurroundingCoord.contains(neighborOfNeighbor)) {
                    commonFreeTiles.add(neighborOfNeighbor);
                }
            }
        }

        return commonFreeTiles;

    }

    /**
     * Recursive DFS function to find all valid spider paths.
     */
    private void findSpiderPathsRecursive(HexCoordinate current, List<HexCoordinate> path,
                                 Set<HexCoordinate> visited, List<List<HexCoordinate>> paths, int depth) {
        if (depth == 0) {
            if (path.size() == 3) { // Spider must move exactly 3 steps
                paths.add(new ArrayList<>(path)); // Add a valid path
            }
            return;
        }

        visited.add(current);
        path.add(current);

        for (HexCoordinate neighbor : current.getNeighbors()) {
            if (!visited.contains(neighbor) && isValidMove(current, neighbor)) {
                findSpiderPathsRecursive(neighbor, path, visited, paths, depth - 1);
            }
        }

        visited.remove(current);
        path.remove(path.size() - 1);
    }

    private List<HexCoordinate> getFreeCoords() {
        List<HexCoordinate> freeCoords = new ArrayList<>();
        for (Map.Entry<HexCoordinate, Deque<ImageView>> entry : grid.entrySet()) {
            ImageView imageView = entry.getValue().peek();
            if (imageView != null && imageView.getImage() == PieceImage.BLANK_TILE.getImage()) {
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
    private List<ImageView> getValidAntMovements(HexCoordinate pieceCoordinate) {
        List<ImageView> validMovements = new ArrayList<>();
        // Ant can move any number of tiles in a straight line ("sliding").
        Queue<HexCoordinate> queue = new LinkedList<>();
        List<HexCoordinate> freeCoords = getFreeCoords();
        Set<HexCoordinate> visited = new HashSet<>();
        if (hasFreedom(grid, pieceCoordinate)) {

            queue.add(pieceCoordinate);
            visited.add(pieceCoordinate);

            boolean flag = true;
            while (!queue.isEmpty()) {
                flag = false;
                System.out.println(queue);
                HexCoordinate coord = queue.poll();
                for (HexCoordinate neighbor : coord.getNeighbors()) {
                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);
                        if (isValidMove(pieceCoordinate, neighbor)) {
                            queue.add(neighbor);
                            flag = true;
                            validMovements.add(grid.get(neighbor).peek());
                        }
                    }
                }

            }
            System.out.println("\t\t" + flag + "\t\t");


        }
        return validMovements;
    }

    /**
     * Returns a list of valid movements for the Grasshopper piece from its current position.
     * The Grasshopper must jump over exactly one piece.
     *
     * @param pieceCoordinate The current position of the Grasshopper piece.
     * @return A list of valid movement tiles for the Grasshopper.
     */
    private List<ImageView> getValidGrasshopperMovements(HexCoordinate pieceCoordinate) {
        List<ImageView> validMovements = new ArrayList<>();
        // Grasshopper must jump over pieces in a straight line.
        if (hasFreedom(grid, pieceCoordinate)) {
            for (HexCoordinate direction : HexCoordinate.DIRECTIONS) {
                HexCoordinate targetCoordinate = pieceCoordinate.add(direction);
                if (grid.get(targetCoordinate) != null && Objects.requireNonNull(grid.get(targetCoordinate).peek()).getImage() != PieceImage.BLANK_TILE.getImage()) {
                    do targetCoordinate = targetCoordinate.add(direction);
                    while (grid.get(targetCoordinate) != null && Objects.requireNonNull(grid.get(targetCoordinate).peek()).getImage() != PieceImage.BLANK_TILE.getImage());
                    if (isConnectedAfterRemoval(pieceCoordinate, targetCoordinate))
                        validMovements.add(grid.get(targetCoordinate).peek());
                }
            }
        }
        return validMovements;
    }

    /**
     * Returns a list of valid movements for the Beetle piece from its current position.
     * The Beetle shares the same movement rules as the Ant piece.
     *
     * @param pieceCoordinate The current position of the Beetle piece.
     * @return A list of valid movement tiles for the Beetle.
     */
    private List<ImageView> getValidBeetleMovements(HexCoordinate pieceCoordinate) {
        List<ImageView> validMovements = new ArrayList<>();

        if (hasFreedom(grid, pieceCoordinate)) {
            for (HexCoordinate neighbor : pieceCoordinate.getNeighbors()) {
                if (isFreedomToMoveBeetle(pieceCoordinate, neighbor))
                    validMovements.add(grid.get(neighbor).peek());
            }
        }
        return validMovements;
    }

    /**
     * Moves a selected piece to the target tile and updates the grid accordingly.
     *
     * @param selectedPiece The piece that is being moved.
     * @param targetPiece   The target position where the piece will be moved to.
     */
    public void movePiece(ImageView selectedPiece, ImageView targetPiece) {
        HexCoordinate targetCoord = getKeyByValue(grid, targetPiece);
        HexCoordinate sourceCoord = getKeyByValue(grid, selectedPiece);

        Deque<ImageView> targetStack = grid.get(targetCoord);
        Deque<ImageView> sourceStack = grid.get(sourceCoord);


        targetStack.push(sourceStack.pop());

        if (sourceStack.isEmpty()) {
            sourceStack.push(new ImageView(PieceImage.BLANK_TILE.getImage()));
        }

        for (HexCoordinate neighbor : targetCoord.getNeighbors()) {
            if (!grid.containsKey(neighbor)) {
                Deque<ImageView> stack = new ArrayDeque<>();
                stack.push(new ImageView(PieceImage.BLANK_TILE.getImage()));
                grid.put(neighbor, stack);
            }
        }

        ArrayList<HexCoordinate> sourceCoordNeighbors = sourceCoord.getNeighbors();
        System.out.println("Neighbors: " + sourceCoordNeighbors);
        for (HexCoordinate neighbor : sourceCoord.getNeighbors()) {
            if (grid.get(neighbor).peek().getImage() != PieceImage.BLANK_TILE.getImage()) {
                for (HexCoordinate neighborOfNeighbor : neighbor.getNeighbors()) {
                    if (neighborOfNeighbor != targetCoord)
                        sourceCoordNeighbors.remove(neighborOfNeighbor);
                }
                sourceCoordNeighbors.remove(neighbor);
            }
        }

        /*
        System.out.println("Neighbors: " + sourceCoordNeighbors);
        for (HexCoordinate tile : sourceCoordNeighbors)
            grid.get(tile).pop();
         */


        // clearNullValues();

    }

    private void clearNullValues() {
        Iterator<Map.Entry<HexCoordinate, Deque<ImageView>>> iterator = grid.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<HexCoordinate, Deque<ImageView>> tile = iterator.next();
            if (tile.getValue().peek() == null)
                iterator.remove();
        }
    }

    /**
     * Returns the PieceImage type associated with a given ImageView.
     *
     * @param imageView The ImageView of the piece.
     * @return The corresponding PieceImage type, or null if no match is found.
     */
    private static PieceImage getPieceTypeFromImageView(ImageView imageView) {
        Image image = imageView.getImage();
        for (PieceImage pieceImage : PieceImage.values()) {
            if (pieceImage.getImage().equals(image)) {
                return pieceImage; // Found matching piece
            }
        }
        return null; // If not found
    }

    /**
     * Determines if the piece can be placed according to game rules.
     *
     * @param isWhite    Whether the piece belongs to the white player.
     * @param pieceImage The ImageView representing the piece being placed.
     * @return True if the piece can be placed, false otherwise.
     */
    private boolean canPlacePiece(Boolean isWhite, ImageView pieceImage) {
        PieceImage pieceType = getPieceTypeFromImageView(pieceImage);
        int moves = moveCount.get(isWhite);
        boolean queenIsPlaced = queenPlaced.get(isWhite);

        if (pieceType == PieceImage.QUEEN_BEE_BLACK || pieceType == PieceImage.QUEEN_BEE_WHITE) {
            queenPlaced.put(isWhite, true);
            return true;
        }

        if (moves == 3 && !queenIsPlaced) {
            System.out.println(isWhite + " must place the Queen Bee on this turn!");
            return false;
        }

        return true;
    }

    /**
     * Places a new piece in the grid, replacing the old piece.
     * Updates the game state, including piece counts and move counts.
     *
     * @param isWhite  Whether the piece belongs to the white player.
     * @param newPiece The new piece being placed.
     * @param oldPiece The old piece being replaced.
     * @return 1 if the Queen Bee has not been placed and the player needs to place it, 0 otherwise.
     */
    public int placePiece(Boolean isWhite, ImageView newPiece, ImageView oldPiece) {
        List<HexCoordinate> neighborsToAdd = new ArrayList<>();

        Iterator<Map.Entry<HexCoordinate, Deque<ImageView>>> iterator = grid.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<HexCoordinate, Deque<ImageView>> entry = iterator.next();
            Deque<ImageView> stack = entry.getValue();
            ImageView pieceImageView = entry.getValue().peek();
            HexCoordinate pieceImageCoord = entry.getKey();

            if (pieceImageView == oldPiece) {
                stack.push(new ImageView(newPiece.getImage()));
                PieceImage pieceType = getPieceTypeFromImageView(newPiece);
                if (pieceType == PieceImage.QUEEN_BEE_BLACK || pieceType == PieceImage.QUEEN_BEE_WHITE) {
                    queenPlaced.put(isWhite, true);
                }
                ArrayList<HexCoordinate> neighboors = pieceImageCoord.getNeighbors();
                for (HexCoordinate neighbor : neighboors) {
                    if (!grid.containsKey(neighbor)) {
                        neighborsToAdd.add(neighbor);
                    }
                }
            }
        }

        for (HexCoordinate neighbor : neighborsToAdd) {
            Deque<ImageView> stack = new ArrayDeque<>();
            stack.push(new ImageView(PieceImage.BLANK_TILE.getImage()));
            grid.put(neighbor, stack);
        }
        PieceImage pieceType = getPieceTypeFromImageView(newPiece);
        if (pieceType != null) {
            int count = piecesCount.get(pieceType);
            count--;
            piecesCount.put(pieceType, count);
        }

        moveCount.put(isWhite, moveCount.get(isWhite) + 1);

        int moves = moveCount.get(isWhite);
        boolean queenIsPlaced = queenPlaced.get(isWhite);
        if (moves == 3 && !queenIsPlaced) {
            return 1;
        }

        return 0;
    }

    /**
     * Returns the color of the piece based on its ImageView.
     *
     * @param piece The ImageView of the piece.
     * @return The PieceColor (BLACK, WHITE, or NONE) corresponding to the piece's color.
     */
    private PieceColor getPieceColor(ImageView piece) {
        PieceImage pieceImage = getPieceTypeFromImageView(piece);
        return switch (Objects.requireNonNull(pieceImage)) {
            case QUEEN_BEE_BLACK, SPIDER_BLACK, ANT_BLACK, BEETLE_BLACK, GRASSHOPPER_BLACK -> PieceColor.BLACK;
            case QUEEN_BEE_WHITE, SPIDER_WHITE, ANT_WHITE, BEETLE_WHITE, GRASSHOPPER_WHITE -> PieceColor.WHITE;
            default -> PieceColor.NONE;
        };
    }

    /**
     * Checks if there is a winner in the game.
     * The game is won when the opponent's Queen Bee is surrounded by their pieces.
     *
     * @return True if there is a winner, false otherwise.
     */
    public Map<Boolean, Boolean> checkWin() {
        Map<Boolean, Boolean> winner = new HashMap<>();
        winner.put(true, false);
        winner.put(false, false);
        boolean wFlag = false, bFlag = false;
        for (Map.Entry<HexCoordinate, Deque<ImageView>> entry : grid.entrySet()) {
            ImageView imageView = entry.getValue().peek();
            if (imageView.getImage() == PieceImage.QUEEN_BEE_BLACK.getImage()) {
                winner.put(true, true);
                for (HexCoordinate neighbor : getKeyByValue(grid, imageView).getNeighbors()) {
                    if (getPieceColor(grid.get(neighbor).peek()) == PieceColor.NONE)
                        winner.put(true, false);
                }
            }
        }
        for (Map.Entry<HexCoordinate, Deque<ImageView>> entry : grid.entrySet()) {
            ImageView imageView = entry.getValue().peek();
            if (imageView.getImage() == PieceImage.QUEEN_BEE_WHITE.getImage()) {
                winner.put(false, true);
                for (HexCoordinate neighbor : getKeyByValue(grid, imageView).getNeighbors()) {
                    if (getPieceColor(grid.get(neighbor).peek()) == PieceColor.NONE)
                        winner.put(false, false);
                }
            }
        }
        return winner;
    }

    /**
     * Returns a copy of the pieces count map.
     *
     * @return A map of PieceImage types and their respective counts.
     */
    public int getPlacedPiecesCount() {
        int counter = 0;
        for (Map.Entry<HexCoordinate, Deque<ImageView>> entry : grid.entrySet()) {
            if (entry.getValue().peek().getImage() != PieceImage.BLANK_TILE.getImage())
                counter++;
        }
        return counter;
    }

    /**
     * Returns the count of a specific piece type in the game.
     *
     * @param pieceImageView The ImageView of the piece.
     * @return The count of the piece type, or -1 if the piece is not found.
     */
    public int getPieceCount(ImageView pieceImageView) {
        PieceImage pieceType = getPieceTypeFromImageView(pieceImageView);
        if (pieceType != null) {
            int count = piecesCount.get(pieceType);
            return count;
        }
        return -1;
    }

    /**
     * Retrieves the HexCoordinate of a specific piece based on its ImageView.
     *
     * @param piece The ImageView of the piece.
     * @return The HexCoordinate of the piece, or null if the piece is not found.
     */
    public HexCoordinate getHexCoord(ImageView piece) {
        for (Map.Entry<HexCoordinate, Deque<ImageView>> entry : grid.entrySet()) {
            ImageView pieceImageView = entry.getValue().peek();
            HexCoordinate pieceImageCoord = entry.getKey();

            if (pieceImageView == piece) {
                return pieceImageCoord;
            }
        }
        return null;
    }

    /**
     * Returns the current player's turn.
     *
     * @return True if it is the white player's turn, false if it is the black player's turn.
     */
    public boolean getTurn() {
        return isWhiteTurn;
    }

    /**
     * Advances the turn to the next player.
     */
    public void advanceTurn() {
        isWhiteTurn = !isWhiteTurn;
    }

    public boolean canMovePieces() {
        return queenPlaced.get(isWhiteTurn);
    }

}
