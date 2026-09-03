package com.herobattle.config;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.config.StompBrokerRelayRegistration;

class WebSocketConfigTest {

    private static final String ORIGINS = "http://localhost:5173";

    private WebSocketConfig config(boolean relay) {
        return new WebSocketConfig(relay, "rabbit", 61613, "guest", "guest", ORIGINS);
    }

    @Test
    void usesSimpleBrokerWhenRelayDisabled() {
        MessageBrokerRegistry registry = mock(MessageBrokerRegistry.class, RETURNS_DEEP_STUBS);

        config(false).configureMessageBroker(registry);

        verify(registry).setApplicationDestinationPrefixes("/app");
        verify(registry).enableSimpleBroker("/topic", "/queue");
    }

    @Test
    void wiresRabbitRelayWhenEnabled() {
        MessageBrokerRegistry registry = mock(MessageBrokerRegistry.class, RETURNS_DEEP_STUBS);
        StompBrokerRelayRegistration relayReg =
                mock(StompBrokerRelayRegistration.class, Answers.RETURNS_SELF);
        when(registry.enableStompBrokerRelay("/topic", "/queue")).thenReturn(relayReg);

        config(true).configureMessageBroker(registry);

        verify(registry).enableStompBrokerRelay("/topic", "/queue");
        verify(relayReg).setRelayHost("rabbit");
        verify(relayReg).setRelayPort(61613);
    }
}
