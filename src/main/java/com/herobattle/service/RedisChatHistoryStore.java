package com.herobattle.service;

import com.herobattle.model.ChatMessage;
import java.time.Duration;
import java.util.List;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * Redis-backed {@link ChatHistoryStore}. Each room's chat is one Redis list, capped at
 * {@link #MAX_HISTORY} and expiring after {@link #TTL} of inactivity so abandoned rooms
 * don't leak.
 */
@Repository
public class RedisChatHistoryStore implements ChatHistoryStore {

    private static final String KEY_PREFIX = "heroclash:chat:";
    private static final Duration TTL = Duration.ofHours(2);

    private final RedisTemplate<String, ChatMessage> redis;

    public RedisChatHistoryStore(RedisTemplate<String, ChatMessage> chatMessageRedisTemplate) {
        this.redis = chatMessageRedisTemplate;
    }

    @Override
    public void append(ChatMessage message) {
        String key = key(message.roomCode());
        redis.opsForList().rightPush(key, message);
        redis.opsForList().trim(key, -MAX_HISTORY, -1);
        redis.expire(key, TTL);
    }

    @Override
    public List<ChatMessage> recent(String roomCode) {
        List<ChatMessage> messages = redis.opsForList().range(key(roomCode), 0, -1);
        return messages == null ? List.of() : messages;
    }

    private static String key(String roomCode) {
        return KEY_PREFIX + roomCode;
    }
}
