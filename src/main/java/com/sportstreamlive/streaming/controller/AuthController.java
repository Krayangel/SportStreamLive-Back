package com.sportstreamlive.streaming.controller;

import com.sportstreamlive.streaming.model.User;
import com.sportstreamlive.streaming.model.UserProfile;
import com.sportstreamlive.streaming.repository.UserProfileRepository;
import com.sportstreamlive.streaming.repository.UserRepository;
import com.sportstreamlive.streaming.security.JwtUtil;
import com.sportstreamlive.streaming.service.BadgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * E1 - Autenticacion: Registro y Login con JWT.
 * Preparado para OAuth2 extension.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final BadgeService badgeService;

    /** POST /api/auth/register */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email ya registrado"));
        }
        if (userRepository.existsByUsername(request.username())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username ya en uso"));
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRoles(List.of("ROLE_USER"));
        user.setProvider(User.AuthProvider.LOCAL);
        User saved = userRepository.save(user);

        // Crear perfil inicial
        UserProfile profile = new UserProfile(null, saved.getId(), 0, 0, "");
        userProfileRepository.save(profile);

        // Medalla por primer registro
        badgeService.awardBadge(saved.getId(), "PRIMER_LOGRO", "Bienvenido",
                "Medalla por registrarse en SportStreamLive");

        String token = jwtUtil.generateToken(saved.getEmail());
        return ResponseEntity.ok(Map.of("token", token, "userId", saved.getId(), "username", saved.getUsername()));
    }

    /** POST /api/auth/login */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        return userRepository.findByEmail(request.email())
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .map(u -> {
                    String token = jwtUtil.generateToken(u.getEmail());
                    return ResponseEntity.ok(Map.of("token", token, "userId", u.getId(), "username", u.getUsername()));
                })
                .orElse(ResponseEntity.status(401).body(Map.of("error", "Credenciales invalidas")));
    }

    // Records como DTOs internos (inmutables)
    public record RegisterRequest(String username, String email, String password) {
    }

    public record LoginRequest(String email, String password) {
    }
}
