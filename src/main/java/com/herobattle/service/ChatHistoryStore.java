package com.herobattle.service;

import com.herobattle.model.ChatMessage;
import java.util.List;

/**
 * Recent-message history for a room's chat. Production impl is a capped, TTL'd Redis list so
 * any backend instance can serve the backlog; tests swap in an in-memory variant.
 */
public interface ChatHistoryStore {

    /** Max messages retained per room. Older messages are dropped. */
    int MAX_HISTORY = 50;

    /** Append a message and trim the room's history to {@link #MAX_HISTORY}. */
    void append(ChatMessage message);

    /** Recent messages for a room, oldest first (at most {@link #MAX_HISTORY}). */
    List<ChatMessage> recent(String roomCode);
}
