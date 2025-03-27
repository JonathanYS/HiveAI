package com.example.hive;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * The {@code WelcomeScreenController} class handles the user interface for selecting the color of the player's pieces
 * (black or white) in the Hive game.
 * It provides functionality for button hover effects and scene transition animations.
 */
public class WelcomeScreenController {

    private static Stage primaryStage = GameApplication.primaryStage;

    @FXML
    private Button black;

    @FXML
    private Button white;

    @FXML
    private StackPane root; // Reference to the StackPane for animation.

    /**
     * Initializes the scene with button hover effects and a fade-in animation for the root StackPane.
     */
    public void initialize() {
        // Button Hover Effects
        black.setOnMouseEntered(event -> black.setStyle("-fx-background-color: grey; -fx-scale-x:2.1; -fx-scale-y:2.1;"));
        black.setOnMouseExited(event -> black.setStyle("-fx-background-color: black; -fx-scale-x:2; -fx-scale-y:2;"));
        white.setOnMouseEntered(event -> white.setStyle("-fx-background-color: grey; -fx-scale-x:2.1; -fx-scale-y:2.1;"));
        white.setOnMouseExited(event -> white.setStyle("-fx-background-color: white; -fx-scale-x:2; -fx-scale-y:2;"));

        // Scene Fade-in Effect
        FadeTransition fadeTransition = new FadeTransition(Duration.seconds(1), root);
        fadeTransition.setFromValue(0);
        fadeTransition.setToValue(1);
        fadeTransition.play();
    }

    /**
     * Event handler for the white pieces button. Sets up the game scene with white pieces.
     * Initializes a new {@code GameController} with white pieces and sets the game scene accordingly.
     */
    @FXML
    protected void whitePieces() {
        GameController gameController = new GameController(primaryStage, true);
        gameController.setGameScene();
    }

    /**
     * Event handler for the black pieces button. Sets up the game scene with black pieces.
     * Initializes a new {@code GameController} with black pieces and sets the game scene accordingly.
     */
    @FXML
    protected void blackPieces() {
        GameController gameController = new GameController(primaryStage, false);
        gameController.setGameScene();
    }
}
