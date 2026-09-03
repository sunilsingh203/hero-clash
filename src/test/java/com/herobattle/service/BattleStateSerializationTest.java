package com.herobattle.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.herobattle.battle.BattleEngine;
import com.herobattle.battle.BattleState;
import com.herobattle.game.GameCard;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;

/**
 * The Redis store round-trips {@link BattleState} as JSON between every attack. This guards
 * that champions, HP, turn order, and the last-attack summary survive that trip and that the
 * deserialized state is still a legal engine input.
 */
class BattleStateSerializationTest {

    private final Jackson2JsonRedisSerializer<BattleState> serializer =
            new Jackson2JsonRedisSerializer<>(new ObjectMapper(), BattleState.class);

    private static GameCard card(long id, int v) {
        return new GameCard(id, "C" + id, null, v, v, v, v, v, v);
    }

    private BattleState roundTrip(BattleState state) {
        return serializer.deserialize(serializer.serialize(state));
    }

    @Test
    void freshBattleSurvivesRoundTrip() {
        List<GameCard> pool = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            pool.add(card(i, i * 10));
        }
        BattleState original = new BattleEngine(new Random(1))
                .startBattle("ROOM42", List.of("p1", "p2"), pool, 25);

        BattleState copy = roundTrip(original);

        assertThat(copy.getRoomCode()).isEqualTo("ROOM42");
        assertThat(copy.getTurnOrder()).containsExactlyInAnyOrder("p1", "p2");
        assertThat(copy.getCombatants().keySet()).containsExactlyInAnyOrder("p1", "p2");
        assertThat(copy.combatantOf("p1").getMaxHp())
                .isEqualTo(original.combatantOf("p1").getMaxHp());
        assertThat(copy.combatantOf("p1").getCard().id())
                .isEqualTo(original.combatantOf("p1").getCard().id());
    }

    @Test
    void midBattleStateSurvivesRoundTripAndStaysPlayable() {
        BattleEngine engine = new BattleEngine(new Random(7));
        List<GameCard> pool = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            pool.add(card(i, 40));
        }
        BattleState state = engine.startBattle("ROOM43", List.of("p1", "p2", "p3"), pool, 25);
        String attacker = state.activePlayerId();
        String target = state.alivePlayers().stream().filter(id -> !id.equals(attacker))
                .findFirst().orElseThrow();
        engine.attack(state, attacker, target);

        BattleState copy = roundTrip(state);

        assertThat(copy.getLastAttack()).isNotNull();
        assertThat(copy.getLastAttack().attackerId()).isEqualTo(attacker);
        assertThat(copy.combatantOf(target).getCurrentHp())
                .isEqualTo(state.combatantOf(target).getCurrentHp());

        // still a legal engine input
        String nextAttacker = copy.activePlayerId();
        String nextTarget = copy.alivePlayers().stream().filter(id -> !id.equals(nextAttacker))
                .findFirst().orElseThrow();
        assertThat(engine.attack(copy, nextAttacker, nextTarget)).isNotNull();
    }
}
