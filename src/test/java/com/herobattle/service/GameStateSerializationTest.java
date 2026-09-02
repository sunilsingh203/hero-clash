package com.herobattle.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.herobattle.game.GameCard;
import com.herobattle.game.GameEngine;
import com.herobattle.game.GameState;
import com.herobattle.game.Round;
import com.herobattle.game.Stat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;

/**
 * The Redis store round-trips {@link GameState} as JSON. This guards that every field the
 * engine relies on — decks, round phase, chosen stat, showdown plays — survives that trip.
 */
class GameStateSerializationTest {

    private final Jackson2JsonRedisSerializer<GameState> serializer =
            new Jackson2JsonRedisSerializer<>(new ObjectMapper(), GameState.class);

    private static GameCard card(long id, int v) {
        return new GameCard(id, "C" + id, null, v, v, v, v, v, v);
    }

    private GameState roundTrip(GameState state) {
        return serializer.deserialize(serializer.serialize(state));
    }

    @Test
    void freshMatchSurvivesRoundTrip() {
        List<GameCard> pool = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            pool.add(card(i, i));
        }
        GameState original = new GameEngine(new Random(1))
                .startMatch("ROOM42", List.of("p1", "p2"), pool, 25);

        GameState copy = roundTrip(original);

        assertThat(copy.getRoomCode()).isEqualTo("ROOM42");
        assertThat(copy.getPlayerOrder()).containsExactly("p1", "p2");
        assertThat(copy.getActivePlayerId()).isEqualTo("p1");
        assertThat(copy.deckSize("p1")).isEqualTo(original.deckSize("p1"));
        assertThat(copy.deckSize("p2")).isEqualTo(original.deckSize("p2"));
        assertThat(copy.getCurrentRound().getPhase()).isEqualTo(Round.Phase.PICKING);
        assertThat(copy.deckOf("p1").peekFirst().id())
                .isEqualTo(original.deckOf("p1").peekFirst().id());
    }

    @Test
    void midRevealStateSurvivesRoundTrip() {
        List<GameCard> pool = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            pool.add(card(i, i));
        }
        GameEngine engine = new GameEngine(new Random(7));
        GameState state = engine.startMatch("ROOM43", List.of("p1", "p2"), pool, 25);
        engine.pickCategory(state, "p1", Stat.SPEED);
        engine.submitReveal(state, "p1");

        GameState copy = roundTrip(state);

        assertThat(copy.getCurrentRound().getPhase()).isEqualTo(Round.Phase.REVEALING);
        assertThat(copy.getCurrentRound().getStat()).isEqualTo(Stat.SPEED);
        assertThat(copy.getCurrentRound().getResponded()).containsExactly("p1");
        assertThat(copy.getCurrentRound().getContenders()).containsExactly("p1", "p2");
        assertThat(copy.getCurrentRound().getPlays().get("p1")).hasSize(1);

        // the deserialized state must still be a legal engine input
        assertThat(engine.submitReveal(copy, "p2").roundResolved()).isTrue();
    }
}
