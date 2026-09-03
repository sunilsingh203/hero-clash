package com.herobattle.controller.dto;

import com.herobattle.model.ChatMessage;

/** Payloads for in-room chat (STOMP send + broadcast, plus REST history). */
public final class ChatDtos {

    private ChatDtos() {
    }

    /** STOMP action: a player sends a chat line. Display name is server-derived, not trusted here. */
    public record SendChatMessage(String playerId, String text) {
    }

    /** A chat line as broadcast to subscribers / returned from history. */
    public record ChatMessageView(
            String id,
            String playerId,
            String displayName,
            String text,
            long sentAt) {

        public static ChatMessageView from(ChatMessage m) {
            return new ChatMessageView(m.id(), m.playerId(), m.displayName(), m.text(), m.sentAt());
        }
    }
}
