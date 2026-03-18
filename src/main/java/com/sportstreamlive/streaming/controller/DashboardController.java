package com.sportstreamlive.streaming.controller;

import com.sportstreamlive.streaming.model.UserProfile;
import com.sportstreamlive.streaming.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * E2 - Dashboard: Perfil, racha y puntos del usuario.
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

    /** PUT /api/dashboard/{userId}/metas */
    @PutMapping("/{userId}/metas")
    public ResponseEntity<UserProfile> updateMetas(
            @PathVariable String userId,
            @RequestBody MetasRequest request) {
        return userProfileRepository.findByUserId(userId)
                .map(profile -> {
                    profile.setMetas(request.metas());
                    return ResponseEntity.ok(userProfileRepository.save(profile));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    public record MetasRequest(String metas) {}
}
