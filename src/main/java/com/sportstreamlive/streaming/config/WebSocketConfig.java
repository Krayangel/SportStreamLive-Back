package com.sportstreamlive.streaming.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configura el broker STOMP sobre WebSocket.
 * - Endpoint de conexion: /ws (con fallback SockJS)
 * - Prefijo de mensajes de aplicacion: /app
 * - Prefijo de topics broadcast: /topic
 * - Prefijo de colas privadas: /queue
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Broker en memoria para /topic (broadcast) y /queue (privado)
        registry.enableSimpleBroker("/topic", "/queue");
        // Los mensajes enviados desde el cliente deben llevar el prefijo /app
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint WebSocket con soporte SockJS para navegadores sin WS nativo
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
