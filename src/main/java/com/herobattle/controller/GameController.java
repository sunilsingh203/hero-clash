package com.herobattle.controller;

import com.herobattle.battle.BattleState;
import com.herobattle.controller.dto.BattleDtos.BattleView;
import com.herobattle.controller.dto.GameDtos.GameView;
import com.herobattle.controller.dto.GameDtos.HandView;
import com.herobattle.game.GameState;
import com.herobattle.service.BattleService;
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
    private final BattleService battleService;

    public GameController(MatchService matchService, BattleService battleService) {
        this.matchService = matchService;
        this.battleService = battleService;
    }

    @GetMapping("/game")
    public GameView game(@PathVariable String code) {
        GameState state = matchService.current(code);
        return GameView.from(state, matchService.playerNames(code));
    }

    @GetMapping("/battle")
    public BattleView battle(@PathVariable String code) {
        BattleState state = battleService.current(code);
        return BattleView.from(state, battleService.playerNames(code));
    }

    @GetMapping("/players/{playerId}/hand")
    public HandView hand(@PathVariable String code, @PathVariable String playerId) {
        return HandView.from(matchService.current(code), playerId);
    }
}
