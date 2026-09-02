package com.herobattle.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.herobattle.model.Player;
import com.herobattle.model.Room;
import com.herobattle.model.RoomStatus;
import com.herobattle.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(RoomService.class)
class RoomServiceTest {

    @Autowired
    private RoomService roomService;

    @Autowired
    private RoomRepository roomRepository;

    private Room openRoom;

    @BeforeEach
    void setUp() {
        openRoom = roomService.createRoom();
    }

    @Test
    void createRoomGeneratesSixCharCodeAndWaitingStatus() {
        assertThat(openRoom.getCode()).hasSize(6);
        assertThat(openRoom.getStatus()).isEqualTo(RoomStatus.WAITING);
        assertThat(roomRepository.findByCode(openRoom.getCode())).isPresent();
    }

    @Test
    void joinRoomAddsPlayer() {
        Player player = roomService.joinRoom(openRoom.getCode().toLowerCase(), "  Ada  ");

        assertThat(player.getId()).isNotNull();
        assertThat(player.getDisplayName()).isEqualTo("Ada");
        assertThat(roomService.getRoom(openRoom.getCode()).getPlayers()).hasSize(1);
    }

    @Test
    void joinRoomRejectsFifthPlayer() {
        for (int i = 0; i < 4; i++) {
            roomService.joinRoom(openRoom.getCode(), "P" + i);
        }
        assertThatThrownBy(() -> roomService.joinRoom(openRoom.getCode(), "P5"))
                .isInstanceOf(RoomException.RoomNotJoinable.class);
    }

    @Test
    void joinRoomRejectsWhenNotWaiting() {
        Room r = roomRepository.findByCode(openRoom.getCode()).orElseThrow();
        r.setStatus(RoomStatus.IN_PROGRESS);
        roomRepository.save(r);

        assertThatThrownBy(() -> roomService.joinRoom(openRoom.getCode(), "Late"))
                .isInstanceOf(RoomException.RoomNotJoinable.class);
    }

    @Test
    void getMissingRoomThrows() {
        assertThatThrownBy(() -> roomService.getRoom("ZZZZZZ"))
                .isInstanceOf(RoomException.RoomNotFound.class);
    }
}
