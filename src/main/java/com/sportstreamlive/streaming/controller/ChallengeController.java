package com.sportstreamlive.streaming.controller;

import com.sportstreamlive.streaming.model.Challenge;
import com.sportstreamlive.streaming.model.UserProfile;
import com.sportstreamlive.streaming.repository.ChallengeRepository;
import com.sportstreamlive.streaming.repository.UserProfileRepository;
import com.sportstreamlive.streaming.service.BadgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

/**
 * E4 - Retos con dificultad, XP, progreso diario (1 entrada por día, editable).
 */
@RestController
@RequestMapping("/api/challenges")
@RequiredArgsConstructor
public class ChallengeController {

    private final ChallengeRepository challengeRepository;
    private final UserProfileRepository userProfileRepository;
    private final BadgeService badgeService;

    @GetMapping
    public List<Challenge> listAll() {
        return challengeRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Challenge> getById(@PathVariable String id) {
        return challengeRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/challenges
     * Body: { nombre, descripcion, duracionDias, creatorId, dificultad }
     * El xpRecompensa se calcula automáticamente según la dificultad.
     */
    @PostMapping
    public ResponseEntity<Challenge> create(@RequestBody Challenge challenge) {
        // Calcular XP según dificultad
        if (challenge.getDificultad() == null) {
            challenge.setDificultad(Challenge.Dificultad.MEDIA);
        }
        challenge.setXpRecompensa(challenge.getDificultad().getXp());
        return ResponseEntity.ok(challengeRepository.save(challenge));
    }

    /**
     * POST /api/challenges/{id}/unirse?userId=...
     * Une al usuario. La medalla UNIRSE_RETO es única por usuario (BadgeService lo verifica).
     */
    @PostMapping("/{id}/unirse")
    public ResponseEntity<Challenge> unirse(@PathVariable String id,
                                            @RequestParam String userId) {
        return challengeRepository.findById(id)
                .map(challenge -> {
                    if (!challenge.getParticipantes().contains(userId)) {
                        challenge.getParticipantes().add(userId);
                        challengeRepository.save(challenge);
                        badgeService.awardBadge(userId, "UNIRSE_RETO", "Retador",
                                "Medalla por unirse al reto: " + challenge.getNombre());
                    }
                    return ResponseEntity.ok(challenge);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * DELETE /api/challenges/{id}/salir?userId=...
     * Saca al usuario del reto y elimina su medalla.
     */
    @DeleteMapping("/{id}/salir")
    public ResponseEntity<Map<String, Object>> salir(@PathVariable String id,
                                                     @RequestParam String userId) {
        return challengeRepository.findById(id)
                .map(challenge -> {
                    boolean estaba = challenge.getParticipantes().remove(userId);
                    if (estaba) {
                        challenge.getEvidencias().remove(userId);
                        challenge.getProgresoDiario().remove(userId);
                        challengeRepository.save(challenge);
                        badgeService.removeBadgeForChallenge(userId, challenge.getNombre());
                    }
                    return ResponseEntity.ok(Map.<String, Object>of(
                            "salido", estaba,
                            "message", estaba
                                    ? "Saliste del reto. Tu medalla fue eliminada."
                                    : "No estabas en este reto."
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/challenges/{id}/progreso?userId=...
     * Registra o EDITA el avance del día actual.
     * Body: { "texto": "Completé 5km hoy" }
     *
     * Reglas:
     * - Solo 1 entrada por día por usuario (la fecha del servidor es la clave)
     * - El usuario puede EDITAR la entrada del día, pero no agregar otra
     * - No puede completar el reto antes de duracionDias días
     *   (el número de días distintos con entrada debe ser >= duracionDias)
     */
    @PostMapping("/{id}/progreso")
    public ResponseEntity<Map<String, Object>> registrarProgreso(
            @PathVariable String id,
            @RequestParam String userId,
            @RequestBody Map<String, String> body) {

        String texto = body.get("texto");
        if (texto == null || texto.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El texto es obligatorio"));
        }

        return challengeRepository.findById(id)
                .map(challenge -> {
                    if (!challenge.getParticipantes().contains(userId)) {
                        return ResponseEntity.<Map<String, Object>>status(403)
                                .body(Map.of("error", "No eres participante"));
                    }

                    String hoy = LocalDate.now().toString(); // "yyyy-MM-dd"

                    // Obtener o crear el mapa de progreso del usuario
                    Map<String, Map<String, String>> todosProgreso = challenge.getProgresoDiario();
                    if (todosProgreso == null) {
                        todosProgreso = new HashMap<>();
                        challenge.setProgresoDiario(todosProgreso);
                    }
                    Map<String, String> progresoUsuario = todosProgreso.computeIfAbsent(userId, k -> new LinkedHashMap<>());

                    // Guardar o editar la entrada de hoy
                    progresoUsuario.put(hoy, texto.trim());
                    challengeRepository.save(challenge);

                    int diasCompletados = progresoUsuario.size();
                    int diasRequeridos  = challenge.getDuracionDias();
                    boolean completado  = diasCompletados >= diasRequeridos;

                    // Si completó el reto: sumar XP y otorgar medalla
                    if (completado) {
                        // Sumar XP al perfil
                        userProfileRepository.findByUserId(userId).ifPresent(profile -> {
                            profile.setXpTotal(profile.getXpTotal() + challenge.getXpRecompensa());
                            userProfileRepository.save(profile);
                        });
                        // Medalla de completar (única por reto)
                        badgeService.awardBadge(userId, "COMPLETAR_RETO",
                                "Reto Completado: " + challenge.getNombre(),
                                "Completaste el reto: " + challenge.getNombre());
                    }

                    return ResponseEntity.ok(Map.<String, Object>of(
                            "ok", true,
                            "fecha", hoy,
                            "diasCompletados", diasCompletados,
                            "diasRequeridos", diasRequeridos,
                            "porcentaje", Math.min(100, (diasCompletados * 100) / diasRequeridos),
                            "completado", completado,
                            "xpGanado", completado ? challenge.getXpRecompensa() : 0
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/challenges/{id}/progreso/{userId}
     * Devuelve el progreso diario de un usuario: Map<fecha, texto>
     */
    @GetMapping("/{id}/progreso/{userId}")
    public ResponseEntity<Map<String, String>> getProgreso(@PathVariable String id,
                                                           @PathVariable String userId) {
        return challengeRepository.findById(id)
                .map(challenge -> {
                    Map<String, String> prog = challenge.getProgresoDiario() != null
                            ? challenge.getProgresoDiario().getOrDefault(userId, new LinkedHashMap<>())
                            : new LinkedHashMap<>();
                    return ResponseEntity.ok(prog);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/challenges/{id}/evidencia?userId=...
     * Agrega evidencia libre (sin restricción de 1 por día).
     */
    @PostMapping("/{id}/evidencia")
    public ResponseEntity<Map<String, Object>> addEvidencia(
            @PathVariable String id,
            @RequestParam String userId,
            @RequestBody Map<String, String> body) {

        String texto = body.get("texto");
        if (texto == null || texto.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Texto obligatorio"));
        }

        return challengeRepository.findById(id)
                .map(challenge -> {
                    if (!challenge.getParticipantes().contains(userId)) {
                        return ResponseEntity.<Map<String, Object>>status(403)
                                .body(Map.of("error", "No eres participante"));
                    }
                    String entrada = "[" + LocalDate.now() + "] " + texto.trim();
                    challenge.getEvidencias()
                            .computeIfAbsent(userId, k -> new ArrayList<>())
                            .add(entrada);
                    challengeRepository.save(challenge);
                    return ResponseEntity.ok(Map.<String, Object>of(
                            "ok", true,
                            "entrada", entrada,
                            "total", challenge.getEvidencias().get(userId).size()
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/challenges/{id}/evidencia/{userId}
     */
    @GetMapping("/{id}/evidencia/{userId}")
    public ResponseEntity<List<String>> getEvidencias(@PathVariable String id,
                                                      @PathVariable String userId) {
        return challengeRepository.findById(id)
                .map(c -> ResponseEntity.ok(
                        c.getEvidencias().getOrDefault(userId, List.of())))
                .orElse(ResponseEntity.notFound().build());
    }
}
