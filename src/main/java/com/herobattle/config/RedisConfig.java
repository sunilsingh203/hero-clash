package com.herobattle.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.herobattle.game.GameState;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis wiring for live match state. Every {@link GameState} is stored as plain JSON under
 * a single key per room, so any backend instance can load a room, advance it, and write it
 * back (see {@code CLAUDE.md} §"Shared state").
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, GameState> gameStateRedisTemplate(
            RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        RedisTemplate<String, GameState> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(objectMapper, GameState.class));
        template.afterPropertiesSet();
        return template;
    }
}
