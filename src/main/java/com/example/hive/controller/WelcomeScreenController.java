package com.example.hive.controller;

import com.example.hive.utils.UIHelper;
import com.example.hive.model.enums.PieceColor;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

/**
 * The {@code WelcomeScreenController} class handles the user interface for selecting the color of the player's pieces
 * (black or white) in the Hive game.
 * It provides functionality for button hover effects and scene transition animations.
 */
public class WelcomeScreenController {

    private Stage primaryStage;

    @FXML
    private Button pvP;

    @FXML
    private Button pvAI;

    @FXML
    private Label chooseLabel;

    @FXML
    private Button blackButton;

    @FXML
    private Button whiteButton;

    @FXML
    private Button rulesButton;

    @FXML
    private Button exitButton;

    /**
     * Initializes the scene by setting up hover effects on buttons and adding a fade-in animation for the root StackPane.
     * This method is automatically called when the scene is loaded.
     */
    public void initialize() {
        // Button Hover Effects
        UIHelper.applyHoverEffect(pvP, "-fx-background-color: black; -fx-scale-x:2; -fx-scale-y:2;",
                "-fx-background-color: grey; -fx-scale-x:2.1; -fx-scale-y:2.1;");
        UIHelper.applyHoverEffect(pvAI, "-fx-background-color: black; -fx-scale-x:2; -fx-scale-y:2;",
                "-fx-background-color: grey; -fx-scale-x:2.1; -fx-scale-y:2.1;");

        pvP.setPrefWidth(40);
        pvAI.setPrefWidth(40);

        UIHelper.applyHoverEffect(blackButton, "-fx-background-color: black; -fx-scale-x:2; -fx-scale-y:2;",
                "-fx-background-color: grey; -fx-scale-x:2.1; -fx-scale-y:2.1;");
        UIHelper.applyHoverEffect(whiteButton, "-fx-background-color: white; -fx-scale-x:2; -fx-scale-y:2;",
                "-fx-background-color: grey; -fx-scale-x:2.1; -fx-scale-y:2.1;");

        UIHelper.applyHoverEffect(rulesButton, "-fx-background-color: black; -fx-scale-x:2; -fx-scale-y:2;",
                "-fx-background-color: grey; -fx-scale-x:2.1; -fx-scale-y:2.1;");
        UIHelper.applyHoverEffect(exitButton, "-fx-background-color: black; -fx-scale-x:2; -fx-scale-y:2;",
                "-fx-background-color: grey; -fx-scale-x:2.1; -fx-scale-y:2.1;");

        rulesButton.setPrefWidth(45);
        exitButton.setPrefWidth(45);
    }

    /**
     * Displays an exit confirmation dialog when the exit button is clicked.
     */
    @FXML
    protected void exit() {
        UIHelper.showExitConfirmation();
    }

    /**
     * Displays a popup showing the game rules when the rules button is clicked.
     */
    @FXML
    protected void showRules() {
        UIHelper.showRulesPopup();
    }

    /**
     * Sets the primary stage (window) for this controller.
     *
     * @param primaryStage The primary stage of the application.
     */
    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    /**
     * Displays the piece color selection buttons (black and white) when the player chooses to play against AI.
     */
    @FXML
    protected void pvAIGame() {
        chooseLabel.setVisible(true);
        blackButton.setVisible(true);
        whiteButton.setVisible(true);
    }

    /**
     * Starts a player vs AI game with black pieces by initializing the GameController and setting up the game scene.
     * This method is triggered when the player selects the black pieces option.
     */
    @FXML
    protected void blackPieces() {
        GameController gameController = new GameController(primaryStage, PieceColor.BLACK);
        gameController.setGameScene();
    }

    /**
     * Starts a player vs AI game with white pieces by initializing the GameController and setting up the game scene.
     * This method is triggered when the player selects the white pieces option.
     */
    @FXML
    protected void whitePieces() {
        GameController gameController = new GameController(primaryStage, PieceColor.WHITE);
        gameController.setGameScene();
    }

    /**
     * Event handler for the black pieces button. Sets up the game scene with black pieces.
     * Initializes a new {@code GameController} with black pieces and sets the game scene accordingly.
     */
    @FXML
    protected void pvpGame() {
        GameController gameController = new GameController(primaryStage);
        gameController.setGameScene();
    }
}
