package com.herobattle.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.herobattle.controller.dto.GameDtos.GameView;
import com.herobattle.controller.dto.GameDtos.PickMessage;
import com.herobattle.controller.dto.GameDtos.RevealMessage;
import com.herobattle.model.Card;
import com.herobattle.model.Player;
import com.herobattle.model.Room;
import com.herobattle.model.RoomStatus;
import com.herobattle.repository.CardRepository;
import com.herobattle.repository.RoomRepository;
import com.herobattle.service.MatchStateRepository;
import com.herobattle.support.InMemoryMatchStateRepository;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.lang.NonNull;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

/**
 * End-to-end STOMP flow: connect, start a match, pick a stat, both players reveal, and
 * confirm the round resolves — all over the wire against a running server.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GameSocketControllerTest {

    @TestConfiguration
    static class InMemoryStore {
        @Bean
        @Primary
        MatchStateRepository inMemoryMatchStateRepository() {
            return new InMemoryMatchStateRepository();
        }
    }

    @org.springframework.boot.test.web.server.LocalServerPort
    int port;

    @Autowired RoomRepository roomRepository;
    @Autowired CardRepository cardRepository;

    private String roomCode;
    private String p1;
    private String p2;

    @BeforeEach
    void seed() {
        cardRepository.deleteAll();
        for (int i = 0; i < 12; i++) {
            Card c = new Card();
            c.setName("C" + i);
            c.setStrength(10 + i);
            c.setSpeed(50);
            c.setIntelligence(50);
            c.setDurability(50);
            c.setPower(50);
            c.setCombat(50);
            cardRepository.save(c);
        }
        Room room = new Room();
        room.setCode("SOCK01");
        room.setStatus(RoomStatus.WAITING);
        Player a = new Player();
        a.setDisplayName("Ada");
        Player b = new Player();
        b.setDisplayName("Boop");
        room.addPlayer(a);
        room.addPlayer(b);
        room = roomRepository.save(room);
        roomCode = room.getCode();
        p1 = String.valueOf(room.getPlayers().get(0).getId());
        p2 = String.valueOf(room.getPlayers().get(1).getId());
    }

    private StompSession connect() throws Exception {
        WebSocketStompClient client = new WebSocketStompClient(new SockJsClient(
                List.of(new WebSocketTransport(new StandardWebSocketClient()))));
        client.setMessageConverter(new MappingJackson2MessageConverter());
        return client.connectAsync("http://localhost:" + port + "/ws",
                new StompSessionHandlerAdapter() {}).get(5, TimeUnit.SECONDS);
    }

    @Test
    void playsAFullRoundOverStomp() throws Exception {
        StompSession session = connect();
        BlockingQueue<String> errors = new ArrayBlockingQueue<>(8);
        session.subscribe("/topic/rooms/" + roomCode + "/errors", new StompFrameHandler() {
            @Override
            @NonNull
            public Type getPayloadType(@NonNull StompHeaders headers) {
                return com.herobattle.controller.dto.GameDtos.GameError.class;
            }

            @Override
            public void handleFrame(@NonNull StompHeaders headers, Object payload) {
                errors.add(((com.herobattle.controller.dto.GameDtos.GameError) payload).message());
            }
        });
        BlockingQueue<GameView> views = new ArrayBlockingQueue<>(32);
        session.subscribe("/topic/rooms/" + roomCode, new StompFrameHandler() {
            @Override
            @NonNull
            public Type getPayloadType(@NonNull StompHeaders headers) {
                return GameView.class;
            }

            @Override
            public void handleFrame(@NonNull StompHeaders headers, Object payload) {
                views.add((GameView) payload);
            }
        });

        session.send("/app/rooms/" + roomCode + "/start", java.util.Map.of());
        GameView started = views.poll(5, TimeUnit.SECONDS);
        assertThat(started).isNotNull();
        assertThat(started.phase()).isEqualTo("RUNNING");
        assertThat(started.players()).hasSize(2);
        assertThat(started.round().phase()).isEqualTo("PICKING");

        session.send("/app/rooms/" + roomCode + "/pick", new PickMessage(p1, "STRENGTH"));
        GameView picked = views.poll(5, TimeUnit.SECONDS);
        assertThat(picked).isNotNull();
        assertThat(picked.round().phase()).isEqualTo("REVEALING");
        assertThat(picked.round().stat()).isEqualTo("STRENGTH");

        // MatchService serializes actions per room, so both reveals resolve into one round
        session.send("/app/rooms/" + roomCode + "/reveal", new RevealMessage(p1));
        session.send("/app/rooms/" + roomCode + "/reveal", new RevealMessage(p2));

        GameView resolved = null;
        for (int i = 0; i < 8 && resolved == null; i++) {
            GameView v = views.poll(5, TimeUnit.SECONDS);
            if (v == null) {
                break;
            }
            if (v.resolution() != null) {
                resolved = v;
            }
        }
        assertThat(resolved).as("errors=%s", errors).isNotNull();
        GameView end = resolved;
        assertThat(end.resolution().roundWinnerId()).isIn(p1, p2);
        assertThat(end.resolution().stat()).isEqualTo("STRENGTH");
        assertThat(end.resolution().values()).containsOnlyKeys(p1, p2);
        // pot moved to the winner: their deck grew to 7 (6 held - 1 played + 2 pot)
        int winnerDeck = end.players().stream()
                .filter(ps -> ps.playerId().equals(end.resolution().roundWinnerId()))
                .findFirst().orElseThrow().deckCount();
        assertThat(winnerDeck).isEqualTo(7);

        session.disconnect();
    }
}
