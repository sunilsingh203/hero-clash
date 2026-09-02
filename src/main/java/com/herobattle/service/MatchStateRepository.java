package com.herobattle.service;

import com.herobattle.game.GameState;
import java.util.Optional;

/**
 * Storage for in-flight matches, keyed by room code. The production implementation is
 * Redis-backed so any backend instance can advance any match; tests swap in an in-memory
 * variant.
 */
public interface MatchStateRepository {

    void save(GameState state);

    Optional<GameState> find(String roomCode);

    void delete(String roomCode);

    default GameState require(String roomCode) {
        return find(roomCode)
                .orElseThrow(() -> new IllegalStateException("No active match for room " + roomCode));
    }
}
