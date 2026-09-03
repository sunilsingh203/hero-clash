package com.herobattle.battle;

import com.herobattle.game.GameCard;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.springframework.stereotype.Component;

/**
 * Pure rules engine for "Battle Mode v2". Stateless: every method takes a {@link BattleState},
 * mutates it in place, and returns.
 *
 * <p>Rules: each player is dealt one random champion. HP = durability + strength. Turn order
 * is fixed by initiative (power + combat + speed, descending; seating order breaks ties). On
 * your turn you attack one living opponent for {@code max(1, attackPower - target.mitigation)}.
 * The match ends when one champion is left standing, or the round cap is hit (most HP wins).
 */
@Component
public class BattleEngine {

    private static final int MIN_PLAYERS = 2;
    private static final int MAX_PLAYERS = 4;
    private static final int MIN_DAMAGE = 1;

    private final Random random;

    public BattleEngine() {
        this(new Random());
    }

    /** Test seam: inject a seeded {@link Random} for deterministic champion deals. */
    public BattleEngine(Random random) {
        this.random = random;
    }

    public BattleState startBattle(String roomCode, List<String> playerIds,
                                   List<GameCard> pool, int roundCap) {
        if (playerIds.size() < MIN_PLAYERS || playerIds.size() > MAX_PLAYERS) {
            throw new BattleException("Battle Mode needs 2–4 players, got " + playerIds.size());
        }
        if (pool.size() < playerIds.size()) {
            throw new BattleException("Not enough cards to deal a champion to every player");
        }

        List<GameCard> shuffled = new ArrayList<>(pool);
        java.util.Collections.shuffle(shuffled, random);

        BattleState s = new BattleState();
        s.setRoomCode(roomCode);
        s.setRoundCap(roundCap);

        Map<String, Combatant> combatants = new LinkedHashMap<>();
        for (int i = 0; i < playerIds.size(); i++) {
            String id = playerIds.get(i);
            combatants.put(id, new Combatant(id, shuffled.get(i)));
        }
        s.setCombatants(combatants);

        // initiative order; seating order (index in playerIds) breaks ties, deterministically
        List<String> order = new ArrayList<>(playerIds);
        order.sort(Comparator
                .comparingInt((String id) -> combatants.get(id).initiative()).reversed()
                .thenComparingInt(playerIds::indexOf));
        s.setTurnOrder(order);
        s.setTurnIndex(0);
        return s;
    }

    public AttackResult attack(BattleState s, String attackerId, String targetId) {
        if (s.getPhase() != BattleState.Phase.RUNNING) {
            throw new BattleException("Match is not running");
        }
        if (!attackerId.equals(s.activePlayerId())) {
            throw new BattleException("It is not " + attackerId + "'s turn");
        }
        if (attackerId.equals(targetId)) {
            throw new BattleException("You cannot attack yourself");
        }
        Combatant attacker = s.combatantOf(attackerId);
        Combatant target = s.combatantOf(targetId);
        if (target == null) {
            throw new BattleException("Unknown target " + targetId);
        }
        if (!target.alive() || s.getEliminated().contains(targetId)) {
            throw new BattleException("Target " + targetId + " is already down");
        }

        int raw = Math.max(MIN_DAMAGE, attacker.attackPower() - target.mitigation());
        int dealt = target.takeHit(raw);

        boolean targetDown = !target.alive();
        if (targetDown) {
            s.getEliminated().add(targetId);
        }

        List<String> alive = s.alivePlayers();
        boolean matchOver = false;
        String matchWinner = null;
        if (alive.size() <= 1) {
            matchOver = true;
            matchWinner = alive.isEmpty() ? attackerId : alive.get(0);
        } else if (s.getRoundNumber() >= s.getRoundCap()) {
            // this attack completes the capped round → decide on remaining HP
            matchOver = true;
            matchWinner = alive.stream()
                    .max(Comparator.<String>comparingInt(id -> s.combatantOf(id).getCurrentHp())
                            .thenComparing(id -> -s.getTurnOrder().indexOf(id)))
                    .orElse(attackerId);
        }

        AttackResult result = new AttackResult(attackerId, targetId, dealt,
                target.getCurrentHp(), targetDown, matchOver, matchWinner);
        s.setLastAttack(result);

        if (matchOver) {
            s.setPhase(BattleState.Phase.FINISHED);
            s.setMatchWinnerId(matchWinner);
        } else {
            s.advanceTurn();
        }
        return result;
    }
}
