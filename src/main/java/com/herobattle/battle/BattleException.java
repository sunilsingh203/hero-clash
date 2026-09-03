package com.herobattle.battle;

/** An illegal Battle Mode action (wrong turn, dead target, match over, …). Maps to HTTP 422. */
public class BattleException extends RuntimeException {

    public BattleException(String message) {
        super(message);
    }
}
