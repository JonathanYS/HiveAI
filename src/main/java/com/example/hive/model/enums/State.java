package com.example.hive.model.enums;

/**
 * Enum representing the different states of the game or the AI's decision-making process.
 * Each state corresponds to a specific phase or action in the game, guiding the AI's behavior.
 */
public enum State {
    OPENING, CHECK_IMMEDIATE_WIN, BLOCK_THREAT, SURROUND, IMPROVE_MOBILITY, STANDARD, FINISHED
}
