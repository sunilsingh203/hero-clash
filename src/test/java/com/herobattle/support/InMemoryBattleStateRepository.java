package com.herobattle.support;

import com.herobattle.battle.BattleState;
import com.herobattle.service.BattleStateRepository;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Redis-free {@link BattleStateRepository} for tests. */
public class InMemoryBattleStateRepository implements BattleStateRepository {

    private final Map<String, BattleState> store = new ConcurrentHashMap<>();

    @Override
    public void save(BattleState state) {
        store.put(state.getRoomCode(), state);
    }

    @Override
    public Optional<BattleState> find(String roomCode) {
        return Optional.ofNullable(store.get(roomCode));
    }

    @Override
    public void delete(String roomCode) {
        store.remove(roomCode);
    }
}
