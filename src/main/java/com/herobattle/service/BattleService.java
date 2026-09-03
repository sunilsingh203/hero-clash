package com.herobattle.service;

import com.herobattle.battle.BattleEngine;
import com.herobattle.battle.BattleException;
import com.herobattle.battle.BattleState;
import com.herobattle.game.GameCard;
import com.herobattle.model.GameMode;
import com.herobattle.model.Player;
import com.herobattle.model.Room;
import com.herobattle.model.RoomStatus;
import com.herobattle.repository.CardRepository;
import com.herobattle.repository.RoomRepository;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates a live Battle Mode v2 match: loads the room + card pool, drives the pure
 * {@link BattleEngine}, and persists {@link BattleState} to the {@link BattleStateRepository}
 * between every action. Mirrors {@link MatchService} for classic mode.
 */
@Service
public class BattleService {

    private static final Logger log = LoggerFactory.getLogger(BattleService.class);

    private final RoomRepository roomRepository;
    private final CardRepository cardRepository;
    private final BattleStateRepository battleStore;
    private final BattleEngine engine;
    private final int defaultRoundCap;

    /** Per-room lock so two near-simultaneous attacks can't interleave a load-mutate-save. */
    private final Map<String, Lock> roomLocks = new ConcurrentHashMap<>();

    public BattleService(RoomRepository roomRepository,
                         CardRepository cardRepository,
                         BattleStateRepository battleStore,
                         BattleEngine engine,
                         @Value("${heroclash.game.round-cap:25}") int defaultRoundCap) {
        this.roomRepository = roomRepository;
        this.cardRepository = cardRepository;
        this.battleStore = battleStore;
        this.engine = engine;
        this.defaultRoundCap = defaultRoundCap;
    }

    @Transactional
    public BattleState startBattle(String code) {
        Lock lock = lockFor(code);
        lock.lock();
        try {
            Room room = requireRoom(code);
            if (room.getMode() != GameMode.BATTLE) {
                throw new BattleException("Room " + code + " is not a Battle Mode room");
            }
            if (room.getStatus() != RoomStatus.WAITING) {
                throw new RoomException.RoomNotJoinable("Room " + code + " has already started");
            }
            List<Player> players = room.getPlayers();
            if (players.size() < 2) {
                throw new BattleException("Need at least 2 players to start the battle");
            }
            List<String> playerIds = players.stream().map(p -> String.valueOf(p.getId())).toList();

            List<GameCard> pool = cardRepository.findAll().stream().map(GameCard::from).toList();
            if (pool.size() < playerIds.size()) {
                throw new BattleException("Card pool is empty — seed the cards table before starting");
            }

            BattleState state = engine.startBattle(room.getCode(), playerIds, pool, defaultRoundCap);
            room.setStatus(RoomStatus.IN_PROGRESS);
            roomRepository.save(room);
            battleStore.save(state);
            log.info("Battle started for room {} with {} players", room.getCode(), playerIds.size());
            return state;
        } finally {
            lock.unlock();
        }
    }

    @Transactional
    public BattleState attack(String code, String attackerId, String targetId) {
        Lock lock = lockFor(code);
        lock.lock();
        try {
            BattleState state = battleStore.require(normalize(code));
            engine.attack(state, attackerId, targetId);
            battleStore.save(state);
            if (state.getPhase() == BattleState.Phase.FINISHED) {
                roomRepository.findByCode(normalize(code)).ifPresent(r -> {
                    r.setStatus(RoomStatus.FINISHED);
                    roomRepository.save(r);
                });
                log.info("Battle finished for room {}, winner {}", state.getRoomCode(),
                        state.getMatchWinnerId());
            }
            return state;
        } finally {
            lock.unlock();
        }
    }

    public BattleState current(String code) {
        return battleStore.require(normalize(code));
    }

    @Transactional(readOnly = true)
    public Map<String, String> playerNames(String code) {
        Map<String, String> names = new java.util.LinkedHashMap<>();
        requireRoom(code).getPlayers()
                .forEach(p -> names.put(String.valueOf(p.getId()), p.getDisplayName()));
        return names;
    }

    private Lock lockFor(String code) {
        return roomLocks.computeIfAbsent(normalize(code), k -> new ReentrantLock());
    }

    private Room requireRoom(String code) {
        return roomRepository.findByCode(normalize(code))
                .orElseThrow(() -> new RoomException.RoomNotFound(code));
    }

    private static String normalize(String code) {
        return code.toUpperCase();
    }
}
