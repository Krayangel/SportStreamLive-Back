package com.sportstreamlive.streaming.controller;

import com.sportstreamlive.streaming.model.Badge;
import com.sportstreamlive.streaming.service.BadgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * E6 - Logros: Ver medallas y "Atrapar Medalla" concurrencia.
 *
 * El endpoint /claim implementa el patron atomic badge-claim:
 * Solo el primer usuario que llame obtiene la medalla.
 * Los demas reciben un 409 Conflict.
 */
@RestController
@RequestMapping("/api/badges")
@RequiredArgsConstructor
public class BadgeController {

    private final BadgeService badgeService;

    /** GET /api/badges/{userId} — Ver todos los logros de un usuario */
    @GetMapping("/{userId}")
    public List<Badge> getUserBadges(@PathVariable String userId) {
        return badgeService.getUserBadges(userId);
    }

    /**
     * POST /api/badges/{badgeId}/claim
     * "Atrapar Medalla" — Solo el primero gana.
     *
     * Body: { "userId": "...", "tipo": "PRIMER_LOGRO", "nombre": "Veloz" }
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

    public record ClaimRequest(String userId, String tipo, String nombre) {
    }
}
