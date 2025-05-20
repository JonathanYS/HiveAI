package com.example.hive;

/**
 * The {@code Main} class serves as the entry point for the Hive game application.
 * <p>
 * It delegates to {@link GameApplication}, which is responsible for initializing
 * and launching the JavaFX application.
 * Its main purpose is to create the JAR file for this project and to act as the Launcher of it.
 */
public class Main {

    /**
     * The main method of the application. This method is called by the Java runtime
     * to start the program. It passes command-line arguments to the {@link GameApplication}
     * class, which launches the JavaFX UI.
     *
     * @param args the command-line arguments passed to the program
     */
    public static void main(String[] args) {
        GameApplication.main(args);
    }
}
