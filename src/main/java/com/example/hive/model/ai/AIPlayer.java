package com.example.hive.model.ai;

import com.example.hive.model.enums.PieceColor;
import com.example.hive.model.enums.PieceType;
import com.example.hive.model.enums.State;
import com.example.hive.model.grid.*;
import com.example.hive.model.logic.MoveAction;
import com.example.hive.model.logic.MovementAction;
import com.example.hive.model.logic.PlacementAction;
import com.example.hive.model.utils.AutoCloseableExecutor;
import com.example.hive.model.utils.Pair;
import org.pcollections.PMap;
import org.pcollections.PStack;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static com.example.hive.model.grid.ImmutableGrid.removeBlankTiles;
import static com.example.hive.model.enums.PieceType.*;
import static com.example.hive.model.enums.State.*;

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

    private static final int pieceThresholdForSurroundingQB = 5;
    private double opponentQBSurroundPercent = 3;
    private int aiPieceCount = 0;

    private int placementsCount = 0;

    // Weights for the heuristic components.
    private static final int MOBILITY_WEIGHT = 1;
    private static final int CONNECTIVITY_WEIGHT = 5;
    private static final int LESS_OPPONENT_SURROUNDING_BONUS = 5;

    private final PieceType[] pieceTypes = PieceType.values();

    /**
     * Holds the results of processing a subset of moves for the threat-blocking phase.
     * This simple DTO is used to merge results from parallel slices of the move list.
     */
    private static class BlockResult {
        /**
         * Number of direct-win opponent threats blocked by the best move.
         */
        int bestBlockingScore = 0;

        /**
         * The move (and optional piece) that blocks the most immediate winning threats.
         */
        Pair<MoveAction, PieceWrapper> bestBlockingMove = null;

        /**
         * Reduction in the total count of opponent's legal moves achieved by the best partial-block move.
         */
        int bestPartialCount = 0;

        /**
         * The move (and optional piece) that maximizes partial blocking of an opponent mobility.
         */
        Pair<MoveAction, PieceWrapper> bestPartialMove = null;
    }

    /**
     * Constructs an AIPlayer with a reference to the game model and color.
     *
     * @param gameModel the game model instance
     * @param myColor the color representing this AI player
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
     */
    public Pair<? extends MoveAction, PieceWrapper> makeMove() {
        Pair<? extends MoveAction, PieceWrapper> move;
        ImmutableGrid currentGridState = gameModel.getImmutableGridCopy();
        currentGridState.setGrid(removeBlankTiles(currentGridState.getGrid()));
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
     */
    private Pair<? extends MoveAction, PieceWrapper> determineBestMove() {
        MoveAction bestMove;

        List<MovementAction> legalMoves = gameModel.getLegalMoves(myColor);
        List<PlacementAction> legalPlacements = gameModel.getValidPlacements(myColor);
        List<MoveAction> validMoves = new ArrayList<>();
        if (!legalMoves.isEmpty())
            validMoves.addAll(legalMoves);
        if (!legalPlacements.isEmpty())
            validMoves.addAll(legalPlacements);

        PriorityQueue<Pair<Integer, Pair<MoveAction, PieceWrapper>>> bestMoves = new PriorityQueue<>();
        PriorityQueue<Pair<Integer, Pair<MoveAction, PieceWrapper>>> oppBestMoves = new PriorityQueue<>();
        if (legalMoves.isEmpty() && legalPlacements.isEmpty()) {
            return null;
        }

        State state;
        if (!gameModel.isQueenPlaced(myColor) || placementsCount < 4)
            state = OPENING;
        else {
            List<MovementAction> oppLegalMoves = gameModel.getLegalMoves(myColor.getOpposite());
            List<PlacementAction> oppLegalPlacements = gameModel.getValidPlacements(myColor.getOpposite());
            List<MoveAction> oppValidMoves = new ArrayList<>();
            if (!oppLegalMoves.isEmpty())
                oppValidMoves.addAll(oppLegalMoves);
            if (!oppLegalPlacements.isEmpty())
                oppValidMoves.addAll(oppLegalPlacements);
            state = CHECK_IMMEDIATE_WIN;
            bestMoves = getBestSimpleMoves(gameModel.getGrid(), validMoves, myColor);
            oppBestMoves = getBestSimpleMoves(gameModel.getGrid(), oppValidMoves, myColor.getOpposite());
        }

        while (state != FINISHED) {
            switch (state) {
                case OPENING:
                    // System.out.println("OPENING"); - for debugging
                    // In the opening, prioritize placing lower ranked pieces (less strong).
                    Pair<? extends MoveAction, PieceWrapper> openingMove = selectMoveOpening(legalPlacements);
                    if (openingMove != null)
                        return openingMove;
                    state = CHECK_IMMEDIATE_WIN;
                    break;

                case CHECK_IMMEDIATE_WIN:
                    // System.out.println("CHECK_IMMEDIATE_WIN"); - for debugging
                    bestMove = checkImmediateWin(bestMoves);
                    if (bestMove != null)
                        return new Pair<>(bestMove, null);
                    state = BLOCK_THREAT;
                    break;

                case BLOCK_THREAT:
                    // System.out.println("BLOCK_THREAT"); - for debugging
                    try {
                        Pair<? extends MoveAction, PieceWrapper> moveByBlocking = checkBlockThreat(validMoves);
                        if (moveByBlocking != null)
                            return moveByBlocking;
                    } catch (Exception e) {
                        System.out.println(e);
                    }
                    state = SURROUND;
                    break;

                case SURROUND:
                    // System.out.println("SURROUND"); - for debugging
                    Pair<? extends MoveAction, PieceWrapper> moveBySurrounding = checkSurrounding(bestMoves, oppBestMoves);
                    if (moveBySurrounding != null)
                        return moveBySurrounding;
                    state = IMPROVE_MOBILITY;
                    break;

                case IMPROVE_MOBILITY:
                    // System.out.println("IMPROVE_MOBILITY"); - for debugging
                    Pair<? extends MoveAction, PieceWrapper> moveByMobility = selectMoveByMobility(legalMoves, legalPlacements, bestMoves, oppBestMoves);
                    if (moveByMobility != null)
                        return moveByMobility;
                    state = STANDARD;
                    break;

                case STANDARD:
                    // System.out.println("STANDARD"); - for debugging.
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
     * @param bestMoves PriorityQueue of AI's best moves.
     * @return The move that leads to an immediate win, or null if none exists.
     */
    private MoveAction checkImmediateWin(PriorityQueue<Pair<Integer, Pair<MoveAction, PieceWrapper>>> bestMoves) {
        if (bestMoves != null && bestMoves.peek() != null) {
            Integer key = bestMoves.peek().getKey();
            if (key == 10 || key == 5) {
                return bestMoves.peek().getValue().getKey();
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
     */
    private PriorityQueue<Pair<Integer, Pair<MoveAction, PieceWrapper>>> getBestSimpleMoves(PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, List<MoveAction> legalMoves, PieceColor color) {
        PriorityQueue<Pair<Integer, Pair<MoveAction, PieceWrapper>>> bestMoves = new PriorityQueue<>(Comparator.comparing(Pair<Integer, Pair<MoveAction, PieceWrapper>>::getKey).reversed());
        PMap<HexCoordinate, PStack<PieceWrapper>> simulatedGridState;
        HexCoordinate opponentQueenCoord = gameModel.getQueenCoordinate(color.getOpposite());
        if (opponentQueenCoord == null) { // Possible in the first 8 moves into the game.
            return null;
        }
        int numQueenNeighborsBeforeMove = gameModel.countNeighbours(gridCopy, opponentQueenCoord);

        for (MoveAction move : legalMoves) {
            if (move.isPlacement()) {
                PieceWrapper pieceWrapper = pickRandomPiece(color);
                simulatedGridState = gameModel.simulatePlacePiece(color, gridCopy, pieceWrapper, (PlacementAction) move).getKey();
            } else {
                simulatedGridState = gameModel.simulateMovePiece(gridCopy, (MovementAction) move);
            }
            if (gameModel.checkWin(simulatedGridState, true).getKey().get(color) && gameModel.checkWin(simulatedGridState, true).getKey().get(color.getOpposite())) {
                Pair<Integer, Pair<MoveAction, PieceWrapper>> pair = new Pair<>(5, new Pair<>(move, null));
                bestMoves.add(pair);
            }
            else if (gameModel.checkWin(simulatedGridState, true).getKey().get(color) && !gameModel.checkWin(simulatedGridState, true).getKey().get(color.getOpposite())) {
                Pair<Integer, Pair<MoveAction, PieceWrapper>> pair = new Pair<>(10, new Pair<>(move, null));
                bestMoves.add(pair);
            }
            else if (gameModel.countNeighbours(simulatedGridState, opponentQueenCoord) > numQueenNeighborsBeforeMove && !gameModel.checkWin(simulatedGridState, true).getKey().get(color.getOpposite())) {
                Pair<Integer, Pair<MoveAction, PieceWrapper>> pair = new Pair<>(1, new Pair<>(move, null));
                bestMoves.add(pair);
            }
        }

        return bestMoves;
    }

    /**
     * Selects a random piece type that is still available for placement for the given color.
     * <p>It repeatedly picks a random PieceType until it finds one with a positive
     * remaining count (excluding BLANK types), then wraps it in a PieceWrapper.</p>
     *
     * @param color the player color for which to pick a piece
     * @return a new PieceWrapper containing the randomly selected piece
     */
    private PieceWrapper pickRandomPiece(PieceColor color) {
        Map<PieceType, Integer> remainingPiecesToPlace = gameModel.getRemainingPiecesToPlace(color);
        Random random = new Random();
        PieceType randomType = pieceTypes[random.nextInt(pieceTypes.length)];
        while (!remainingPiecesToPlace.containsKey(randomType) || remainingPiecesToPlace.get(randomType) <= 0 || randomType == BLANK) {
            randomType = pieceTypes[random.nextInt(pieceTypes.length)];
        }
        return new PieceWrapper(new Piece(randomType, color));
    }

    /**
     * Checks whether any of the given legal moves on the provided grid copy result in an immediate win for the specified color.
     * <p>This method simulates each move (or placement) and then calls gameModel.checkWin(). It returns true as soon
     * as it finds a move that wins exclusively for the given color.</p>
     *
     * @param gridCopy the current board state as an immutable grid
     * @param legalMoves the list of legal moves or placements to test
     * @param color the color of the player to test winning moves for
     * @return true if at least one move yields an immediate win; false otherwise
     */
    private boolean hasWinningMove(PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, List<MoveAction> legalMoves, PieceColor color) {
        PMap<HexCoordinate, PStack<PieceWrapper>> simulatedGridState;
        for (MoveAction move : legalMoves) {
            if (!move.isPlacement())
                simulatedGridState = gameModel.simulateMovePiece(gridCopy, (MovementAction) move);
            else {
                PieceWrapper pieceWrapper = pickRandomPiece(color);
                simulatedGridState = gameModel.simulatePlacePiece(color, gridCopy, pieceWrapper, (PlacementAction) move).getKey();
            }
            if (gameModel.checkWin(simulatedGridState, true).getKey().get(color) && !gameModel.checkWin(simulatedGridState, true).getKey().get(color.getOpposite())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Processes a slice of the AI's legal moves to compute blocking metrics.
     * <p>This method simulates each move or placement in the provided slice, rebuilds the opponent's
     * best-simple PQ on the simulated grid, and tracks:</p>
     * <ul>
     *   <li>bestBlockingScore and move (direct-win blocks, with bonus if adjacent to opponent queen)</li>
     *   <li>bestPartialCount and move (partial-block based on opponent mobility reduction)</li>
     * </ul>
     *
     * @param slice the subset of moves to evaluate
     * @param origGrid the original board state before slicing
     * @param myQueenCoord the coordinate of this player's queen in the original grid
     * @param myQueenNeighborsAmount the original number of neighbors around this player's queen
     * @param totalOppMovesBefore the opponent's total legal move count before any slice simulation
     * @param oppBestSimple the opponent's precomputed best-simple move PQ on the original grid
     * @return a BlockResult object containing the best direct-win and partial-block metrics for this slice
     */
    private BlockResult processBlockThreatSlice(
            List<MoveAction> slice,
            PMap<HexCoordinate,PStack<PieceWrapper>> origGrid,
            HexCoordinate myQueenCoord,
            int myQueenNeighborsAmount,
            int totalOppMovesBefore,
            PriorityQueue<Pair<Integer,Pair<MoveAction,PieceWrapper>>> oppBestSimple
    ) {
        BlockResult res = new BlockResult();

        HexCoordinate opponentQueenCoord = gameModel.getQueenCoordinate(myColor.getOpposite());
        PriorityQueue<Pair<Integer, Pair<MoveAction, PieceWrapper>>> bestOpponentMovesPQAfterMove;
        PMap<HexCoordinate,PStack<PieceWrapper>> simGrid;
        PieceWrapper pw;
        for (MoveAction move : slice) {
            // simulate move.
            if (move.isPlacement()) {
                pw = pickRandomPiece(myColor);
                simGrid = gameModel.simulatePlacePiece(myColor, origGrid, pw, (PlacementAction) move).getKey();
            } else {
                simGrid = gameModel.simulateMovePiece(origGrid, (MovementAction) move);
            }

            List<MoveAction> newOppMoves = new ArrayList<>();
            newOppMoves.addAll(gameModel.getLegalMoves(simGrid, myColor.getOpposite()));
            newOppMoves.addAll(gameModel.getValidPlacements(simGrid, myColor.getOpposite()));
            bestOpponentMovesPQAfterMove = getBestSimpleMoves(simGrid, newOppMoves, myColor.getOpposite());

            int blockedWinningMoves = 0;
            for (Pair<Integer, Pair<MoveAction, PieceWrapper>> pair : oppBestSimple) {
                MoveAction bestOpponentMove = pair.getValue().getKey();
                if (!newOppMoves.contains(bestOpponentMove) || Objects.requireNonNull(bestOpponentMovesPQAfterMove).stream().anyMatch(entry -> entry.getValue().getKey().equals(bestOpponentMove) && entry.getKey() != 10)) {
                    if (pair.getKey() == 10 && (bestOpponentMovesPQAfterMove == null || bestOpponentMovesPQAfterMove.isEmpty() || bestOpponentMovesPQAfterMove.peek().getKey() != 10)) {
                        Map<PieceColor, Boolean> checkWins = gameModel.checkWin(simGrid, true).getKey();
                        if (!(checkWins.get(myColor.getOpposite()) && !checkWins.get(myColor)))
                            blockedWinningMoves++; // Track if it blocks a direct win.
                    }
                }
            }


            // 1. Always prefer blocking a winning move.
            if (blockedWinningMoves >= res.bestBlockingScore && blockedWinningMoves != 0) {
                boolean isBonus = false;
                if (move.isPlacement()) {
                    assert move instanceof PlacementAction;
                    PieceWrapper pieceWrapper = pickRandomPiece(myColor);
                    res.bestBlockingMove = new Pair<>(move, pieceWrapper);
                    isBonus = opponentQueenCoord.getNeighbors().contains(((PlacementAction) move).getDestination());
                } else {
                    assert move instanceof MovementAction;
                    if (res.bestBlockingMove == null || blocksWithoutLosingSurrounding((MovementAction) move, simGrid)) {
                        res.bestBlockingMove = new Pair<>(move, null);
                        isBonus = opponentQueenCoord.getNeighbors().contains(((MovementAction) move).getTo());
                    }
                }
                if (isBonus)
                    blockedWinningMoves += 5;
                res.bestBlockingScore = blockedWinningMoves;
            } else {
                int currentNumLegalOpponentMoves = gameModel.countTotalLegalMoves(simGrid, myColor.getOpposite());
                int numLegalOpponentMovesDiff = totalOppMovesBefore - currentNumLegalOpponentMoves;
                List<MoveAction> tempLegalOpponentMoves = new ArrayList<>();
                tempLegalOpponentMoves.addAll(gameModel.getLegalMoves(simGrid, myColor.getOpposite()));
                tempLegalOpponentMoves.addAll(gameModel.getValidPlacements(simGrid, myColor.getOpposite()));
                PriorityQueue<Pair<Integer, Pair<MoveAction, PieceWrapper>>> bestOpponentMovesAfterMyMove = getBestSimpleMoves(simGrid, tempLegalOpponentMoves, myColor.getOpposite());
                // 2. Otherwise, pick the move that blocks the most important moves.
                if (!move.isPlacement()) {
                    assert move instanceof MovementAction;
                    if (numLegalOpponentMovesDiff > res.bestPartialCount && myQueenCoord.getNeighbors().stream().noneMatch(((MovementAction) move).getTo().getNeighbors()::contains) && gameModel.countNeighbours(simGrid, myQueenCoord) <= myQueenNeighborsAmount && (bestOpponentMovesAfterMyMove == null || bestOpponentMovesAfterMyMove.isEmpty() || bestOpponentMovesAfterMyMove.peek().getKey() != 10)) {
                        if (res.bestPartialMove == null || blocksWithoutLosingSurrounding((MovementAction) move, simGrid)) {
                            int diffSurroundingsMoves = (int) (oppBestSimple.stream().filter(entry -> entry.getKey() == 1).count() - bestOpponentMovesAfterMyMove.stream().filter(entry -> entry.getKey() == 1).count());
                            res.bestPartialCount = numLegalOpponentMovesDiff + diffSurroundingsMoves * LESS_OPPONENT_SURROUNDING_BONUS;
                            res.bestPartialMove = new Pair<>(move, null);
                        }
                    }
                } else {
                    assert move instanceof PlacementAction;
                    if (numLegalOpponentMovesDiff > res.bestPartialCount && myQueenCoord.getNeighbors().stream().noneMatch(((PlacementAction) move).getDestination().getNeighbors()::contains) && gameModel.countNeighbours(simGrid, myQueenCoord) <= myQueenNeighborsAmount && (bestOpponentMovesAfterMyMove == null || bestOpponentMovesAfterMyMove.isEmpty() || bestOpponentMovesAfterMyMove.peek().getKey() != 10)) {
                        int diffSurroundingsMoves = (int) (oppBestSimple.stream().filter(entry -> entry.getKey() == 1).count() - bestOpponentMovesAfterMyMove.stream().filter(entry -> entry.getKey() == 1).count());
                        res.bestPartialCount = numLegalOpponentMovesDiff + diffSurroundingsMoves * LESS_OPPONENT_SURROUNDING_BONUS;
                        PieceWrapper pieceWrapper = pickRandomPiece(myColor);
                        res.bestPartialMove = new Pair<>(move, pieceWrapper);
                    }
                }
            }
        }

        return res;
    }

    /**
     * Splits the given legalMoves list into two halves, processes them in parallel to compute blocking metrics,
     * and merges the results to find the overall best direct-win or partial-block move.
     * <p>Uses AutoCloseableExecutor to manage a two-thread pool and merges the two BlockResult instances
     * according to the original FSM's preference: direct-win blocks first, then partial blocks.
     * </p>
     *
     * @param legalMoves the full list of legal moves to evaluate for blocking threats
     * @return the MoveAction and optional PieceWrapper that best blocks opponent threats, or null if none
     * @throws Exception if thread execution or merging is interrupted
     */
    private Pair<MoveAction, PieceWrapper> checkBlockThreat(List<MoveAction> legalMoves) throws Exception {
        PMap<HexCoordinate, PStack<PieceWrapper>> origGrid = gameModel.getGrid();
        HexCoordinate myQueenCoord = gameModel.getQueenCoordinate(myColor);
        HexCoordinate opponentQueenCoord = gameModel.getQueenCoordinate(myColor.getOpposite());
        if (myQueenCoord == null || opponentQueenCoord == null) {return null;}

        // Check how many neighbors our queen has.
        int myQueenNeighbors = gameModel.countNeighbours(origGrid, myQueenCoord);

        int totalOppMovesBefore = gameModel.countTotalLegalMoves(origGrid, myColor.getOpposite());

        // Get possible moves by opponent to surround our queen.
        List<MoveAction> legalOpponentMoves = new ArrayList<>();
        legalOpponentMoves.addAll(gameModel.getLegalMoves(myColor.getOpposite()));
        legalOpponentMoves.addAll(gameModel.getValidPlacements(myColor.getOpposite()));

        PriorityQueue<Pair<Integer, Pair<MoveAction, PieceWrapper>>> oppBestSimple = getBestSimpleMoves(origGrid, legalOpponentMoves, myColor.getOpposite());

        if (oppBestSimple == null) return null;

        // split legalMoves.
        int mid = legalMoves.size()/2;
        List<MoveAction> slice1 = legalMoves.subList(0, mid);
        List<MoveAction> slice2 = legalMoves.subList(mid, legalMoves.size());

        try (AutoCloseableExecutor ace = new AutoCloseableExecutor(2)) {
            ExecutorService exec = ace.service();
            Future<BlockResult> f1 = exec.submit(() ->
                    processBlockThreatSlice(slice1, origGrid, myQueenCoord, myQueenNeighbors, totalOppMovesBefore, oppBestSimple)
            );
            Future<BlockResult> f2 = exec.submit(() ->
                    processBlockThreatSlice(slice2, origGrid, myQueenCoord, myQueenNeighbors, totalOppMovesBefore, oppBestSimple)
            );
            exec.shutdown();

            BlockResult r1 = f1.get();
            BlockResult r2 = f2.get();

            // merge direct‐win.
            BlockResult merged = new BlockResult();
            if (r1.bestBlockingScore > r2.bestBlockingScore) {
                merged.bestBlockingScore = r1.bestBlockingScore;
                merged.bestBlockingMove = r1.bestBlockingMove;
            } else {
                merged.bestBlockingScore = r2.bestBlockingScore;
                merged.bestBlockingMove = r2.bestBlockingMove;
            }
            // merge partial.
            if (r1.bestPartialCount > r2.bestPartialCount) {
                merged.bestPartialCount = r1.bestPartialCount;
                merged.bestPartialMove = r1.bestPartialMove;
            } else {
                merged.bestPartialCount = r2.bestPartialCount;
                merged.bestPartialMove = r2.bestPartialMove;
            }

            // 1) prefer any direct‐win block.
            if (merged.bestBlockingScore > 0) {
                return merged.bestBlockingMove;
            }
            // 2) partial‐block threshold.
            if (merged.bestPartialCount >= 10
                    && gameModel.getPlacedPiecesCount(myColor.getOpposite()) <= aiPieceCount) {
                return merged.bestPartialMove;
            }
            return null;
        }
    }

    /**
     * Checks whether a move maintains queen surround integrity.
     *
     * @param move the candidate move
     * @param grid the grid to validate against
     * @return true if move does not weaken surround
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
     * Selects a move that improves mobility while avoiding risky or poor positions.
     *
     * @param legalMoves Legal movement actions.
     * @param legalPlacements Legal placement options.
     * @param bestMoves PriorityQueue of AI's best moves.
     * @param oppBestMoves PriorityQueue of Opponent's best moves.
     * @return The best move that optimizes for mobility.
     */
    private Pair<? extends MoveAction, PieceWrapper> selectMoveByMobility(List<MovementAction> legalMoves, List<PlacementAction> legalPlacements, PriorityQueue<Pair<Integer, Pair<MoveAction, PieceWrapper>>> bestMoves, PriorityQueue<Pair<Integer, Pair<MoveAction, PieceWrapper>>> oppBestMoves) {
        HexCoordinate myQueenCoord = gameModel.getQueenCoordinate(myColor);
        MoveAction bestMove = null;
        int bestScore = -2000;

        List<MoveAction> validMoves = new ArrayList<>();
        validMoves.addAll(legalMoves);
        validMoves.addAll(legalPlacements);

        HexCoordinate opponentQBCoordinate = gameModel.getQueenCoordinate(myColor.getOpposite());
        PieceWrapper opponentQBImageView = gameModel.getGrid().get(opponentQBCoordinate).get(0);
        int totalOpponentMovesBeforeMove = gameModel.countTotalLegalMoves(gameModel.getGrid(), myColor.getOpposite());

        Set<Pair<PieceType, HexCoordinate>> hypotheticDestinationsSet = new HashSet<>();
        PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy;
        if (shouldSurroundQueen()) {
            for (HexCoordinate coordinate : opponentQBCoordinate.getNeighbors()) {
                if (gameModel.getGrid().get(coordinate).get(0).getPiece().type() == BLANK) {
                    gridCopy = gameModel.getGrid();
                    gridCopy = gridCopy.plus(coordinate, gridCopy.get(coordinate).plus(pickRandomPiece(myColor)));
                    Map<PieceColor, Boolean> winner = gameModel.checkWin(gridCopy, true).getKey();
                    if (!(winner.get(myColor.getOpposite()) && !winner.get(myColor)))
                        hypotheticDestinationsSet.addAll(gameModel.getHypotheticDestinationsFrom(coordinate, myColor));
                }
            }
        }

        PMap<HexCoordinate, PStack<PieceWrapper>> simulatedGridState;
        int score;
        for (MovementAction move : legalMoves) {
            simulatedGridState = gameModel.simulateMovePiece(gameModel.getGrid(), move);
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

            List<MoveAction> newLegalOpponentMoves = new ArrayList<>();
            newLegalOpponentMoves.addAll(gameModel.getLegalMoves(simulatedGridState, myColor.getOpposite()));
            newLegalOpponentMoves.addAll(gameModel.getValidPlacements(simulatedGridState, myColor.getOpposite()));
            boolean hasWinningKey = hasWinningMove(simulatedGridState, newLegalOpponentMoves, myColor.getOpposite());
            int diffSurroundingsMoves = (int) (oppBestMoves.stream().filter(entry -> entry.getKey() == 1).count() - getBestSimpleMoves(simulatedGridState, newLegalOpponentMoves, myColor.getOpposite()).stream().filter(entry -> entry.getKey() == 1).count());

            if (gameModel.countTotalLegalMoves(simulatedGridState, myColor.getOpposite()) <= totalOpponentMovesBeforeMove && !hasWinningKey && myQueenCoord.getNeighbors().stream().noneMatch(move.getTo().getNeighbors()::contains) && diffSurroundingsMoves >= 0) {
                Pair<PieceType, HexCoordinate> pair = new Pair<>(simulatedGridState.get(move.getTo()).get(0).getPiece().type(), move.getTo());
                if (hypotheticDestinationsSet.contains(pair) && !opponentQBCoordinate.getNeighbors().contains(move.getFrom()) && bestMoves.stream().noneMatch(entry -> entry.getKey() == 1 && !entry.getValue().getKey().isPlacement() && ((MovementAction) entry.getValue().getKey()).getFrom().equals(move.getFrom()))) {
                    return new Pair<>(move, null);
                }

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
                    simulatedGridState = gameModel.simulatePlacePiece(myColor, gameModel.getGrid(), new PieceWrapper(key.getValue()), key.getKey()).getKey();
                    score = evaluateMobility(simulatedGridState, myColor);

                    if (aiPieceCount < gameModel.getPlacedPiecesCount(myColor.getOpposite()))
                        score += 50;

                    List<MoveAction> newLegalOpponentMoves = new ArrayList<>();
                    newLegalOpponentMoves.addAll(gameModel.getLegalMoves(simulatedGridState, myColor.getOpposite()));
                    newLegalOpponentMoves.addAll(gameModel.getValidPlacements(simulatedGridState, myColor.getOpposite()));
                    boolean hasWinningKey = hasWinningMove(simulatedGridState, newLegalOpponentMoves, myColor.getOpposite());
                    if (!hasWinningKey && myQueenCoord.getNeighbors().stream().noneMatch(placement.getDestination().getNeighbors()::contains)) {
                        Pair<PieceType, HexCoordinate> pair = new Pair<>(simulatedGridState.get(placement.getDestination()).get(0).getPiece().type(), placement.getDestination());
                        if (hypotheticDestinationsSet.contains(pair)) {
                            return new Pair<>(placement, new PieceWrapper(new Piece(pair.getKey(), myColor)));
                        }
                        if (score > bestScore) {
                            pieceToPlace = newPlacedPiece;
                            bestMove = placementAction;
                            bestScore = score;
                        }
                    }
                }
            }
        }

        Pair<? extends  MoveAction, PieceWrapper> returnedPair = null;
        if (bestMove != null)
            returnedPair = new Pair<>(bestMove, pieceToPlace);
        return returnedPair;
    }

    /**
     * Evaluates a grid state's mobility score for a player.
     *
     * @param gridState The current game state.
     * @param color The player's color.
     * @return The total mobility score.
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
     */
    private Pair<? extends MoveAction, PieceWrapper> selectMoveByOverallHeuristic(
            List<MovementAction> legalMoves, List<PlacementAction> legalPlacements) {

        int bestScore = Integer.MIN_VALUE;
        MoveAction bestOverallMove = null;



        // Evaluate all moves.
        for (MovementAction move : legalMoves) {
            PMap<HexCoordinate, PStack<PieceWrapper>> simulatedState = gameModel.simulateMovePiece(gameModel.getGrid(), move);

            int mobilityScore = evaluateMobility(simulatedState, myColor);
            int connectivityScore = evaluateConnectivity(simulatedState, myColor);
            int overallScore = MOBILITY_WEIGHT * mobilityScore + CONNECTIVITY_WEIGHT * connectivityScore;

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
                int overallScore = MOBILITY_WEIGHT * mobilityScore + CONNECTIVITY_WEIGHT * connectivityScore;

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
     * @param bestMoves PriorityQueue of AI's best moves.
     * @param oppBestMoves PriorityQueue of Opponent's best moves.
     * @return The optimal move or placement for surrounding the queen.
     */
    private Pair<? extends MoveAction, PieceWrapper> checkSurrounding(PriorityQueue<Pair<Integer, Pair<MoveAction, PieceWrapper>>> bestMoves, PriorityQueue<Pair<Integer, Pair<MoveAction, PieceWrapper>>> oppBestMoves) {
        if (!shouldSurroundQueen()) {return null;}
        PMap<HexCoordinate, PStack<PieceWrapper>> gridStateBeforeMove = gameModel.getGrid();
        int totalOpponentMovesBeforeMove =  gameModel.countTotalLegalMoves(gridStateBeforeMove, myColor.getOpposite());
        int totalOpponentMovesAfterMove;
        PMap<HexCoordinate, PStack<PieceWrapper>> simulatedGridState;
        Pair<MoveAction, PieceWrapper> bestMove = null;
        MoveAction tempMove;
        HexCoordinate opponentQBCoord = gameModel.getQueenCoordinate(myColor.getOpposite());
        Queue<Pair<Integer, Pair<MoveAction, PieceWrapper>>> queue = new LinkedList<>();

        while (bestMoves != null && !bestMoves.isEmpty()) {
            Pair<Integer, Pair<MoveAction, PieceWrapper>> pair = bestMoves.poll();
            queue.add(pair);
            tempMove = pair.getValue().getKey();
            if (tempMove.isPlacement()) {
                PieceWrapper pieceWrapper = pickRandomPiece(myColor);
                Pair<PlacementAction, Piece> key = new Pair<>((PlacementAction) tempMove, pieceWrapper.getPiece());
                simulatedGridState = gameModel.simulatePlacePiece(myColor, gameModel.getGrid(), new PieceWrapper(key.getValue()), key.getKey()).getKey();
            } else {
                simulatedGridState = gameModel.simulateMovePiece(gameModel.getGrid(), (MovementAction) tempMove);
            }

            totalOpponentMovesAfterMove = gameModel.countTotalLegalMoves(simulatedGridState, myColor.getOpposite());
            List<MoveAction> newLegalOpponentMoves = new ArrayList<>();
            newLegalOpponentMoves.addAll(gameModel.getLegalMoves(simulatedGridState, myColor.getOpposite()));
            newLegalOpponentMoves.addAll(gameModel.getValidPlacements(simulatedGridState, myColor.getOpposite()));
            PriorityQueue<Pair<Integer, Pair<MoveAction, PieceWrapper>>> bestOpponentMovesPQ = getBestSimpleMoves(simulatedGridState, newLegalOpponentMoves, myColor.getOpposite());
            boolean hasWinningKeyForOpponent = bestOpponentMovesPQ != null && bestOpponentMovesPQ.peek() != null && bestOpponentMovesPQ.peek().getKey() == 10;
            int diffSurroundingsMoves = (int) (oppBestMoves.stream().filter(entry -> entry.getKey() == 1).count() - bestOpponentMovesPQ.stream().filter(entry -> entry.getKey() == 1).count());

            if (totalOpponentMovesAfterMove <= totalOpponentMovesBeforeMove && !gameModel.checkWin(simulatedGridState, true).getKey().get(myColor.getOpposite()) && !hasWinningKeyForOpponent && diffSurroundingsMoves >= 0) {
                totalOpponentMovesBeforeMove = totalOpponentMovesAfterMove;
                if (tempMove.isPlacement()) {
                    if (bestMove == null || newLegalOpponentMoves.stream().noneMatch(move -> move instanceof MovementAction && ((MovementAction) move).getFrom().equals(opponentQBCoord)))
                            bestMove = new Pair<>(tempMove, pickRandomPiece(myColor));
                } else {
                    if (bestMove == null || newLegalOpponentMoves.stream().noneMatch(move -> move instanceof MovementAction && ((MovementAction) move).getFrom().equals(opponentQBCoord)))
                        bestMove = new Pair<>(tempMove, null);
                }
            }

        }

        while (!queue.isEmpty())
            bestMoves.add(queue.poll());

        return bestMove;
    }

    /**
     * Decides whether the AI should focus on surrounding the opponent's Queen Bee based on heuristics.
     *
     * @return true if the queen should be surrounded, false otherwise
     */
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
     */
    private void recordGridForBestMoves(PMap<HexCoordinate, PStack<PieceWrapper>> gridCopy, Map<PieceType, Integer> piecesCount, PieceColor currentTurn, Pair<? extends MoveAction, PieceWrapper> bestMove) {
        ImmutableGrid immutableGrid = new ImmutableGrid(gridCopy, piecesCount, currentTurn);
        bestMovesCache.put(immutableGrid, bestMove);
    }

}
