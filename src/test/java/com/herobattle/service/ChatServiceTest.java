package com.herobattle.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.herobattle.model.ChatMessage;
import com.herobattle.model.Player;
import com.herobattle.model.Room;
import com.herobattle.model.RoomStatus;
import com.herobattle.repository.RoomRepository;
import com.herobattle.support.InMemoryChatHistoryStore;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatServiceTest {

    @Mock RoomRepository roomRepository;

    private final InMemoryChatHistoryStore history = new InMemoryChatHistoryStore();

    private ChatService service() {
        return new ChatService(roomRepository, history);
    }

    private static Room room() {
        Room r = new Room();
        r.setCode("ABCDEF");
        r.setStatus(RoomStatus.WAITING);
        for (int i = 1; i <= 2; i++) {
            Player p = new Player();
            p.setId((long) i);
            p.setDisplayName("P" + i);
            r.addPlayer(p);
        }
        return r;
    }

    @Test
    void postStampsDisplayNameFromRoomAndRecordsHistory() {
        when(roomRepository.findByCode("ABCDEF")).thenReturn(Optional.of(room()));

        ChatMessage msg = service().post("abcdef", "2", "  gg wp  ");

        assertThat(msg.displayName()).isEqualTo("P2");
        assertThat(msg.text()).isEqualTo("gg wp");
        assertThat(msg.roomCode()).isEqualTo("ABCDEF");
        assertThat(service().history("abcdef")).extracting(ChatMessage::text).containsExactly("gg wp");
    }

    @Test
    void postRejectsBlankMessage() {
        when(roomRepository.findByCode("ABCDEF")).thenReturn(Optional.of(room()));
        assertThatThrownBy(() -> service().post("abcdef", "1", "   "))
                .isInstanceOf(ChatException.class);
    }

    @Test
    void postRejectsOverlongMessage() {
        when(roomRepository.findByCode("ABCDEF")).thenReturn(Optional.of(room()));
        String tooLong = "x".repeat(ChatService.MAX_LENGTH + 1);
        assertThatThrownBy(() -> service().post("abcdef", "1", tooLong))
                .isInstanceOf(ChatException.class);
    }

    @Test
    void postRejectsNonMember() {
        when(roomRepository.findByCode("ABCDEF")).thenReturn(Optional.of(room()));
        assertThatThrownBy(() -> service().post("abcdef", "99", "hi"))
                .isInstanceOf(ChatException.class);
    }

    @Test
    void postRejectsUnknownRoom() {
        when(roomRepository.findByCode("ZZZZZZ")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service().post("zzzzzz", "1", "hi"))
                .isInstanceOf(RoomException.RoomNotFound.class);
    }

    @Test
    void historyIsCappedAtMaxHistory() {
        when(roomRepository.findByCode("ABCDEF")).thenReturn(Optional.of(room()));
        ChatService service = service();
        for (int i = 0; i < ChatHistoryStore.MAX_HISTORY + 10; i++) {
            service.post("abcdef", "1", "msg " + i);
        }
        assertThat(service.history("abcdef")).hasSize(ChatHistoryStore.MAX_HISTORY);
        assertThat(service.history("abcdef").get(ChatHistoryStore.MAX_HISTORY - 1).text())
                .isEqualTo("msg " + (ChatHistoryStore.MAX_HISTORY + 9));
    }
}
