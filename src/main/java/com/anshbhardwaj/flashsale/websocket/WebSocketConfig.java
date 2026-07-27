package com.anshbhardwaj.flashsale.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Clients SUBSCRIBE to destinations prefixed with /topic to receive broadcasts
        registry.enableSimpleBroker("/topic");
        // Clients would SEND to destinations prefixed with /app (not used yet,
        // since our events are server-initiated from Kafka, not client-initiated)
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // The frontend connects here: new SockJS('http://localhost:8080/ws')
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // tighten this to your Next.js origin in production
                .withSockJS(); // fallback support for browsers/networks that block raw WebSockets
    }
}
