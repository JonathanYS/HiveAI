package com.example.hive;

import com.example.hive.model.*;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Separator;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.*;
import java.util.List;

public class GameController {

    private Pane gameBoardPane;

    private int HEX_SIZE = 25;
    private Grid gameGrid = new Grid();
    private Stage primaryStage;
    private ImageView selectedPiece = null;
    private List<ImageView> currentDisplayedPlacements = null;
    private boolean isWhite = false; // Indicates if the human player is the white pieces or the black.
    private int movesCount;
    private boolean isQueenBeePlaced;
    private VBox piecesPanel;
    private ArrayList<ImageView> disabledPieces = new ArrayList<>();
    private ArrayList<ImageView> whitePlacementPieces = new ArrayList<>();
    private ArrayList<ImageView> blackPlacementPieces = new ArrayList<>();

    private boolean moveMade;
    private boolean[] disableAllPiecesExceptOfQueenBee = new boolean[2];
    private boolean[] isQueenBeeForcedPlaced = {false, false};
    private boolean currentTurn = true;

    private BorderPane root;

    /**
     * Constructor to initialize the GameController with the stage and whether the human player is white.
     *
     * @param stage  the main stage for the game
     * @param isWhite boolean indicating if the human player is white
     */
    public GameController(Stage stage, boolean isWhite) {
        primaryStage = stage;
        this.isWhite = isWhite;
        movesCount = 0;
        isQueenBeePlaced = false;
    }

    /**
     * Sets up the initial scene with the game board and the pieces panel.
     */
    public void setGameScene() {
        root = new BorderPane();

        gameBoardPane = new Pane();
        gameBoardPane.setStyle("-fx-background-color: #444244;");

        root.setCenter(gameBoardPane);

        gameBoardPane.widthProperty().addListener((observableValue, number, t1) -> {
            updateHexGrid();
        });
        gameBoardPane.heightProperty().addListener((observableValue, number, t1) -> {
            updateHexGrid();
        });


        piecesPanel = new VBox(10);
        piecesPanel.setStyle("-fx-background-color: lightgray; -fx-padding: 10;");

        ImageView queenBeeBlackView = new ImageView(PieceImage.QUEEN_BEE_BLACK.getImage());
        ImageView antBlackView = new ImageView(PieceImage.ANT_BLACK.getImage());
        ImageView spiderBlackView = new ImageView(PieceImage.SPIDER_BLACK.getImage());
        ImageView grasshopperBlackView = new ImageView(PieceImage.GRASSHOPPER_BLACK.getImage());
        ImageView beetleBlackView = new ImageView(PieceImage.BEETLE_BLACK.getImage());

        ImageView queenBeeWhiteView = new ImageView(PieceImage.QUEEN_BEE_WHITE.getImage());
        ImageView antWhiteView = new ImageView(PieceImage.ANT_WHITE.getImage());
        ImageView spiderWhiteView = new ImageView(PieceImage.SPIDER_WHITE.getImage());
        ImageView grasshopperWhiteView = new ImageView(PieceImage.GRASSHOPPER_WHITE.getImage());
        ImageView beetleWhiteView = new ImageView(PieceImage.BEETLE_WHITE.getImage());

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

    /**
     * Updates the hex grid to properly position the pieces based on the grid.
     */
    private void updateHexGrid() {
        gameBoardPane.getChildren().clear();

        double centerX = gameBoardPane.getWidth() / 2;
        double centerY = gameBoardPane.getHeight() / 2;
        // System.out.println("centerX: " + centerX + "\ncenterY: " + centerY);

        Map<HexCoordinate, Deque<ImageView>> grid = gameGrid.getGrid();
        for (Map.Entry<HexCoordinate, Deque<ImageView>> entry : grid.entrySet()) {
            HexCoordinate hexCoordinate = entry.getKey();
            ImageView pieceImageView = entry.getValue().peek();
            // System.out.println("Layoutx: " + pieceImageView.getLayoutX() + "\nLayouty: " + pieceImageView.getLayoutY());

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
        List<PieceImage> pieceImages;
        if (currentTurn) {
            pieceImages = PieceImage.getPiecesByColor(PieceColor.WHITE);
        }
        else {
            pieceImages = PieceImage.getPiecesByColor(PieceColor.BLACK);
        }
        // System.out.println(pieceImages);

        Map<HexCoordinate, Deque<ImageView>> grid = gameGrid.getGrid();
        for (Map.Entry<HexCoordinate, Deque<ImageView>> entry : grid.entrySet()) {
            ImageView pieceImageView = entry.getValue().peek();
            if (pieceImageView.getImage() != PieceImage.BLANK_TILE.getImage()) {
                if (pieceImages.contains(getPieceTypeFromImageView(pieceImageView))) {
                    pieceImageView.setOnMouseClicked(event -> placedPieceMouseClickedEvent(pieceImageView));
                    pieceImageView.setOnMouseEntered(event -> placedPieceMouseEnteredEvent(pieceImageView));
                    pieceImageView.setOnMouseExited(event -> placedPieceMouseExitedEvent(pieceImageView));
                } else {
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
            currentTurn = gameGrid.getTurn();
            enablePlacedPieces();
            moveMade = false;
            if (currentTurn) {
                // Ensure UI updates are run on the JavaFX Application Thread
                Platform.runLater(() -> {
                    if (disableAllPiecesExceptOfQueenBee[currentTurn ? 1 : 0])
                        disableAllPiecesExceptOfQueenBee();
                    else
                        makeSelectablePieces(whitePlacementPieces);
                    makeUnSelectablePieces(blackPlacementPieces);
                });
            }
            else {
                Platform.runLater(() -> {
                    if (disableAllPiecesExceptOfQueenBee[currentTurn ? 1 : 0])
                        disableAllPiecesExceptOfQueenBee();
                    else
                        makeSelectablePieces(blackPlacementPieces);
                    makeUnSelectablePieces(whitePlacementPieces);
                });
            }

            // Wait until the move is made
            while (!moveMade) {
                try {
                    Thread.sleep(100); // Prevent CPU overuse
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return; // Exit if interrupted
                }
            }
            stopping = gameGrid.checkWin();
            if (stopping) {
                String message;
                if (currentTurn) {
                    message = "White Won!";
                } else {
                    message = "Black Won!";
                }
                System.out.println(message);
            }
            gameGrid.advanceTurn();

        }

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
        updateHexGrid();
    }

    /**
     * Helper method to create an ImageView for a piece.
     *
     * @param image the Image object to display
     * @return the ImageView with preset dimensions
     */
    private ImageView createPieceImageView(Image image, boolean isSelectable) {
        ImageView imageView = new ImageView(image);

        return imageView;
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
        if (selectedPiece != null && imageView == selectedPiece) {
            stopDisplayValidPlacements(currentDisplayedPlacements);
            selectedPiece.setStyle("");
            selectedPiece = null;
        } else {
            if (selectedPiece != null) {
                selectedPiece.setStyle("");
                stopDisplayValidPlacements(currentDisplayedPlacements);
            }
            imageView.setStyle("-fx-effect: innershadow(gaussian, green, 20, 0.5, 0, 0);");
            selectedPiece = imageView;
            List<ImageView> validMovements = gameGrid.getValidMovements(imageView);
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
        if (selectedPiece != null && imageView == selectedPiece) {
            stopDisplayValidPlacements(currentDisplayedPlacements);
            selectedPiece.setStyle("");
            selectedPiece = null;
        } else {
            if (selectedPiece != null) {
                selectedPiece.setStyle("");
                stopDisplayValidPlacements(currentDisplayedPlacements);
            }
            imageView.setStyle("-fx-effect: dropshadow(gaussian, green, 10, 0.5, 0, 0);");
            selectedPiece = imageView;
            List<ImageView> validPlacements = gameGrid.getValidPlacements();
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
            imageView.setStyle("-fx-effect: dropshadow(gaussian, yellow, 10, 0.5, 0, 0);");
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
    private void stopDisplayValidPlacements(List<ImageView> placements) {
        for (ImageView pieceImageView : placements) {
            pieceImageView.setStyle(null);
            pieceImageView.setOnMouseClicked(null);
            pieceImageView.setOnMouseEntered(null);
            pieceImageView.setOnMouseExited(null);

        }
    }

    /**
     * Displays valid placements by adding a yellow shadow effect and setting up click and hover event handlers.
     *
     * @param placements The list of ImageViews representing valid placements for the piece.
     */
    private void displayValidPlacements(List<ImageView> placements) {
        for (ImageView pieceImageView : placements) {
            /*
            HexCoordinate hexCoord = gameGrid.getHexCoord(pieceImageView);
            Point2D point2D = hex_to_pixel(hexCoord);
            pieceImageView.setLayoutX(point2D.getX());
            pieceImageView.setLayoutY(point2D.getY());
             */

            pieceImageView.setStyle("-fx-effect: dropshadow(gaussian, yellow, 10, 0.5, 0, 0);");

            pieceImageView.setOnMouseClicked(event -> {
                if (selectedPiece != null) {

                    int response = gameGrid.placePiece(currentTurn, selectedPiece, pieceImageView);
                    int pieceCount = gameGrid.getPieceCount(selectedPiece);
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
                    stopDisplayValidPlacements(placements);
                    pieceImageView.setStyle(null);
                    pieceImageView.setOnMouseExited(null);
                    pieceImageView.setOnMouseClicked(null);
                    pieceImageView.setOnMouseEntered(null);

                    updateHexGrid();
                    moveMade = true;

                    if (isQueenBeeForcedPlaced[currentTurn ? 1 : 0]) {
                        enableAllPiecesExceptOfQueenBee();
                    }

                    if (response == 1) {
                        disableAllPiecesExceptOfQueenBee[currentTurn ? 1 : 0] = true;
                        // disableAllPiecesExceptOfQueenBee();
                    }
                }
            });

            pieceImageView.setOnMouseEntered(event -> {
                pieceImageView.setStyle("-fx-effect: dropshadow(gaussian, green, 10, 0.5, 0, 0);");

            });
            pieceImageView.setOnMouseExited(event -> {
                pieceImageView.setStyle("-fx-effect: dropshadow(gaussian, yellow, 10, 0.5, 0, 0);");
            });

        }
        updateHexGrid();

    }

    /**
     * Displays valid movements by adding a yellow shadow effect and setting up click and hover event handlers.
     *
     * @param movements The list of ImageViews representing valid movements for the selected piece.
     */
    private void displayValidMovements(List<ImageView> movements) {
        for (ImageView pieceImageView : movements) {
            pieceImageView.setStyle("-fx-effect: dropshadow(gaussian, yellow, 10, 0.5, 0, 0);");

            pieceImageView.setOnMouseClicked(event -> {
                if (selectedPiece != null) {

                    gameGrid.movePiece(selectedPiece, pieceImageView);
                    updateHexGrid();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                    selectedPiece.setStyle(null);
                    selectedPiece = null;
                    stopDisplayValidPlacements(movements);
                    pieceImageView.setStyle(null);
                    pieceImageView.setOnMouseExited(null);
                    pieceImageView.setOnMouseClicked(null);
                    pieceImageView.setOnMouseEntered(null);
                    
                    moveMade = true;

                    /*
                    if (response == -1) {
                        // TODO
                    }
                     */
                }
            });

            pieceImageView.setOnMouseEntered(event -> {
                pieceImageView.setStyle("-fx-effect: innershadow(gaussian, green, 20, 0.5, 0, 0);");

            });
            pieceImageView.setOnMouseExited(event -> {
                pieceImageView.setStyle("-fx-effect: innershadow(gaussian, yellow, 20, 0.5, 0, 0);");
            });

        }
        updateHexGrid();
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
                if (currentTurn) {
                    if (pieceType != null && pieceType != PieceImage.QUEEN_BEE_WHITE) {
                        node.setOnMouseClicked(event -> panelPieceMouseClickedEvent((ImageView) node));
                        node.setOnMouseEntered(event -> panelPieceMouseEnteredEvent((ImageView) node));
                        node.setOnMouseExited(event -> panelPieceMouseExitedEvent((ImageView) node));
                    }
                } else {
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
     * This prevents the player from interacting with pieces that are not the Queen Bee during their turn.
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
                if (currentTurn) {
                    if (pieceType != null && pieceType != PieceImage.QUEEN_BEE_WHITE) {
                        node.setOnMouseClicked(null);
                        node.setOnMouseEntered(null);
                        node.setOnMouseExited(null);
                    } else if (pieceType != null) {
                        node.setOnMouseClicked(event -> panelPieceMouseClickedEvent((ImageView) node));
                        node.setOnMouseEntered(event -> panelPieceMouseEnteredEvent((ImageView) node));
                        node.setOnMouseExited(event -> panelPieceMouseExitedEvent((ImageView) node));
                    }
                } else {
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
        isQueenBeeForcedPlaced[currentTurn ? 1 : 0] = true;
        disableAllPiecesExceptOfQueenBee[currentTurn ? 1 : 0] = false;
    }
}
