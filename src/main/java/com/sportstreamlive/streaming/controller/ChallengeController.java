package com.sportstreamlive.streaming.controller;

import com.sportstreamlive.streaming.model.Challenge;
import com.sportstreamlive.streaming.repository.ChallengeRepository;
import com.sportstreamlive.streaming.service.BadgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * E4 - Retos: Crear y unirse a retos deportivos (ej: 30 dias de ejercicio).
 */
@RestController
@RequestMapping("/api/challenges")
@RequiredArgsConstructor
public class ChallengeController {

    private final ChallengeRepository challengeRepository;
    private final BadgeService badgeService;

    /** GET /api/challenges */
    @GetMapping
    public List<Challenge> listAll() {
        return challengeRepository.findAll();
    }

    /** GET /api/challenges/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<Challenge> getById(@PathVariable String id) {
        return challengeRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** POST /api/challenges — Crear reto */
    @PostMapping
    public ResponseEntity<Challenge> create(@RequestBody Challenge challenge) {
        return ResponseEntity.ok(challengeRepository.save(challenge));
    }

    /**
     * POST /api/challenges/{id}/unirse?userId=...
     * El usuario se une al reto y recibe medalla.
     */
    @PostMapping("/{id}/unirse")
    public ResponseEntity<Challenge> unirse(@PathVariable String id,
                                            @RequestParam String userId) {
        return challengeRepository.findById(id)
                .map(challenge -> {
                    if (!challenge.getParticipantes().contains(userId)) {
                        challenge.getParticipantes().add(userId);
                        challengeRepository.save(challenge);

                        // Medalla por unirse a reto
                        badgeService.awardBadge(userId, "UNIRSE_RETO",
                                "Retador",
                                "Medalla por unirse al reto: " + challenge.getNombre());
                    }
                    return ResponseEntity.ok(challenge);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
