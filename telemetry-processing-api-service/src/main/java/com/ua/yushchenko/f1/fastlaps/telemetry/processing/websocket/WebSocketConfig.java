package com.ua.yushchenko.f1.fastlaps.telemetry.processing.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration with STOMP protocol.
 * See: implementation_steps_plan.md § Етап 9.1.
 * See: rest_web_socket_api_contracts_f_1_telemetry.md § 6.
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple broker for broadcasting to /topic destinations
        config.enableSimpleBroker("/topic");
        
        // Client messages destined to @MessageMapping methods use /app prefix
        config.setApplicationDestinationPrefixes("/app");
        
        log.info("WebSocket message broker configured: /topic for broadcasts, /app for client messages");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register STOMP endpoint at /ws/live
        registry.addEndpoint("/ws/live")
                .setAllowedOriginPatterns("*") // Allow all origins for development
                .withSockJS(); // Enable SockJS fallback for browsers without WebSocket support
        
        log.info("STOMP endpoint registered at /ws/live with SockJS fallback");
    }
}
