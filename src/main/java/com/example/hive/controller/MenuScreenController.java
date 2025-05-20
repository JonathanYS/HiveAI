package com.example.hive.controller;

import com.example.hive.utils.UIHelper;
import com.example.hive.model.enums.PieceColor;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Controller class for managing the menu screen of the Hive game.
 * This class handles button interactions, scene transitions, and animations for the menu screen.
 */
public class MenuScreenController {

    @FXML
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

    @FXML
    private Button returnButton;

    @FXML
    private Label titleLabel;

    private Scene cachedScene;

    /**
     * Initializes the menu screen by setting up hover effects on buttons
     * and adding a fade-in animation for the root StackPane.
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

        UIHelper.applyHoverEffect(rulesButton, "-fx-background-color: black; -fx-scale-x:3; -fx-scale-y:3;",
                "-fx-background-color: grey; -fx-scale-x:3.1; -fx-scale-y:3.1;");
        UIHelper.applyHoverEffect(exitButton, "-fx-background-color: black; -fx-scale-x:3; -fx-scale-y:3;",
                "-fx-background-color: grey; -fx-scale-x:3.1; -fx-scale-y:3.1;");
        UIHelper.applyHoverEffect(returnButton, "-fx-background-color: black; -fx-scale-x:3; -fx-scale-y:3;",
                "-fx-background-color: grey; -fx-scale-x:3.1; -fx-scale-y:3.1;");

        rulesButton.setPrefWidth(55);
        exitButton.setPrefWidth(55);
        returnButton.setPrefWidth(55);
    }

    /**
     * Set the return button as visible or not. If it is a winning screen, there is nowhere to return to.
     * @param on true for visible and false for not visible.
     */
    public void setReturnButton(boolean on) {
        returnButton.setVisible(on);
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
     * Sets the title label with the given winner message.
     *
     * @param winnerMessage The message to display in the title label.
     */
    public void setTitleLabel(String winnerMessage) {
        titleLabel.setText(winnerMessage);
    }

    /**
     * Sets the cached scene to be used when transitioning back to the menu.
     *
     * @param scene The cached scene that was previously set.
     */
    public void setCachedScene(Scene scene) {
        cachedScene = scene;
    }

    /**
     * Displays an exit confirmation dialog to the user.
     */
    @FXML
    protected void exit() {
        UIHelper.showExitConfirmation();
    }

    /**
     * Displays the rules popup for the game.
     */
    @FXML
    protected void showRules() {
        UIHelper.showRulesPopup();
    }

    /**
     * Handles the action of returning to the previous game scene.
     * Animates the menu screen sliding to the right and shows the cached scene.
     */
    @FXML
    protected void returnToGame() {
        Scene currentScene = primaryStage.getScene();
        Parent oldRoot = currentScene.getRoot();

        Parent newRoot = cachedScene.getRoot();

        // Create a temporary container with both roots.
        StackPane overlay = new StackPane();
        overlay.getChildren().add(newRoot);
        overlay.getChildren().add(oldRoot);

        // Temporarily set the overlay as the root.
        currentScene.setRoot(overlay);

        // Animate the current (menu) screen sliding to the right.
        Timeline slideOut = new Timeline();
        KeyValue keyValue = new KeyValue(oldRoot.translateXProperty(), primaryStage.getWidth(), Interpolator.EASE_BOTH);
        KeyFrame keyFrame = new KeyFrame(Duration.millis(500), keyValue);
        slideOut.getKeyFrames().add(keyFrame);

        slideOut.play();
    }

    /**
     * Starts a player vs player (PvP) game by initializing the GameController and setting up the game scene.
     */
    @FXML
    protected void pvpGame() {
        GameController gameController = new GameController(primaryStage);
        gameController.setGameScene();
    }

    /**
     * Prepares the game for a player vs AI (PvAI) game by showing the piece color selection buttons.
     */
    @FXML
    protected void pvAIGame() {
        chooseLabel.setVisible(true);
        blackButton.setVisible(true);
        whiteButton.setVisible(true);
    }

    /**
     * Starts a player vs AI game with black pieces by initializing the GameController and setting up the game scene.
     */
    @FXML
    protected void blackPieces() {
        GameController gameController = new GameController(primaryStage, PieceColor.BLACK);
        gameController.setGameScene();
    }

    /**
     * Starts a player vs AI game with white pieces by initializing the GameController and setting up the game scene.
     */
    @FXML
    protected void whitePieces() {
        GameController gameController = new GameController(primaryStage, PieceColor.WHITE);
        gameController.setGameScene();
    }
}
