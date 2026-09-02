package com.herobattle.controller;

import com.herobattle.controller.dto.RoomDtos.JoinRoomRequest;
import com.herobattle.controller.dto.RoomDtos.JoinRoomResponse;
import com.herobattle.controller.dto.RoomDtos.PlayerView;
import com.herobattle.controller.dto.RoomDtos.RoomView;
import com.herobattle.model.Player;
import com.herobattle.model.Room;
import com.herobattle.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoomView createRoom() {
        Room room = roomService.createRoom();
        return RoomView.from(room);
    }

    @GetMapping("/{code}")
    public RoomView getRoom(@PathVariable String code) {
        return RoomView.from(roomService.getRoom(code));
    }

    @PostMapping("/{code}/players")
    @ResponseStatus(HttpStatus.CREATED)
    public JoinRoomResponse joinRoom(@PathVariable String code,
                                     @Valid @RequestBody JoinRoomRequest request) {
        Player player = roomService.joinRoom(code, request.displayName());
        Room room = roomService.getRoom(code);
        return new JoinRoomResponse(PlayerView.from(player), RoomView.from(room));
    }
}
