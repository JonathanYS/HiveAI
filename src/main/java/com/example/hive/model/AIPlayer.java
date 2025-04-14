package com.example.hive.model;

import java.util.*;

import static com.example.hive.model.PieceType.*;
import static com.example.hive.model.State.*;


public class AIPlayer {


    private GameModel gameModel;
    private PieceColor myColor;
    private boolean mustPlaceQB = false;

    private int pieceThresholdForSurroundingQB = 5;
    private double opponentQBSurroundPercent = 3;
    private int aiPieceCount = 0;

    // New weight constants from report refinements.
    private final int MOBILITY_WEIGHT = 1;
    private final int CONNECTIVITY_WEIGHT = 5;
    private final int BOARD_CONTROL_WEIGHT = 3;

    private int placementsCount = 0;

    public AIPlayer(GameModel gameModel, PieceColor myColor) {
        this.gameModel = gameModel;
        this.myColor = myColor;
    }


    public PieceWrapper makeMove() {
        Pair<? extends MoveAction, PieceWrapper> move = determineBestMove();
        if (move != null) {
            // System.out.println("++++++++++++++++++++++++++++++++++++++\n" + move.getKey().isPlacement() + "\n++++++++++++++++++++++++++++++++++++++");
            if (!move.getKey().isPlacement()) {
                gameModel.movePiece((MovementAction) move.getKey(), false);
            }
            else {
                int response = gameModel.placePiece(myColor, move.getValue(), (PlacementAction) move.getKey(), false);
                placementsCount++;
                if (response == 1)
                    mustPlaceQB = true;
                aiPieceCount++;
                if (aiPieceCount == 7)
                    opponentQBSurroundPercent = 2;
                else if (aiPieceCount >= 9)
                    opponentQBSurroundPercent = 1;
                return move.getValue();
            }
        }
        else
            gameModel.advanceTurn();

        return null;
    }


    // TODO: gridState would be a copy of the grid transferred by the Grid class via a function that would deep copy the grid.
    private Pair<? extends MoveAction, PieceWrapper> determineBestMove() {
        MoveAction bestMove;
        List<MovementAction> legalMoves = gameModel.getLegalMoves(myColor);
        System.out.println("Legal Moves: " + legalMoves);
        List<PlacementAction> legalPlacements = gameModel.getValidPlacements(myColor);
        // System.out.println("Legal Placements: " + legalPlacements);
        if (legalMoves.isEmpty() && legalPlacements.isEmpty()) {
            // System.out.println("79");
            return null;
        }

        State state;
        if (!gameModel.isQueenPlaced(myColor) || placementsCount < 4)
            state = OPENING;
        else
            state = CHECK_IMMEDIATE_WIN;

        while (state != FINISHED) {
            switch (state) {
                case OPENING:
                    System.out.println("OPENING");
                    // In the opening, prioritize placing lower ranked pieces (less strong).
                    Pair<? extends MoveAction, PieceWrapper> openingMove = selectMoveOpening(legalPlacements);
                    if (openingMove != null)
                        return openingMove;
                    state = CHECK_IMMEDIATE_WIN;
                    break;

                case CHECK_IMMEDIATE_WIN:
                    System.out.println("CHECK_IMMEDIATE_WIN");
                    bestMove = checkImmediateWin(legalMoves);
                    if (bestMove != null)
                        return new Pair<>(bestMove, null);
                    state = BLOCK_THREAT;
                    break;

                case BLOCK_THREAT:
                    System.out.println("BLOCK_THREAT");
                    bestMove = checkBlockThreat(legalMoves);
                    if (bestMove != null)
                        return new Pair<>(bestMove, null);
                    state = SURROUND;
                    break;

                case SURROUND: // TODO: can also surround with placements.
                    System.out.println("SURROUND");
                    Pair<? extends MoveAction, PieceWrapper> moveBySurrounding = checkSurrounding(legalMoves, legalPlacements);
                    if (moveBySurrounding != null)
                        return moveBySurrounding;
                    state = IMPROVE_MOBILITY;
                    break;

                case IMPROVE_MOBILITY:
                    System.out.println("IMPROVE_MOBILITY");
                    Pair<? extends MoveAction, PieceWrapper> moveByMobility = selectMoveByMobility(legalMoves, legalPlacements);
                    if (moveByMobility != null)
                        return moveByMobility;
                    state = STANDARD;
                    break;

                case STANDARD:
                    System.out.println("STANDARD");
                    Pair<? extends MoveAction, PieceWrapper> move = selectMoveByOverallHeuristic(legalMoves, legalPlacements);
                    if (move != null)
                        return move;
                    state = FINISHED;
                    break;
            }
        }


        /*
        System.out.println("RANDOM");
        Random random = new Random();
        boolean randomBoolean = random.nextBoolean();
        int randomMove;
        if (randomBoolean) {
            randomMove = random.nextInt(legalPlacements.size());
            return legalPlacements.get(randomMove);
        }
        randomMove = random.nextInt(legalMoves.size());
        return legalMoves.get(randomMove);
         */
        System.out.println("154");
        return null;
    }

    private Pair<? extends MoveAction, PieceWrapper> selectMoveOpening(List<PlacementAction> legalPlacements) {
        Map<PieceType, Integer> piecesCount = gameModel.getRemainingPiecesToPlace(myColor);
        piecesCount.entrySet().removeIf(entry -> entry.getValue() == 0);
        int[] numbers = {10, 20, 50};
        Random random = new Random();
        PieceType chosenPiece = null;

        // System.out.println("MUST PLACE QUEEN: " + mustPlaceQB);
        if (!mustPlaceQB) {
            do {
                int randomCost = numbers[random.nextInt(numbers.length)];
                chosenPiece = getPieceTypeByCost(randomCost);
            } while (!piecesCount.containsKey(chosenPiece));
        }
        else {
            chosenPiece = getPieceTypeByCost(50);
        }

        if (chosenPiece == null && !piecesCount.isEmpty()) {
            chosenPiece = piecesCount.keySet().iterator().next();
        }

        return createRandomPlacementAction(chosenPiece, legalPlacements, random);
    }

    /**
     * Helper method to create a PlacementAction.
     *
     * @param pieceType       the type of piece to place
     * @param legalPlacements list of legal placement coordinates
     * @param random          the Random instance used for selecting a random placement
     * @return a new PlacementAction with a PieceWrapper containing the piece and its ImageView
     */
    private Pair<PlacementAction, PieceWrapper> createRandomPlacementAction(PieceType pieceType, List<PlacementAction> legalPlacements, Random random) {
        // Create a PieceWrapper from the chosen piece and set its corresponding ImageView.
        PieceWrapper pieceWrapper = new PieceWrapper(new Piece(pieceType, myColor));

        // Pick a random legal placement coordinate.
        int randomPlacementIndex = random.nextInt(legalPlacements.size());
        HexCoordinate destination = legalPlacements.get(randomPlacementIndex).getDestination();

        return new Pair<>(new PlacementAction(destination), pieceWrapper);
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

    private PieceType getPieceTypeByCost(int cost) {
        if (cost == 30)
            return ANT;
        if (cost == 50)
            return QUEEN_BEE;
        if (cost == 10)
            return SPIDER;
        if (cost == 20) {
            Random random = new Random();
            boolean randomValue = random.nextBoolean();
            if (randomValue)
                return BEETLE;
            return GRASSHOPPER;
        }
        return null;
    }

    // TODO: Handle cases of stalemate in a better way than just returning the stalemate move if no winning move is found.
    private MovementAction checkImmediateWin(List<MovementAction> legalMoves) {
        PriorityQueue<Pair<Integer, MovementAction>> bestMoves = getBestSimpleMoves(gameModel.getGrid(), legalMoves, myColor);
        // System.out.println(bestMoves);

        while (bestMoves != null && bestMoves.peek() != null && isQueenMove(bestMoves.peek().getValue())) {
            // System.out.println("Skipping queen move: " + bestMoves.peek());
            bestMoves.poll();
        }

        if (bestMoves != null && bestMoves.peek() != null) {
            // System.out.println("bestMoves.peek(): " + bestMoves.peek());
            Integer key = bestMoves.peek().getKey();
            if (key == 10 || key == 5)
                return bestMoves.peek().getValue();
        }
        return null;
    }

    /**
     * Returns true if the move involves moving the queen piece.
     */
    private boolean isQueenMove(MovementAction move) {
        HexCoordinate source = move.getFrom();
        PieceWrapper sourcePiece = gameModel.getGrid().get(source).peek();
        return sourcePiece.getPiece().getType() == QUEEN_BEE;
    }

    private PriorityQueue<Pair<Integer, MovementAction>> getBestSimpleMoves(Map<HexCoordinate, Deque<PieceWrapper>> gridCopy, List<MovementAction> legalMoves, PieceColor color) {
        PriorityQueue<Pair<Integer, MovementAction>> bestMoves = new PriorityQueue<>(Comparator.comparing(Pair<Integer, MovementAction>::getKey).reversed());
        Map<HexCoordinate, Deque<PieceWrapper>> simulatedGridState;
        HexCoordinate opponentQueenCoord = gameModel.getQueenCoordinate(color.getOpposite());
        if (opponentQueenCoord == null) { // Possible in the first 8 moves into the game.
            // System.out.println("Nah");
            return null;
        }
        int numQueenNeighborsBeforeMove = gameModel.countNeighbours(gameModel.getGrid(), opponentQueenCoord);

        // System.out.println("NUM QUEEN NEIGHBORS BEFORE: " + numQueenNeighborsBeforeMove);
        for (MovementAction move : legalMoves) {
            // System.out.println("OK");
            simulatedGridState = gameModel.getGrid(gridCopy);
            simulatedGridState = gameModel.movePiece(simulatedGridState, move, true);
            if (gameModel.checkWin(simulatedGridState).get(color) && gameModel.checkWin(simulatedGridState).get(color.getOpposite())) {
                Pair<Integer, MovementAction> pair = new Pair<>(5, move);
                bestMoves.add(pair);
            }
            else if (gameModel.checkWin(simulatedGridState).get(color) && !gameModel.checkWin(simulatedGridState).get(color.getOpposite())) {
                Pair<Integer, MovementAction> pair = new Pair<>(10, move);
                bestMoves.add(pair);
            }
            else if (gameModel.countNeighbours(simulatedGridState, opponentQueenCoord) > numQueenNeighborsBeforeMove && !gameModel.checkWin(simulatedGridState).get(color.getOpposite())) {
                Pair<Integer, MovementAction> pair = new Pair<>(1, move);
                bestMoves.add(pair);
            }
        }
        return bestMoves;
    }

    private List<MovementAction> priorityQueueMovesToListMoves(PriorityQueue<Pair<Integer, MovementAction>> bestMoves) {
        List<MovementAction> list = new ArrayList<>();
        for (Pair<Integer, MovementAction> item: bestMoves) {
            list.add(item.getValue());
        }
        return list;
    }


    private MovementAction checkBlockThreat(List<MovementAction> legalMoves) {
        HexCoordinate myQueenCoord = gameModel.getQueenCoordinate(myColor);
        HexCoordinate opponentQueenCoord = gameModel.getQueenCoordinate(myColor.getOpposite());
        if (myQueenCoord == null || opponentQueenCoord == null) {return null;}

        // Check how many neighbors our queen has.
        int queenNeighbors = gameModel.countNeighbours(gameModel.getGrid(), myQueenCoord);
        // Define a threshold under which the queen is considered safe.
        //int threatThreshold = 3;
        //if (queenNeighbors < threatThreshold) {
        // If the queen is not sufficiently surrounded, there's no need to block a threat.
        //    return null;
        //}

        // Get possible moves by opponent to surround our queen.
        List<MovementAction> legalOpponentMoves = gameModel.getLegalMoves(myColor.getOpposite());
        List<MovementAction> newLegalOpponentMoves;
        PriorityQueue<Pair<Integer, MovementAction>> bestOpponentMovesPQ = getBestSimpleMoves(gameModel.getGrid(), legalOpponentMoves, myColor.getOpposite());
        if (bestOpponentMovesPQ == null) return null;
        List<MovementAction> bestOpponentMovesList = priorityQueueMovesToListMoves(bestOpponentMovesPQ);

        Map<HexCoordinate, Deque<PieceWrapper>> simulatedGridState;

        int bestBlockedCount = 0;
        MovementAction bestBlockingMove = null;
        int numLegalOpponentMoves = gameModel.countTotalLegalMoves(gameModel.getGrid(), myColor.getOpposite());

        for (MovementAction move : legalMoves) {
            simulatedGridState = gameModel.getGrid();
            simulatedGridState = gameModel.movePiece(simulatedGridState, move, true);
            newLegalOpponentMoves = gameModel.getLegalMoves(simulatedGridState, myColor.getOpposite());
            int blockedWinningMoves = 0;
            for (MovementAction bestOpponentMove : bestOpponentMovesList) {
                if (!newLegalOpponentMoves.contains(bestOpponentMove)) {
                    // System.out.println("BLOCKING WINNING MOVE: " + isWinningMove(bestOpponentMove, bestOpponentMovesPQ));
                    if (isWinningMove(bestOpponentMove, bestOpponentMovesPQ) && !gameModel.checkWin(simulatedGridState).get(myColor.getOpposite())) {
                        blockedWinningMoves++; // Track if it blocks a direct win.
                    }
                }
            }


            // 1. Always prefer blocking a winning move.
            if (blockedWinningMoves > 0) {
                System.out.println("Choosing move " + move + " because it blocks a WINNING move.");
                return move;
            }

            int currentNumLegalOpponentMoves = gameModel.countTotalLegalMoves(simulatedGridState, myColor.getOpposite());
            int numLegalOpponentMovesDiff = numLegalOpponentMoves - currentNumLegalOpponentMoves;
            PriorityQueue<Pair<Integer, MovementAction>> bestOpponentMovesAfterMyMove = getBestSimpleMoves(simulatedGridState, gameModel.getLegalMoves(simulatedGridState, myColor.getOpposite()), myColor.getOpposite());
            // 2. Otherwise, pick the move that blocks the most important moves.
            if (numLegalOpponentMovesDiff > bestBlockedCount && myQueenCoord.getNeighbors().stream().noneMatch(move.getTo().getNeighbors()::contains) && gameModel.countNeighbours(simulatedGridState, myQueenCoord) <= queenNeighbors && (bestOpponentMovesAfterMyMove == null || bestOpponentMovesAfterMyMove.isEmpty() || bestOpponentMovesAfterMyMove.peek().getKey() == 10)) {
                bestBlockedCount = numLegalOpponentMovesDiff;
                bestBlockingMove = move;
            }
        }

        // If no winning moves are blocked, return the best available blocking move (if it meets a threshold)
        if (bestBlockedCount >= 10 && gameModel.getPlacedPiecesCount(myColor.getOpposite()) <= aiPieceCount) {
            System.out.println("Choosing best blocking move: " + bestBlockingMove + " blocking " + bestBlockedCount + " important moves.");
            return bestBlockingMove;
        }

        return null;
    }

    private boolean isWinningMove(MovementAction move, PriorityQueue<Pair<Integer, MovementAction>> opponentMovesPQ) {
        for (Pair<Integer, MovementAction> rankedMove : opponentMovesPQ) {
            if (rankedMove.getKey() == 10 && rankedMove.getValue().equals(move)) {
                return true;
            }
        }
        return false;
    }

    private Pair<? extends MoveAction, PieceWrapper> selectMoveByMobility(List<MovementAction> legalMoves, List<PlacementAction> legalPlacements) {
        // Map<HexCoordinate, Deque<PieceWrapper>> gridStateBeforeMove = gameModel.getGrid();
        MoveAction bestMove = null;
        int bestScore = -2000;

        HexCoordinate opponentQBCoordinate = gameModel.getQueenCoordinate(myColor.getOpposite());
        // System.out.println("OPPONENTQB COORD: " + opponentQBCoordinate);
        PieceWrapper opponentQBImageView = gameModel.getGrid().get(opponentQBCoordinate).peek();
        int totalOpponentMovesBeforeMove = gameModel.countTotalLegalMoves(gameModel.getGrid(), myColor.getOpposite());

        Map<HexCoordinate, Deque<PieceWrapper>> simulatedGridState;
        int score;
        for (MovementAction move : legalMoves) {
            simulatedGridState = gameModel.getGrid();
            simulatedGridState = gameModel.movePiece(simulatedGridState, move, true);
            score = evaluateMobility(simulatedGridState, myColor);
            // System.out.println("SCORE: " + score);
            // Add a bias to moves so that if scores are similar, moves are preferred.
            // score += 5;
            if (aiPieceCount >= gameModel.getPlacedPiecesCount(myColor.getOpposite()))
                score += 20;
            if (opponentQBImageView != simulatedGridState.get(opponentQBCoordinate).peek()) {
                // Only way is that the move is of the beetle and the beetle got on top of the queen.
                score += 50;
            }

            List<MovementAction> newLegalOpponentMoves = gameModel.getLegalMoves(simulatedGridState, myColor.getOpposite());
            PriorityQueue<Pair<Integer, MovementAction>> bestOpponentMovesPQ = getBestSimpleMoves(simulatedGridState, newLegalOpponentMoves, myColor.getOpposite());
            boolean hasWinningKey = bestOpponentMovesPQ.stream().anyMatch(p -> p.getKey() == 10);

            if (gameModel.countTotalLegalMoves(simulatedGridState, myColor.getOpposite()) <= totalOpponentMovesBeforeMove && !hasWinningKey) {
                if (score > bestScore) {
                    bestMove = move;
                    bestScore = score;
                }
            }
        }

        System.out.println("MOVING SCORE: " + bestScore);

        Pair<Map<HexCoordinate, Deque<PieceWrapper>>, Integer> pair;
        Map<PieceType, Integer> myPiecesCount = gameModel.getRemainingPiecesToPlace(myColor);
        Map<PieceType, Integer> opponentPiecesCount = gameModel.getRemainingPiecesToPlace(myColor.getOpposite());
        PieceWrapper newPlacedPiece, pieceToPlace = null;
        for (PlacementAction placement : legalPlacements) {
            for (Map.Entry<PieceType, Integer> entry : myPiecesCount.entrySet()) {
                newPlacedPiece = new PieceWrapper(new Piece(entry.getKey(), myColor));

                simulatedGridState = gameModel.getGrid();
                PlacementAction placementAction = new PlacementAction(placement.getDestination());
                pair = gameModel.placePiece(simulatedGridState, myColor, newPlacedPiece, placementAction, true);
                simulatedGridState = pair.getKey();
                score = evaluateMobility(simulatedGridState, myColor);
                // Subtract a bias from placements so that moves win if scores are close.
                // System.out.println("AI Pieces count: " + aiPieceCount +"\nScore: " + score);

                if (aiPieceCount < gameModel.getPlacedPiecesCount(myColor.getOpposite()))
                    score += 20;
                if (score > bestScore) {
                    pieceToPlace = newPlacedPiece;
                    bestMove = placementAction;
                    bestScore = score;
                }
            }
        }

        Pair<? extends  MoveAction, PieceWrapper> returnedPair = new Pair<>(bestMove, pieceToPlace);
        System.out.println("PLACEMENT SCORE: " + bestScore);

        return returnedPair;
    }

    private int evaluateMobility(Map<HexCoordinate, Deque<PieceWrapper>> gridState, PieceColor color) {
        return gameModel.countTotalLegalMoves(gridState, color);
    }

    /**
     * Overall heuristic function that considers both moves and placements.
     * For each legal move and each legal placement (for every available piece), it simulates the resulting board
     * and evaluates it using a weighted sum of mobility and connectivity.
     * Returns the move with the highest overall score.
     */
    private Pair<? extends MoveAction, PieceWrapper> selectMoveByOverallHeuristic(
            List<MovementAction> legalMoves, List<PlacementAction> legalPlacements) {

        int bestScore = Integer.MIN_VALUE;
        MoveAction bestOverallMove = null;

        // Weights for the heuristic components.
        int mobilityWeight = 1;
        int connectivityWeight = 5;

        // Evaluate all moves.
        for (MovementAction move : legalMoves) {
            // simulate state after move.
            Map<HexCoordinate, Deque<PieceWrapper>> simulatedState = gameModel.getGrid();
            simulatedState = gameModel.movePiece(simulatedState, move, true);

            int mobilityScore = evaluateMobility(simulatedState, myColor);
            int connectivityScore = evaluateConnectivity(simulatedState, myColor);
            int overallScore = mobilityWeight * mobilityScore + connectivityWeight * connectivityScore;

            System.out.println("Move: " + move + " | Mobility: " + mobilityScore +
                    " | Connectivity: " + connectivityScore + " | Overall: " + overallScore);

            if (overallScore > bestScore) {
                bestScore = overallScore;
                bestOverallMove = move;
            }
        }

        PieceWrapper pieceToPlace = null;
        // Evaluate all placements.
        Map<PieceType, Integer> piecesCount = gameModel.getRemainingPiecesToPlace(myColor);
        for (PlacementAction placement : legalPlacements) {
            for (Map.Entry<PieceType, Integer> entry : piecesCount.entrySet()) {
                PieceWrapper pieceWrapper = new PieceWrapper(new Piece(entry.getKey(), myColor));

                Map<HexCoordinate, Deque<PieceWrapper>> simulatedState = gameModel.getGrid();
                // Simulate placement; assuming the placePiece method returns a pair of (simulatedState, response)
                PlacementAction placementAction = new PlacementAction(placement.getDestination());
                Pair<Map<HexCoordinate, Deque<PieceWrapper>>, Integer> placeResult = gameModel.placePiece(
                        simulatedState, myColor, pieceWrapper, placementAction, true);
                simulatedState = placeResult.getKey();

                int mobilityScore = evaluateMobility(simulatedState, myColor);
                int connectivityScore = evaluateConnectivity(simulatedState, myColor);
                int overallScore = mobilityWeight * mobilityScore + connectivityWeight * connectivityScore;

                System.out.println("Placement: " + placement + " with " + entry.getKey() +
                        " | Mobility: " + mobilityScore + " | Connectivity: " + connectivityScore +
                        " | Overall: " + overallScore);

                if (overallScore > bestScore) {
                    pieceToPlace = pieceWrapper;
                    bestScore = overallScore;
                    bestOverallMove = placementAction;
                }

            }
        }

        if (bestOverallMove == null) return null;
        return new Pair<>(bestOverallMove, pieceToPlace);
    }

    /**
     * Evaluate connectivity by summing the number of adjacent friendly pieces for each friendly piece on the board.
     */
    private int evaluateConnectivity(Map<HexCoordinate, Deque<PieceWrapper>> gridState, PieceColor color) {
        int connectivityScore = 0;
        for (Map.Entry<HexCoordinate, Deque<PieceWrapper>> entry : gridState.entrySet()) {
            PieceWrapper piece = entry.getValue().peek();
            if (piece != null && pieceBelongsTo(piece, color)) {
                int neighbors = gameModel.countNeighbours(gridState, entry.getKey());
                connectivityScore += neighbors;
            }
        }
        return connectivityScore;
    }

    private int evaluateBoardState(Map<HexCoordinate, Deque<PieceWrapper>> gridState, PieceColor color) {
        int mobilityScore = evaluateMobility(gridState, color);
        int connectivityScore = evaluateConnectivity(gridState, color);
        int boardControlScore = evaluateBoardControl(gridState, color);
        int totalScore = MOBILITY_WEIGHT * mobilityScore +
                CONNECTIVITY_WEIGHT * connectivityScore +
                BOARD_CONTROL_WEIGHT * boardControlScore;
        return totalScore;
    }


    /**
     * Helper method to check if a given ImageView belongs to the player.
     */
    private boolean pieceBelongsTo(PieceWrapper piece, PieceColor color) {
        return piece.getPiece().getColor() == color;
    }

    private Pair<? extends MoveAction, PieceWrapper> checkSurrounding(List<MovementAction> legalMoves, List<PlacementAction> legalPlacements) {
        if (!shouldSurroundQueen()) {return null;}

        PriorityQueue<Pair<Integer, MovementAction>> bestMovesToSurround = new PriorityQueue<>(Comparator.comparing(Pair::getKey));
        Map<HexCoordinate, Deque<PieceWrapper>> gridStateBeforeMove = gameModel.getGrid();
        PriorityQueue<Pair<Integer, MovementAction>> bestMoves = getBestSimpleMoves(gridStateBeforeMove, legalMoves, myColor);
        // System.out.println("Best moves in surrounding stage: " + bestMoves);
        int totalOpponentMovesBeforeMove =  gameModel.countTotalLegalMoves(gridStateBeforeMove, myColor.getOpposite());
        int totalOpponentMovesAfterMove;
        Map<HexCoordinate, Deque<PieceWrapper>> simulatedGridState;
        MovementAction tempMove, bestMove = null;
        PlacementAction bestPlacement = null;
        PieceWrapper pieceToPlace = null;
        HexCoordinate opponentQBCoord = gameModel.getQueenCoordinate(myColor.getOpposite());
        int numOpponentQBNeighborsBeforeMove = gameModel.countNeighbours(gridStateBeforeMove, opponentQBCoord);

        while (bestMoves != null && !bestMoves.isEmpty()) {
            // System.out.println("Skipping queen move: " + bestMoves.peek());
            tempMove = bestMoves.poll().getValue();
            simulatedGridState = gameModel.getGrid();
            simulatedGridState = gameModel.movePiece(simulatedGridState, tempMove, true);

            totalOpponentMovesAfterMove = gameModel.countTotalLegalMoves(simulatedGridState, myColor.getOpposite());
            List<MovementAction> newLegalOpponentMoves = gameModel.getLegalMoves(simulatedGridState, myColor.getOpposite());
            PriorityQueue<Pair<Integer, MovementAction>> bestOpponentMovesPQ = getBestSimpleMoves(simulatedGridState, newLegalOpponentMoves, myColor.getOpposite());
            boolean hasWinningKeyForOpponent = bestOpponentMovesPQ.stream().anyMatch(p -> p.getKey() == 10);

            if (totalOpponentMovesAfterMove <= totalOpponentMovesBeforeMove && !gameModel.checkWin(simulatedGridState).get(myColor.getOpposite()) && !hasWinningKeyForOpponent) {
                bestMovesToSurround.add(new Pair<>(getPieceCostByPieceType(gameModel.getGrid().get(tempMove.getFrom()).peek().getPiece().getType()), tempMove));
                totalOpponentMovesBeforeMove = totalOpponentMovesAfterMove;
                bestMove = tempMove;
            }

        }

        Map<PieceType, Integer> myPiecesCount = gameModel.getRemainingPiecesToPlace(myColor);
        Pair<Map<HexCoordinate, Deque<PieceWrapper>>, Integer> pair;
        for (PlacementAction placementAction : legalPlacements) {
            for (Map.Entry<PieceType, Integer> entry : myPiecesCount.entrySet()) {
                PieceWrapper pieceWrapper = new PieceWrapper(new Piece(entry.getKey(), myColor));

                simulatedGridState = gameModel.getGrid();
                pair = gameModel.placePiece(simulatedGridState, myColor, pieceWrapper, placementAction, true);
                simulatedGridState = pair.getKey();

                totalOpponentMovesAfterMove = gameModel.countTotalLegalMoves(simulatedGridState, myColor.getOpposite());
                if (gameModel.countNeighbours(simulatedGridState, opponentQBCoord) > numOpponentQBNeighborsBeforeMove && totalOpponentMovesAfterMove <= totalOpponentMovesBeforeMove && !gameModel.checkWin(simulatedGridState).get(myColor.getOpposite())) {
                    totalOpponentMovesBeforeMove = totalOpponentMovesAfterMove;
                    bestPlacement = placementAction;
                    pieceToPlace = pieceWrapper;
                }
            }
        }

        if (bestMove == null && bestPlacement == null)
            return null;
        return bestMove == null ? new Pair<>(bestPlacement, pieceToPlace) : new Pair<>(bestMove, null);
    }


    private int getPieceCostByPieceType(PieceType pieceType) {
        return switch (pieceType) {
            case QUEEN_BEE -> 50;
            case ANT -> 30;
            case GRASSHOPPER, BEETLE -> 20;
            case SPIDER -> 10;
            default -> 1;
        };
    }


    // TODO: Add a bias.
    private boolean shouldSurroundQueen() {
        HexCoordinate opponentQueenCoord = gameModel.getQueenCoordinate(myColor.getOpposite());
        if (opponentQueenCoord == null) { // Possible in the first 8 moves into the game.
            return false;
        }
        int surroundedCount = gameModel.countNeighbours(gameModel.getGrid(), opponentQueenCoord) + 1; // +1 is for bias because we want to prioritize surrounding the queen.
        return aiPieceCount >= pieceThresholdForSurroundingQB && surroundedCount >= opponentQBSurroundPercent;
    }

    private int evaluateBoardControl(Map<HexCoordinate, Deque<PieceWrapper>> gridState, PieceColor color) {
        // For example, reward centralization or pieces that threaten many opponent moves.
        int controlScore = 0;
        for (Map.Entry<HexCoordinate, Deque<PieceWrapper>> entry : gridState.entrySet()) {
            PieceWrapper piece = entry.getValue().peek();
            if (piece != null && pieceBelongsTo(piece, color)) {
                // A simple metric: the lower the coordinate values, the more central (as an example).
                controlScore += (10 - Math.abs(entry.getKey().getQ())) + (10 - Math.abs(entry.getKey().getR()));
            }
        }
        return controlScore;
    }
}

// 00:06