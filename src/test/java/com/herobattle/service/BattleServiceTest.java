package com.herobattle.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.herobattle.battle.BattleEngine;
import com.herobattle.battle.BattleException;
import com.herobattle.battle.BattleState;
import com.herobattle.model.Card;
import com.herobattle.model.GameMode;
import com.herobattle.model.Player;
import com.herobattle.model.Room;
import com.herobattle.model.RoomStatus;
import com.herobattle.repository.CardRepository;
import com.herobattle.repository.RoomRepository;
import com.herobattle.support.InMemoryBattleStateRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BattleServiceTest {

    @Mock RoomRepository roomRepository;
    @Mock CardRepository cardRepository;

    private final InMemoryBattleStateRepository battleStore = new InMemoryBattleStateRepository();
    private final BattleEngine engine = new BattleEngine(new Random(1));

    private BattleService service() {
        return new BattleService(roomRepository, cardRepository, battleStore, engine, 25);
    }

    private static Room room(RoomStatus status, GameMode mode, int players) {
        Room r = new Room();
        r.setCode("ABCDEF");
        r.setStatus(status);
        r.setMode(mode);
        for (int i = 1; i <= players; i++) {
            Player p = new Player();
            p.setId((long) i);
            p.setDisplayName("P" + i);
            r.addPlayer(p);
        }
        return r;
    }

    private static List<Card> pool(int n) {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Card c = new Card();
            c.setId((long) i);
            c.setName("C" + i);
            c.setStrength(40);
            c.setSpeed(40);
            c.setIntelligence(40);
            c.setDurability(40);
            c.setPower(40);
            c.setCombat(40 + i); // vary initiative so turn order is well-defined
            cards.add(c);
        }
        return cards;
    }

    @Test
    void startBattleRejectsClassicRoom() {
        when(roomRepository.findByCode("ABCDEF"))
                .thenReturn(Optional.of(room(RoomStatus.WAITING, GameMode.CLASSIC, 2)));
        assertThatThrownBy(() -> service().startBattle("abcdef"))
                .isInstanceOf(BattleException.class);
    }

    @Test
    void startBattleDealsChampionsAndFlipsRoomInProgress() {
        Room room = room(RoomStatus.WAITING, GameMode.BATTLE, 2);
        when(roomRepository.findByCode("ABCDEF")).thenReturn(Optional.of(room));
        when(cardRepository.findAll()).thenReturn(pool(10));

        BattleState state = service().startBattle("abcdef");

        assertThat(state.getCombatants()).containsOnlyKeys("1", "2");
        assertThat(state.getTurnOrder()).containsExactlyInAnyOrder("1", "2");
        assertThat(room.getStatus()).isEqualTo(RoomStatus.IN_PROGRESS);
        verify(roomRepository).save(room);
    }

    @Test
    void attackPersistsAndMarksRoomFinishedOnKill() {
        // start a real match, then hand-wound a target down to 1 HP in the store
        Room room = room(RoomStatus.WAITING, GameMode.BATTLE, 2);
        when(roomRepository.findByCode("ABCDEF")).thenReturn(Optional.of(room));
        when(cardRepository.findAll()).thenReturn(pool(10));
        BattleState state = service().startBattle("abcdef");
        String attacker = state.activePlayerId();
        String target = state.getTurnOrder().stream().filter(id -> !id.equals(attacker))
                .findFirst().orElseThrow();
        state.combatantOf(target).setCurrentHp(1);
        battleStore.save(state);

        BattleState after = service().attack("abcdef", attacker, target);

        assertThat(after.getPhase()).isEqualTo(BattleState.Phase.FINISHED);
        assertThat(after.getMatchWinnerId()).isEqualTo(attacker);
        ArgumentCaptor<Room> saved = ArgumentCaptor.forClass(Room.class);
        verify(roomRepository, org.mockito.Mockito.atLeastOnce()).save(saved.capture());
        assertThat(saved.getAllValues()).anyMatch(r -> r.getStatus() == RoomStatus.FINISHED);
    }
}
