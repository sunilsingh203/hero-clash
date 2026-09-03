package com.herobattle.model;

import java.util.UUID;

/**
 * One line of in-room chat. Not persisted to Postgres — chat is ephemeral and lives only in
 * a capped, TTL'd Redis list per room (see {@code ChatHistoryStore}).
 *
 * @param id          stable id, so clients can dedupe REST history against live broadcasts
 * @param roomCode    room this message belongs to
 * @param playerId    author's player id
 * @param displayName author's display name at send time (server-derived, never client-supplied)
 * @param text        message body, already trimmed and length-checked
 * @param sentAt      epoch milliseconds
 */
public record ChatMessage(
        String id,
        String roomCode,
        String playerId,
        String displayName,
        String text,
        long sentAt) {

    public static ChatMessage create(String roomCode, String playerId, String displayName, String text) {
        return new ChatMessage(
                UUID.randomUUID().toString(),
                roomCode,
                playerId,
                displayName,
                text,
                System.currentTimeMillis());
    }
}
