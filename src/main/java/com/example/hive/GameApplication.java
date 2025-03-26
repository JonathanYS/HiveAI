package com.example.hive;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class GameApplication extends Application {

    public static Stage primaryStage; // for the controllers.

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        FXMLLoader fxmlLoader = new FXMLLoader(GameController.class.getResource("color-choosing.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 600);
        stage.setTitle("Hive Game");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
        // GameController controller = new GameController(stage);
        // controller.setGameScene();

    }

    public static void main(String[] args) {
        launch();
    }
}