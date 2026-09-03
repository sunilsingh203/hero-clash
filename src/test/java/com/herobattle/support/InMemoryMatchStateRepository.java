package com.herobattle.support;

import com.herobattle.game.GameState;
import com.herobattle.service.MatchStateRepository;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Redis-free {@link MatchStateRepository} for tests. */
public class InMemoryMatchStateRepository implements MatchStateRepository {

    private final Map<String, GameState> store = new ConcurrentHashMap<>();

    @Override
    public void save(GameState state) {
        store.put(state.getRoomCode(), state);
    }

    @Override
    public Optional<GameState> find(String roomCode) {
        return Optional.ofNullable(store.get(roomCode));
    }

    @Override
    public void delete(String roomCode) {
        store.remove(roomCode);
    }
}
