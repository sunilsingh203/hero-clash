package com.herobattle.controller.dto;

import java.util.List;

import com.herobattle.model.Player;
import com.herobattle.model.Room;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request/response payloads for the room REST API. */
public final class RoomDtos {

    private RoomDtos() {
    }

    public record JoinRoomRequest(
            @NotBlank @Size(min = 1, max = 30) String displayName) {
    }

    public record PlayerView(Long id, String displayName) {
        public static PlayerView from(Player p) {
            return new PlayerView(p.getId(), p.getDisplayName());
        }
    }

    public record RoomView(String code, String status, String mode, List<PlayerView> players) {
        public static RoomView from(Room room) {
            return new RoomView(
                    room.getCode(),
                    room.getStatus().name(),
                    room.getMode().name(),
                    room.getPlayers().stream().map(PlayerView::from).toList());
        }
    }

    /** Returned from join so the client knows which player it is. */
    public record JoinRoomResponse(PlayerView you, RoomView room) {
    }
}
