package com.sportstreamlive.streaming.repository;

import com.sportstreamlive.streaming.model.Badge;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface BadgeRepository extends MongoRepository<Badge, String> {

    List<Badge> findByUserId(String userId);

    boolean existsByUserIdAndTipo(String userId, String tipo);

    boolean existsByUserIdAndTipoAndDescripcion(String userId, String tipo, String descripcion);

    Optional<Badge> findFirstByUserIdAndTipo(String userId, String tipo);

    Optional<Badge> findByUserIdAndTipoAndDescripcionContaining(
            String userId, String tipo, String descripcion);

    void deleteByUserIdAndTipoAndDescripcion(String userId, String tipo, String descripcion);
}
