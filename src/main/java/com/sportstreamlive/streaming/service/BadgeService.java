package com.sportstreamlive.streaming.service;

import com.sportstreamlive.streaming.model.Badge;
import com.sportstreamlive.streaming.repository.BadgeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class BadgeService {

    /**
     * Mapa de medallas reclamables (ej: medalla especial de live).
     * Key: badgeId único (ej: "evento-abc123-primero")
     * Value: AtomicBoolean — false=disponible, true=ya reclamada
     */
    private final ConcurrentHashMap<String, AtomicBoolean> pendingBadges = new ConcurrentHashMap<>();

    private final BadgeRepository badgeRepository;

    public BadgeService(BadgeRepository badgeRepository) {
        this.badgeRepository = badgeRepository;
    }

    // ── Medalla reclamable (live especial) ───────────────────

    public void makeBadgeAvailable(String badgeId) {
        pendingBadges.put(badgeId, new AtomicBoolean(false));
        log.info("Medalla {} disponible para reclamar", badgeId);
    }

    /**
     * Reclamar medalla especial de live.
     * Solo el primer usuario que llame con este badgeId la obtiene.
     * Garantía atómica con compareAndSet.
     */
    public boolean claimBadge(String badgeId, String userId, String tipo, String nombre) {
        AtomicBoolean claimed = pendingBadges.get(badgeId);
        if (claimed == null) {
            log.warn("Medalla inexistente o ya reclamada: {}", badgeId);
            return false;
        }

        boolean ganador = claimed.compareAndSet(false, true);
        if (ganador) {
            // Verificar que este usuario no tenga ya esta medalla específica
            String desc = "Medalla obtenida por logro: " + nombre;
            if (!badgeRepository.existsByUserIdAndTipoAndDescripcion(userId, tipo, desc)) {
                Badge badge = new Badge(
                        UUID.randomUUID().toString(), userId, tipo, nombre,
                        desc, LocalDateTime.now()
                );
                badgeRepository.save(badge);
                log.info("Usuario {} obtuvo medalla '{}' (badgeId: {})", userId, nombre, badgeId);
            }
            pendingBadges.remove(badgeId);
        } else {
            log.info("Usuario {} intentó reclamar {} pero ya fue reclamada", userId, badgeId);
        }
        return ganador;
    }

    // ── Medalla automática (registro, inscripción, reto) ────

    /**
     * Otorga una medalla automática SOLO si el usuario NO la tiene aún.
     *
     * Reglas:
     * - PRIMER_LOGRO: solo 1 por usuario total
     * - INSCRIPCION_EVENTO: solo 1 por usuario total (la primera inscripción)
     * - UNIRSE_RETO: solo 1 por usuario total (el primer reto)
     * - COMPLETAR_RETO: 1 por reto completado (descripción única por reto)
     * - ESPECTADOR_VIP: manejada por claimBadge
     */
    public Badge awardBadge(String userId, String tipo, String nombre, String descripcion) {
        // Para tipos de logro único (el usuario solo puede tenerlo una vez)
        boolean esTipoUnico = tipo.equals("PRIMER_LOGRO")
                || tipo.equals("INSCRIPCION_EVENTO")
                || tipo.equals("UNIRSE_RETO");

        if (esTipoUnico) {
            // Solo puede tener UNA medalla de este tipo en toda su vida
            if (badgeRepository.existsByUserIdAndTipo(userId, tipo)) {
                log.info("Medalla {} ya existe para userId={}, ignorando", tipo, userId);
                return badgeRepository.findFirstByUserIdAndTipo(userId, tipo).orElse(null);
            }
        } else {
            // Para otros tipos (COMPLETAR_RETO, etc.) verificar descripción exacta
            if (badgeRepository.existsByUserIdAndTipoAndDescripcion(userId, tipo, descripcion)) {
                log.info("Medalla duplicada ignorada: userId={} tipo={}", userId, tipo);
                return badgeRepository
                        .findByUserIdAndTipoAndDescripcionContaining(userId, tipo, descripcion)
                        .orElse(null);
            }
        }

        Badge badge = new Badge(
                UUID.randomUUID().toString(), userId, tipo, nombre,
                descripcion, LocalDateTime.now()
        );
        return badgeRepository.save(badge);
    }

    /**
     * Elimina la medalla UNIRSE_RETO de un reto específico
     * cuando el usuario sale sin completarlo.
     */
    public void removeBadgeForChallenge(String userId, String challengeName) {
        String descripcion = "Medalla por unirse al reto: " + challengeName;
        badgeRepository.deleteByUserIdAndTipoAndDescripcion(userId, "UNIRSE_RETO", descripcion);
        log.info("Medalla UNIRSE_RETO eliminada: userId={} reto='{}'", userId, challengeName);
    }

    public List<Badge> getUserBadges(String userId) {
        return badgeRepository.findByUserId(userId);
    }
}
