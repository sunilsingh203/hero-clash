package com.herobattle.repository;

import java.util.Optional;

import com.herobattle.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {

    Optional<Room> findByCode(String code);
}
