package com.example.hive.model;

import javafx.geometry.Point2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Pair;

import java.text.DecimalFormat;
import java.util.*;

/**
 * The Grid class represents the game board for Hive. It manages the placement and movement of pieces,
 * checking for connectivity, and determining the game's state (such as turns, valid moves, and win conditions).
 */
public class Grid {

    public final static ArrayList<Image> BLACK_PIECES = new ArrayList<>(Arrays.asList(PieceImage.QUEEN_BEE_BLACK.getImage(), PieceImage.ANT_BLACK.getImage(), PieceImage.BEETLE_BLACK.getImage(), PieceImage.GRASSHOPPER_BLACK.getImage(), PieceImage.SPIDER_BLACK.getImage()));
    public final static ArrayList<Image> WHITE_PIECES = new ArrayList<>(Arrays.asList(PieceImage.QUEEN_BEE_WHITE.getImage(), PieceImage.ANT_WHITE.getImage(), PieceImage.BEETLE_WHITE.getImage(), PieceImage.GRASSHOPPER_WHITE.getImage(), PieceImage.SPIDER_WHITE.getImage()));
    private final Map<HexCoordinate, Deque<ImageView>> grid = new HashMap<>();

    private Map<Boolean, Integer> moveCount;
    private Map<Boolean, Boolean> queenPlaced;
    private Map<PieceImage, Integer> piecesCount;  // Track counts of each piece type.

    private boolean isWhiteTurn = true; // White's turn.

    // private Set<ImageView> placedBlackPieces = new HashSet<>();
    // private Set<ImageView> placedWhitePieces = new HashSet<>();

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
     * Returns the current grid of the game (deep copied grid).
     *
     * @return A map of HexCoordinates to ImageViews representing the grid.
     */
    public Map<HexCoordinate, Deque<ImageView>> getGrid() {
        Map<HexCoordinate, Deque<ImageView>> gridCopy = new HashMap<>(grid);
        for (Map.Entry<HexCoordinate, Deque<ImageView>> entry : grid.entrySet()) {
            gridCopy.put(entry.getKey(), new ArrayDeque<>(entry.getValue())); // Ensures a deep copy of stacks.
        }
        return gridCopy;
    }

    public Map<HexCoordinate, Deque<ImageView>> getGrid(Map<HexCoordinate, Deque<ImageView>> gameGrid) {
        Map<HexCoordinate, Deque<ImageView>> gridCopy = new HashMap<>(gameGrid);
        for (Map.Entry<HexCoordinate, Deque<ImageView>> entry : gameGrid.entrySet()) {
            gridCopy.put(entry.getKey(), new ArrayDeque<>(entry.getValue())); // Ensures a deep copy of stacks.
        }
        return gridCopy;
    }

    public Map<PieceImage, Integer> getPiecesCount() {
        Map<PieceImage, Integer> piecesCountCopy = new HashMap<>();
        for (Map.Entry<PieceImage, Integer> entry : piecesCount.entrySet()) {
            if (entry.getValue() > 0)
                piecesCountCopy.put(entry.getKey(), entry.getValue());
        }
        return piecesCountCopy;
    }

    public Map<Boolean, Boolean> getQueenPlaced() {
        return new HashMap<>(queenPlaced);
    }

    public Map<Boolean, Integer> getMoveCount() {
        return new HashMap<>(moveCount);
    }

    /**
     * Returns a list of ImageViews representing valid placements on the grid.
     *
     * @return A list of ImageViews representing valid placements.
     */
    public List<ImageView> getValidPlacements(boolean isWhiteTurn) {
        return getValidPlacements(grid, isWhiteTurn);
    }

    public List<ImageView> getValidPlacements(Map<HexCoordinate, Deque<ImageView>> gridCopy, boolean isWhiteTurn) {
        boolean stillLeft = isStillLeftPanelPiece(isWhiteTurn);
        if (!stillLeft)
            return null;
        List<ImageView> placements = new ArrayList<>();
        for (Map.Entry<HexCoordinate, Deque<ImageView>> entry : gridCopy.entrySet()) {
            HexCoordinate coord = entry.getKey();
            ImageView tile = entry.getValue().peek();
            assert tile != null;
            if (tile.getImage().equals(PieceImage.BLANK_TILE.getImage()) && isValidPlacement(coord, isWhiteTurn)) {
                placements.add(tile);
            }
        }

        /*
        if (placements.isEmpty())
            advanceTurn();
         */

        return placements;
    }

    private boolean isStillLeftPanelPiece(boolean isWhiteTurn) {
        boolean stillLeft = false;
        for (Map.Entry<PieceImage, Integer> entry : piecesCount.entrySet()) {
            if (isWhiteTurn) {
                if (entry.getKey() == PieceImage.QUEEN_BEE_WHITE || entry.getKey() == PieceImage.ANT_WHITE || entry.getKey() == PieceImage.BEETLE_WHITE || entry.getKey() == PieceImage.GRASSHOPPER_WHITE || entry.getKey() == PieceImage.SPIDER_WHITE)
                    stillLeft = true;
            } else {
                if (entry.getKey() == PieceImage.QUEEN_BEE_BLACK || entry.getKey() == PieceImage.ANT_BLACK || entry.getKey() == PieceImage.BEETLE_BLACK || entry.getKey() == PieceImage.GRASSHOPPER_BLACK || entry.getKey() == PieceImage.SPIDER_BLACK)
                    stillLeft = true;
            }
        }
        return stillLeft;
    }

    /**
     * Helper method to get the key (HexCoordinate) associated with a given value (ImageView) from a map.
     *
     * @param map   The map to search through.
     * @param value The value to search for.
     * @return The key associated with the value, or null if not found.
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
     * Returns a list of valid movements for a given piece on the grid.
     *
     * @param piece The ImageView representing the piece.
     * @return A list of valid movements as ImageViews.
     */
    public List<ImageView> getValidMovements(ImageView piece) {
        return getValidMovements(grid, piece);
    }


    public List<ImageView> getValidMovements(Map<HexCoordinate, Deque<ImageView>> gridCopy, ImageView piece) {
        List<ImageView> validMovements = new ArrayList<>();
        HexCoordinate pieceCoordinate = getKeyByValue(gridCopy, piece);
        PieceImage pieceImage = getPieceTypeFromImageView(piece);


        switch (Objects.requireNonNull(pieceImage)) {
            case QUEEN_BEE_BLACK:
            case QUEEN_BEE_WHITE:
                validMovements.addAll(getValidBeeMovements(gridCopy, pieceCoordinate));
                break;
            case SPIDER_BLACK:
            case SPIDER_WHITE:
                validMovements.addAll(getValidSpiderMovements(gridCopy, pieceCoordinate));
                break;
            case ANT_BLACK:
            case ANT_WHITE:
                validMovements.addAll(getValidAntMovements(gridCopy, pieceCoordinate));
                break;
            case GRASSHOPPER_BLACK:
            case GRASSHOPPER_WHITE:
                validMovements.addAll(getValidGrasshopperMovements(gridCopy, pieceCoordinate));
                break;
            case BEETLE_BLACK:
            case BEETLE_WHITE:
                validMovements.addAll(getValidBeetleMovements(gridCopy, pieceCoordinate));
                break;
            default:
                break;
        }

        return validMovements;
    }



    public boolean isValidMove(Map<HexCoordinate, Deque<ImageView>> gridCopy, HexCoordinate from, HexCoordinate to) {
        if (gridCopy.containsKey(to) && gridCopy.get(to).peek().getImage() != PieceImage.BLANK_TILE.getImage()) {
            // System.out.println(131);
            return false;
        }

        Deque<ImageView> fromStack = gridCopy.get(from);
        Deque<ImageView> toStack = gridCopy.get(to);
        if (fromStack == null || fromStack.isEmpty() || toStack == null || toStack.isEmpty()) {
            // System.out.println(138);
            return false;
        }

        // ImageView movingPiece = fromStack.peek();
        // boolean isWhite = movingPiece.getId().contains("white");

        if (!canSlide(gridCopy, from, to)) {
            // System.out.println(to);
            // System.out.println(146);
            return false;
        }

        if (!hasFreedomToMove(gridCopy, from, to)) {
            // System.out.println(151);
            return false;
        }

        return true;
    }

    private boolean canSlide(Map<HexCoordinate, Deque<ImageView>> gridCopy, HexCoordinate from, HexCoordinate to) {
        if (from.equals(to)) return false;  // Can't slide to the same tile.

        Set<HexCoordinate> visited = new HashSet<>();
        Queue<HexCoordinate> queue = new LinkedList<>();

        queue.add(from);
        visited.add(from);

        while (!queue.isEmpty()) {
            HexCoordinate current = queue.poll();

            for (HexCoordinate neighbor : current.getNeighbors()) {
                if (!visited.contains(neighbor) && gridCopy.containsKey(neighbor) && gridCopy.get(neighbor).peek().getImage() == PieceImage.BLANK_TILE.getImage()) {
                    queue.add(neighbor);
                    visited.add(neighbor);

                    if (neighbor.equals(to)) return true;  // Found a valid sliding path
                }
            }
        }
        return false;  // No valid path found
    }


    private boolean hasFreedomToMove(Map<HexCoordinate, Deque<ImageView>> gridCopy, HexCoordinate from, HexCoordinate to) {
        return isConnectedAfterRemoval(gridCopy, from, to);
    }

    private boolean isFreedomToMoveBeetle(Map<HexCoordinate, Deque<ImageView>> gridCopy, HexCoordinate from, HexCoordinate to) {
        if (gridCopy.get(to).size() == gridCopy.get(from).size() + 1) {
            if (!canSlide(gridCopy, from, to))
                return false;
        }

        return hasFreedomToMove(gridCopy, from, to);
    }

    private boolean isConnectedAfterRemoval(Map<HexCoordinate, Deque<ImageView>> gameGrid, HexCoordinate from, HexCoordinate to) {
        Map<HexCoordinate, Deque<ImageView>> gridCopy = getGrid(gameGrid);

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

    public HexCoordinate getQueenCoordinate(Map<HexCoordinate, Deque<ImageView>> gridCopy, boolean isWhiteTurn) {
        HexCoordinate queenCoord = null;
        for (Map.Entry<HexCoordinate, Deque<ImageView>> entry : gridCopy.entrySet()) {
            Deque<ImageView> queue = entry.getValue();
            Deque<ImageView> restoreQueue = new ArrayDeque<>();
            ImageView imageView = null;
            boolean queenFound = false;

            while (queue.size() > 1 && !queenFound) {
                imageView = queue.pop();
                restoreQueue.push(imageView);

                if (((isWhiteTurn && imageView.getImage() == PieceImage.QUEEN_BEE_WHITE.getImage()) ||(!isWhiteTurn && imageView.getImage() == PieceImage.QUEEN_BEE_BLACK.getImage()))) {
                    queenCoord = entry.getKey();
                    queenFound = true;
                }
            }
            while (!restoreQueue.isEmpty()) {
                queue.push(restoreQueue.pop());
            }
            if (queenFound)
                return queenCoord;
        }
        return null;
    }

    public int countNeighbours(Map<HexCoordinate, Deque<ImageView>> gridCopy, HexCoordinate coord) {
        int counter = 0;
        for (HexCoordinate neighbor : coord.getNeighbors()) {
            if (gridCopy.containsKey(neighbor) && gridCopy.get(neighbor) != null && Objects.requireNonNull(gridCopy.get(neighbor).peek()).getImage() != PieceImage.BLANK_TILE.getImage())
                counter++;
        }
        return counter;
    }

    private boolean hasFreedom(Map<HexCoordinate, Deque<ImageView>> gridCopy, HexCoordinate coord) {
        // System.out.println("COORDINATE: " + coord);
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
    private List<ImageView> getValidBeeMovements(Map<HexCoordinate, Deque<ImageView>> gridCopy, HexCoordinate pieceCoordinate) {
        List<ImageView> validMovements = new ArrayList<>();
        // Bee can move one tile in any direction.
        if (hasFreedom(gridCopy, pieceCoordinate)) {
            for (HexCoordinate neighbor : pieceCoordinate.getNeighbors()) {
                ImageView targetTile = gridCopy.get(neighbor).peek();
                if (isValidMove(gridCopy, pieceCoordinate, neighbor)) {
                    validMovements.add(targetTile);
                }
            }
            // System.out.println("validMovements: " + validMovements);
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
    private List<ImageView> getValidSpiderMovements(Map<HexCoordinate, Deque<ImageView>> gridCopy, HexCoordinate pieceCoordinate) {
        List<ImageView> validMovements = new ArrayList<>();
        // Ant can move any number of tiles in a straight line ("sliding").
        Queue<HexCoordinate> queue = new LinkedList<>();
        Set<HexCoordinate> visited = new HashSet<>();
        if (hasFreedom(gridCopy, pieceCoordinate)) {

            queue.add(pieceCoordinate);
            visited.add(pieceCoordinate);

            int counter = 0;
            // System.out.println(queue);
            HexCoordinate coord = queue.poll();


            for (HexCoordinate neighbor1 : getCommonFreeTiles(coord)) {
                visited.add(neighbor1);
                if (isValidMove(gridCopy, pieceCoordinate, neighbor1)) {
                    // System.out.println("HERE +++++++++++++++++++++++++++++++");
                    // System.out.println(neighbor1);
                    // System.out.println(getCommonFreeTiles(neighbor1));
                    for (HexCoordinate neighbor2 : getCommonFreeTiles(neighbor1)) {
                        visited.add(neighbor2);
                        if (isValidMove(gridCopy, pieceCoordinate, neighbor2)) {
                            for (HexCoordinate neighbor3 : getCommonFreeTiles(neighbor2)) {
                                if (!visited.contains(neighbor3)) {
                                    visited.add(neighbor3);
                                    if (isValidMove(gridCopy, pieceCoordinate, neighbor3)) {
                                        validMovements.add(gridCopy.get(neighbor3).peek());
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
    /*
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
    */

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
    private List<ImageView> getValidAntMovements(Map<HexCoordinate, Deque<ImageView>> gridCopy, HexCoordinate pieceCoordinate) {
        List<ImageView> validMovements = new ArrayList<>();
        // Ant can move any number of tiles in a straight line ("sliding").
        Queue<HexCoordinate> queue = new LinkedList<>();
        List<HexCoordinate> freeCoords = getFreeCoords();
        Set<HexCoordinate> visited = new HashSet<>();
        if (hasFreedom(gridCopy, pieceCoordinate)) {

            queue.add(pieceCoordinate);
            visited.add(pieceCoordinate);

            boolean flag = true;
            while (!queue.isEmpty()) {
                flag = false;
                // System.out.println(queue);
                HexCoordinate coord = queue.poll();
                for (HexCoordinate neighbor : coord.getNeighbors()) {
                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);
                        if (isValidMove(gridCopy, pieceCoordinate, neighbor)) {
                            queue.add(neighbor);
                            flag = true;
                            validMovements.add(gridCopy.get(neighbor).peek());
                        }
                    }
                }

            }
            // System.out.println("\t\t" + flag + "\t\t");


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
    private List<ImageView> getValidGrasshopperMovements(Map<HexCoordinate, Deque<ImageView>> gridCopy, HexCoordinate pieceCoordinate) {
        List<ImageView> validMovements = new ArrayList<>();
        // Grasshopper must jump over pieces in a straight line.
        if (hasFreedom(gridCopy, pieceCoordinate)) {
            for (HexCoordinate direction : HexCoordinate.DIRECTIONS) {
                HexCoordinate targetCoordinate = pieceCoordinate.add(direction);
                if (gridCopy.get(targetCoordinate) != null && Objects.requireNonNull(gridCopy.get(targetCoordinate).peek()).getImage() != PieceImage.BLANK_TILE.getImage()) {
                    do targetCoordinate = targetCoordinate.add(direction);
                    while (gridCopy.get(targetCoordinate) != null && Objects.requireNonNull(gridCopy.get(targetCoordinate).peek()).getImage() != PieceImage.BLANK_TILE.getImage());
                    if (isConnectedAfterRemoval(gridCopy, pieceCoordinate, targetCoordinate))
                        validMovements.add(gridCopy.get(targetCoordinate).peek());
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
    private List<ImageView> getValidBeetleMovements(Map<HexCoordinate, Deque<ImageView>> gridCopy, HexCoordinate pieceCoordinate) {
        List<ImageView> validMovements = new ArrayList<>();

        if (hasFreedom(gridCopy, pieceCoordinate)) {
            for (HexCoordinate neighbor : pieceCoordinate.getNeighbors()) {
                if (isFreedomToMoveBeetle(gridCopy, pieceCoordinate, neighbor))
                    validMovements.add(gridCopy.get(neighbor).peek());
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
        Pair<ImageView, ImageView> pair = new Pair<>(selectedPiece, targetPiece);
        movePiece(grid, pair);
        advanceTurn();
    }

    public Map<HexCoordinate, Deque<ImageView>> movePiece(Map<HexCoordinate, Deque<ImageView>> gridCopy, Pair<ImageView, ImageView> move) {
        HexCoordinate targetCoord = getKeyByValue(gridCopy, move.getValue());
        HexCoordinate sourceCoord = getKeyByValue(gridCopy, move.getKey());

        Deque<ImageView> targetStack = gridCopy.get(targetCoord);
        Deque<ImageView> sourceStack = gridCopy.get(sourceCoord);


        targetStack.push(sourceStack.pop());

        if (sourceStack.isEmpty()) {
            sourceStack.push(new ImageView(PieceImage.BLANK_TILE.getImage()));
        }

        for (HexCoordinate neighbor : targetCoord.getNeighbors()) {
            if (!gridCopy.containsKey(neighbor)) {
                Deque<ImageView> stack = new ArrayDeque<>();
                stack.push(new ImageView(PieceImage.BLANK_TILE.getImage()));
                gridCopy.put(neighbor, stack);
            }
        }

        ArrayList<HexCoordinate> sourceCoordNeighbors = sourceCoord.getNeighbors();
        // System.out.println("Neighbors: " + sourceCoordNeighbors);
        for (HexCoordinate neighbor : sourceCoord.getNeighbors()) {
            if (gridCopy.get(neighbor).peek().getImage() != PieceImage.BLANK_TILE.getImage()) {
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
        return gridCopy;
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
        Pair<Map<HexCoordinate, Deque<ImageView>>, Integer> pair = placePiece(grid, isWhite, newPiece, oldPiece, queenPlaced, piecesCount, moveCount);
        advanceTurn();
        return pair.getValue();
    }

    public Pair<Map<HexCoordinate, Deque<ImageView>>, Integer> placePiece(Map<HexCoordinate, Deque<ImageView>> gridCopy,
                                                                          Boolean isWhite, ImageView newPiece,
                                                                          ImageView oldPiece, Map<Boolean,
            Boolean> queenPlaced, Map<PieceImage, Integer> piecesCount, Map<Boolean, Integer> moveCount) {
        Pair<Map<HexCoordinate, Deque<ImageView>>, Integer> returnedPair;
        List<HexCoordinate> neighborsToAdd = new ArrayList<>();
        ImageView placedImageView = new ImageView(newPiece.getImage());

        Iterator<Map.Entry<HexCoordinate, Deque<ImageView>>> iterator = gridCopy.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<HexCoordinate, Deque<ImageView>> entry = iterator.next();
            Deque<ImageView> stack = entry.getValue();
            ImageView pieceImageView = entry.getValue().peek();
            HexCoordinate pieceImageCoord = entry.getKey();

            if (pieceImageView == oldPiece) {
                stack.push(placedImageView);
                PieceImage pieceType = getPieceTypeFromImageView(newPiece);
                if (pieceType == PieceImage.QUEEN_BEE_BLACK || pieceType == PieceImage.QUEEN_BEE_WHITE) {
                    queenPlaced.put(isWhite, true);
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
            Deque<ImageView> stack = new ArrayDeque<>();
            stack.push(new ImageView(PieceImage.BLANK_TILE.getImage()));
            gridCopy.put(neighbor, stack);
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
            returnedPair = new Pair<>(gridCopy, 1);
            return returnedPair;
        }
        returnedPair = new Pair<>(gridCopy, 0);
        return returnedPair;
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
        return checkWin(grid);
    }

    public Map<Boolean, Boolean> checkWin(Map<HexCoordinate, Deque<ImageView>> gridCopy) {
        Map<Boolean, Boolean> winner = new HashMap<>();
        winner.put(true, false);
        winner.put(false, false);
        for (Map.Entry<HexCoordinate, Deque<ImageView>> entry : gridCopy.entrySet()) {
            ImageView imageView = entry.getValue().peek();
            if (imageView.getImage() == PieceImage.QUEEN_BEE_BLACK.getImage()) {
                winner.put(true, true);
                for (HexCoordinate neighbor : getKeyByValue(gridCopy, imageView).getNeighbors()) {
                    if (getPieceColor(gridCopy.get(neighbor).peek()) == PieceColor.NONE)
                        winner.put(true, false);
                }
            }
        }
        for (Map.Entry<HexCoordinate, Deque<ImageView>> entry : gridCopy.entrySet()) {
            ImageView imageView = entry.getValue().peek();
            if (imageView.getImage() == PieceImage.QUEEN_BEE_WHITE.getImage()) {
                winner.put(false, true);
                for (HexCoordinate neighbor : getKeyByValue(gridCopy, imageView).getNeighbors()) {
                    if (getPieceColor(gridCopy.get(neighbor).peek()) == PieceColor.NONE)
                        winner.put(false, false);
                }
            }
        }

        if (!winner.get(true) && !winner.get(false)) {
            if (getLegalMoves(isWhiteTurn).isEmpty() && getValidPlacements(isWhiteTurn).isEmpty()) {
                advanceTurn();
                if (getLegalMoves(isWhiteTurn).isEmpty() && getValidPlacements(isWhiteTurn).isEmpty()) {
                    winner.put(true, true);
                    winner.put(false, true);
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

    public int getPlacedPiecesCount(boolean isWhite) {
        int counter = 0;
        for (Map.Entry<HexCoordinate, Deque<ImageView>> entry : grid.entrySet()) {
            if (isWhite) {
                if (WHITE_PIECES.contains(entry.getValue().peek().getImage())) {
                    counter++;
                }
            } else {
                if (BLACK_PIECES.contains(entry.getValue().peek().getImage())) {
                    counter++;
                }
            }
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


    public List<Pair<ImageView, ImageView>> getLegalMoves(boolean isWhiteTurn) {
        return getLegalMoves(grid, isWhiteTurn);
    }

    public List<Pair<ImageView, ImageView>> getLegalMoves(Map<HexCoordinate, Deque<ImageView>> gridCopy, boolean isWhiteTurn) {
        List<Pair<ImageView, ImageView>> legalMoves = new ArrayList<>();
        List<ImageView> possibleMoves;

        // Set<ImageView> placedPieces = isWhiteTurn ? placedWhitePieces : placedBlackPieces;

        for (Map.Entry<HexCoordinate, Deque<ImageView>> entry : gridCopy.entrySet()) {
            ImageView piece = entry.getValue().peek();
            if (piece != null && ((isWhiteTurn && WHITE_PIECES.contains(piece.getImage())) || (!isWhiteTurn && BLACK_PIECES.contains(piece.getImage())))) {
                possibleMoves = getValidMovements(gridCopy, piece);
                for (ImageView possibleMove : possibleMoves) {
                    Pair<ImageView, ImageView> pair = new Pair<>(piece, possibleMove);
                    legalMoves.add(pair);
                }
            }
        }
        return legalMoves;
    }

    public int countTotalLegalMoves(Map<HexCoordinate, Deque<ImageView>> gridCopy, boolean isWhite) {
        List<Pair<ImageView, ImageView>> legalMoves = getLegalMoves(gridCopy, isWhite);
        List<ImageView> legalPlacements = getValidPlacements(gridCopy, isWhite);
        return legalMoves.size() + legalPlacements.size();
    }

    public int countLegalMovesForPiece(Map<HexCoordinate, Deque<ImageView>> gridCopy, ImageView piece) {
        List<ImageView> validPieceMovements = getValidMovements(piece);
        return validPieceMovements.size();
    }
}
