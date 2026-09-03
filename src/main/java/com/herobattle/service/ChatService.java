package com.herobattle.service;

import com.herobattle.model.ChatMessage;
import com.herobattle.model.Player;
import com.herobattle.model.Room;
import com.herobattle.repository.RoomRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * In-room chat. Validates that the sender is actually a member of the room, normalizes the
 * text, records it in {@link ChatHistoryStore}, and returns it for broadcast.
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    /** Longest message we accept; anything over is a client bug or abuse. */
    static final int MAX_LENGTH = 280;

    private final RoomRepository roomRepository;
    private final ChatHistoryStore history;

    public ChatService(RoomRepository roomRepository, ChatHistoryStore history) {
        this.roomRepository = roomRepository;
        this.history = history;
    }

    @Transactional(readOnly = true)
    public ChatMessage post(String code, String playerId, String rawText) {
        String text = rawText == null ? "" : rawText.strip();
        if (text.isEmpty()) {
            throw new ChatException("Message is empty");
        }
        if (text.length() > MAX_LENGTH) {
            throw new ChatException("Message is too long (max " + MAX_LENGTH + " characters)");
        }

        Room room = roomRepository.findByCode(normalize(code))
                .orElseThrow(() -> new RoomException.RoomNotFound(code));
        String displayName = room.getPlayers().stream()
                .filter(p -> String.valueOf(p.getId()).equals(playerId))
                .map(Player::getDisplayName)
                .findFirst()
                .orElseThrow(() -> new ChatException("Player " + playerId + " is not in room " + code));

        ChatMessage message = ChatMessage.create(room.getCode(), playerId, displayName, text);
        history.append(message);
        log.debug("Chat in {} from {}: {}", room.getCode(), displayName, text);
        return message;
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> history(String code) {
        Room room = roomRepository.findByCode(normalize(code))
                .orElseThrow(() -> new RoomException.RoomNotFound(code));
        return history.recent(room.getCode());
    }

    private static String normalize(String code) {
        return code == null ? "" : code.toUpperCase();
    }
}
