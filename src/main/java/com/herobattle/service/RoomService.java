package com.herobattle.service;

import java.security.SecureRandom;

import com.herobattle.model.Player;
import com.herobattle.model.Room;
import com.herobattle.model.RoomStatus;
import com.herobattle.repository.PlayerRepository;
import com.herobattle.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Room lifecycle: create a room, join players, look up state. Persists to Postgres.
 * Live in-progress game state (decks, rounds) is layered on top in later features.
 */
@Service
public class RoomService {

    private static final Logger log = LoggerFactory.getLogger(RoomService.class);

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;
    private static final int MAX_PLAYERS = 4;
    private static final int CODE_GENERATION_ATTEMPTS = 20;

    private final RoomRepository roomRepository;
    private final PlayerRepository playerRepository;
    private final SecureRandom random = new SecureRandom();

    public RoomService(RoomRepository roomRepository, PlayerRepository playerRepository) {
        this.roomRepository = roomRepository;
        this.playerRepository = playerRepository;
    }

    @Transactional
    public Room createRoom() {
        Room room = new Room();
        room.setCode(generateUniqueCode());
        room.setStatus(RoomStatus.WAITING);
        Room saved = roomRepository.save(room);
        log.info("Created room {}", saved.getCode());
        return saved;
    }

    @Transactional
    public Player joinRoom(String code, String displayName) {
        Room room = requireRoom(code);
        if (room.getStatus() != RoomStatus.WAITING) {
            throw new RoomException.RoomNotJoinable("Room " + code + " is not accepting players");
        }
        if (room.getPlayers().size() >= MAX_PLAYERS) {
            throw new RoomException.RoomNotJoinable("Room " + code + " is full");
        }
        Player player = new Player();
        player.setDisplayName(displayName.trim());
        room.addPlayer(player);
        Player saved = playerRepository.saveAndFlush(player);
        log.info("Player {} joined room {}", saved.getDisplayName(), code);
        return saved;
    }

    @Transactional(readOnly = true)
    public Room getRoom(String code) {
        Room room = requireRoom(code);
        room.getPlayers().size(); // initialize lazy collection inside the tx
        return room;
    }

    private Room requireRoom(String code) {
        return roomRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new RoomException.RoomNotFound(code));
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < CODE_GENERATION_ATTEMPTS; attempt++) {
            String code = randomCode();
            if (roomRepository.findByCode(code).isEmpty()) {
                return code;
            }
        }
        throw new IllegalStateException("Could not generate a unique room code after "
                + CODE_GENERATION_ATTEMPTS + " attempts");
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
        }
        return sb.toString();
    }
}
