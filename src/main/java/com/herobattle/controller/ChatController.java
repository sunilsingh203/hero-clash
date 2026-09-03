package com.herobattle.controller;

import com.herobattle.controller.dto.ChatDtos.ChatMessageView;
import com.herobattle.controller.dto.ChatDtos.SendChatMessage;
import com.herobattle.controller.dto.GameDtos.GameError;
import com.herobattle.model.ChatMessage;
import com.herobattle.service.ChatService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * In-room chat. Clients send to {@code /app/rooms/{code}/chat}; accepted messages are
 * broadcast to {@code /topic/rooms.{code}.chat}, rejections go to
 * {@code /topic/rooms.{code}.errors} (same channel the game uses). {@code GET
 * /api/rooms/{code}/chat} returns the recent backlog for a client that just (re)connected.
 *
 * <p>Topic names use {@code .} separators, not {@code /} — RabbitMQ's STOMP plugin rejects
 * slashes (same constraint as {@link GameSocketController}).
 */
@Controller
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;
    private final SimpMessagingTemplate broker;

    public ChatController(ChatService chatService, SimpMessagingTemplate broker) {
        this.chatService = chatService;
        this.broker = broker;
    }

    @MessageMapping("/rooms/{code}/chat")
    public void chat(@DestinationVariable String code, SendChatMessage msg) {
        try {
            ChatMessage saved = chatService.post(code, msg.playerId(), msg.text());
            broker.convertAndSend("/topic/rooms." + saved.roomCode() + ".chat",
                    ChatMessageView.from(saved));
        } catch (RuntimeException e) {
            log.debug("Rejected chat on room {}: {}", code, e.getMessage());
            broker.convertAndSend("/topic/rooms." + code.toUpperCase() + ".errors",
                    new GameError(e.getMessage()));
        }
    }

    @GetMapping("/api/rooms/{code}/chat")
    @ResponseBody
    public List<ChatMessageView> history(@PathVariable String code) {
        return chatService.history(code).stream().map(ChatMessageView::from).toList();
    }
}
