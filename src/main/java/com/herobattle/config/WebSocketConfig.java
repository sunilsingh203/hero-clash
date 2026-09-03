package com.herobattle.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP-over-SockJS wiring.
 *
 * <p>By default this uses the in-memory {@code SimpleBroker}, which is fine for a single
 * backend instance (local dev, tests). Set {@code heroclash.broker.relay=true} (see the
 * {@code docker} profile) to switch to a RabbitMQ STOMP broker relay so broadcasts fan
 * out correctly across multiple backend instances — the horizontal-scaling setup
 * described in the project spec.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final boolean useRelay;
    private final String relayHost;
    private final int relayPort;
    private final String relayUser;
    private final String relayPassword;
    private final String allowedOrigins;

    public WebSocketConfig(
            @Value("${heroclash.broker.relay:false}") boolean useRelay,
            @Value("${heroclash.broker.relay-host:localhost}") String relayHost,
            @Value("${heroclash.broker.relay-port:61613}") int relayPort,
            @Value("${heroclash.broker.relay-user:guest}") String relayUser,
            @Value("${heroclash.broker.relay-password:guest}") String relayPassword,
            @Value("${heroclash.cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
            String allowedOrigins) {
        this.useRelay = useRelay;
        this.relayHost = relayHost;
        this.relayPort = relayPort;
        this.relayUser = relayUser;
        this.relayPassword = relayPassword;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        if (useRelay) {
            registry.enableStompBrokerRelay("/topic", "/queue")
                    .setRelayHost(relayHost)
                    .setRelayPort(relayPort)
                    .setClientLogin(relayUser)
                    .setClientPasscode(relayPassword)
                    .setSystemLogin(relayUser)
                    .setSystemPasscode(relayPassword);
        } else {
            registry.enableSimpleBroker("/topic", "/queue");
        }
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOrigins.split(","))
                .withSockJS();
    }
}
