package com.herobattle.service;

import com.herobattle.battle.BattleState;
import java.util.Optional;

/**
 * Storage for in-flight Battle Mode matches, keyed by room code. Mirrors
 * {@link MatchStateRepository} for classic mode; Redis-backed in production, in-memory in tests.
 */
public interface BattleStateRepository {

    void save(BattleState state);

    Optional<BattleState> find(String roomCode);

    void delete(String roomCode);

    default BattleState require(String roomCode) {
        return find(roomCode)
                .orElseThrow(() -> new IllegalStateException("No active battle for room " + roomCode));
    }
}
