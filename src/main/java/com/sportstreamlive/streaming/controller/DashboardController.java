package com.sportstreamlive.streaming.controller;

import com.sportstreamlive.streaming.model.UserProfile;
import com.sportstreamlive.streaming.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Dashboard: racha diaria, XP, metas privadas.
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final UserProfileRepository userProfileRepository;

    /** GET /api/dashboard/{userId} */
    @GetMapping("/{userId}")
    public ResponseEntity<UserProfile> getProfile(@PathVariable String userId) {
        return userProfileRepository.findByUserId(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/dashboard/{userId}/actividad
     * Registra actividad del día y actualiza la racha.
     * Llamar una vez al día cuando el usuario hace algo (marca avance en reto, etc.)
     *
     * Lógica de racha:
     *  - Si ultimaActividad == hoy → racha no cambia (ya contó hoy)
     *  - Si ultimaActividad == ayer → racha + 1
     *  - Si ultimaActividad < ayer o null → racha = 1 (se reinicia)
     */
    @PostMapping("/{userId}/actividad")
    public ResponseEntity<UserProfile> registrarActividad(@PathVariable String userId) {
        return userProfileRepository.findByUserId(userId)
                .map(profile -> {
                    LocalDate hoy = LocalDate.now();
                    LocalDate ultima = profile.getUltimaActividad();

                    if (ultima == null) {
                        profile.setRachaActual(1);
                    } else if (ultima.equals(hoy)) {
                        // Ya registró hoy — no hacer nada
                    } else if (ultima.equals(hoy.minusDays(1))) {
                        // Ayer registró — continúa racha
                        profile.setRachaActual(profile.getRachaActual() + 1);
                    } else {
                        // Rompió la racha
                        profile.setRachaActual(1);
                    }

                    profile.setUltimaActividad(hoy);
                    return ResponseEntity.ok(userProfileRepository.save(profile));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/dashboard/{userId}/xp
     * Suma XP al perfil del usuario.
     * Body: { "cantidad": 150 }
     */
    @PostMapping("/{userId}/xp")
    public ResponseEntity<UserProfile> sumarXp(
            @PathVariable String userId,
            @RequestBody Map<String, Integer> body) {

        int cantidad = body.getOrDefault("cantidad", 0);
        if (cantidad <= 0) {
            return ResponseEntity.badRequest().build();
        }

        return userProfileRepository.findByUserId(userId)
                .map(profile -> {
                    profile.setXpTotal(profile.getXpTotal() + cantidad);
                    return ResponseEntity.ok(userProfileRepository.save(profile));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/dashboard/{userId}/metas
     * Devuelve las metas privadas del usuario (lista de strings).
     */
    @GetMapping("/{userId}/metas")
    public ResponseEntity<List<String>> getMetas(@PathVariable String userId) {
        return userProfileRepository.findByUserId(userId)
                .map(p -> ResponseEntity.ok(p.getMetas() != null ? p.getMetas() : new ArrayList<String>()))
                .orElse(ResponseEntity.<List<String>>notFound().build());
    }

    /**
     * PUT /api/dashboard/{userId}/metas
     * Reemplaza la lista completa de metas privadas.
     * Body: { "metas": ["Meta 1", "Meta 2"] }
     */
    @PutMapping("/{userId}/metas")
    public ResponseEntity<UserProfile> updateMetas(
            @PathVariable String userId,
            @RequestBody MetasRequest request) {
        return userProfileRepository.findByUserId(userId)
                .map(profile -> {
                    profile.setMetas(request.metas() != null ? request.metas() : new ArrayList<>());
                    return ResponseEntity.ok(userProfileRepository.save(profile));
                })
                .orElse(ResponseEntity.<UserProfile>notFound().build());
    }

    /**
     * POST /api/dashboard/{userId}/metas
     * Agrega una meta a la lista.
     * Body: { "texto": "Correr 5km sin parar" }
     */
    @PostMapping("/{userId}/metas")
    public ResponseEntity<UserProfile> addMeta(
            @PathVariable String userId,
            @RequestBody Map<String, String> body) {

        String texto = body.get("texto");
        if (texto == null || texto.isBlank()) {
            return ResponseEntity.<UserProfile>badRequest().build();
        }

        return userProfileRepository.findByUserId(userId)
                .map(profile -> {
                    if (profile.getMetas() == null) profile.setMetas(new ArrayList<>());
                    profile.getMetas().add(texto.trim());
                    return ResponseEntity.ok(userProfileRepository.save(profile));
                })
                .orElse(ResponseEntity.<UserProfile>notFound().build());
    }

    /**
     * DELETE /api/dashboard/{userId}/metas/{index}
     * Elimina la meta en la posición indicada.
     */
    @DeleteMapping("/{userId}/metas/{index}")
    public ResponseEntity<UserProfile> deleteMeta(
            @PathVariable String userId,
            @PathVariable int index) {
        var profileOpt = userProfileRepository.findByUserId(userId);
        if (profileOpt.isEmpty()) {
            return ResponseEntity.<UserProfile>notFound().build();
        }

        UserProfile profile = profileOpt.get();
        List<String> metas = profile.getMetas();
        if (metas == null || index < 0 || index >= metas.size()) {
            return ResponseEntity.<UserProfile>badRequest().build();
        }

        metas.remove(index);
        return ResponseEntity.ok(userProfileRepository.save(profile));
    }

    public record MetasRequest(List<String> metas) {}
}
