package com.herobattle.service;

import com.herobattle.game.GameCard;
import com.herobattle.game.GameEngine;
import com.herobattle.game.GameException;
import com.herobattle.game.GameState;
import com.herobattle.game.Stat;
import com.herobattle.game.TurnResult;
import com.herobattle.model.Player;
import com.herobattle.model.Room;
import com.herobattle.model.RoomStatus;
import com.herobattle.repository.CardRepository;
import com.herobattle.repository.RoomRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates a live match: loads the room + card pool, drives the pure {@link GameEngine},
 * and persists {@link GameState} to the {@link MatchStateRepository} between every action so
 * any backend instance can pick up the next one.
 */
@Service
public class MatchService {

    private static final Logger log = LoggerFactory.getLogger(MatchService.class);

    private final RoomRepository roomRepository;
    private final CardRepository cardRepository;
    private final MatchStateRepository matchStore;
    private final GameEngine engine;
    private final int defaultRoundCap;

    public MatchService(RoomRepository roomRepository,
                        CardRepository cardRepository,
                        MatchStateRepository matchStore,
                        GameEngine engine,
                        @Value("${heroclash.game.round-cap:25}") int defaultRoundCap) {
        this.roomRepository = roomRepository;
        this.cardRepository = cardRepository;
        this.matchStore = matchStore;
        this.engine = engine;
        this.defaultRoundCap = defaultRoundCap;
    }

    @Transactional
    public MatchUpdate startMatch(String code) {
        Room room = requireRoom(code);
        if (room.getStatus() != RoomStatus.WAITING) {
            throw new RoomException.RoomNotJoinable("Room " + code + " has already started");
        }
        List<Player> players = room.getPlayers();
        if (players.size() < 2) {
            throw new GameException("Need at least 2 players to start the match");
        }
        List<String> playerIds = players.stream().map(p -> String.valueOf(p.getId())).toList();

        List<GameCard> pool = cardRepository.findAll().stream().map(GameCard::from).toList();
        if (pool.size() < playerIds.size()) {
            throw new GameException("Card pool is empty — seed the cards table before starting");
        }

        GameState state = engine.startMatch(room.getCode(), playerIds, pool, defaultRoundCap);
        room.setStatus(RoomStatus.IN_PROGRESS);
        roomRepository.save(room);
        matchStore.save(state);
        log.info("Match started for room {} with {} players", room.getCode(), playerIds.size());
        return MatchUpdate.of(state);
    }

    public MatchUpdate pick(String code, String playerId, Stat stat) {
        GameState state = matchStore.require(normalize(code));
        engine.pickCategory(state, playerId, stat);
        matchStore.save(state);
        return MatchUpdate.of(state);
    }

    @Transactional
    public MatchUpdate reveal(String code, String playerId) {
        GameState state = matchStore.require(normalize(code));
        TurnResult result = engine.submitReveal(state, playerId);
        matchStore.save(state);
        if (state.getPhase() == GameState.Phase.FINISHED) {
            roomRepository.findByCode(normalize(code)).ifPresent(r -> {
                r.setStatus(RoomStatus.FINISHED);
                roomRepository.save(r);
            });
            log.info("Match finished for room {}, winner {}", state.getRoomCode(),
                    state.getMatchWinnerId());
        }
        return new MatchUpdate(state, result);
    }

    public GameState current(String code) {
        return matchStore.require(normalize(code));
    }

    /** Player id → display name, for labelling the public game view. */
    @Transactional(readOnly = true)
    public Map<String, String> playerNames(String code) {
        Map<String, String> names = new LinkedHashMap<>();
        requireRoom(code).getPlayers()
                .forEach(p -> names.put(String.valueOf(p.getId()), p.getDisplayName()));
        return names;
    }

    private Room requireRoom(String code) {
        return roomRepository.findByCode(normalize(code))
                .orElseThrow(() -> new RoomException.RoomNotFound(code));
    }

    private static String normalize(String code) {
        return code.toUpperCase();
    }
}
