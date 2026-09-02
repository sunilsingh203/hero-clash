package com.herobattle.game;

/** Illegal game action (wrong turn, wrong phase, unknown player, …). */
public class GameException extends RuntimeException {
    public GameException(String message) {
        super(message);
    }
}
