package com.sportstreamlive.streaming.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Mensaje de chat de una sala de streaming/reto (E5 - Chat en vivo).
 * Se persiste en MongoDB para historial.
 *
 * Concurrencia: el broadcast es manejado por el broker STOMP.
 * El historial en memoria se gestiona con ConcurrentLinkedQueue en ChatController.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "chat_messages")
public class ChatMessage {

    @Id
    private String id;

    /** Identificador de la sala (streamId o challengeId) */
    private String roomId;

    /** Username del remitente */
    private String sender;

    private String content;
    private LocalDateTime timestamp;
}
