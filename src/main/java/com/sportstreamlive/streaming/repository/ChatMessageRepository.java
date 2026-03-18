package com.sportstreamlive.streaming.repository;

import com.sportstreamlive.streaming.model.ChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
    /** Devuelve el historial de una sala ordenado cronologicamente */
    List<ChatMessage> findByRoomIdOrderByTimestampAsc(String roomId);
}
