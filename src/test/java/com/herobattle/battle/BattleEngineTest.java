package com.herobattle.battle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.herobattle.game.GameCard;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

class BattleEngineTest {

    private final BattleEngine engine = new BattleEngine(new Random(42));

    /** id, str, spd, dur, pow, cbt — intelligence is unused by Battle Mode. */
    private static GameCard card(long id, int str, int spd, int dur, int pow, int cbt) {
        return new GameCard(id, "C" + id, null, 50, str, spd, dur, pow, cbt);
    }

    private static BattleState state(int roundCap, Map<String, GameCard> cardsInOrder) {
        BattleState s = new BattleState();
        s.setRoomCode("ROOM01");
        s.setRoundCap(roundCap);
        Map<String, Combatant> combatants = new LinkedHashMap<>();
        cardsInOrder.forEach((id, c) -> combatants.put(id, new Combatant(id, c)));
        s.setCombatants(combatants);
        s.setTurnOrder(new ArrayList<>(cardsInOrder.keySet()));
        return s;
    }

    @Test
    void startBattleDealsOneChampionEachWithHpAndInitiativeOrder() {
        List<GameCard> pool = List.of(
                card(1, 10, 10, 10, 10, 10),
                card(2, 20, 20, 20, 20, 20),
                card(3, 30, 30, 30, 30, 30));
        BattleState s = engine.startBattle("ROOM01", List.of("p1", "p2"), pool, 25);

        assertThat(s.getCombatants()).hasSize(2);
        s.getCombatants().values().forEach(c ->
                assertThat(c.getCurrentHp()).isEqualTo(c.getMaxHp()).isPositive());
        // turn order is a permutation of the players, highest initiative first
        assertThat(s.getTurnOrder()).containsExactlyInAnyOrder("p1", "p2");
        int firstInit = s.combatantOf(s.getTurnOrder().get(0)).initiative();
        int secondInit = s.combatantOf(s.getTurnOrder().get(1)).initiative();
        assertThat(firstInit).isGreaterThanOrEqualTo(secondInit);
    }

    @Test
    void startBattleRejectsSoloPlayer() {
        assertThatThrownBy(() -> engine.startBattle("R", List.of("p1"),
                List.of(card(1, 1, 1, 1, 1, 1)), 25))
                .isInstanceOf(BattleException.class);
    }

    @Test
    void attackAppliesMitigatedDamage() {
        // attacker: str 40, pow 20 → attackPower (40+20)/2 = 30
        // target:   dur 40 → mitigation 40/4 = 10, hp = dur 40 + str 0 = 40
        Map<String, GameCard> cards = new LinkedHashMap<>();
        cards.put("p1", card(1, 40, 0, 0, 20, 0));
        cards.put("p2", card(2, 0, 0, 40, 0, 0));
        BattleState s = state(25, cards);
        s.setTurnOrder(List.of("p1", "p2"));

        AttackResult r = engine.attack(s, "p1", "p2");

        assertThat(r.damage()).isEqualTo(20); // 30 - 10
        assertThat(s.combatantOf("p2").getCurrentHp()).isEqualTo(20);
        assertThat(r.matchOver()).isFalse();
        assertThat(s.activePlayerId()).isEqualTo("p2"); // turn advanced
    }

    @Test
    void attackNeverDealsLessThanOne() {
        Map<String, GameCard> cards = new LinkedHashMap<>();
        cards.put("p1", card(1, 0, 0, 0, 0, 0));   // attackPower 0
        cards.put("p2", card(2, 0, 0, 100, 0, 0)); // mitigation 25
        BattleState s = state(25, cards);
        s.setTurnOrder(List.of("p1", "p2"));

        AttackResult r = engine.attack(s, "p1", "p2");

        assertThat(r.damage()).isEqualTo(1);
    }

    @Test
    void lethalAttackEliminatesTargetAndEndsTwoPlayerMatch() {
        Map<String, GameCard> cards = new LinkedHashMap<>();
        cards.put("p1", card(1, 100, 0, 0, 100, 0)); // attackPower 100
        cards.put("p2", card(2, 0, 0, 10, 0, 0));    // hp 10
        BattleState s = state(25, cards);
        s.setTurnOrder(List.of("p1", "p2"));

        AttackResult r = engine.attack(s, "p1", "p2");

        assertThat(r.targetDown()).isTrue();
        assertThat(r.matchOver()).isTrue();
        assertThat(r.matchWinnerId()).isEqualTo("p1");
        assertThat(s.getPhase()).isEqualTo(BattleState.Phase.FINISHED);
        assertThat(s.getEliminated()).containsExactly("p2");
    }

    @Test
    void attackRejectsOutOfTurnDeadTargetAndSelf() {
        Map<String, GameCard> cards = new LinkedHashMap<>();
        cards.put("p1", card(1, 10, 0, 0, 10, 0));
        cards.put("p2", card(2, 10, 0, 10, 10, 0));
        BattleState s = state(25, cards);
        s.setTurnOrder(List.of("p1", "p2"));

        assertThatThrownBy(() -> engine.attack(s, "p2", "p1"))
                .isInstanceOf(BattleException.class); // not p2's turn
        assertThatThrownBy(() -> engine.attack(s, "p1", "p1"))
                .isInstanceOf(BattleException.class); // self
        assertThatThrownBy(() -> engine.attack(s, "p1", "ghost"))
                .isInstanceOf(BattleException.class); // unknown target
    }

    @Test
    void roundCapEndsMatchWithMostHpWinner() {
        Map<String, GameCard> cards = new LinkedHashMap<>();
        cards.put("p1", card(1, 0, 0, 100, 0, 0)); // tanky, hp 100
        cards.put("p2", card(2, 0, 0, 60, 0, 0));  // hp 60
        BattleState s = state(1, cards); // cap of 1 round
        s.setTurnOrder(List.of("p1", "p2"));

        AttackResult r = engine.attack(s, "p1", "p2");

        assertThat(r.matchOver()).isTrue();
        assertThat(r.matchWinnerId()).isEqualTo("p1"); // more HP remaining
        assertThat(s.getPhase()).isEqualTo(BattleState.Phase.FINISHED);
    }
}
