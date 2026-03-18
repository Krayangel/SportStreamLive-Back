package com.sportstreamlive.streaming.repository;

import com.sportstreamlive.streaming.model.Event;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface EventRepository extends MongoRepository<Event, String> {
    List<Event> findByCreatorId(String creatorId);
    List<Event> findByAsistentesContaining(String userId);
}
