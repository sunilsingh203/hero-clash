package com.herobattle.controller;

import com.herobattle.battle.BattleState;
import com.herobattle.controller.dto.BattleDtos.AttackMessage;
import com.herobattle.controller.dto.BattleDtos.BattleView;
import com.herobattle.controller.dto.GameDtos.GameError;
import com.herobattle.service.BattleService;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * STOMP entry points for Battle Mode v2. Clients send to {@code /app/rooms/{code}/battle/…};
 * every action broadcasts a fresh {@link BattleView} to {@code /topic/rooms.{code}} (the same
 * topic classic mode uses — clients switch on the view's {@code kind}), or a
 * {@link GameError} to {@code /topic/rooms.{code}.errors} on an illegal move.
 */
@Controller
public class BattleSocketController {

    private static final Logger log = LoggerFactory.getLogger(BattleSocketController.class);

    private final BattleService battleService;
    private final SimpMessagingTemplate broker;

    public BattleSocketController(BattleService battleService, SimpMessagingTemplate broker) {
        this.battleService = battleService;
        this.broker = broker;
    }

    @MessageMapping("/rooms/{code}/battle/start")
    public void start(@DestinationVariable String code) {
        dispatch(code, () -> battleService.startBattle(code));
    }

    @MessageMapping("/rooms/{code}/battle/attack")
    public void attack(@DestinationVariable String code, AttackMessage msg) {
        dispatch(code, () -> battleService.attack(code, msg.playerId(), msg.targetId()));
    }

    private void dispatch(String code, Supplier<BattleState> action) {
        try {
            BattleState state = action.get();
            broker.convertAndSend("/topic/rooms." + state.getRoomCode(),
                    BattleView.from(state, battleService.playerNames(state.getRoomCode())));
        } catch (RuntimeException e) {
            log.debug("Rejected battle action on room {}: {}", code, e.getMessage());
            broker.convertAndSend("/topic/rooms." + code.toUpperCase() + ".errors",
                    new GameError(e.getMessage()));
        }
    }
}
