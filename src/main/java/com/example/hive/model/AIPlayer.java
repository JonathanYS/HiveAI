package com.example.hive.model;

import org.pcollections.PMap;
import org.pcollections.PStack;

import java.util.*;

import static com.example.hive.model.ImmutableGrid.removeBlankTiles;
import static com.example.hive.model.PieceType.*;
import static com.example.hive.model.State.*;

/**
 * The {@code AIPlayer} class represents the computer-controlled player in the Hive game.
 * It uses a Finite State Machine to determine its behavior across various phases of gameplay:
 * Opening, Immediate Win Check, Threat Blocking, Surrounding Opponent's Queen, Mobility Heuristics, and Overall Heuristics.
 * It evaluates moves based on blocking, mobility, and winning conditions.
 */
public class AIPlayer {


    private final GameModel gameModel;
    private final PieceColor myColor;
    private boolean mustPlaceQB = false;

    private final Map<ImmutableGrid, Pair<? extends MoveAction, PieceWrapper>> bestMovesCache = new HashMap<>();

    private int pieceThresholdForSurroundingQB = 5;
    private double opponentQBSurroundPercent = 3;
    private int aiPieceCount = 0;

    private int placementsCount = 0;
    private Map<MovementAction, PMap<HexCoordinate, PStack<PieceWrapper>>> moveSimulationCache;
    private Map<Pair<PlacementAction, Piece>, PMap<HexCoordinate, PStack<PieceWrapper>>> placementSimulationCache;

    /**
     * Constructs an AIPlayer with a reference to the game model and color.
     *
     * @param gameModel the game model instance
     * @param myColor the color representing this AI player
     * @timeComplexity O(1)
     * @spaceComplexity O(1)
     */
    public AIPlayer(GameModel gameModel, PieceColor myColor) {
        this.gameModel = gameModel;
        this.myColor = myColor;
    }

    /**
     * Makes the AI's move for the current turn by selecting and executing the best move.
     * It may place a piece or move an existing one based on heuristics and state.
     *
     * @return The selected move and associated piece (or null if it's a movement and not a placement),
     * or null if no move is made.
     * @timeComplexity O(n * m) where n = number of legal moves, m = evaluation cost per move.
     * @spaceComplexity O(n) for storing temporary best moves.
     */
    public Pair<? extends MoveAction, PieceWrapper> makeMove() {
        Pair<? extends MoveAction, PieceWrapper> move = null;
        ImmutableGrid currentGridState = gameModel.getImmutableGrid();
        if (!bestMovesCache.containsKey(currentGridState)) {
            move = determineBestMove();
        } else {
            return bestMovesCache.get(currentGridState);
        }
        if (move != null) {
            recordGridForBestMoves(removeBlankTiles(currentGridState.getGrid()), currentGridState.getPiecesCount(), currentGridState.getTurn(), move);
            if (!move.getKey().isPlacement()) {
                gameModel.movePiece((MovementAction) move.getKey());
            }
            else {
                int response = gameModel.placePiece(myColor, move.getValue(), (PlacementAction) move.getKey()).getValue();
                placementsCount++;
                if (response == 1)
                    mustPlaceQB = true;
                aiPieceCount++;
                if (aiPieceCount == 7)
                    opponentQBSurroundPercent = 2;
                else if (aiPieceCount >= 9)
                    opponentQBSurroundPercent = 1;
            }
        }
        else
            gameModel.advanceTurn();

        return move;
    }

    /**
     * Determines the best move available for the AI using FSM logic.
     *
     * @return A pair representing the move and piece to execute.
     * @timeComplexity O(n * m) depending on how many moves or placements are evaluated.
     * @spaceComplexity O(n) for caching simulations.
     */
    private Pair<? extends MoveAction, PieceWrapper> determineBestMove() {
        MoveAction bestMove;
        List<MovementAction> legalMoves = gameModel.getLegalMoves(myColor);
        List<PlacementAction> legalPlacements = gameModel.getValidPlacements(myColor);
        if (legalMoves.isEmpty() && legalPlacements.isEmpty()) {
            return null;
        }

        State state;
        if (!gameModel.isQueenPlaced(myColor) || placementsCount < 4)
            state = OPENING;
        else {
            state = CHECK_IMMEDIATE_WIN;
            moveSimulationCache = new HashMap<>();
            placementSimulationCache = new HashMap<>();
        }

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
        return null;
    }

    /**
     * Selects an opening move by prioritizing less powerful pieces early in the game.
     *
     * @param legalPlacements List of legal placements available.
     * @return A random placement of a suitable piece type.
     * @timeComplexity O(p) where p is number of piece types.
     * @spaceComplexity O(1)
     */
    private Pair<? extends MoveAction, PieceWrapper> selectMoveOpening(List<PlacementAction> legalPlacements) {
        Map<PieceType, Integer> piecesCount = gameModel.getRemainingPiecesToPlace(myColor);
        piecesCount.entrySet().removeIf(entry -> entry.getValue() == 0);
        int[] numbers = {10, 20, 50};
        Random random = new Random();
        PieceType chosenPiece = null;

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
     * Gets a piece type based on a cost heuristic.
     *
     * @param cost heuristic cost (10, 20, 30, 50)
     * @return a corresponding PieceType or null if there is no match
     * @timeComplexity O(1)
     * @spaceComplexity O(1)
     */
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

    /**
     * Checks if there is an immediate win available from legal moves.
     *
     * @param legalMoves List of legal movement actions.
     * @return The move that leads to an immediate win, or null if none exists.
     * @timeComplexity O(n * k + n log n)
     * @spaceComplexity O(n)
     */
    // TODO: Handle cases of stalemate in a better way than just returning the stalemate move if no winning move is found.
    private MovementAction checkImmediateWin(List<MovementAction> legalMoves) {
        PriorityQueue<Pair<Integer, MovementAction>> bestMoves = getBestSimpleMoves(gameModel.getGrid(), legalMoves, myColor);

        if (bestMoves != null && bestMoves.peek() != null) {
            Integer key = bestMoves.peek().getKey();
            if (key == 10 || key == 5) {
                return bestMoves.peek().getValue();
            }
        }
        return null;
    }

    /**
     * Returns a PriorityQueue of the best scoring simple moves in descending order.
     *
     * @param gridCopy the grid state
     * @param legalMoves list of possible moves
     * @param color the player color
     * @return a priority queue of scored moves
     * @timeComplexity O(n * k + n log n)
     * @spaceComplexity O(n)
     */
    private PriorityQueue<Pair<Integer, MovementAction>> getBestSimpleMoves(PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, List<MovementAction> legalMoves, PieceColor color) {
        PriorityQueue<Pair<Integer, MovementAction>> bestMoves = new PriorityQueue<>(Comparator.comparing(Pair<Integer, MovementAction>::getKey).reversed());
        PMap<HexCoordinate, PStack<PieceWrapper>> simulatedGridState;
        HexCoordinate opponentQueenCoord = gameModel.getQueenCoordinate(color.getOpposite());
        if (opponentQueenCoord == null) { // Possible in the first 8 moves into the game.
            return null;
        }
        int numQueenNeighborsBeforeMove = gameModel.countNeighbours(gameModel.getGrid(), opponentQueenCoord);

        for (MovementAction move : legalMoves) {
            simulatedGridState = moveSimulationCache.computeIfAbsent(move, m -> gameModel.simulateMovePiece(gridCopy, m));
            if (gameModel.checkWin(simulatedGridState, true).getKey().get(color) && gameModel.checkWin(simulatedGridState, true).getKey().get(color.getOpposite())) {
                Pair<Integer, MovementAction> pair = new Pair<>(5, move);
                bestMoves.add(pair);
            }
            else if (gameModel.checkWin(simulatedGridState, true).getKey().get(color) && !gameModel.checkWin(simulatedGridState, true).getKey().get(color.getOpposite())) {
                Pair<Integer, MovementAction> pair = new Pair<>(10, move);
                bestMoves.add(pair);
            }
            else if (gameModel.countNeighbours(simulatedGridState, opponentQueenCoord) > numQueenNeighborsBeforeMove && !gameModel.checkWin(simulatedGridState, true).getKey().get(color.getOpposite())) {
                Pair<Integer, MovementAction> pair = new Pair<>(1, move);
                bestMoves.add(pair);
            }
        }
        return bestMoves;
    }

    /**
     * Converts a priority queue of moves into a list.
     *
     * @param bestMoves priority queue of moves
     * @return a list of movement actions
     * @timeComplexity O(n)
     * @spaceComplexity O(n)
     */
    private List<MovementAction> priorityQueueMovesToListMoves(PriorityQueue<Pair<Integer, MovementAction>> bestMoves) {
        List<MovementAction> list = new ArrayList<>();
        for (Pair<Integer, MovementAction> item: bestMoves) {
            list.add(item.getValue());
        }
        return list;
    }

    /**
     * Attempts to block threats from the opponent that may lead to a loss.
     *
     * @param legalMoves List of available moves.
     * @return A defensive move that blocks a threat or null if none are found.
     * @timeComplexity O(n * m) where m = opponent move simulations.
     * @spaceComplexity O(n)
     */
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
        PriorityQueue<Pair<Integer, MovementAction>> bestOpponentMovesPQAfterMove;
        if (bestOpponentMovesPQ == null) return null;
        List<MovementAction> bestOpponentMovesList = priorityQueueMovesToListMoves(bestOpponentMovesPQ);

        PMap<HexCoordinate, PStack<PieceWrapper>> simulatedGridState;

        int bestBlockedCount = 0;
        MovementAction bestBlockingMove = null;
        int numLegalOpponentMoves = gameModel.countTotalLegalMoves(gameModel.getGrid(), myColor.getOpposite());

        for (MovementAction move : legalMoves) {
            simulatedGridState = moveSimulationCache.computeIfAbsent(move, m -> gameModel.simulateMovePiece(gameModel.getGrid(), m));
            newLegalOpponentMoves = gameModel.getLegalMoves(simulatedGridState, myColor.getOpposite());
            bestOpponentMovesPQAfterMove = getBestSimpleMoves(simulatedGridState, newLegalOpponentMoves, myColor.getOpposite());

            int blockedWinningMoves = 0;
            for (MovementAction bestOpponentMove : bestOpponentMovesList) {
                if (!newLegalOpponentMoves.contains(bestOpponentMove) || Objects.requireNonNull(bestOpponentMovesPQAfterMove).stream().anyMatch(entry -> entry.getValue().equals(bestOpponentMove) && entry.getKey() != 10)) {
                    if (isWinningMove(bestOpponentMove, bestOpponentMovesPQ) && (bestOpponentMovesPQAfterMove == null || bestOpponentMovesPQAfterMove.isEmpty() || bestOpponentMovesPQAfterMove.peek().getKey() != 10)) {
                        blockedWinningMoves++; // Track if it blocks a direct win.
                    }
                }
            }


            // 1. Always prefer blocking a winning move.
            if (blockedWinningMoves > 0) {
                return move;
            }

            int currentNumLegalOpponentMoves = gameModel.countTotalLegalMoves(simulatedGridState, myColor.getOpposite());
            int numLegalOpponentMovesDiff = numLegalOpponentMoves - currentNumLegalOpponentMoves;
            PriorityQueue<Pair<Integer, MovementAction>> bestOpponentMovesAfterMyMove = getBestSimpleMoves(simulatedGridState, gameModel.getLegalMoves(simulatedGridState, myColor.getOpposite()), myColor.getOpposite());
            // 2. Otherwise, pick the move that blocks the most important moves.
            if (numLegalOpponentMovesDiff > bestBlockedCount && myQueenCoord.getNeighbors().stream().noneMatch(move.getTo().getNeighbors()::contains) && gameModel.countNeighbours(simulatedGridState, myQueenCoord) <= queenNeighbors && (bestOpponentMovesAfterMyMove == null || bestOpponentMovesAfterMyMove.isEmpty() || bestOpponentMovesAfterMyMove.peek().getKey() == 10) && blocksWithoutLosingSurrounding(move, simulatedGridState)) {
                bestBlockedCount = numLegalOpponentMovesDiff;
                bestBlockingMove = move;
            }
        }

        // If no winning moves are blocked, return the best available blocking move (if it meets a threshold).
        if (bestBlockedCount >= 10 && gameModel.getPlacedPiecesCount(myColor.getOpposite()) <= aiPieceCount) {
            return bestBlockingMove;
        }

        return null;
    }

    /**
     * Checks whether a move maintains queen surround integrity.
     *
     * @param move the candidate move
     * @param grid the grid to validate against
     * @return true if move does not weaken surround
     * @timeComplexity O(1)
     * @spaceComplexity O(1)
     */
    private boolean blocksWithoutLosingSurrounding(MovementAction move, PMap<HexCoordinate, PStack<PieceWrapper>> grid) {
        HexCoordinate opponentQueen = gameModel.getQueenCoordinate(myColor.getOpposite());
        if (opponentQueen == null) return true; // no opponent queen yet.

        Set<HexCoordinate> queenNeighbors = opponentQueen.getNeighbors();

        // Check if the piece directly surrounds the queen.
        if (queenNeighbors.contains(move.getFrom())) {
            return false;
        }

        // Check if the piece is pinning a surrounding piece.
        return !gameModel.isPinning(grid, move, queenNeighbors);
    }

    /**
     * Checks whether a given move in the opponent's queue is a winning move.
     *
     * @param move candidate move
     * @param opponentMovesPQ opponent's ranked moves
     * @return true if the move is winning
     * @timeComplexity O(n)
     * @spaceComplexity O(1)
     */
    private boolean isWinningMove(MovementAction move, PriorityQueue<Pair<Integer, MovementAction>> opponentMovesPQ) {
        for (Pair<Integer, MovementAction> rankedMove : opponentMovesPQ) {
            if (rankedMove.getKey() == 10 && rankedMove.getValue().equals(move)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Selects a move that improves mobility while avoiding risky or poor positions.
     *
     * @param legalMoves Legal movement actions.
     * @param legalPlacements Legal placement options.
     * @return The best move that optimizes for mobility.
     * @timeComplexity O(n + m) where n = moves, m = placements.
     * @spaceComplexity O(n + m)
     */
    private Pair<? extends MoveAction, PieceWrapper> selectMoveByMobility(List<MovementAction> legalMoves, List<PlacementAction> legalPlacements) {
        HexCoordinate myQueenCoord = gameModel.getQueenCoordinate(myColor);
        MoveAction bestMove = null;
        int bestScore = -2000;

        HexCoordinate opponentQBCoordinate = gameModel.getQueenCoordinate(myColor.getOpposite());
        PieceWrapper opponentQBImageView = gameModel.getGrid().get(opponentQBCoordinate).get(0);
        int totalOpponentMovesBeforeMove = gameModel.countTotalLegalMoves(gameModel.getGrid(), myColor.getOpposite());

        PMap<HexCoordinate, PStack<PieceWrapper>> simulatedGridState;
        int score;
        for (MovementAction move : legalMoves) {
            simulatedGridState = moveSimulationCache.computeIfAbsent(move, m -> gameModel.simulateMovePiece(gameModel.getGrid(), m));
            score = evaluateMobility(simulatedGridState, myColor);
            // Add a bias to moves so that if scores are similar, moves are preferred.
            if (aiPieceCount >= gameModel.getPlacedPiecesCount(myColor.getOpposite()))
                score += 20;
            if (opponentQBImageView != simulatedGridState.get(opponentQBCoordinate).get(0)) {
                // Only way is that the move is of the beetle and the beetle got on top of the queen.
                score += 50;
            }

            int distanceAfterMove = move.getTo().distance(opponentQBCoordinate);
            int distanceBeforeMove = move.getFrom().distance(opponentQBCoordinate);
            score += (distanceBeforeMove - distanceAfterMove) * 10;

            List<MovementAction> newLegalOpponentMoves = gameModel.getLegalMoves(simulatedGridState, myColor.getOpposite());
            PriorityQueue<Pair<Integer, MovementAction>> bestOpponentMovesPQ = getBestSimpleMoves(simulatedGridState, newLegalOpponentMoves, myColor.getOpposite());
            boolean hasWinningKey = Objects.requireNonNull(bestOpponentMovesPQ).stream().anyMatch(p -> p.getKey() == 10);

            if (gameModel.countTotalLegalMoves(simulatedGridState, myColor.getOpposite()) <= totalOpponentMovesBeforeMove && !hasWinningKey && myQueenCoord.getNeighbors().stream().noneMatch(move.getTo().getNeighbors()::contains)) {
                if (score > bestScore) {
                    bestMove = move;
                    bestScore = score;
                }
            }
        }

        Map<PieceType, Integer> myPiecesCount = gameModel.getRemainingPiecesToPlace(myColor);
        PieceWrapper newPlacedPiece, pieceToPlace = null;
        for (PlacementAction placement : legalPlacements) {
            for (Map.Entry<PieceType, Integer> entry : myPiecesCount.entrySet()) {
                if (entry.getValue() > 0) {
                    newPlacedPiece = new PieceWrapper(new Piece(entry.getKey(), myColor));

                    PlacementAction placementAction = new PlacementAction(placement.getDestination());
                    Pair<PlacementAction, Piece> key = new Pair<>(placementAction, newPlacedPiece.getPiece());
                    simulatedGridState = placementSimulationCache.computeIfAbsent(key, k ->
                            gameModel.simulatePlacePiece(myColor, gameModel.getGrid(), new PieceWrapper(k.getValue()), k.getKey()).getKey()
                    );
                    score = evaluateMobility(simulatedGridState, myColor);

                    if (aiPieceCount < gameModel.getPlacedPiecesCount(myColor.getOpposite()))
                        score += 20;
                    if (score > bestScore) {
                        pieceToPlace = newPlacedPiece;
                        bestMove = placementAction;
                        bestScore = score;
                    }
                }
            }
        }

        Pair<? extends  MoveAction, PieceWrapper> returnedPair = new Pair<>(bestMove, pieceToPlace);

        return returnedPair;
    }

    /**
     * Evaluates a grid state's mobility score for a player.
     *
     * @param gridState The current game state.
     * @param color The player's color.
     * @return The total mobility score.
     * @timeComplexity O(p) where p = pieces owned by the player.
     * @spaceComplexity O(1)
     */
    private int evaluateMobility(PMap<HexCoordinate, PStack<PieceWrapper>> gridState, PieceColor color) {
        return gameModel.countTotalLegalMoves(gridState, color);
    }

    /**
     * Chooses a move based on a heuristic combining mobility and connectivity.
     *
     * @param legalMoves List of valid movement actions.
     * @param legalPlacements List of valid placement actions.
     * @return The move with the best heuristic score.
     * @timeComplexity O(n + m)
     * @spaceComplexity O(n + m)
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
            PMap<HexCoordinate, PStack<PieceWrapper>> simulatedState = gameModel.getGrid();
            simulatedState = gameModel.simulateMovePiece(simulatedState, move);

            int mobilityScore = evaluateMobility(simulatedState, myColor);
            int connectivityScore = evaluateConnectivity(simulatedState, myColor);
            int overallScore = mobilityWeight * mobilityScore + connectivityWeight * connectivityScore;

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

                PMap<HexCoordinate, PStack<PieceWrapper>> simulatedState;
                PlacementAction placementAction = new PlacementAction(placement.getDestination());
                simulatedState = gameModel.simulatePlacePiece(myColor, gameModel.getGrid(), pieceWrapper, placementAction).getKey();

                int mobilityScore = evaluateMobility(simulatedState, myColor);
                int connectivityScore = evaluateConnectivity(simulatedState, myColor);
                int overallScore = mobilityWeight * mobilityScore + connectivityWeight * connectivityScore;

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
     * Evaluates the board's connectivity score for a given color.
     *
     * @param gridState The current state of the board.
     * @param color The player being evaluated.
     * @return The total connectivity score.
     * @timeComplexity O(t) where t = total tiles.
     * @spaceComplexity O(1)
     */
    private int evaluateConnectivity(PMap<HexCoordinate, PStack<PieceWrapper>> gridState, PieceColor color) {
        int connectivityScore = 0;
        for (PMap.Entry<HexCoordinate, PStack<PieceWrapper>> entry : gridState.entrySet()) {
            PieceWrapper piece = entry.getValue().get(0);
            if (piece != null && pieceBelongsTo(piece, color)) {
                int neighbors = gameModel.countNeighbours(gridState, entry.getKey());
                connectivityScore += neighbors;
            }
        }
        return connectivityScore;
    }


    /**
     * Helper method to check if a given PieceWrapper belongs to the given player (by the player's color).
     * This method compares the color of the piece contained within the PieceWrapper to the specified player's color.
     *
     * @param piece The PieceWrapper to check.
     * @param color The color of the player (PieceColor) to compare against.
     * @return true if the piece belongs to the specified player color, false otherwise.
     */
    private boolean pieceBelongsTo(PieceWrapper piece, PieceColor color) {
        return piece.getPiece().color() == color;
    }

    /**
     * Chooses a move or placement that contributes to surrounding the opponent's queen.
     *
     * @param legalMoves List of legal movement actions.
     * @param legalPlacements List of legal placement actions.
     * @return The optimal move or placement for surrounding the queen.
     * @timeComplexity O(n + m), n = legal moves, m = placements.
     * @spaceComplexity O(n + m)
     */
    private Pair<? extends MoveAction, PieceWrapper> checkSurrounding(List<MovementAction> legalMoves, List<PlacementAction> legalPlacements) {
        if (!shouldSurroundQueen()) {return null;}
        PMap<HexCoordinate, PStack<PieceWrapper>> gridStateBeforeMove = gameModel.getGrid();
        PriorityQueue<Pair<Integer, MovementAction>> bestMoves = getBestSimpleMoves(gridStateBeforeMove, legalMoves, myColor);
        int totalOpponentMovesBeforeMove =  gameModel.countTotalLegalMoves(gridStateBeforeMove, myColor.getOpposite());
        int totalOpponentMovesAfterMove;
        PMap<HexCoordinate, PStack<PieceWrapper>> simulatedGridState;
        MovementAction tempMove, bestMove = null;
        PlacementAction bestPlacement = null;
        PieceWrapper pieceToPlace = null;
        HexCoordinate opponentQBCoord = gameModel.getQueenCoordinate(myColor.getOpposite());
        int numOpponentQBNeighborsBeforeMove = gameModel.countNeighbours(gridStateBeforeMove, opponentQBCoord);

        while (bestMoves != null && !bestMoves.isEmpty()) {
            tempMove = bestMoves.poll().getValue();
            simulatedGridState = moveSimulationCache.computeIfAbsent(tempMove, m -> gameModel.simulateMovePiece(gameModel.getGrid(), m));

            totalOpponentMovesAfterMove = gameModel.countTotalLegalMoves(simulatedGridState, myColor.getOpposite());
            List<MovementAction> newLegalOpponentMoves = gameModel.getLegalMoves(simulatedGridState, myColor.getOpposite());
            PriorityQueue<Pair<Integer, MovementAction>> bestOpponentMovesPQ = getBestSimpleMoves(simulatedGridState, newLegalOpponentMoves, myColor.getOpposite());
            boolean hasWinningKeyForOpponent = Objects.requireNonNull(bestOpponentMovesPQ).stream().anyMatch(p -> p.getKey() == 10);

            if (totalOpponentMovesAfterMove <= totalOpponentMovesBeforeMove && !gameModel.checkWin(simulatedGridState, true).getKey().get(myColor.getOpposite()) && !hasWinningKeyForOpponent) {
                totalOpponentMovesBeforeMove = totalOpponentMovesAfterMove;
                bestMove = tempMove;
            }

        }

        Map<PieceType, Integer> myPiecesCount = gameModel.getRemainingPiecesToPlace(myColor);
        Pair<Map<HexCoordinate, Deque<PieceWrapper>>, Integer> pair;
        for (PlacementAction placementAction : legalPlacements) {
            for (Map.Entry<PieceType, Integer> entry : myPiecesCount.entrySet()) {
                if (entry.getValue() > 0) {
                    PieceWrapper pieceWrapper = new PieceWrapper(new Piece(entry.getKey(), myColor));
                    Pair<PlacementAction, Piece> key = new Pair<>(placementAction, pieceWrapper.getPiece());
                    simulatedGridState = placementSimulationCache.computeIfAbsent(key, k ->
                        gameModel.simulatePlacePiece(myColor, gameModel.getGrid(), new PieceWrapper(k.getValue()), k.getKey()).getKey()
                    );

                    totalOpponentMovesAfterMove = gameModel.countTotalLegalMoves(simulatedGridState, myColor.getOpposite());
                    if (gameModel.countNeighbours(simulatedGridState, opponentQBCoord) > numOpponentQBNeighborsBeforeMove && totalOpponentMovesAfterMove <= totalOpponentMovesBeforeMove && !gameModel.checkWin(simulatedGridState, true).getKey().get(myColor.getOpposite())) {
                        totalOpponentMovesBeforeMove = totalOpponentMovesAfterMove;
                        bestPlacement = placementAction;
                        pieceToPlace = pieceWrapper;
                    }
                }
            }
        }

        if (bestMove == null && bestPlacement == null)
            return null;
        return pieceToPlace == null ? new Pair<>(bestMove, null) : new Pair<>(bestPlacement, pieceToPlace);
    }

    /**
     * Decides whether the AI should focus on surrounding the opponent's Queen Bee based on heuristics.
     *
     * @return true if the queen should be surrounded, false otherwise
     * @timeComplexity O(1)
     * @spaceComplexity O(1)
     */
    // TODO: Add a bias.
    private boolean shouldSurroundQueen() {
        HexCoordinate opponentQueenCoord = gameModel.getQueenCoordinate(myColor.getOpposite());
        if (opponentQueenCoord == null) { // Possible in the first 8 moves into the game.
            return false;
        }
        int surroundedCount = gameModel.countNeighbours(gameModel.getGrid(), opponentQueenCoord) + 1; // +1 is for bias because we want to prioritize surrounding the queen.
        return aiPieceCount >= pieceThresholdForSurroundingQB && surroundedCount >= opponentQBSurroundPercent;
    }

    /**
     * Caches the best move for a specific grid configuration to avoid re-computation.
     *
     * @param gridCopy the grid state
     * @param piecesCount remaining pieces
     * @param currentTurn current turn
     * @param bestMove the move to cache
     * @timeComplexity O(1)
     * @spaceComplexity O(1)
     */
    private void recordGridForBestMoves(PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, Map<PieceType, Integer> piecesCount, PieceColor currentTurn, Pair<? extends MoveAction, PieceWrapper> bestMove) {
        ImmutableGrid immutableGrid = new ImmutableGrid(gridCopy, piecesCount, currentTurn);
        bestMovesCache.put(immutableGrid, bestMove);
    }

}