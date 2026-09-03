package com.herobattle.service;

import com.herobattle.game.GameState;
import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * Redis-backed {@link MatchStateRepository}. Each match is one JSON value keyed by room
 * code, with a TTL so abandoned rooms expire instead of leaking.
 */
@Repository
public class RedisMatchStateRepository implements MatchStateRepository {

    private static final String KEY_PREFIX = "heroclash:game:";
    private static final Duration TTL = Duration.ofHours(2);

    private final RedisTemplate<String, GameState> redis;

    public RedisMatchStateRepository(RedisTemplate<String, GameState> gameStateRedisTemplate) {
        this.redis = gameStateRedisTemplate;
    }

    @Override
    public void save(GameState state) {
        redis.opsForValue().set(key(state.getRoomCode()), state, TTL);
    }

    @Override
    public Optional<GameState> find(String roomCode) {
        return Optional.ofNullable(redis.opsForValue().get(key(roomCode)));
    }

    @Override
    public void delete(String roomCode) {
        redis.delete(key(roomCode));
    }

    private static String key(String roomCode) {
        return KEY_PREFIX + roomCode;
    }
}
