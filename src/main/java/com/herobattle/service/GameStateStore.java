package com.herobattle.service;

import com.herobattle.game.GameState;
import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * Redis-backed store for in-flight matches, keyed by room code. Matches are transient —
 * they expire automatically so abandoned rooms don't leak memory.
 */
@Repository
public class GameStateStore {

    private static final String KEY_PREFIX = "heroclash:game:";
    private static final Duration TTL = Duration.ofHours(2);

    private final RedisTemplate<String, GameState> redis;

    public GameStateStore(RedisTemplate<String, GameState> gameStateRedisTemplate) {
        this.redis = gameStateRedisTemplate;
    }

    public void save(GameState state) {
        redis.opsForValue().set(key(state.getRoomCode()), state, TTL);
    }

    public Optional<GameState> find(String roomCode) {
        return Optional.ofNullable(redis.opsForValue().get(key(roomCode)));
    }

    public GameState require(String roomCode) {
        return find(roomCode)
                .orElseThrow(() -> new IllegalStateException("No active match for room " + roomCode));
    }

    public void delete(String roomCode) {
        redis.delete(key(roomCode));
    }

    private static String key(String roomCode) {
        return KEY_PREFIX + roomCode;
    }
}
