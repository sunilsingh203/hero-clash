package com.herobattle.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.herobattle.model.Player;
import com.herobattle.model.Room;
import com.herobattle.model.RoomStatus;
import com.herobattle.service.RoomException;
import com.herobattle.service.RoomService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RoomController.class)
class RoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RoomService roomService;

    private Room room(String code, RoomStatus status) {
        Room r = new Room();
        r.setCode(code);
        r.setStatus(status);
        return r;
    }

    @Test
    void createRoomReturns201WithCode() throws Exception {
        when(roomService.createRoom()).thenReturn(room("ABC123", RoomStatus.WAITING));

        mockMvc.perform(post("/api/rooms"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("ABC123"))
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andExpect(jsonPath("$.players").isArray());
    }

    @Test
    void joinRoomReturns201WithPlayerAndRoom() throws Exception {
        Player p = new Player();
        p.setDisplayName("Ada");
        when(roomService.joinRoom(eq("ABC123"), eq("Ada"))).thenReturn(p);
        when(roomService.getRoom("ABC123")).thenReturn(room("ABC123", RoomStatus.WAITING));

        mockMvc.perform(post("/api/rooms/ABC123/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Ada\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.you.displayName").value("Ada"))
                .andExpect(jsonPath("$.room.code").value("ABC123"));
    }

    @Test
    void joinRoomBlankNameReturns400() throws Exception {
        mockMvc.perform(post("/api/rooms/ABC123/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMissingRoomReturns404() throws Exception {
        when(roomService.getRoom("NOPE12")).thenThrow(new RoomException.RoomNotFound("NOPE12"));

        mockMvc.perform(get("/api/rooms/NOPE12"))
                .andExpect(status().isNotFound());
    }

    @Test
    void joinFullRoomReturns409() throws Exception {
        when(roomService.joinRoom(any(), any()))
                .thenThrow(new RoomException.RoomNotJoinable("Room ABC123 is full"));

        mockMvc.perform(post("/api/rooms/ABC123/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Grace\"}"))
                .andExpect(status().isConflict());
    }
}
