package com.example.hive.utils;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A utility class providing helper methods for UI interactions in the Hive game.
 * Includes hover effects for buttons, dialog popups, and loading rule text from files.
 */
public class UIHelper {

    /**
     * Applies a hover effect to a button by switching its CSS style on mouse enter/exit events.
     *
     * @param button      The {@link Button} to apply the effect to.
     * @param normalStyle The CSS style to apply when the mouse is not hovering.
     * @param hoverStyle  The CSS style to apply when the mouse is hovering.
     */
    public static void applyHoverEffect(Button button, String normalStyle, String hoverStyle) {
        button.setOnMouseEntered(e -> button.setStyle(hoverStyle));
        button.setOnMouseExited(e -> button.setStyle(normalStyle));
    }

    /**
     * Displays a confirmation dialog asking the user if they want to exit the game.
     * If the user confirms, the application will terminate.
     */
    public static void showExitConfirmation() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Exit");
        alert.setHeaderText("Are you sure you want to exit the game?");
        alert.setContentText("This will end your current game. All progress will be lost.");

        ButtonType yes = new ButtonType("Yes", ButtonBar.ButtonData.YES);
        ButtonType no = new ButtonType("No", ButtonBar.ButtonData.NO);

        alert.getButtonTypes().setAll(yes, no);

        Optional<ButtonType> result = alert.showAndWait();
        result.ifPresent(buttonType -> {
            if (buttonType == yes) {
                javafx.application.Platform.exit();
            }
        });
    }

    /**
     * Loads the game rules from a text file located at "/rules/rules.txt" in the "resources" folder.
     *
     * @return The content of the rules file as a String, or a fallback message if the file couldn't be loaded.
     */
    public static String loadRulesFromFile() {
        try (InputStream input = UIHelper.class.getResourceAsStream("/rules/rules.txt")) {
            assert input != null;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (IOException | NullPointerException e) {
            return "Rules could not be loaded.";
        }
    }

    /**
     * Displays a popup window containing the game rules.
     * The popup includes a scrollable text area and a button to return to the main menu.
     */
    public static void showRulesPopup() {
        Stage rulesStage = new Stage();
        rulesStage.setTitle("Game Rules");

        Label rulesLabel = new Label(loadRulesFromFile());
        rulesLabel.setWrapText(true);
        rulesLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #333;");

        ScrollPane scrollPane = new ScrollPane(rulesLabel);
        scrollPane.setFitToWidth(true);
        scrollPane.setPadding(new Insets(20));
        scrollPane.setPrefSize(600, 400);

        Button backButton = new Button("Back to Menu");
        backButton.setStyle("-fx-background-color: #444; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20;");
        backButton.setOnAction(e -> rulesStage.close());

        applyHoverEffect(backButton, "-fx-background-color: #444; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20;",
                "-fx-background-color: grey; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20;");

        VBox layout = new VBox(20, scrollPane, backButton);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: #f4f4f4;");

        rulesStage.setScene(new Scene(layout));
        rulesStage.initModality(Modality.APPLICATION_MODAL);
        rulesStage.show();
    }
}
