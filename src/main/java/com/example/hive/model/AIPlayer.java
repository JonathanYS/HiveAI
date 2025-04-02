package com.example.hive.model;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Pair;


import java.util.*;

import static com.example.hive.model.Grid.WHITE_PIECES;
import static com.example.hive.model.Grid.BLACK_PIECES;
import static com.example.hive.model.State.*;


public class AIPlayer {


    private Grid gameGrid;
    private boolean isWhite;
    private boolean mustPlaceQB = false;

    private int pieceThresholdForSurroundingQB = 5;
    private double opponentQBSurroundPercent = 3;
    private int aiPieceCount = 0;

    // New weight constants from report refinements.
    private final int MOBILITY_WEIGHT = 1;
    private final int CONNECTIVITY_WEIGHT = 5;
    private final int BOARD_CONTROL_WEIGHT = 3;
    // Depth for lookahead simulation.
    private final int LOOKAHEAD_DEPTH = 2;

    public AIPlayer(Grid gameGrid, boolean isWhite) {
        this.gameGrid = gameGrid;
        this.isWhite = isWhite;
    }


    public ImageView makeMove() {
        Pair<Pair<ImageView, ImageView>, Integer> move = determineBestMove();
        if (move != null) {
            System.out.println("++++++++++++++++++++++++++++++++++++++\n" + move.getValue() + "\n++++++++++++++++++++++++++++++++++++++");
            if (move.getValue() == 0)
                gameGrid.movePiece(move.getKey().getKey(), move.getKey().getValue());
            else {
                int response = gameGrid.placePiece(isWhite, move.getKey().getValue(), move.getKey().getKey());
                if (response == 1)
                    mustPlaceQB = true;
                aiPieceCount++;
                if (aiPieceCount == 7)
                    opponentQBSurroundPercent = 2;
                else if (aiPieceCount >= 9)
                    opponentQBSurroundPercent = 1;
                return move.getKey().getValue();
            }
        }
        else
            gameGrid.advanceTurn();

        return null;
    }


    // TODO: gridState would be a copy of the grid transferred by the Grid class via a function that would deep copy the grid.
    private Pair<Pair<ImageView, ImageView>, Integer> determineBestMove() {
        Pair<ImageView, ImageView> bestMove;
        List<Pair<ImageView, ImageView>> legalMoves = gameGrid.getLegalMoves(isWhite);
        System.out.println("Legal Moves: " + legalMoves);
        List<ImageView> legalPlacements = gameGrid.getValidPlacements(isWhite);
        System.out.println("Legal Placements: " + legalPlacements);
        if (legalMoves.isEmpty() && legalPlacements.isEmpty())
            return null;

        State state;
        if (!gameGrid.getQueenPlaced().get(isWhite))
            state = OPENING;
        else
            state = CHECK_IMMEDIATE_WIN;

        while (state != FINISHED) {
            switch (state) {
                case OPENING:
                    System.out.println("OPENING");
                    // In the opening, prioritize placing the Queen Bee.
                    Pair<Pair<ImageView, ImageView>, Integer> openingMove = selectMoveOpening(legalPlacements);
                    if (openingMove != null)
                        return openingMove;
                    state = CHECK_IMMEDIATE_WIN;
                    break;

                case CHECK_IMMEDIATE_WIN:
                    System.out.println("CHECK_IMMEDIATE_WIN");
                    bestMove = checkImmediateWin(legalMoves);
                    if (bestMove != null)
                        return new Pair<>(bestMove, 0);
                    state = BLOCK_THREAT;
                    break;

                case BLOCK_THREAT:
                    System.out.println("BLOCK_THREAT");
                    bestMove = checkBlockThreat(legalMoves);
                    if (bestMove != null)
                        return new Pair<>(bestMove, 0);
                    state = SURROUND;
                    break;

                case SURROUND:
                    System.out.println("SURROUND");
                    bestMove = checkSurrounding(legalMoves);
                    if (bestMove != null)
                        return new Pair<>(bestMove, 0);
                    state = IMPROVE_MOBILITY;
                    break;

                case IMPROVE_MOBILITY:
                    System.out.println("IMPROVE_MOBILITY");
                    Pair<Pair<ImageView, ImageView>, Integer> moveByMobility = selectMoveByMobility(legalMoves, legalPlacements);
                    if (moveByMobility != null)
                        return moveByMobility;
                    state = STANDARD;
                    break;

                case STANDARD:
                    System.out.println("STANDARD");
                    Pair<Pair<ImageView, ImageView>, Integer> move = selectMoveByOverallHeuristic(legalMoves, legalPlacements);
                    if (move != null)
                        return move;
                    state = FINISHED;
                    break;
            }
        }


        System.out.println("RANDOM");
        Random random = new Random();
        boolean randomBoolean = random.nextBoolean();
        if (randomBoolean) {
            return new Pair<>(new Pair<>(legalPlacements.get(0), new ImageView(gameGrid.getPiecesCount().keySet().iterator().next().getImage())), 1);
        }
        return new Pair<>(legalMoves.get(0), 0);
    }

    // This implementation prioritizes placing the Queen Bee if it is available.
    private Pair<Pair<ImageView, ImageView>, Integer> selectMoveOpening(List<ImageView> legalPlacements) {
        Map<PieceImage, Integer> piecesCount = gameGrid.getPiecesCount();
        piecesCount.entrySet().removeIf(entry -> entry.getValue() == 0);
        int[] numbers = {10, 20, 50};
        Random random = new Random();
        System.out.println("MUST PLACE QUEEN: " + mustPlaceQB);
        if (!mustPlaceQB) {
            PieceImage chosenPiece;
            do {
                int randomCost = numbers[random.nextInt(numbers.length)];
                chosenPiece = getPieceImageByCost(randomCost);
            } while (!piecesCount.containsKey(chosenPiece));

            if (chosenPiece != null) {
                ImageView newPlacedPiece = new ImageView(chosenPiece.getImage());
                int randomPlacement = random.nextInt(legalPlacements.size());
                return new Pair<>(new Pair<>(legalPlacements.get(randomPlacement), newPlacedPiece), 1);
            }
        }
        else {
            PieceImage queenImage = getPieceImageByCost(50);
            assert queenImage != null;
            ImageView queenPiece = new ImageView(queenImage.getImage());
            int randomPlacement = random.nextInt(legalPlacements.size());
            return new Pair<>(new Pair<>(legalPlacements.get(randomPlacement), queenPiece), 1);
        }

        PieceImage defaultPiece = piecesCount.keySet().iterator().next();
        ImageView newPlacedPiece = new ImageView(defaultPiece.getImage());
        return new Pair<>(new Pair<>(legalPlacements.get(0), newPlacedPiece), 1);
    }


    /**
     * Helper method: returns a cost for placing a piece.
     * In early game, high-cost pieces (like Ants) incur a penalty.
     */
    private int getPieceCost(PieceImage piece) {
        // Adjust these cost values as needed.
        if (piece == PieceImage.ANT_WHITE || piece == PieceImage.ANT_BLACK)
            return 30; // High cost early on.
        if (piece == PieceImage.QUEEN_BEE_WHITE || piece == PieceImage.QUEEN_BEE_BLACK)
            return 50;
        if (piece == PieceImage.SPIDER_WHITE || piece == PieceImage.SPIDER_BLACK)
            return 10;
        if (piece == PieceImage.BEETLE_WHITE || piece == PieceImage.BEETLE_BLACK)
            return 20;
        if (piece == PieceImage.GRASSHOPPER_WHITE || piece == PieceImage.GRASSHOPPER_BLACK)
            return 20;
        return 1;
    }

    private PieceImage getPieceImageByCost(int cost) {
        if (cost == 30)
            return isWhite ? PieceImage.ANT_WHITE : PieceImage.ANT_BLACK;
        if (cost == 50)
            return isWhite ? PieceImage.QUEEN_BEE_WHITE : PieceImage.QUEEN_BEE_BLACK;
        if (cost == 10)
            return isWhite ? PieceImage.SPIDER_WHITE : PieceImage.SPIDER_BLACK;
        if (cost == 20) {
            Random random = new Random();
            boolean randomValue = random.nextBoolean();
            if (randomValue)
                return isWhite ? PieceImage.BEETLE_WHITE : PieceImage.BEETLE_BLACK;
            return isWhite ? PieceImage.GRASSHOPPER_WHITE : PieceImage.GRASSHOPPER_BLACK;
        }
        return null;
    }

    // TODO: Handle cases of stalemate in a better way than just returning the stalemate move if no winning move is found.
    private Pair<ImageView, ImageView> checkImmediateWin(List<Pair<ImageView, ImageView>> legalMoves) {
        PriorityQueue<Pair<Integer, Pair<ImageView, ImageView>>> bestMoves = getBestSimpleMoves(gameGrid.getGrid(), legalMoves, isWhite);
        System.out.println(bestMoves);

        while (bestMoves != null && bestMoves.peek() != null && isQueenMove(bestMoves.peek().getValue())) {
            System.out.println("Skipping queen move: " + bestMoves.peek());
            bestMoves.poll();
        }

        if (bestMoves != null && bestMoves.peek() != null) {
            System.out.println("bestMoves.peek(): " + bestMoves.peek());
            Integer key = bestMoves.peek().getKey();
            if (key == 10 || key == 5)
                return bestMoves.peek().getValue();
        }
        return null;
    }

    /**
     * Returns true if the move involves moving the queen piece.
     */
    private boolean isQueenMove(Pair<ImageView, ImageView> move) {
        ImageView source = move.getKey();
        if (isWhite) {
            return source.getImage().equals(PieceImage.QUEEN_BEE_WHITE.getImage());
        } else {
            return source.getImage().equals(PieceImage.QUEEN_BEE_BLACK.getImage());
        }
    }

    private PriorityQueue<Pair<Integer, Pair<ImageView, ImageView>>> getBestSimpleMoves(Map<HexCoordinate, Deque<ImageView>> gridCopy, List<Pair<ImageView, ImageView>> legalMoves, boolean isWhiteTurn) {
        PriorityQueue<Pair<Integer, Pair<ImageView, ImageView>>> bestMoves = new PriorityQueue<>(Comparator.comparing(Pair<Integer, Pair<ImageView, ImageView>>::getKey).reversed());
        Map<HexCoordinate, Deque<ImageView>> simulatedGridState;
        HexCoordinate opponentQueenCoord = gameGrid.getQueenCoordinate(gameGrid.getGrid(), !isWhiteTurn);
        if (opponentQueenCoord == null) { // Possible in the first 8 moves into the game.
            System.out.println("Nah");
            return null;
        }
        int numQueenNeighborsBeforeMove = gameGrid.countNeighbours(gameGrid.getGrid(), opponentQueenCoord);
        System.out.println("NUM QUEEN NEIGHBORS BEFORE: " + numQueenNeighborsBeforeMove);
        for (Pair<ImageView, ImageView> move : legalMoves) {
            System.out.println("OK");
            simulatedGridState = gameGrid.getGrid(gridCopy);
            simulatedGridState = gameGrid.movePiece(simulatedGridState, move);
            if (gameGrid.checkWin(simulatedGridState).get(isWhiteTurn) && gameGrid.checkWin(simulatedGridState).get(!isWhiteTurn)) {
                Pair<Integer, Pair<ImageView, ImageView>> pair = new Pair<>(5, move);
                bestMoves.add(pair);
            }
            else if (gameGrid.checkWin(simulatedGridState).get(isWhiteTurn) && !gameGrid.checkWin(simulatedGridState).get(!isWhiteTurn)) {
                System.out.println("Winning Move: " + gameGrid.getKeyByValue(simulatedGridState, move.getValue()));
                Pair<Integer, Pair<ImageView, ImageView>> pair = new Pair<>(10, move);
                bestMoves.add(pair);
            }
            else if (gameGrid.countNeighbours(simulatedGridState, opponentQueenCoord) > numQueenNeighborsBeforeMove && !gameGrid.checkWin(simulatedGridState).get(!isWhiteTurn)) {
                Pair<Integer, Pair<ImageView, ImageView>> pair = new Pair<>(1, move);
                bestMoves.add(pair);
            }
        }
        return bestMoves;
    }

    private List<Pair<ImageView, ImageView>> priorityQueueMovesToListMoves(PriorityQueue<Pair<Integer, Pair<ImageView, ImageView>>> bestMoves) {
        List<Pair<ImageView, ImageView>> list = new ArrayList<>();
        for (Pair<Integer, Pair<ImageView, ImageView>> item: bestMoves) {
            list.add(item.getValue());
        }
        return list;
    }


    private Pair<ImageView, ImageView> checkBlockThreat(List<Pair<ImageView, ImageView>> legalMoves) {
        HexCoordinate myQueenCoord = gameGrid.getQueenCoordinate(gameGrid.getGrid(), isWhite);
        if (myQueenCoord == null) {
            return null;
        }

        // Check how many neighbors our queen has.
        int queenNeighbors = gameGrid.countNeighbours(gameGrid.getGrid(), myQueenCoord);
        // Define a threshold under which the queen is considered safe.
        int threatThreshold = 3;
        if (queenNeighbors < threatThreshold) {
            // If the queen is not sufficiently surrounded, there's no need to block a threat.
            return null;
        }

        // Get possible moves by opponent to surround our queen.
        List<Pair<ImageView, ImageView>> legalOpponentMoves = gameGrid.getLegalMoves(!isWhite);
        List<Pair<ImageView, ImageView>> newLegalOpponentMoves;
        PriorityQueue<Pair<Integer, Pair<ImageView, ImageView>>> bestOpponentMovesPQ = getBestSimpleMoves(gameGrid.getGrid(), legalOpponentMoves, !isWhite);
        if (bestOpponentMovesPQ == null) return null;
        List<Pair<ImageView, ImageView>> bestOpponentMovesList = priorityQueueMovesToListMoves(bestOpponentMovesPQ);

        Map<HexCoordinate, Deque<ImageView>> simulatedGridState;
        int bestBlockedCount = 0;
        Pair<ImageView, ImageView> bestBlockingMove = null;

        for (Pair<ImageView, ImageView> move : legalMoves) {
            simulatedGridState = gameGrid.getGrid();
            simulatedGridState = gameGrid.movePiece(simulatedGridState, move);
            newLegalOpponentMoves = gameGrid.getLegalMoves(simulatedGridState, !isWhite);

            int blockedWinningMoves = 0;
            int blockedImportantMoves = 0;
            for (Pair<ImageView, ImageView> bestOpponentMove : bestOpponentMovesList) {
                if (!newLegalOpponentMoves.contains(bestOpponentMove)) {
                    System.out.println("BLOCKING WINNING MOVE: " + isWinningMove(bestOpponentMove, bestOpponentMovesPQ));
                    if (isWinningMove(bestOpponentMove, bestOpponentMovesPQ) && !gameGrid.checkWin(simulatedGridState).get(!isWhite)) {
                        blockedWinningMoves++; // Track if it blocks a direct win.
                    } else {
                        blockedImportantMoves++; // Track if it blocks a strong move.
                    }
                }
            }

            // 1. Always prefer blocking a winning move.
            if (blockedWinningMoves > 0) {
                System.out.println("Choosing move " + move + " because it blocks a WINNING move.");
                return move;
            }

            // 2. Otherwise, pick the move that blocks the most important moves.
            if (blockedImportantMoves > bestBlockedCount) {
                bestBlockedCount = blockedImportantMoves;
                bestBlockingMove = move;
            }
        }

        // If no winning moves are blocked, return the best available blocking move (if it meets a threshold)
        if (bestBlockedCount >= 3 && gameGrid.getPlacedPiecesCount(!isWhite) <= aiPieceCount) {
            System.out.println("Choosing best blocking move: " + bestBlockingMove + " blocking " + bestBlockedCount + " important moves.");
            return bestBlockingMove;
        }

        return null;
    }

    private boolean isWinningMove(Pair<ImageView, ImageView> move, PriorityQueue<Pair<Integer, Pair<ImageView, ImageView>>> opponentMovesPQ) {
        for (Pair<Integer, Pair<ImageView, ImageView>> rankedMove : opponentMovesPQ) {
            if (rankedMove.getKey() == 10 && rankedMove.getValue().equals(move)) {
                return true;
            }
        }
        return false;
    }

    private Pair<Pair<ImageView, ImageView>, Integer> selectMoveByMobility(List<Pair<ImageView, ImageView>> legalMoves, List<ImageView> legalPlacements) {
        Pair<Pair<ImageView, ImageView>, Integer> bestMove = null;
        int bestScore = -2000;

        int totalOpponentMovesBeforeMove = gameGrid.countTotalLegalMoves(gameGrid.getGrid(), !isWhite);

        Map<HexCoordinate, Deque<ImageView>> simulatedGridState;
        int score;
        for (Pair<ImageView, ImageView> move : legalMoves) {
            simulatedGridState = gameGrid.getGrid();
            simulatedGridState = gameGrid.movePiece(simulatedGridState, move);
            score = evaluateMobility(simulatedGridState, isWhite);
            System.out.println("SCORE: " + score);
            // Add a bias to moves so that if scores are similar, moves are preferred.
            // score += 5;
            if (aiPieceCount >= gameGrid.getPlacedPiecesCount(!isWhite))
                score += 20;

            List<Pair<ImageView, ImageView>> newLegalOpponentMoves = gameGrid.getLegalMoves(simulatedGridState, !isWhite);
            PriorityQueue<Pair<Integer, Pair<ImageView, ImageView>>> bestOpponentMovesPQ = getBestSimpleMoves(simulatedGridState, newLegalOpponentMoves, !isWhite);
            boolean hasWinningKey = bestOpponentMovesPQ.stream().anyMatch(p -> p.getKey() == 10);

            if (gameGrid.countTotalLegalMoves(simulatedGridState, !isWhite) <= totalOpponentMovesBeforeMove && !hasWinningKey) {
                if (score > bestScore) {
                    bestMove = new Pair<>(move, 0);
                    bestScore = score;
                }
            }
        }

        System.out.println("MOVING SCORE: " + bestScore);

        Pair<Map<HexCoordinate, Deque<ImageView>>, Integer> pair;
        Map<PieceImage, Integer> piecesCount = gameGrid.getPiecesCount();
        ImageView newPlacedPiece;
        for (ImageView placement : legalPlacements) {
            for (Map.Entry<PieceImage, Integer> entry : piecesCount.entrySet()) {
                if (isWhite && WHITE_PIECES.contains(entry.getKey().getImage()) || !isWhite && BLACK_PIECES.contains(entry.getKey().getImage())) {
                    newPlacedPiece = new ImageView(entry.getKey().getImage());
                    simulatedGridState = gameGrid.getGrid();
                    pair = gameGrid.placePiece(simulatedGridState, isWhite, newPlacedPiece, placement, gameGrid.getQueenPlaced(), gameGrid.getPiecesCount(), gameGrid.getMoveCount());
                    simulatedGridState = pair.getKey();
                    score = evaluateMobility(simulatedGridState, isWhite);
                    // Subtract a bias from placements so that moves win if scores are close.
                    System.out.println("AI Pieces count: " + aiPieceCount +"\nScore: " + score);

                    if (aiPieceCount < gameGrid.getPlacedPiecesCount(!isWhite))
                        score += 20;
                    if (score > bestScore) {
                        bestMove = new Pair<>(new Pair<>(placement, newPlacedPiece), 1);
                        bestScore = score;
                    }
                }
            }
        }
        System.out.println("PLACEMENT SCORE: " + bestScore);

        return bestMove;
    }

    private int evaluateMobility(Map<HexCoordinate, Deque<ImageView>> gridState, boolean isWhite) {
        return gameGrid.countTotalLegalMoves(gridState, isWhite);
    }

    /**
     * Overall heuristic function that considers both moves and placements.
     * For each legal move and each legal placement (for every available piece), it simulates the resulting board
     * and evaluates it using a weighted sum of mobility and connectivity.
     * Returns the move with the highest overall score.
     */
    private Pair<Pair<ImageView, ImageView>, Integer> selectMoveByOverallHeuristic(
            List<Pair<ImageView, ImageView>> legalMoves, List<ImageView> legalPlacements) {

        int bestScore = Integer.MIN_VALUE;
        Pair<Pair<ImageView, ImageView>, Integer> bestOverallMove = null;

        // Weights for the heuristic components
        int mobilityWeight = 1;
        int connectivityWeight = 5;

        // Evaluate all moves
        for (Pair<ImageView, ImageView> move : legalMoves) {
            // simulate state after move
            Map<HexCoordinate, Deque<ImageView>> simulatedState = gameGrid.getGrid();
            simulatedState = gameGrid.movePiece(simulatedState, move);

            int mobilityScore = evaluateMobility(simulatedState, isWhite);
            int connectivityScore = evaluateConnectivity(simulatedState, isWhite);
            int overallScore = mobilityWeight * mobilityScore + connectivityWeight * connectivityScore;

            System.out.println("Move: " + move + " | Mobility: " + mobilityScore +
                    " | Connectivity: " + connectivityScore + " | Overall: " + overallScore);

            if (overallScore > bestScore) {
                bestScore = overallScore;
                bestOverallMove = new Pair<>(move, 0);
            }
        }

        // Evaluate all placements
        Map<PieceImage, Integer> piecesCount = gameGrid.getPiecesCount();
        for (ImageView placement : legalPlacements) {
            for (Map.Entry<PieceImage, Integer> entry : piecesCount.entrySet()) {
                // Check that the piece is available for the player's color.
                if ((isWhite && WHITE_PIECES.contains(entry.getKey())) || (!isWhite && BLACK_PIECES.contains(entry.getKey()))) {
                    ImageView newPlacedPiece = new ImageView(entry.getKey().getImage());

                    Map<HexCoordinate, Deque<ImageView>> simulatedState = gameGrid.getGrid();
                    // Simulate placement; assuming the placePiece method returns a pair of (simulatedState, response)
                    Pair<Map<HexCoordinate, Deque<ImageView>>, Integer> placeResult = gameGrid.placePiece(
                            simulatedState, isWhite, newPlacedPiece, placement,
                            gameGrid.getQueenPlaced(), gameGrid.getPiecesCount(), gameGrid.getMoveCount());
                    simulatedState = placeResult.getKey();

                    int mobilityScore = evaluateMobility(simulatedState, isWhite);
                    int connectivityScore = evaluateConnectivity(simulatedState, isWhite);
                    int overallScore = mobilityWeight * mobilityScore + connectivityWeight * connectivityScore;

                    System.out.println("Placement: " + placement + " with " + entry.getKey() +
                            " | Mobility: " + mobilityScore + " | Connectivity: " + connectivityScore +
                            " | Overall: " + overallScore);

                    if (overallScore > bestScore) {
                        bestScore = overallScore;
                        bestOverallMove = new Pair<>(new Pair<>(placement, newPlacedPiece), 1);
                    }
                }
            }
        }

        return bestOverallMove;
    }

    /**
     * Evaluate connectivity by summing the number of adjacent friendly pieces for each friendly piece on the board.
     */
    private int evaluateConnectivity(Map<HexCoordinate, Deque<ImageView>> gridState, boolean isWhite) {
        int connectivityScore = 0;
        for (Map.Entry<HexCoordinate, Deque<ImageView>> entry : gridState.entrySet()) {
            ImageView piece = entry.getValue().peek();
            if (piece != null && pieceBelongsTo(piece, isWhite)) {
                int neighbors = gameGrid.countNeighbours(gridState, entry.getKey());
                connectivityScore += neighbors;
            }
        }
        return connectivityScore;
    }

    private int evaluateBoardState(Map<HexCoordinate, Deque<ImageView>> gridState, boolean isWhite) {
        int mobilityScore = evaluateMobility(gridState, isWhite);
        int connectivityScore = evaluateConnectivity(gridState, isWhite);
        int boardControlScore = evaluateBoardControl(gridState, isWhite);
        int totalScore = MOBILITY_WEIGHT * mobilityScore +
                CONNECTIVITY_WEIGHT * connectivityScore +
                BOARD_CONTROL_WEIGHT * boardControlScore;
        return totalScore;
    }


    /**
     * Helper method to check if a given ImageView belongs to the player.
     */
    private boolean pieceBelongsTo(ImageView piece, boolean isWhite) {
        // Check against the known piece images for each side.
        ArrayList<Image> pieces = isWhite ? WHITE_PIECES : BLACK_PIECES;
        for (Image pi : pieces) {
            if (piece.getImage().equals(pi))
                return true;
        }
        return false;
    }

    private Pair<ImageView, ImageView> checkSurrounding(List<Pair<ImageView, ImageView>> legalMoves) {
        if (!shouldSurroundQueen()) {return null;}
        PriorityQueue<Pair<Integer, Pair<ImageView, ImageView>>> bestMoves = getBestSimpleMoves(gameGrid.getGrid(), legalMoves, isWhite);
        int totalOpponentMovesBeforeMove =  gameGrid.countTotalLegalMoves(gameGrid.getGrid(), !isWhite);
        Map<HexCoordinate, Deque<ImageView>> simulatedGridState;
        Pair<ImageView, ImageView> tempMove, bestMove = null;

        while (bestMoves != null && !bestMoves.isEmpty()) {
            System.out.println("Skipping queen move: " + bestMoves.peek());
            tempMove = bestMoves.poll().getValue();
            simulatedGridState = gameGrid.getGrid();
            simulatedGridState = gameGrid.movePiece(simulatedGridState, tempMove);

            List<Pair<ImageView, ImageView>> newLegalOpponentMoves = gameGrid.getLegalMoves(simulatedGridState, !isWhite);
            PriorityQueue<Pair<Integer, Pair<ImageView, ImageView>>> bestOpponentMovesPQ = getBestSimpleMoves(simulatedGridState, newLegalOpponentMoves, !isWhite);
            boolean hasWinningKey = bestOpponentMovesPQ.stream().anyMatch(p -> p.getKey() == 10);

            if (gameGrid.countTotalLegalMoves(simulatedGridState, !isWhite) <= totalOpponentMovesBeforeMove && !gameGrid.checkWin(simulatedGridState).get(!isWhite) && !hasWinningKey) {
                totalOpponentMovesBeforeMove = gameGrid.countTotalLegalMoves(simulatedGridState, !isWhite);
                bestMove = tempMove;
            }

        }

        return bestMove;
    }

    private boolean shouldSurroundQueen() {
        HexCoordinate opponentQueenCoord = gameGrid.getQueenCoordinate(gameGrid.getGrid(), !isWhite);
        if (opponentQueenCoord == null) { // Possible in the first 8 moves into the game.
            return false;
        }
        int surroundedCount = gameGrid.countNeighbours(gameGrid.getGrid(), opponentQueenCoord);
        return aiPieceCount >= pieceThresholdForSurroundingQB && surroundedCount >= opponentQBSurroundPercent;
    }

    private int evaluateBoardControl(Map<HexCoordinate, Deque<ImageView>> gridState, boolean isWhite) {
        // For example, reward centralization or pieces that threaten many opponent moves.
        int controlScore = 0;
        for (Map.Entry<HexCoordinate, Deque<ImageView>> entry : gridState.entrySet()) {
            ImageView piece = entry.getValue().peek();
            if (piece != null && pieceBelongsTo(piece, isWhite)) {
                // A simple metric: the lower the coordinate values, the more central (as an example).
                controlScore += (10 - Math.abs(entry.getKey().getQ())) + (10 - Math.abs(entry.getKey().getR()));
            }
        }
        return controlScore;
    }
}

// 00:06