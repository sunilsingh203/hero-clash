package com.herobattle.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.herobattle.controller.dto.BattleDtos.AttackMessage;
import com.herobattle.controller.dto.BattleDtos.BattleView;
import com.herobattle.model.Card;
import com.herobattle.model.GameMode;
import com.herobattle.model.Player;
import com.herobattle.model.Room;
import com.herobattle.model.RoomStatus;
import com.herobattle.repository.CardRepository;
import com.herobattle.repository.RoomRepository;
import com.herobattle.service.BattleStateRepository;
import com.herobattle.support.InMemoryBattleStateRepository;
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
 * End-to-end STOMP flow for Battle Mode v2: connect, start the battle, land an attack, and
 * confirm the broadcast BattleView reflects the damage — over the wire against a live server.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BattleSocketControllerTest {

    @TestConfiguration
    static class InMemoryStore {
        @Bean
        @Primary
        BattleStateRepository inMemoryBattleStateRepository() {
            return new InMemoryBattleStateRepository();
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
        for (int i = 0; i < 8; i++) {
            Card c = new Card();
            c.setName("C" + i);
            c.setStrength(50);
            c.setSpeed(50);
            c.setIntelligence(50);
            c.setDurability(50);
            c.setPower(50);
            c.setCombat(30 + i * 5); // distinct initiative so turn order is deterministic
            cardRepository.save(c);
        }
        Room room = new Room();
        room.setCode("BTL001");
        room.setStatus(RoomStatus.WAITING);
        room.setMode(GameMode.BATTLE);
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
    void startsAndResolvesAnAttackOverStomp() throws Exception {
        StompSession session = connect();
        BlockingQueue<BattleView> views = new ArrayBlockingQueue<>(32);
        session.subscribe("/topic/rooms." + roomCode, new StompFrameHandler() {
            @Override
            @NonNull
            public Type getPayloadType(@NonNull StompHeaders headers) {
                return BattleView.class;
            }

            @Override
            public void handleFrame(@NonNull StompHeaders headers, Object payload) {
                views.add((BattleView) payload);
            }
        });

        session.send("/app/rooms/" + roomCode + "/battle/start", java.util.Map.of());
        BattleView started = views.poll(5, TimeUnit.SECONDS);
        assertThat(started).isNotNull();
        assertThat(started.kind()).isEqualTo("BATTLE");
        assertThat(started.phase()).isEqualTo("RUNNING");
        assertThat(started.combatants()).hasSize(2);

        String attacker = started.activePlayerId();
        String target = attacker.equals(p1) ? p2 : p1;
        int targetHpBefore = started.combatants().stream()
                .filter(c -> c.playerId().equals(target)).findFirst().orElseThrow().currentHp();

        session.send("/app/rooms/" + roomCode + "/battle/attack", new AttackMessage(attacker, target));
        BattleView afterHit = views.poll(5, TimeUnit.SECONDS);
        assertThat(afterHit).isNotNull();
        assertThat(afterHit.lastAttack()).isNotNull();
        assertThat(afterHit.lastAttack().attackerId()).isEqualTo(attacker);
        assertThat(afterHit.lastAttack().targetId()).isEqualTo(target);
        int targetHpAfter = afterHit.combatants().stream()
                .filter(c -> c.playerId().equals(target)).findFirst().orElseThrow().currentHp();
        assertThat(targetHpAfter).isLessThan(targetHpBefore);
        assertThat(afterHit.activePlayerId()).isEqualTo(target); // turn passed

        session.disconnect();
    }
}
