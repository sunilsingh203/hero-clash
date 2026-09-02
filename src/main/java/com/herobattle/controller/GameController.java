package com.herobattle.controller;

import com.herobattle.controller.dto.GameDtos.GameView;
import com.herobattle.controller.dto.GameDtos.HandView;
import com.herobattle.game.GameState;
import com.herobattle.service.MatchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only views of a live match. Gameplay actions go over STOMP
 * ({@link GameSocketController}); this is for the initial load and for a player polling
 * their own hand.
 */
@RestController
@RequestMapping("/api/rooms/{code}")
public class GameController {

    private final MatchService matchService;

    public GameController(MatchService matchService) {
        this.matchService = matchService;
    }

    @GetMapping("/game")
    public GameView game(@PathVariable String code) {
        GameState state = matchService.current(code);
        return GameView.from(state, matchService.playerNames(code));
    }

    @GetMapping("/players/{playerId}/hand")
    public HandView hand(@PathVariable String code, @PathVariable String playerId) {
        return HandView.from(matchService.current(code), playerId);
    }
}
