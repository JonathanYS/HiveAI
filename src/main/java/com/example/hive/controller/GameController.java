package com.example.hive.controller;

import com.example.hive.model.ai.AIPlayer;
import com.example.hive.model.enums.EndGameStatus;
import com.example.hive.model.enums.PieceColor;
import com.example.hive.model.enums.PieceImage;
import com.example.hive.model.enums.PieceType;
import com.example.hive.model.grid.GameModel;
import com.example.hive.model.grid.HexCoordinate;
import com.example.hive.model.grid.Piece;
import com.example.hive.model.grid.PieceWrapper;
import com.example.hive.model.logic.MoveAction;
import com.example.hive.model.logic.MovementAction;
import com.example.hive.model.logic.PlacementAction;
import com.example.hive.model.utils.Pair;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.pcollections.PMap;
import org.pcollections.PStack;

import java.io.IOException;
import java.util.*;
import java.util.List;

import static com.example.hive.model.enums.PieceColor.*;

/**
 * The GameController class is responsible for managing the game's UI, handling user interactions,
 * and controlling the game loop. It initializes and updates the game board, handles the placement
 * and movement of pieces, and manages the user interface for starting, restarting, and controlling the game.
 */
public class GameController {

    private Pane gameBoardPane;

    private static final int HEX_SIZE = 25;
    private GameModel gameModel;
    private Stage primaryStage;
    private ImageView selectedPiece = null;
    private List<? extends MoveAction> currentDisplayedPlacements = null;
    private PieceColor humanPiecesColor = null; // Indicates if the human player is the white pieces or the black.
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

    private static final Image MENU_ICON = new Image(Objects.requireNonNull(GameController.class.getResource("/icons/menu.png")).toExternalForm());
    private static final Image REPEAT_ICON = new Image(Objects.requireNonNull(GameController.class.getResource("/icons/repeat.png")).toExternalForm());

    private Label turnLabel;
    private Timeline dots;
    private Label thinkingLabel;

    /**
     * Constructs a GameController with the given stage and human player's piece color.
     *
     * @param stage             the primary stage for the game window
     * @param humanPiecesColor  the color of the pieces controlled by the human player
     */
    public GameController(Stage stage, PieceColor humanPiecesColor) {
        primaryStage = stage;
        gameModel = new GameModel();
        if (humanPiecesColor != null) {
            this.humanPiecesColor = humanPiecesColor;
            aiPlayer = new AIPlayer(gameModel, humanPiecesColor.getOpposite());
        }
    }

    /**
     * Constructs a GameController with the given stage.
     *
     * @param stage the primary stage for the game window
     */
    public GameController(Stage stage) {
        primaryStage = stage;
        gameModel = new GameModel();
    }

    /**
     * Sets up the initial scene with the game board and the pieces panel, along with the UI components.
     */
    public void setGameScene() {
        gameBoardPane = new Pane();
        gameBoardPane.setStyle("-fx-background-color: #444244;");
        gameBoardPane.setPrefSize(3000, 3000);

        Group group = new Group(gameBoardPane);
        StackPane centeredContainer = new StackPane(group);

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
                // Block left and right mouse drags if they are not inside a scroll bar.
                // In addition, block middle mouse button drag using the scroll bars.
                event.consume();
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

        Button menuButton = createIconButton(MENU_ICON, e -> {
            try {
                FXMLLoader loader = new FXMLLoader(
                        MenuScreenController.class.getResource("/fxml/menu-screen.fxml")
                );
                Parent welcomeRoot = loader.load();
                MenuScreenController controller = loader.getController();
                controller.setPrimaryStage(primaryStage);
                controller.setTitleLabel("Menu");
                controller.setReturnButton(true);

                double currentWidth = primaryStage.getScene().getWidth();
                double currentHeight = primaryStage.getScene().getHeight();
                controller.setCachedScene(primaryStage.getScene());
                Scene  welcomeScene = new Scene(welcomeRoot, currentWidth, currentHeight);
                primaryStage.setScene(welcomeScene);
                primaryStage.setMaximized(true);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        Button restartButton = createIconButton(REPEAT_ICON, e -> confirmRestart());

        turnLabel = new Label("");
        turnLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        thinkingLabel = new Label();
        thinkingLabel.setStyle("-fx-text-fill: #EAD94A; -fx-font-size: 14px; -fx-font-style: italic; -fx-font-weight: bold;");
        thinkingLabel.setVisible(false);
        dots = new Timeline(
                new KeyFrame(Duration.ZERO,      e -> thinkingLabel.setText("AI thinking")),
                new KeyFrame(Duration.seconds(0.5), e -> thinkingLabel.setText("AI thinking.")),
                new KeyFrame(Duration.seconds(1.0), e -> thinkingLabel.setText("AI thinking..")),
                new KeyFrame(Duration.seconds(1.5), e -> thinkingLabel.setText("AI thinking..."))
        );
        dots.setCycleCount(Animation.INDEFINITE);
        VBox statusBox = new VBox(turnLabel, thinkingLabel);

        HBox leftControls = new HBox(10, menuButton, restartButton);
        leftControls.setAlignment(Pos.CENTER_LEFT);

        HBox centerBox = new HBox(statusBox);
        centerBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(centerBox, Priority.ALWAYS);

        HBox topBar = new HBox(leftControls, centerBox);
        topBar.setPadding(new Insets(8));
        topBar.setStyle("-fx-background-color: #212021;");
        topBar.setAlignment(Pos.CENTER_LEFT);

        root.setTop(topBar);

        animateSceneSlideUp(root);

        initializeGame();
    }

    /**
     * Confirms if the user wants to restart the game with a confirmation dialog.
     * If confirmed, a fresh instance of GameController is created, and the game scene is reset.
     */
    private void confirmRestart() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Restart");
        alert.setHeaderText("Are you sure you want to restart the game?");
        alert.setContentText("This will reset all progress.");

        ButtonType yesButton = new ButtonType("Yes", ButtonBar.ButtonData.YES);
        ButtonType noButton = new ButtonType("No", ButtonBar.ButtonData.NO);

        alert.getButtonTypes().setAll(yesButton, noButton);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == yesButton) {
            GameController fresh = new GameController(primaryStage, humanPiecesColor);
            fresh.setGameScene();
        }
    }

    /**
     * Creates a button with an icon and event handler for mouse clicks.
     * The button animates on hover with scaling and rotation.
     *
     * @param icon         the icon image to be displayed on the button
     * @param eventHandler the event handler to be triggered on mouse click
     * @return the created button with the icon and event handler
     */
    private Button createIconButton(Image icon, EventHandler<MouseEvent> eventHandler) {
        Button button = new Button();
        ImageView iconView = new ImageView(icon);

        // Set the button's graphic to the icon.
        button.setGraphic(iconView);

        ScaleTransition zoomIn = new ScaleTransition(Duration.millis(200), button);
        zoomIn.setToX(1.1);
        zoomIn.setToY(1.1);

        ScaleTransition zoomOut = new ScaleTransition(Duration.millis(200), button);
        zoomOut.setToX(1);
        zoomOut.setToY(1);

        RotateTransition rotate = new RotateTransition(Duration.millis(200), button);

        if (icon.equals(REPEAT_ICON)) {
            button.setOnMouseEntered(e -> {rotate.setByAngle(-45); rotate.play(); button.setOpacity(0.7);});
            button.setOnMouseExited(e -> {rotate.setByAngle(45); rotate.play(); button.setOpacity(1.0);});
        } else if (icon.equals(MENU_ICON)) {
            button.setOnMouseEntered(e -> {zoomIn.play(); button.setOpacity(0.7);});
            button.setOnMouseExited(e -> {zoomOut.play(); button.setOpacity(1.0);});
        }

        button.setOnMouseClicked(eventHandler);
        button.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        return button;
    }

    /**
     * Animates the scene slide up to the top, transitioning to the new scene.
     *
     * @param newRoot the new root for the scene
     */
    private void animateSceneSlideUp(Parent newRoot) {
        Scene currentScene = primaryStage.getScene();
        Parent oldRoot = currentScene.getRoot();

        // Create a temporary container to hold both the current root (old scene) and the new root.
        StackPane overlay = new StackPane();
        overlay.getChildren().add(oldRoot);
        overlay.getChildren().add(newRoot);

        // Position the new root below the visible area (out of view).
        newRoot.setTranslateY(primaryStage.getHeight());

        // Temporarily set the overlay as the root of the scene.
        currentScene.setRoot(overlay);

        Timeline slideIn = new Timeline();
        KeyValue keyValue = new KeyValue(newRoot.translateYProperty(), 0, Interpolator.EASE_BOTH);
        KeyFrame keyFrame = new KeyFrame(Duration.millis(500), keyValue);
        slideIn.getKeyFrames().add(keyFrame);

        slideIn.play();
    }

    /**
     * Checks if the mouse event occurred inside a scrollbar.
     *
     * @param event the mouse event
     * @return true if the event occurred inside a scrollbar, false otherwise
     */
    private boolean isInsideScrollbar(MouseEvent event) {
        // Get the viewport's bounds and the scrollbars' bounds.
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
     * Updates the hex grid's layout and the placement of pieces based on the game state.
     */
    private void updateHexGrid() {
        gameBoardPane.getChildren().clear();


        double centerX = gameBoardPane.getWidth() / 2;
        double centerY = gameBoardPane.getHeight() / 2;

        PMap<HexCoordinate, PStack<PieceWrapper>> immutableGrid = gameModel.getGrid();

        for (PMap.Entry<HexCoordinate, PStack<PieceWrapper>> entry : immutableGrid.entrySet()) {
            HexCoordinate hexCoordinate = entry.getKey();
            PieceWrapper pieceWrapper = entry.getValue().get(0);
            ImageView pieceImageView = (pieceWrapper != null) ? pieceWrapper.getImageView() : new ImageView(PieceImage.BLANK_TILE.getImage());

            Point2D newPixelPosition = hexToPixel(hexCoordinate, centerX, centerY);

            pieceImageView.setLayoutX(newPixelPosition.getX());
            pieceImageView.setLayoutY(newPixelPosition.getY());

            if (!gameBoardPane.getChildren().contains(pieceImageView)) {
                gameBoardPane.getChildren().add(pieceImageView);
            }

        }
        gameBoardPane.requestLayout();
    }

    /**
     * Enables interaction with placed pieces on the grid.
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

        PMap<HexCoordinate, PStack<PieceWrapper>> immutableGrid = gameModel.getGrid();
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
     * Disables interaction with all placed pieces on the grid.
     */
    private void disableAllPlacedPieces() {
        PMap<HexCoordinate, PStack<PieceWrapper>> immutableGrid = gameModel.getGrid();
        for (PMap.Entry<HexCoordinate, PStack<PieceWrapper>> entry : immutableGrid.entrySet()) {
            PieceWrapper pieceWrapper = entry.getValue().get(0);
            assert pieceWrapper != null;
            ImageView pieceImageView = pieceWrapper.getImageView();
            if (pieceImageView.getImage() != PieceImage.BLANK_TILE.getImage()) {
                pieceImageView.setOnMouseClicked(null);
                pieceImageView.setOnMouseEntered(null);
                pieceImageView.setOnMouseExited(null);
            }
        }
    }

    /**
     * Initializes the game with a starting grid and prepares the board for player interaction.
     */
    private void initializeGame() {
        displayStartingGrid();

        Thread gameLoopThread = new Thread(this::gameLoop);
        gameLoopThread.setDaemon(true);
        gameLoopThread.start();
    }

    /**
     * The main game loop that manages the turns and checks for a win. It alternates between player turns
     * (Human and AI), updates the UI, and evaluates the game state.
     */
    private void gameLoop() {
        boolean stopping = false;
        while (!stopping) {
            currentTurn = gameModel.getTurn();
            Platform.runLater(() -> turnLabel.setText(currentTurn == WHITE ? "White's Turn" : "Black's Turn"));
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
                    return;
                }
            }

            markMove();
            Platform.runLater(this::updateHexGrid);
            stopping = checkWin();
        }
    }

    /**
     * Creates and returns a thread to execute the AI move asynchronously.
     * It selects a move, marks the piece, and disables the piece after it has been used.
     *
     * @return a Thread that executes the AI move
     */
    private Thread getAiMove() {
        Platform.runLater(() -> {
            thinkingLabel.setVisible(true);
            dots.playFromStart();
        });
        Thread aiMove = new Thread(() -> {
            Pair<? extends MoveAction, PieceWrapper> pair = aiPlayer.makeMove();
            PieceWrapper piece = pair.getValue();
            markedMove = pair.getKey();
            moveMade = true;
            Platform.runLater(() -> {
                dots.stop();
                thinkingLabel.setVisible(false);
            });
            if (piece != null) {
                ImageView pieceImageView = getPieceImageView(piece);
                int pieceCount = gameModel.getRemainingPiecesToPlace(piece.getPiece().color(), piece.getPiece().type());
                if (pieceCount == 0 && pieceImageView != null) {
                    ColorAdjust grayScale = new ColorAdjust();
                    grayScale.setSaturation(-1);
                    pieceImageView.setEffect(grayScale);
                    pieceImageView.setOnMouseEntered(null);
                    pieceImageView.setOnMouseExited(null);
                    pieceImageView.setOnMouseClicked(null);
                    disabledPieces.add(pieceImageView);
                    try {
                        Thread.sleep(10000); // Prevent CPU overuse.
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

    /**
     * Retrieves the ImageView associated with a given piece.
     *
     * @param piece the piece whose ImageView is to be retrieved
     * @return the ImageView of the piece, or null if not found
     */
    private ImageView getPieceImageView(PieceWrapper piece) {
        ImageView pieceImageView = null;
        if (currentTurn == WHITE) {
            for (PieceWrapper pieceWrapper : whitePanelPieces) {
                if (pieceWrapper.getPiece().type() == piece.getPiece().type())
                    pieceImageView = pieceWrapper.getImageView();
            }
        }
        else if (currentTurn == BLACK) {
            for (PieceWrapper pieceWrapper : blackPanelPieces) {
                if (pieceWrapper.getPiece().type() == piece.getPiece().type())
                    pieceImageView = pieceWrapper.getImageView();
            }
        }
        return pieceImageView;
    }

    /**
     * Checks if the current game state meets the winning or draw conditions.
     *
     * @return true if the game should stop (win or draw detected), false otherwise
     */
    private boolean checkWin() {
        boolean stopping;
        Pair<Map<PieceColor, Boolean>, EndGameStatus> endGameStatus;
        Map<PieceColor, Boolean> winner;
        endGameStatus = gameModel.checkWin(false);
        winner = endGameStatus.getKey();
        stopping = winner.get(WHITE) || winner.get(BLACK);
        if (stopping) {
            String message;
            if (winner.get(WHITE)) {
                if (winner.get(BLACK)) {
                    if (endGameStatus.getValue() == EndGameStatus.STALEMATE) {
                        message = "It is a stalemate!";
                        HexCoordinate whiteQB = gameModel.getQueenCoordinate(WHITE);
                        HexCoordinate blackQB = gameModel.getQueenCoordinate(BLACK);
                        markWinningSurrounding(whiteQB.getNeighbors());
                        markWinningSurrounding(blackQB.getNeighbors());
                    } else if (endGameStatus.getValue() == EndGameStatus.DRAW_BY_NO_MOVES) {
                        message = "It is a draw by no moves!";
                    } else {
                        message = "It is a draw by repetition!";
                    }
                } else {
                    message = "White Won!";
                    HexCoordinate blackQB = gameModel.getQueenCoordinate(BLACK);
                    markWinningSurrounding(blackQB.getNeighbors());
                }
            } else {
                message = "Black Won!";
                HexCoordinate whiteQB = gameModel.getQueenCoordinate(WHITE);
                markWinningSurrounding(whiteQB.getNeighbors());
            }
            try {
                makeUnSelectablePieces(blackPlacementPieces);
                makeUnSelectablePieces(whitePlacementPieces);
                disableAllPlacedPieces();
                Thread.sleep(3000);
                loadWinScreen(message);
            } catch (IOException e) {
                System.out.println("[IOException] when loading Win Screen.");
                e.printStackTrace();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return stopping;
    }

    /**
     * Highlights the surrounding pieces of the winning queen to indicate victory.
     *
     * @param winningSurroundingCoords the set of coordinates surrounding the winning queen
     */
    private void markWinningSurrounding(Set<HexCoordinate> winningSurroundingCoords) {
        unmarkMove();
        PMap<HexCoordinate, PStack<PieceWrapper>> gameGrid = gameModel.getGrid();
        for (HexCoordinate coordinate : winningSurroundingCoords) {
            ImageView imageView = gameGrid.get(coordinate).get(0).getImageView();
            Platform.runLater(() -> imageView.setStyle("-fx-effect: innershadow(three-pass-box, red, 3, 1.0, 0, 0);"));
        }
    }

    /**
     * Loads the win screen and displays the final message (e.g., "White Won!", "Black Won!", or "Stalemate").
     *
     * @param winner the winner message to display
     * @throws IOException if there is an error loading the FXML file
     */
    private void loadWinScreen(String winner) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(GameController.class.getResource("/fxml/menu-screen.fxml"));
        double currentWidth = primaryStage.getScene().getWidth();
        double currentHeight = primaryStage.getScene().getHeight();
        Scene scene = new Scene(fxmlLoader.load(), currentWidth, currentHeight);

        MenuScreenController controller = fxmlLoader.getController();
        if (controller == null) {
            System.out.println("Controller is null! FXML might not be set correctly.");
        } else {
            controller.setPrimaryStage(primaryStage);
            controller.setTitleLabel(winner);
            controller.setReturnButton(false);
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
    private Point2D hexToPixel(HexCoordinate hexCoord, double centerX, double centerY) {
        double x = HEX_SIZE * ((3.0 / 2.0) * hexCoord.getQ());
        double y = HEX_SIZE * (Math.sqrt(3.0) / 2 * hexCoord.getQ() + Math.sqrt(3.0) * hexCoord.getR());

        x += centerX;
        y += centerY;

        return new Point2D(x, y);
    }

    /**
     * Displays the starting grid for the game, preparing the board for the first turn.
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
            List<MovementAction> validMovements = gameModel.getValidMoves(gameModel.getGrid(), gameModel.getHexCoordinateByPieceWrapperImage(imageView));
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
                return pieceImage; // Found matching piece.
            }
        }
        return null;
    }

    /**
     * Stops displaying valid placements by resetting their styles and removing mouse event handlers.
     *
     * @param placements The list of ImageViews representing the valid placements to stop displaying.
     */
    private void stopDisplayValidMoves(List<? extends MoveAction> placements) {
        PMap<HexCoordinate, PStack<PieceWrapper>> gridState = gameModel.getGrid();
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

    /**
     * Retrieves the PieceWrapper associated with a given ImageView.
     * This is used to find the PieceWrapper corresponding to a selected piece.
     *
     * @param imageView The ImageView of the selected piece.
     * @return The PieceWrapper associated with the ImageView, or null if no match is found.
     */
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
     * Displays valid placements for a selected piece by applying a yellow shadow effect
     * and setting up click and hover event handlers.
     * The valid placements are based on the given list of PlacementAction objects.
     *
     * @param placements A list of PlacementAction objects representing the valid placements.
     */
    private void displayValidPlacements(List<PlacementAction> placements) {
        for (PlacementAction placement : placements) {
            ImageView pieceImageView = gameModel.getGrid().get(placement.getDestination()).get(0).getImageView();

            pieceImageView.setStyle("-fx-effect: innershadow(three-pass-box, yellow, 3, 1.0, 0, 0);");

            pieceImageView.setOnMouseClicked(event -> {
                if (selectedPiece != null) {
                    PieceWrapper pieceWrapper = getPieceWrapperByImageView(selectedPiece);
                    int response = gameModel.placePiece(currentTurn, pieceWrapper, placement).getValue();
                    markedMove = placement;
                    int pieceCount = gameModel.getRemainingPiecesToPlace(currentTurn, Objects.requireNonNull(getPieceWrapperByImageView(selectedPiece)).getPiece().type());
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
                    }
                }
            });

            pieceImageView.setOnMouseEntered(event -> pieceImageView.setStyle("-fx-effect: innershadow(three-pass-box, green, 3, 1.0, 0, 0);"));
            pieceImageView.setOnMouseExited(event -> pieceImageView.setStyle("-fx-effect: innershadow(three-pass-box, yellow, 3, 1.0, 1.0, 0);"));

        }
        updateHexGrid();

    }

    /**
     * Displays valid movements for the selected piece by applying a blue shadow effect
     * and setting up click and hover event handlers.
     * The valid movements are based on the given list of MovementAction objects.
     *
     * @param movements A list of MovementAction objects representing the valid movements.
     */
    private void displayValidMovements(List<MovementAction> movements) {
        for (MovementAction movementAction : movements) {
            ImageView pieceImageView = gameModel.getGrid().get(movementAction.getTo()).get(0).getImageView();
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

            pieceImageView.setOnMouseEntered(event -> pieceImageView.setStyle("-fx-effect: innershadow(gaussian, green, 20, 0.5, 0, 0);"));
            pieceImageView.setOnMouseExited(event -> pieceImageView.setStyle("-fx-effect: innershadow(gaussian, blue, 10, 0.5, 0, 0);"));

        }
        updateHexGrid();
    }

    /**
     * Marks the move by applying a green shadow effect to the source and destination
     * of the move, highlighting the action.
     */
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

    /**
     * Unmarks the move by removing any highlighting effects on the source and destination
     * of the move, and resetting the markedMove variable.
     */
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
     * Enables interaction with all pieces except the Queen Bee for the current turn.
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
     * Disables interaction with all pieces except the Queen Bee for the current turn.
     * This ensures that only the Queen Bee can be interacted with during the current player's turn.
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
