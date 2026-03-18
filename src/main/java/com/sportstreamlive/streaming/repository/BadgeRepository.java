package com.sportstreamlive.streaming.repository;

import com.sportstreamlive.streaming.model.Badge;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface BadgeRepository extends MongoRepository<Badge, String> {
    List<Badge> findByUserId(String userId);
}
