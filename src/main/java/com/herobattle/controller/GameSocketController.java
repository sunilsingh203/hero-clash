package com.herobattle.controller;

import com.herobattle.controller.dto.GameDtos.GameError;
import com.herobattle.controller.dto.GameDtos.GameView;
import com.herobattle.controller.dto.GameDtos.PickMessage;
import com.herobattle.controller.dto.GameDtos.RevealMessage;
import com.herobattle.game.Stat;
import com.herobattle.service.MatchService;
import com.herobattle.service.MatchUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * STOMP entry points for gameplay. Clients send to {@code /app/rooms/{code}/…}; every action
 * results in a fresh {@link GameView} broadcast to {@code /topic/rooms.{code}} (or a
 * {@link GameError} to {@code /topic/rooms.{code}.errors} on an illegal move).
 *
 * <p>Broadcast topics use {@code .} separators, not {@code /} — RabbitMQ's STOMP plugin
 * (used by the broker relay) rejects topic names containing a slash.
 */
@Controller
public class GameSocketController {

    private static final Logger log = LoggerFactory.getLogger(GameSocketController.class);

    private final MatchService matchService;
    private final SimpMessagingTemplate broker;

    public GameSocketController(MatchService matchService, SimpMessagingTemplate broker) {
        this.matchService = matchService;
        this.broker = broker;
    }

    @MessageMapping("/rooms/{code}/start")
    public void start(@DestinationVariable String code) {
        dispatch(code, () -> matchService.startMatch(code));
    }

    @MessageMapping("/rooms/{code}/pick")
    public void pick(@DestinationVariable String code, PickMessage msg) {
        dispatch(code, () -> matchService.pick(code, msg.playerId(),
                Stat.valueOf(msg.stat().toUpperCase())));
    }

    @MessageMapping("/rooms/{code}/reveal")
    public void reveal(@DestinationVariable String code, RevealMessage msg) {
        dispatch(code, () -> matchService.reveal(code, msg.playerId()));
    }

    private void dispatch(String code, java.util.function.Supplier<MatchUpdate> action) {
        try {
            MatchUpdate update = action.get();
            String room = update.state().getRoomCode();
            broker.convertAndSend("/topic/rooms." + room,
                    GameView.from(update.state(), matchService.playerNames(room), update.result()));
        } catch (RuntimeException e) {
            log.debug("Rejected game action on room {}: {}", code, e.getMessage());
            broker.convertAndSend("/topic/rooms." + code.toUpperCase() + ".errors",
                    new GameError(e.getMessage()));
        }
    }
}
