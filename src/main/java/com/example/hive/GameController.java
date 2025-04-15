package com.example.hive;

import com.example.hive.model.*;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.pcollections.PMap;
import org.pcollections.PStack;

import java.io.IOException;
import java.util.*;
import java.util.List;

import static com.example.hive.model.PieceColor.*;

public class GameController {

    private Pane gameBoardPane;

    private int HEX_SIZE = 25;
    private GameModel gameModel;
    private Stage primaryStage;
    private ImageView selectedPiece = null;
    private List<? extends MoveAction> currentDisplayedPlacements = null;
    private PieceColor humanPiecesColor = null; // Indicates if the human player is the white pieces or the black.
    private int movesCount;
    private boolean isQueenBeePlaced;
    private VBox piecesPanel;
    private ScrollPane scrollPane;
    private ArrayList<ImageView> disabledPieces = new ArrayList<>();
    private ArrayList<ImageView> whitePlacementPieces = new ArrayList<>();
    private ArrayList<ImageView> blackPlacementPieces = new ArrayList<>();

    private ArrayList<PieceWrapper> blackPanelPieces = new ArrayList<>(5);
    private ArrayList<PieceWrapper> whitePanelPieces = new ArrayList<>(5);

    private volatile boolean moveMade;
    private boolean[] disableAllPiecesExceptOfQueenBee = new boolean[2];
    private boolean[] isQueenBeeForcedPlaced = {false, false};
    private PieceColor currentTurn = WHITE; // Current turn is white's.

    private BorderPane root;
    private AIPlayer aiPlayer;

    private MoveAction markedMove = null;


    public GameController(Stage stage, PieceColor humanPiecesColor) {
        primaryStage = stage;
        this.humanPiecesColor = humanPiecesColor;
        movesCount = 0;
        isQueenBeePlaced = false;
        gameModel = new GameModel();
        aiPlayer = new AIPlayer(gameModel, humanPiecesColor.getOpposite());
    }

    public GameController(Stage stage) {
        primaryStage = stage;
        movesCount = 0;
        isQueenBeePlaced = false;
        gameModel = new GameModel();
    }

    /**
     * Sets up the initial scene with the game board and the pieces panel.
     */
    public void setGameScene() {

        gameBoardPane = new Pane();
        gameBoardPane.setStyle("-fx-background-color: #444244;");
        gameBoardPane.setPrefSize(3000, 3000);

        Group group = new Group(gameBoardPane); // Allows overflow beyond pref size
        StackPane centeredContainer = new StackPane(group); // This will center the group

        scrollPane = new ScrollPane();
        Platform.runLater(() -> {
            Node viewport = scrollPane.lookup(".viewport");
            if (viewport != null) {
                viewport.setStyle("-fx-background-color: #444244;");
            }
        });
        scrollPane.setContent(centeredContainer);
        scrollPane.setPannable(true);
        scrollPane.addEventFilter(MouseEvent.DRAG_DETECTED, event -> {
            if (event.getButton() != MouseButton.MIDDLE) {
                event.consume();
            }
        });


        scrollPane.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            if ((event.isPrimaryButtonDown() || event.isSecondaryButtonDown()) && !isInsideScrollbar(event) || (event.isMiddleButtonDown() && isInsideScrollbar(event))) {
                event.consume(); // Block left button drag panning
            }
        });


        root = new BorderPane();
        root.setCenter(scrollPane);


        piecesPanel = new VBox(10);
        piecesPanel.setStyle("-fx-background-color: lightgray; -fx-padding: 10;");

        ImageView queenBeeBlackView = new ImageView(PieceImage.QUEEN_BEE_BLACK.getImage());
        ImageView antBlackView = new ImageView(PieceImage.ANT_BLACK.getImage());
        ImageView spiderBlackView = new ImageView(PieceImage.SPIDER_BLACK.getImage());
        ImageView grasshopperBlackView = new ImageView(PieceImage.GRASSHOPPER_BLACK.getImage());
        ImageView beetleBlackView = new ImageView(PieceImage.BEETLE_BLACK.getImage());
        blackPanelPieces.add(new PieceWrapper(new Piece(PieceType.QUEEN_BEE, BLACK), queenBeeBlackView));
        blackPanelPieces.add(new PieceWrapper(new Piece(PieceType.ANT, BLACK), antBlackView));
        blackPanelPieces.add(new PieceWrapper(new Piece(PieceType.SPIDER, BLACK), spiderBlackView));
        blackPanelPieces.add(new PieceWrapper(new Piece(PieceType.GRASSHOPPER, BLACK), grasshopperBlackView));
        blackPanelPieces.add(new PieceWrapper(new Piece(PieceType.BEETLE, BLACK), beetleBlackView));



        ImageView queenBeeWhiteView = new ImageView(PieceImage.QUEEN_BEE_WHITE.getImage());
        ImageView antWhiteView = new ImageView(PieceImage.ANT_WHITE.getImage());
        ImageView spiderWhiteView = new ImageView(PieceImage.SPIDER_WHITE.getImage());
        ImageView grasshopperWhiteView = new ImageView(PieceImage.GRASSHOPPER_WHITE.getImage());
        ImageView beetleWhiteView = new ImageView(PieceImage.BEETLE_WHITE.getImage());
        whitePanelPieces.add(new PieceWrapper(new Piece(PieceType.QUEEN_BEE, WHITE), queenBeeWhiteView));
        whitePanelPieces.add(new PieceWrapper(new Piece(PieceType.ANT, WHITE), antWhiteView));
        whitePanelPieces.add(new PieceWrapper(new Piece(PieceType.SPIDER, WHITE), spiderWhiteView));
        whitePanelPieces.add(new PieceWrapper(new Piece(PieceType.GRASSHOPPER, WHITE), grasshopperWhiteView));
        whitePanelPieces.add(new PieceWrapper(new Piece(PieceType.BEETLE, WHITE), beetleWhiteView));

        Collections.addAll(whitePlacementPieces, queenBeeWhiteView, antWhiteView, spiderWhiteView, grasshopperWhiteView, beetleWhiteView);
        Collections.addAll(blackPlacementPieces, queenBeeBlackView, antBlackView, spiderBlackView, grasshopperBlackView, beetleBlackView);

        piecesPanel.getChildren().addAll(
                queenBeeBlackView,
                antBlackView,
                spiderBlackView,
                grasshopperBlackView,
                beetleBlackView
        );


        Separator separator = new Separator();
        separator.setStyle("-fx-pref-width: 100%; -fx-padding: 10;");
        piecesPanel.getChildren().add(separator);

        piecesPanel.getChildren().addAll(
                queenBeeWhiteView,
                antWhiteView,
                spiderWhiteView,
                grasshopperWhiteView,
                beetleWhiteView
        );


        piecesPanel.setPrefWidth(200);

        root.setRight(piecesPanel);

        Scene scene = new Scene(root, 800, 600);
        primaryStage.setMaximized(true);
        primaryStage.setTitle("Hive Game");
        primaryStage.setScene(scene);
        primaryStage.show();

        initializeGame();
    }

    private boolean isInsideScrollbar(MouseEvent event) {
        // Get the viewport's bounds and the scrollbars' bounds
        Set<Node> scrollBars = scrollPane.lookupAll(".scroll-bar");

        for (Node node : scrollBars) {
            if (node instanceof ScrollBar) {
                Bounds bounds = node.localToScene(node.getBoundsInLocal());
                if (bounds.contains(event.getSceneX(), event.getSceneY())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Updates the hex grid to properly position the pieces based on the grid.
     */
    private void updateHexGrid() {
        gameBoardPane.getChildren().clear();


        double centerX = gameBoardPane.getWidth() / 2;
        double centerY = gameBoardPane.getHeight() / 2;

        // Map<HexCoordinate, Deque<PieceWrapper>> grid = gameModel.getGrid();
        PMap<HexCoordinate, PStack<PieceWrapper>> immutableGrid = gameModel.getImmutableGrid();
        // System.out.println(isGridEqual(grid, immutableGrid));

        for (PMap.Entry<HexCoordinate, PStack<PieceWrapper>> entry : immutableGrid.entrySet()) {
            HexCoordinate hexCoordinate = entry.getKey();
            PieceWrapper pieceWrapper = entry.getValue().get(0);
            ImageView pieceImageView = (pieceWrapper != null) ? pieceWrapper.getImageView() : new ImageView(PieceImage.BLANK_TILE.getImage());

            Point2D newPixelPosition = hex_to_pixel(hexCoordinate, centerX, centerY);

            pieceImageView.setLayoutX(newPixelPosition.getX());
            pieceImageView.setLayoutY(newPixelPosition.getY());

            if (!gameBoardPane.getChildren().contains(pieceImageView)) {
                gameBoardPane.getChildren().add(pieceImageView);
            }

        }
        gameBoardPane.requestLayout();
    }

    /**
     * Enables interaction with the placed pieces based on the current turn.
     */
    private void enablePlacedPieces() {
        List<PieceImage> pieceImages = new ArrayList<>();
        if (gameModel.canMovePieces(currentTurn)) {
            if (currentTurn == WHITE) {
                if (aiPlayer == null || humanPiecesColor == currentTurn)
                    pieceImages = PieceImage.getPiecesByColor(WHITE);
            } else if (currentTurn == BLACK) {
                if (aiPlayer == null || humanPiecesColor == currentTurn)
                    pieceImages = PieceImage.getPiecesByColor(PieceColor.BLACK);
            }
        }

        // Map<HexCoordinate, Deque<PieceWrapper>> grid = gameModel.getGrid();
        PMap<HexCoordinate, PStack<PieceWrapper>> immutableGrid = gameModel.getImmutableGrid();
        for (PMap.Entry<HexCoordinate, PStack<PieceWrapper>> entry : immutableGrid.entrySet()) {
            PieceWrapper pieceWrapper = entry.getValue().get(0);
            assert pieceWrapper != null;
            ImageView pieceImageView = pieceWrapper.getImageView();
            if (pieceImageView.getImage() != PieceImage.BLANK_TILE.getImage()) {
                if (pieceImages.contains(getPieceTypeFromImageView(pieceImageView))) {
                    pieceImageView.setOnMouseClicked(event -> placedPieceMouseClickedEvent(pieceImageView));
                    pieceImageView.setOnMouseEntered(event -> placedPieceMouseEnteredEvent(pieceImageView));
                    pieceImageView.setOnMouseExited(event -> placedPieceMouseExitedEvent(pieceImageView));
                } else {
                    if (!markedMove.isPlacement()) {
                        if (((MovementAction) markedMove).getFrom() != entry.getKey() && ((MovementAction) markedMove).getTo() != entry.getKey())
                            pieceImageView.setStyle(null);
                    } else {
                        if (((PlacementAction) markedMove).getDestination() != entry.getKey())
                            pieceImageView.setStyle(null);
                    }
                    pieceImageView.setOnMouseClicked(null);
                    pieceImageView.setOnMouseEntered(null);
                    pieceImageView.setOnMouseExited(null);
                }
            }
        }
    }

    /**
     * Initializes the game, sets up the grid, and starts the game loop in a separate thread.
     */
    private void initializeGame() {
        displayStartingGrid();

        Thread gameLoopThread = new Thread(this::gameLoop);
        gameLoopThread.setDaemon(true);
        gameLoopThread.start();
    }

    /**
     * The main game loop that manages the turns and checks for a win.
     */
    private void gameLoop() {
        boolean stopping = false;
        while (!stopping) {
            currentTurn = gameModel.getTurn();
            System.out.println("getTurn: " + currentTurn);
            enablePlacedPieces();
            moveMade = false;
            if (currentTurn == WHITE) {
                if (aiPlayer == null || humanPiecesColor == WHITE) {
                    // Ensure UI updates are run on the JavaFX Application Thread.
                    Platform.runLater(() -> {
                        if (disableAllPiecesExceptOfQueenBee[currentTurn == WHITE ? 1 : 0])
                            disableAllPiecesExceptOfQueenBee();
                        else
                            makeSelectablePieces(whitePlacementPieces);
                        makeUnSelectablePieces(blackPlacementPieces);
                    });
                }
            }
            else {
                if (aiPlayer == null || humanPiecesColor == BLACK) {
                    Platform.runLater(() -> {
                        if (disableAllPiecesExceptOfQueenBee[currentTurn == WHITE ? 1 : 0])
                            disableAllPiecesExceptOfQueenBee();
                        else
                            makeSelectablePieces(blackPlacementPieces);
                        makeUnSelectablePieces(whitePlacementPieces);
                    });
                }
            }

            if (aiPlayer != null && currentTurn != humanPiecesColor) {
                unmarkMove();
                makeUnSelectablePieces(blackPlacementPieces);
                makeUnSelectablePieces(whitePlacementPieces);
                Thread aiMove = getAiMove();
                aiMove.start();
            }

            // Wait until the move is made.
            while (!moveMade) {
                try {
                    Thread.sleep(100); // Prevent CPU overuse.
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return; // Exit if interrupted.
                }
            }

            // unmarkMove();
            markMove();
            Platform.runLater(this::updateHexGrid);
            stopping = checkWin();
            // gameGrid.advanceTurn();

        }

    }

    private Thread getAiMove() {
        Thread aiMove = new Thread(() -> {
            Pair<? extends MoveAction, PieceWrapper> pair = aiPlayer.makeMove();
            PieceWrapper piece = pair.getValue();
            markedMove = pair.getKey();
            moveMade = true;
            if (piece != null) {
                ImageView pieceImageView = getPieceImageView(piece);
                int pieceCount = gameModel.getRemainingPiecesToPlace(piece.getPiece().getColor(), piece.getPiece().getType());
                // System.out.println("pieceCount: " + pieceCount);
                if (pieceCount == 0 && pieceImageView != null) {
                    ColorAdjust grayScale = new ColorAdjust();
                    grayScale.setSaturation(-1);
                    pieceImageView.setEffect(grayScale);
                    pieceImageView.setOnMouseEntered(null);
                    pieceImageView.setOnMouseExited(null);
                    pieceImageView.setOnMouseClicked(null);
                    disabledPieces.add(pieceImageView);
                    try {
                        Thread.sleep(10000); // Prevent CPU overuse
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        System.exit(-1);
                    }
                }
            }
        });
        aiMove.setDaemon(true);
        return aiMove;
    }

    private ImageView getPieceImageView(PieceWrapper piece) {
        ImageView pieceImageView = null;
        if (currentTurn == WHITE) {
            for (PieceWrapper pieceWrapper : whitePanelPieces) {
                if (pieceWrapper.getPiece().getType() == piece.getPiece().getType())
                    pieceImageView = pieceWrapper.getImageView();
            }
        }
        else if (currentTurn == BLACK) {
            for (PieceWrapper pieceWrapper : blackPanelPieces) {
                if (pieceWrapper.getPiece().getType() == piece.getPiece().getType())
                    pieceImageView = pieceWrapper.getImageView();
            }
        }
        return pieceImageView;
    }

    private boolean checkWin() {
        boolean stopping;
        Map<PieceColor, Boolean> winner;
        winner = gameModel.checkWin();
        stopping = winner.get(WHITE) || winner.get(BLACK);
        // System.out.println("STOPPING: " + stopping);
        if (stopping) {
            String message;
            if (winner.get(WHITE)) {
                if (winner.get(BLACK)) {
                    message = "It is a stalemate!";
                    HexCoordinate whiteQB = gameModel.getQueenCoordinate(WHITE);
                    HexCoordinate blackQB = gameModel.getQueenCoordinate(BLACK);
                    markWinningSurrounding(whiteQB.getNeighbors());
                    markWinningSurrounding(blackQB.getNeighbors());
                } else {
                    message = "White Won!";
                    HexCoordinate blackQB = gameModel.getQueenCoordinate(BLACK);
                    // System.out.println("BLACK QB Coordinate: " + blackQB);
                    markWinningSurrounding(blackQB.getNeighbors());
                }
            } else {
                message = "Black Won!";
                HexCoordinate whiteQB = gameModel.getQueenCoordinate(WHITE);
                markWinningSurrounding(whiteQB.getNeighbors());
            }
            // System.out.println(message);
            try {
                Thread.sleep(5000);
                loadWinScreen(message);
            } catch (IOException e) {
                System.out.println("[IOException] when loading Win Screen.");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return stopping;
    }

    private void markWinningSurrounding(ArrayList<HexCoordinate> winningSurroundingCoords) {
        unmarkMove();
        // Map<HexCoordinate, Deque<PieceWrapper>> gameGrid = gameModel.getGrid();
        PMap<HexCoordinate, PStack<PieceWrapper>> gameGrid = gameModel.getImmutableGrid();
        for (HexCoordinate coordinate : winningSurroundingCoords) {
            // System.out.println("Surrounding Coordinate: "+ coordinate);
            ImageView imageView = gameGrid.get(coordinate).get(0).getImageView();
            Platform.runLater(() -> {imageView.setStyle("-fx-effect: innershadow(three-pass-box, red, 3, 1.0, 0, 0);");});
        }
    }

    private void loadWinScreen(String winner) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(GameController.class.getResource("win-screen.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 600);

        WinScreenController controller = fxmlLoader.getController();
        if (controller == null) {
            System.out.println("Controller is null! FXML might not be set correctly.");
        } else {
            controller.setWinnerLabel(winner);
        }

        Platform.runLater(() -> {
            primaryStage.setScene(scene);
            primaryStage.setMaximized(true);
            primaryStage.show();
        });
    }

    /**
     * Converts hex coordinates to pixel coordinates for positioning on the screen.
     *
     * @param hexCoord the hex coordinate to convert
     * @param centerX the center X coordinate of the game board
     * @param centerY the center Y coordinate of the game board
     * @return the calculated pixel coordinates
     */
    private Point2D hex_to_pixel(HexCoordinate hexCoord, double centerX, double centerY) {
        double x = HEX_SIZE * ((3.0 / 2.0) * hexCoord.getQ());
        double y = HEX_SIZE * (Math.sqrt(3.0) / 2 * hexCoord.getQ() + Math.sqrt(3.0) * hexCoord.getR());

        x += centerX;
        y += centerY;

        return new Point2D(x, y);
    }

    /**
     * Displays the initial grid by updating the hex grid.
     */
    private void displayStartingGrid() {
        Platform.runLater(() -> {
            scrollPane.setHvalue(0.5);
            scrollPane.setVvalue(0.5);
        });
        updateHexGrid();
    }

    /**
     * Makes the pieces selectable by adding event handlers for mouse clicks and hover events.
     *
     * @param imagesViews the list of ImageView pieces to make selectable
     */
    private void makeSelectablePieces(ArrayList<ImageView> imagesViews) {
        for (ImageView imageView : imagesViews) {
            if (!disabledPieces.contains(imageView)) {
                imageView.setOnMouseClicked(event -> panelPieceMouseClickedEvent(imageView));
                imageView.setOnMouseEntered(event -> panelPieceMouseEnteredEvent(imageView));
                imageView.setOnMouseExited(event -> panelPieceMouseExitedEvent(imageView));
            }
        }
    }

    /**
     * Makes the pieces unselectable by removing their event handlers.
     *
     * @param imagesViews the list of ImageView pieces to make unselectable
     */
    private void makeUnSelectablePieces(ArrayList<ImageView> imagesViews) {
        for (ImageView imageView : imagesViews) {
            if (!disabledPieces.contains(imageView)) {
                imageView.setStyle("");
                imageView.setOnMouseClicked(null);
                imageView.setOnMouseEntered(null);
                imageView.setOnMouseExited(null);
            }
        }
    }

    /**
     * Handles the mouse click event for placed pieces on the board.
     * Deselects the currently selected piece if it's clicked again.
     * If a different piece is clicked, highlights it and displays valid movements for it.
     *
     * @param imageView The ImageView representing the clicked piece.
     */
    private void placedPieceMouseClickedEvent(ImageView imageView) {
        unmarkMove();
        if (selectedPiece != null && imageView == selectedPiece) {
            stopDisplayValidMoves(currentDisplayedPlacements);
            selectedPiece.setStyle("");
            selectedPiece = null;
        } else {
            if (selectedPiece != null) {
                selectedPiece.setStyle("");
                stopDisplayValidMoves(currentDisplayedPlacements);
            }
            imageView.setStyle("-fx-effect: innershadow(gaussian, green, 20, 0.5, 0, 0);");
            selectedPiece = imageView;
            System.out.println(gameModel.getHexCoordinateByPieceWrapperImage(imageView));
            List<MovementAction> validMovements = gameModel.getValidMoves(gameModel.getImmutableGrid(), gameModel.getHexCoordinateByPieceWrapperImage(imageView));
            displayValidMovements(validMovements);
            currentDisplayedPlacements = validMovements;
        }

    }

    /**
     * Handles the mouse entered event for placed pieces.
     * Highlights the piece with a yellow shadow effect when the mouse enters its area.
     *
     * @param imageView The ImageView representing the placed piece.
     */
    private void placedPieceMouseEnteredEvent(ImageView imageView) {
        if (selectedPiece != imageView) {
            imageView.setStyle("-fx-effect: innershadow(gaussian, yellow, 20, 0.5, 0, 0);");
        }
    }

    /**
     * Handles the mouse exited event for placed pieces.
     * Removes the highlight when the mouse exits the piece's area.
     *
     * @param imageView The ImageView representing the placed piece.
     */
    private void placedPieceMouseExitedEvent(ImageView imageView) {
        if (selectedPiece != imageView) {
            imageView.setStyle("");
        }
    }

    /**
     * Handles the mouse click event for pieces in the panel (not yet placed on the board).
     * Deselects the currently selected piece if it's clicked again.
     * If a different piece is clicked, highlights it and displays valid placements for it.
     *
     * @param imageView The ImageView representing the clicked piece in the panel.
     */
    private void panelPieceMouseClickedEvent(ImageView imageView) {
        unmarkMove();
        if (selectedPiece != null && imageView == selectedPiece) {
            stopDisplayValidMoves(currentDisplayedPlacements);
            selectedPiece.setStyle("");
            selectedPiece = null;
        } else {
            if (selectedPiece != null) {
                selectedPiece.setStyle("");
                stopDisplayValidMoves(currentDisplayedPlacements);
            }
            imageView.setStyle("-fx-effect: innershadow(three-pass-box, green, 3, 1.0, 0, 0);");
            selectedPiece = imageView;
            List<PlacementAction> validPlacements = gameModel.getValidPlacements(gameModel.getTurn());
            displayValidPlacements(validPlacements);
            currentDisplayedPlacements = validPlacements;
        }

    }

    /**
     * Handles the mouse entered event for pieces in the panel.
     * Highlights the piece with a yellow shadow effect when the mouse enters its area.
     *
     * @param imageView The ImageView representing the piece in the panel.
     */
    private void panelPieceMouseEnteredEvent(ImageView imageView) {
        if (selectedPiece != imageView) {
            imageView.setStyle("-fx-effect: innershadow(three-pass-box, yellow, 3, 1.0, 0, 0);");
        }
    }

    /**
     * Handles the mouse exited event for pieces in the panel.
     * Removes the highlight when the mouse exits the piece's area.
     *
     * @param imageView The ImageView representing the piece in the panel.
     */
    private void panelPieceMouseExitedEvent(ImageView imageView) {
        if (selectedPiece != imageView) {
            imageView.setStyle("");
        }
    }

    /**
     * Retrieves the piece type from the given ImageView.
     * This function compares the ImageView's image with the available piece images in the PieceImage enum.
     *
     * @param imageView The ImageView representing the piece whose type is to be identified.
     * @return The corresponding PieceImage enum value, or null if no match is found.
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
     * Stops displaying valid placements by resetting their styles and removing mouse event handlers.
     *
     * @param placements The list of ImageViews representing the valid placements to stop displaying.
     */
    private void stopDisplayValidMoves(List<? extends MoveAction> placements) {
        // Map<HexCoordinate, Deque<PieceWrapper>> gridState = gameModel.getGrid();
        PMap<HexCoordinate, PStack<PieceWrapper>> gridState = gameModel.getImmutableGrid();
        for (MoveAction moveAction : placements) {
            ImageView pieceImageView;
            if (moveAction.isPlacement())
                pieceImageView = gridState.get(((PlacementAction) moveAction).getDestination()).get(0).getImageView();
            else
                pieceImageView = gridState.get(((MovementAction) moveAction).getTo()).get(0).getImageView();
            pieceImageView.setStyle(null);
            pieceImageView.setOnMouseClicked(null);
            pieceImageView.setOnMouseEntered(null);
            pieceImageView.setOnMouseExited(null);

        }
    }

    private PieceWrapper getPieceWrapperByImageView(ImageView imageView) {
        if (currentTurn == WHITE) {
            for (PieceWrapper pieceWrapper : whitePanelPieces) {
                if (pieceWrapper.getImageView().equals(imageView))
                    return pieceWrapper;
            }
        } else if (currentTurn == BLACK) {
            for (PieceWrapper pieceWrapper : blackPanelPieces) {
                if (pieceWrapper.getImageView().equals(imageView))
                    return pieceWrapper;
            }
        }
        return null;
    }

    /**
     * Displays valid placements by adding a yellow shadow effect and setting up click and hover event handlers.
     *
     * @param placements The list of ImageViews representing valid placements for the piece.
     */
    private void displayValidPlacements(List<PlacementAction> placements) {
        for (PlacementAction placement : placements) {
            ImageView pieceImageView = gameModel.getImmutableGrid().get(placement.getDestination()).get(0).getImageView();

            pieceImageView.setStyle("-fx-effect: innershadow(three-pass-box, yellow, 3, 1.0, 0, 0);");

            pieceImageView.setOnMouseClicked(event -> {
                if (selectedPiece != null) {
                    PieceWrapper pieceWrapper = getPieceWrapperByImageView(selectedPiece);
                    int response = gameModel.placePiece(currentTurn, pieceWrapper, placement).getValue();
                    markedMove = placement;
                    System.out.println("displayValidPlacements: " + gameModel.getTurn());
                    int pieceCount = gameModel.getRemainingPiecesToPlace(currentTurn, getPieceWrapperByImageView(selectedPiece).getPiece().getType());
                    if (pieceCount == 0) {
                        ColorAdjust grayScale = new ColorAdjust();
                        grayScale.setSaturation(-1);
                        selectedPiece.setEffect(grayScale);
                        selectedPiece.setOnMouseEntered(null);
                        selectedPiece.setOnMouseExited(null);
                        selectedPiece.setOnMouseClicked(null);
                        disabledPieces.add(selectedPiece);

                    }

                    selectedPiece.setStyle(null);
                    selectedPiece = null;
                    stopDisplayValidMoves(placements);
                    pieceImageView.setStyle(null);
                    pieceImageView.setOnMouseExited(null);
                    pieceImageView.setOnMouseClicked(null);
                    pieceImageView.setOnMouseEntered(null);

                    updateHexGrid();
                    moveMade = true;

                    if (isQueenBeeForcedPlaced[currentTurn == WHITE ? 1 : 0]) {
                        enableAllPiecesExceptOfQueenBee();
                    }

                    if (response == 1) {
                        disableAllPiecesExceptOfQueenBee[currentTurn == WHITE ? 1 : 0] = true;
                        // disableAllPiecesExceptOfQueenBee();
                    }
                }
            });

            pieceImageView.setOnMouseEntered(event -> {
                pieceImageView.setStyle("-fx-effect: innershadow(three-pass-box, green, 3, 1.0, 0, 0);");

            });
            pieceImageView.setOnMouseExited(event -> {
                pieceImageView.setStyle("-fx-effect: innershadow(three-pass-box, yellow, 3, 1.0, 1.0, 0);");
            });

        }
        updateHexGrid();

    }

    /**
     * Displays valid movements by adding a yellow shadow effect and setting up click and hover event handlers.
     *
     * @param movements The list of ImageViews representing valid movements for the selected piece.
     */
    private void displayValidMovements(List<MovementAction> movements) {
        for (MovementAction movementAction : movements) {
            ImageView pieceImageView = gameModel.getImmutableGrid().get(movementAction.getTo()).get(0).getImageView();
            pieceImageView.setStyle("-fx-effect: innershadow(gaussian, blue, 10, 0.5, 0, 0);");

            pieceImageView.setOnMouseClicked(event -> {
                if (selectedPiece != null) {
                    gameModel.movePiece(movementAction);
                    markedMove = movementAction;

                    updateHexGrid();

                    selectedPiece.setStyle(null);
                    selectedPiece = null;
                    stopDisplayValidMoves(movements);
                    pieceImageView.setStyle(null);
                    pieceImageView.setOnMouseExited(null);
                    pieceImageView.setOnMouseClicked(null);
                    pieceImageView.setOnMouseEntered(null);

                    moveMade = true;
                }
            });

            pieceImageView.setOnMouseEntered(event -> {
                pieceImageView.setStyle("-fx-effect: innershadow(gaussian, green, 20, 0.5, 0, 0);");

            });
            pieceImageView.setOnMouseExited(event -> {
                pieceImageView.setStyle("-fx-effect: innershadow(gaussian, blue, 10, 0.5, 0, 0);");
            });

        }
        updateHexGrid();
    }

    private void markMove() {
        if (markedMove != null) {
            if (!markedMove.isPlacement()) {
                ImageView fromImageView = gameModel.getPieceWrapperByHexCoordinate(((MovementAction) markedMove).getFrom()).getImageView();
                ImageView toImageView = gameModel.getPieceWrapperByHexCoordinate(((MovementAction) markedMove).getTo()).getImageView();
                fromImageView.setStyle("-fx-effect: innershadow(three-pass-box, #0EE600, 3, 1.0, 0, 0);");
                toImageView.setStyle("-fx-effect: innershadow(three-pass-box, #0EE600, 3, 1.0, 0, 0);");
            } else {
                ImageView destinationImageView = gameModel.getPieceWrapperByHexCoordinate(((PlacementAction) markedMove).getDestination()).getImageView();
                destinationImageView.setStyle("-fx-effect: innershadow(three-pass-box, #0EE600, 3, 1.0, 0, 0);");
            }
        }
    }

    private void unmarkMove() {
        if (markedMove != null) {
            if (!markedMove.isPlacement()) {
                ImageView fromImageView = gameModel.getPieceWrapperByHexCoordinate(((MovementAction) markedMove).getFrom()).getImageView();
                ImageView toImageView = gameModel.getPieceWrapperByHexCoordinate(((MovementAction) markedMove).getTo()).getImageView();
                fromImageView.setStyle("");
                toImageView.setStyle("");
            } else {
                ImageView destinationImageView = gameModel.getPieceWrapperByHexCoordinate(((PlacementAction) markedMove).getDestination()).getImageView();
                destinationImageView.setStyle("");
            }
            markedMove = null;
        }
    }

    /**
     * Enables all pieces except the Queen Bee for the current turn.
     * This allows the player to interact with the pieces (click, enter, exit) during their turn.
     */
    private void enableAllPiecesExceptOfQueenBee() {
        for (Node node : piecesPanel.getChildren()) {
            boolean castWorked = false;
            PieceImage pieceType = null;
            try {
                pieceType = getPieceTypeFromImageView((ImageView) node);
                castWorked = true;
            } catch(ClassCastException e) {
                System.out.println("Cannot cast this node: " + node);

            }
            if (castWorked && !disabledPieces.contains((ImageView) node)) {
                if (currentTurn == WHITE) {
                    if (pieceType != null && pieceType != PieceImage.QUEEN_BEE_WHITE) {
                        node.setOnMouseClicked(event -> panelPieceMouseClickedEvent((ImageView) node));
                        node.setOnMouseEntered(event -> panelPieceMouseEnteredEvent((ImageView) node));
                        node.setOnMouseExited(event -> panelPieceMouseExitedEvent((ImageView) node));
                    }
                } else if (currentTurn == BLACK) {
                    if (pieceType != null && pieceType != PieceImage.QUEEN_BEE_BLACK) {
                        node.setOnMouseClicked(event -> panelPieceMouseClickedEvent((ImageView) node));
                        node.setOnMouseEntered(event -> panelPieceMouseEnteredEvent((ImageView) node));
                        node.setOnMouseExited(event -> panelPieceMouseExitedEvent((ImageView) node));
                    }
                }
            }
        }
    }

    /**
     * Disables all pieces except the Queen Bee for the current turn.
     * <p>
     * Time Complexity: O(n), where n is the number of nodes in piecesPanel.
     * Space Complexity: O(1)
     * </p>
     */
    private void disableAllPiecesExceptOfQueenBee() {
        for (Node node : piecesPanel.getChildren()) {
            boolean castWorked = false;
            PieceImage pieceType = null;
            try {
                pieceType = getPieceTypeFromImageView((ImageView) node);
                castWorked = true;
            } catch(ClassCastException e) {
                System.out.println("Cannot cast this node: " + node);

            }
            if (castWorked) {
                if (currentTurn == WHITE) {
                    if (pieceType != null && pieceType != PieceImage.QUEEN_BEE_WHITE) {
                        node.setOnMouseClicked(null);
                        node.setOnMouseEntered(null);
                        node.setOnMouseExited(null);
                    } else if (pieceType != null) {
                        node.setOnMouseClicked(event -> panelPieceMouseClickedEvent((ImageView) node));
                        node.setOnMouseEntered(event -> panelPieceMouseEnteredEvent((ImageView) node));
                        node.setOnMouseExited(event -> panelPieceMouseExitedEvent((ImageView) node));
                    }
                } else if (currentTurn == BLACK) {
                    if (pieceType != null && pieceType != PieceImage.QUEEN_BEE_BLACK) {
                        node.setOnMouseClicked(null);
                        node.setOnMouseEntered(null);
                        node.setOnMouseExited(null);
                    } else if (pieceType != null) {
                        node.setOnMouseClicked(event -> panelPieceMouseClickedEvent((ImageView) node));
                        node.setOnMouseEntered(event -> panelPieceMouseEnteredEvent((ImageView) node));
                        node.setOnMouseExited(event -> panelPieceMouseExitedEvent((ImageView) node));
                    }

                }
            }
        }
        isQueenBeeForcedPlaced[currentTurn == WHITE ? 1 : 0] = true;
        disableAllPiecesExceptOfQueenBee[currentTurn == WHITE ? 1 : 0] = false;
    }
}
