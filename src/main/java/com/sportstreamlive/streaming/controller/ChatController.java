package com.sportstreamlive.streaming.controller;

import com.sportstreamlive.streaming.model.ChatMessage;
import com.sportstreamlive.streaming.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * E5 - Chat en Vivo via WebSocket (CORE).
 *
 * CONCURRENCIA:
 * - ConcurrentHashMap<roomId, ConcurrentLinkedQueue<ChatMessage>>
 *   Mantiene el historial en memoria por sala, thread-safe sin locks.
 * - El broker STOMP de Spring maneja la concurrencia del broadcast.
 * - Los mensajes se persisten asincrónicamente en MongoDB.
 *
 * Flujo:
 *   Cliente envia a /app/chat/{roomId}
 *   -> ChatController recibe, persiste, guarda en queue, broadcast a /topic/chat/{roomId}
 *   -> Todos los subscritos al room reciben el mensaje en tiempo real
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageRepository chatMessageRepository;

    /**
     * Historial en memoria por sala.
     * ConcurrentLinkedQueue: thread-safe, no bloquea bajo alta concurrencia.
     */
    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<ChatMessage>> chatRooms =
            new ConcurrentHashMap<>();

    /**
     * Recibe mensajes de chat de WebSocket.
     * Destino: /app/chat/{roomId}
     * Broadcast: /topic/chat/{roomId}
     */
    @MessageMapping("/chat/{roomId}")
    public void handleChatMessage(@DestinationVariable String roomId,
                                  @Payload ChatMessage message) {
        message.setRoomId(roomId);
        message.setTimestamp(LocalDateTime.now());

        // Persistir en MongoDB
        ChatMessage saved = chatMessageRepository.save(message);

        // Guardar en cola en memoria (thread-safe)
        chatRooms.computeIfAbsent(roomId, k -> new ConcurrentLinkedQueue<>()).add(saved);

        // Broadcast a todos los suscritos al room
        messagingTemplate.convertAndSend("/topic/chat/" + roomId, saved);
        log.debug("Chat [{}] de {}: {}", roomId, message.getSender(), message.getContent());
    }

    /**
     * REST: Obtener historial de una sala (para cargar al entrar).
     * GET /api/chat/{roomId}/history
     */
    @GetMapping("/{roomId}/history")
    public List<ChatMessage> getHistory(@PathVariable String roomId) {
        return chatMessageRepository.findByRoomIdOrderByTimestampAsc(roomId);
    }
}
