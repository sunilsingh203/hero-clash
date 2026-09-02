package com.herobattle.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.herobattle.game.GameCard;
import com.herobattle.game.GameEngine;
import com.herobattle.game.GameException;
import com.herobattle.game.GameState;
import com.herobattle.game.Stat;
import com.herobattle.model.Card;
import com.herobattle.model.Player;
import com.herobattle.model.Room;
import com.herobattle.model.RoomStatus;
import com.herobattle.repository.CardRepository;
import com.herobattle.repository.RoomRepository;
import java.util.ArrayDeque;
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
class MatchServiceTest {

    @Mock RoomRepository roomRepository;
    @Mock CardRepository cardRepository;
    @Mock MatchStateRepository matchStore;

    private final GameEngine engine = new GameEngine(new Random(1));
    private MatchService service() {
        return new MatchService(roomRepository, cardRepository, matchStore, engine, 25);
    }

    private static Room room(RoomStatus status, int players) {
        Room r = new Room();
        r.setCode("ABCDEF");
        r.setStatus(status);
        for (int i = 1; i <= players; i++) {
            Player p = new Player();
            p.setId((long) i);
            p.setDisplayName("P" + i);
            r.addPlayer(p);
        }
        return r;
    }

    private static Card card(long id) {
        Card c = new Card();
        c.setId(id);
        c.setName("C" + id);
        c.setStrength(50);
        c.setSpeed(50);
        c.setIntelligence(50);
        c.setDurability(50);
        c.setPower(50);
        c.setCombat(50);
        return c;
    }

    private static List<Card> pool(int n) {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            cards.add(card(i));
        }
        return cards;
    }

    @Test
    void startMatchRejectsRoomThatAlreadyStarted() {
        when(roomRepository.findByCode("ABCDEF")).thenReturn(Optional.of(room(RoomStatus.IN_PROGRESS, 2)));
        assertThatThrownBy(() -> service().startMatch("abcdef"))
                .isInstanceOf(RoomException.RoomNotJoinable.class);
    }

    @Test
    void startMatchRejectsSinglePlayer() {
        when(roomRepository.findByCode("ABCDEF")).thenReturn(Optional.of(room(RoomStatus.WAITING, 1)));
        assertThatThrownBy(() -> service().startMatch("abcdef")).isInstanceOf(GameException.class);
    }

    @Test
    void startMatchRejectsEmptyCardPool() {
        when(roomRepository.findByCode("ABCDEF")).thenReturn(Optional.of(room(RoomStatus.WAITING, 2)));
        when(cardRepository.findAll()).thenReturn(List.of());
        assertThatThrownBy(() -> service().startMatch("abcdef")).isInstanceOf(GameException.class);
    }

    @Test
    void startMatchDealsPersistsAndFlipsRoomToInProgress() {
        Room room = room(RoomStatus.WAITING, 2);
        when(roomRepository.findByCode("ABCDEF")).thenReturn(Optional.of(room));
        when(cardRepository.findAll()).thenReturn(pool(20));

        GameState state = service().startMatch("abcdef").state();

        assertThat(state.getPlayerOrder()).containsExactly("1", "2");
        assertThat(state.deckSize("1")).isEqualTo(10);
        assertThat(room.getStatus()).isEqualTo(RoomStatus.IN_PROGRESS);
        verify(roomRepository).save(room);
        verify(matchStore).save(state);
    }

    @Test
    void revealThatEndsMatchMarksRoomFinished() {
        GameState s = new GameState();
        s.setRoomCode("ABCDEF");
        s.setPlayerOrder(List.of("1", "2"));
        s.setActivePlayerId("1");
        s.getDecks().put("1", new ArrayDeque<>(List.of(
                new GameCard(1, "A", null, 0, 90, 0, 0, 0, 0))));
        s.getDecks().put("2", new ArrayDeque<>(List.of(
                new GameCard(2, "B", null, 0, 10, 0, 0, 0, 0))));
        engine.pickCategory(s, "1", Stat.STRENGTH);
        engine.submitReveal(s, "1");

        when(matchStore.require("ABCDEF")).thenReturn(s);
        Room room = room(RoomStatus.IN_PROGRESS, 2);
        when(roomRepository.findByCode("ABCDEF")).thenReturn(Optional.of(room));

        GameState after = service().reveal("abcdef", "2").state();

        assertThat(after.getPhase()).isEqualTo(GameState.Phase.FINISHED);
        assertThat(after.getMatchWinnerId()).isEqualTo("1");
        ArgumentCaptor<Room> saved = ArgumentCaptor.forClass(Room.class);
        verify(roomRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(RoomStatus.FINISHED);
    }
}
