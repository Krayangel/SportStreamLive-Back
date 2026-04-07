package com.sportstreamlive.streaming.controller;

import com.sportstreamlive.streaming.model.Badge;
import com.sportstreamlive.streaming.service.BadgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * E6 - Logros: Ver medallas y "Atrapar Medalla" concurrencia.
 *
 * Flujo de medalla lanzada en vivo:
 *  1. Streamer llama POST /api/badges/{streamId}/launch
 *  2. El back registra la medalla como disponible (makeBadgeAvailable)
 *     y la transmite por WebSocket a /topic/badges/{streamId}
 *  3. Los espectadores reciben el evento y muestran el boton "Atrapar"
 *  4. El primero en llamar POST /api/badges/{badgeId}/claim gana (atomico)
 *  5. Los demas reciben 409 Conflict
 */
@RestController
@RequestMapping("/api/badges")
@RequiredArgsConstructor
public class BadgeController {

    private final BadgeService badgeService;
    private final SimpMessagingTemplate messagingTemplate;

    /** GET /api/badges/{userId} — Ver todos los logros de un usuario */
    @GetMapping("/{userId}")
    public List<Badge> getUserBadges(@PathVariable String userId) {
        return badgeService.getUserBadges(userId);
    }

    /**
     * POST /api/badges/{streamId}/launch
     * El streamer lanza una medalla durante el live.
     * Se genera un badgeId unico, se registra como disponible
     * y se notifica a todos los espectadores via WebSocket.
     *
     * Body: { "userId": "...", "tipo": "MEDALLA_LIVE", "nombre": "Velocista" }
     *
     * Solo el owner del stream deberia llamar este endpoint.
     */
    @PostMapping("/{streamId}/launch")
    public ResponseEntity<Map<String, Object>> launchBadge(
            @PathVariable String streamId,
            @RequestBody LaunchRequest request) {

        String badgeId = streamId + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        badgeService.makeBadgeAvailable(badgeId);

        Map<String, Object> payload = Map.of(
                "action", "LAUNCHED",
                "badgeId", badgeId,
                "tipo", request.tipo(),
                "nombre", request.nombre(),
                "streamId", streamId
        );
        messagingTemplate.convertAndSend("/topic/badges/" + streamId, payload);

        return ResponseEntity.ok(Map.of(
                "badgeId", badgeId,
                "message", "Medalla lanzada: " + request.nombre()
        ));
    }

    /**
     * POST /api/badges/{badgeId}/claim
     * "Atrapar Medalla" — Solo el primero gana.
     *
     * Body: { "userId": "...", "tipo": "MEDALLA_LIVE", "nombre": "Velocista" }
     *
     * Retorna:
     * 200 OK -> Este usuario fue el ganador
     * 409 Conflict -> La medalla ya fue reclamada por otro
     */
    @PostMapping("/{badgeId}/claim")
    public ResponseEntity<Map<String, Object>> claimBadge(
            @PathVariable String badgeId,
            @RequestBody ClaimRequest request) {

        boolean won = badgeService.claimBadge(
                badgeId, request.userId(), request.tipo(), request.nombre());

        if (won) {
            return ResponseEntity.ok(Map.of(
                    "claimed", true,
                    "message", "¡Felicidades! Obtuviste la medalla: " + request.nombre()));
        } else {
            return ResponseEntity.status(409).body(Map.of(
                    "claimed", false,
                    "message", "La medalla ya fue reclamada por otro usuario"));
        }
    }

    public record LaunchRequest(String userId, String tipo, String nombre) {}
    public record ClaimRequest(String userId, String tipo, String nombre) {}
}
