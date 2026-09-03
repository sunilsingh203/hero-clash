package com.herobattle.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

class GameEngineTest {

    private final GameEngine engine = new GameEngine(new Random(42));

    /** Card whose every stat equals {@code v} except STRENGTH which is {@code strength}. */
    private static GameCard card(long id, int strength, int other) {
        return new GameCard(id, "C" + id, null, other, strength, other, other, other, other);
    }

    private GameState twoPlayerState(Deque<GameCard> p1Deck, Deque<GameCard> p2Deck) {
        GameState s = new GameState();
        s.setRoomCode("ROOM01");
        s.setRoundCap(25);
        s.setPlayerOrder(List.of("p1", "p2"));
        s.setActivePlayerId("p1");
        s.getDecks().put("p1", p1Deck);
        s.getDecks().put("p2", p2Deck);
        return s;
    }

    private static Deque<GameCard> deck(GameCard... cards) {
        return new ArrayDeque<>(List.of(cards));
    }

    @Test
    void startMatchDealsEqualHands() {
        List<GameCard> pool = new java.util.ArrayList<>();
        for (int i = 0; i < 11; i++) {
            pool.add(card(i, i, i));
        }
        GameState s = engine.startMatch("ROOM01", List.of("p1", "p2"), pool, 25);

        assertThat(s.deckSize("p1")).isEqualTo(5);
        assertThat(s.deckSize("p2")).isEqualTo(5); // 11th card trimmed to keep hands equal
        assertThat(s.getActivePlayerId()).isEqualTo("p1");
        assertThat(s.getCurrentRound().getPhase()).isEqualTo(Round.Phase.PICKING);
    }

    @Test
    void startMatchRejectsSoloPlayer() {
        assertThatThrownBy(() -> engine.startMatch("R", List.of("p1"), List.of(card(1, 1, 1)), 25))
                .isInstanceOf(GameException.class);
    }

    @Test
    void higherStatTakesThePot() {
        GameState s = twoPlayerState(
                deck(card(1, 90, 10), card(2, 5, 5)),
                deck(card(3, 40, 10), card(4, 5, 5)));

        engine.pickCategory(s, "p1", Stat.STRENGTH);
        assertThat(engine.submitReveal(s, "p1").roundResolved()).isFalse();
        TurnResult result = engine.submitReveal(s, "p2");

        assertThat(result.roundResolved()).isTrue();
        assertThat(result.roundWinnerId()).isEqualTo("p1");
        assertThat(result.revealValues()).containsEntry("p1", 90).containsEntry("p2", 40);
        assertThat(s.deckSize("p1")).isEqualTo(3); // 1 left + 2 from pot
        assertThat(s.deckSize("p2")).isEqualTo(1);
        assertThat(s.getActivePlayerId()).isEqualTo("p1");
        assertThat(s.getRoundNumber()).isEqualTo(2);
    }

    @Test
    void onlyActivePlayerMayPick() {
        GameState s = twoPlayerState(deck(card(1, 1, 1)), deck(card(2, 2, 2)));
        assertThatThrownBy(() -> engine.pickCategory(s, "p2", Stat.STRENGTH))
                .isInstanceOf(GameException.class);
    }

    @Test
    void cannotRevealBeforePick() {
        GameState s = twoPlayerState(deck(card(1, 1, 1)), deck(card(2, 2, 2)));
        assertThatThrownBy(() -> engine.submitReveal(s, "p1"))
                .isInstanceOf(GameException.class);
    }

    @Test
    void tieGoesToShowdownThenResolves() {
        GameState s = twoPlayerState(
                deck(card(1, 50, 0), card(2, 99, 0), card(9, 1, 1)),
                deck(card(3, 50, 0), card(4, 20, 0), card(10, 1, 1)));

        engine.pickCategory(s, "p1", Stat.STRENGTH);
        engine.submitReveal(s, "p1");
        TurnResult tie = engine.submitReveal(s, "p2");
        assertThat(tie.showdown()).isTrue();
        assertThat(tie.roundResolved()).isFalse();

        engine.submitReveal(s, "p1");
        TurnResult done = engine.submitReveal(s, "p2");
        assertThat(done.roundResolved()).isTrue();
        assertThat(done.roundWinnerId()).isEqualTo("p1"); // 99 > 20 on the showdown flip
        assertThat(s.deckSize("p1")).isEqualTo(5); // 1 left + 4 in pot
        assertThat(s.deckSize("p2")).isEqualTo(1);
    }

    @Test
    void runningOutOfCardsEndsTheMatch() {
        GameState s = twoPlayerState(
                deck(card(1, 90, 0)),
                deck(card(3, 10, 0)));

        engine.pickCategory(s, "p1", Stat.STRENGTH);
        engine.submitReveal(s, "p1");
        TurnResult result = engine.submitReveal(s, "p2");

        assertThat(result.matchOver()).isTrue();
        assertThat(result.matchWinnerId()).isEqualTo("p1");
        assertThat(result.eliminated()).contains("p2");
        assertThat(s.getPhase()).isEqualTo(GameState.Phase.FINISHED);
    }

    @Test
    void roundCapEndsMatchWithMostCardsWinning() {
        GameState s = twoPlayerState(
                deck(card(1, 90, 0), card(2, 90, 0), card(5, 1, 1)),
                deck(card(3, 10, 0), card(4, 10, 0)));
        s.setRoundCap(1);

        engine.pickCategory(s, "p1", Stat.STRENGTH);
        engine.submitReveal(s, "p1");
        TurnResult result = engine.submitReveal(s, "p2");

        assertThat(result.matchOver()).isTrue();
        assertThat(result.matchWinnerId()).isEqualTo("p1");
        assertThat(s.getPhase()).isEqualTo(GameState.Phase.FINISHED);
    }

    @Test
    void nonContenderCannotReveal() {
        GameState s = new GameState();
        s.setPlayerOrder(List.of("p1", "p2", "p3"));
        s.setActivePlayerId("p1");
        s.getDecks().put("p1", deck(card(1, 5, 5)));
        s.getDecks().put("p2", deck(card(2, 6, 6)));
        s.getDecks().put("p3", deck(card(3, 7, 7)));
        s.getEliminated().add("p3");

        engine.pickCategory(s, "p1", Stat.STRENGTH);
        assertThatThrownBy(() -> engine.submitReveal(s, "p3"))
                .isInstanceOf(GameException.class);
    }
}
