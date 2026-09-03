package com.herobattle.support;

import com.herobattle.model.ChatMessage;
import com.herobattle.service.ChatHistoryStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Redis-free {@link ChatHistoryStore} for tests. Applies the same per-room cap. */
public class InMemoryChatHistoryStore implements ChatHistoryStore {

    private final Map<String, List<ChatMessage>> store = new ConcurrentHashMap<>();

    @Override
    public void append(ChatMessage message) {
        store.compute(message.roomCode(), (room, existing) -> {
            List<ChatMessage> list = existing == null ? new ArrayList<>() : new ArrayList<>(existing);
            list.add(message);
            if (list.size() > MAX_HISTORY) {
                list = new ArrayList<>(list.subList(list.size() - MAX_HISTORY, list.size()));
            }
            return list;
        });
    }

    @Override
    public List<ChatMessage> recent(String roomCode) {
        return List.copyOf(store.getOrDefault(roomCode, List.of()));
    }
}
