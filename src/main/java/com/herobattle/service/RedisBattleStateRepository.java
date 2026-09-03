package com.herobattle.service;

import com.herobattle.battle.BattleState;
import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * Redis-backed {@link BattleStateRepository}. One JSON value per room, keyed by room code,
 * with a TTL so abandoned battles expire instead of leaking.
 */
@Repository
public class RedisBattleStateRepository implements BattleStateRepository {

    private static final String KEY_PREFIX = "heroclash:battle:";
    private static final Duration TTL = Duration.ofHours(2);

    private final RedisTemplate<String, BattleState> redis;

    public RedisBattleStateRepository(RedisTemplate<String, BattleState> battleStateRedisTemplate) {
        this.redis = battleStateRedisTemplate;
    }

    @Override
    public void save(BattleState state) {
        redis.opsForValue().set(key(state.getRoomCode()), state, TTL);
    }

    @Override
    public Optional<BattleState> find(String roomCode) {
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
