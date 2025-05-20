package com.example.hive;

import com.example.hive.controller.GameController;
import com.example.hive.controller.WelcomeScreenController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * The main entry point for the Hive game application.
 * This class initializes and launches the JavaFX application, setting up the main game window.
 */
public class GameApplication extends Application {

    private static Stage primaryStage; // for the controllers.

    /**
     * Initializes the main game window, loading the welcome screen and setting up the scene.
     * This method is called when the application is launched.
     *
     * @param stage The primary stage provided by JavaFX for the application window.
     * @throws IOException If an error occurs while loading the FXML file for the welcome screen.
     */
    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        FXMLLoader fxmlLoader = new FXMLLoader(GameController.class.getResource("/fxml/welcome-screen.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 600);
        WelcomeScreenController controller = fxmlLoader.getController();
        controller.setPrimaryStage(primaryStage);
        stage.setTitle("Hive Game");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    /**
     * The main method to launch the JavaFX application.
     *
     * @param args Command-line arguments (not used in this case).
     */
    public static void main(String[] args) {
        launch();
    }
}