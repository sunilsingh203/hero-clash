package com.herobattle.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import com.herobattle.model.Room;
import com.herobattle.model.RoomStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RoomRepositoryTest {

    @Autowired
    private RoomRepository roomRepository;

    @Test
    void savesAndFetchesRoomByCode() {
        Room room = new Room();
        room.setCode("ABC123");
        room.setStatus(RoomStatus.WAITING);
        roomRepository.save(room);

        Optional<Room> found = roomRepository.findByCode("ABC123");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isNotNull();
        assertThat(found.get().getCode()).isEqualTo("ABC123");
        assertThat(found.get().getStatus()).isEqualTo(RoomStatus.WAITING);
    }
}
