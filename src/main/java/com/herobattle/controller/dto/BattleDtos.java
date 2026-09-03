package com.herobattle.controller.dto;

import com.herobattle.battle.AttackResult;
import com.herobattle.battle.BattleState;
import com.herobattle.battle.Combatant;
import com.herobattle.game.GameCard;
import java.util.List;
import java.util.Map;

/** Payloads for Battle Mode v2 (STOMP broadcasts + REST reads). */
public final class BattleDtos {

    private BattleDtos() {
    }

    /** STOMP action: the active player attacks a target. */
    public record AttackMessage(String playerId, String targetId) {
    }

    public record CombatantView(
            String playerId,
            String displayName,
            GameCard card,
            int maxHp,
            int currentHp,
            int initiative,
            boolean eliminated) {

        static CombatantView from(Combatant c, String displayName, boolean eliminated) {
            return new CombatantView(
                    c.getPlayerId(), displayName, c.getCard(),
                    c.getMaxHp(), c.getCurrentHp(), c.initiative(), eliminated);
        }
    }

    public record AttackView(
            String attackerId,
            String targetId,
            int damage,
            int targetHpLeft,
            boolean targetDown,
            boolean matchOver,
            String matchWinnerId) {

        static AttackView from(AttackResult r) {
            if (r == null) {
                return null;
            }
            return new AttackView(r.attackerId(), r.targetId(), r.damage(), r.targetHpLeft(),
                    r.targetDown(), r.matchOver(), r.matchWinnerId());
        }
    }

    /** Everything a player/spectator may see about a Battle Mode match. */
    public record BattleView(
            String kind,
            String roomCode,
            String phase,
            int roundNumber,
            int roundCap,
            String activePlayerId,
            String matchWinnerId,
            List<CombatantView> combatants,
            AttackView lastAttack) {

        public static BattleView from(BattleState s, Map<String, String> names) {
            List<CombatantView> combatants = s.getCombatants().values().stream()
                    .map(c -> CombatantView.from(
                            c,
                            names.getOrDefault(c.getPlayerId(), c.getPlayerId()),
                            s.getEliminated().contains(c.getPlayerId())))
                    .toList();
            return new BattleView(
                    "BATTLE",
                    s.getRoomCode(),
                    s.getPhase().name(),
                    s.getRoundNumber(),
                    s.getRoundCap(),
                    s.activePlayerId(),
                    s.getMatchWinnerId(),
                    combatants,
                    AttackView.from(s.getLastAttack()));
        }
    }
}
