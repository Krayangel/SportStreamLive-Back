package com.sportstreamlive.streaming.repository;

import com.sportstreamlive.streaming.model.Challenge;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ChallengeRepository extends MongoRepository<Challenge, String> {
    List<Challenge> findByCreatorId(String creatorId);
    List<Challenge> findByParticipantesContaining(String userId);
}
