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

/**
 * Servicio de logros (E6 - Logros).
 *
 * CONCURRENCIA — Race Condition "Atrapar Medalla":
 * El escenario: N usuarios hacen click en "Atrapar Medalla" al mismo tiempo.
 * Solo UNO debe obtenerla.
 *
 * Solucion: ConcurrentHashMap<badgeId, AtomicBoolean>
 *   - claimed.compareAndSet(false, true) es una operacion ATOMICA.
 *   - Solo el primer thread que la ejecuta retorna true.
 *   - Todos los demas retornan false sin locks ni synchronized.
 *
 * Esto evita:
 *   - Race conditions (dos usuarios obtienen la misma medalla)
 *   - Deadlocks (sin synchronized)
 *   - Busy-waiting (operacion O(1) sin espera)
 */
@Slf4j
@Service
public class BadgeService {

    /**
     * Mapa de medallas pendientes de ser reclamadas.
     * Key: badgeId (identificador unico de la medalla disponible)
     * Value: AtomicBoolean — false = disponible, true = ya reclamada
     */
    private final ConcurrentHashMap<String, AtomicBoolean> pendingBadges = new ConcurrentHashMap<>();

    private final BadgeRepository badgeRepository;

    public BadgeService(BadgeRepository badgeRepository) {
        this.badgeRepository = badgeRepository;
    }

    /**
     * Registra una medalla como disponible para ser reclamada.
     * Debe llamarse cuando se crea un evento/reto que ofrece una medalla.
     *
     * @param badgeId Identificador unico de esta medalla (ej: "evento-123-primero")
     */
    public void makeBadgeAvailable(String badgeId) {
        pendingBadges.put(badgeId, new AtomicBoolean(false));
        log.info("Medalla {} disponible para reclamar", badgeId);
    }

    /**
     * Intenta reclamar una medalla para un usuario.
     * Garantia atomica: solo el primer llamado con este badgeId retorna true.
     *
     * @param badgeId ID de la medalla a reclamar
     * @param userId  ID del usuario que intenta reclamarla
     * @param tipo    Tipo de medalla (PRIMER_LOGRO, INSCRIPCION_EVENTO, etc.)
     * @param nombre  Nombre descriptivo de la medalla
     * @return true si el usuario fue el primero y obtuvo la medalla, false si ya fue reclamada
     */
    public boolean claimBadge(String badgeId, String userId, String tipo, String nombre) {
        AtomicBoolean claimed = pendingBadges.get(badgeId);

        if (claimed == null) {
            log.warn("Intento de reclamar medalla inexistente: {}", badgeId);
            return false;
        }

        // Operacion atomica: solo el PRIMER thread que llega pasa a true
        boolean ganador = claimed.compareAndSet(false, true);

        if (ganador) {
            // Este usuario fue el primero — persistir la medalla
            Badge badge = new Badge(
                    UUID.randomUUID().toString(),
                    userId,
                    tipo,
                    nombre,
                    "Medalla obtenida por logro: " + nombre,
                    LocalDateTime.now()
            );
            badgeRepository.save(badge);
            log.info("Usuario {} reclamó la medalla '{}' (badgeId: {})", userId, nombre, badgeId);
            // Limpiar del mapa para liberar memoria (ya fue reclamada)
            pendingBadges.remove(badgeId);
        } else {
            log.info("Usuario {} intento reclamar medalla {} pero ya fue reclamada", userId, badgeId);
        }

        return ganador;
    }

    /**
     * Otorga una medalla directamente a un usuario sin race condition
     * (para logros automaticos como primer registro, unirse a un reto, etc.)
     */
    public Badge awardBadge(String userId, String tipo, String nombre, String descripcion) {
        Badge badge = new Badge(
                UUID.randomUUID().toString(),
                userId,
                tipo,
                nombre,
                descripcion,
                LocalDateTime.now()
        );
        return badgeRepository.save(badge);
    }

    /** Devuelve todos los logros de un usuario */
    public List<Badge> getUserBadges(String userId) {
        return badgeRepository.findByUserId(userId);
    }
}
