package com.sportstreamlive.streaming.config;

import com.sportstreamlive.streaming.security.JwtFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuracion de seguridad.
 *
 * Diseño Abierto/Cerrado: las rutas publicas y el filtro JWT estan aqui.
 * Para agregar OAuth2 (Microsoft/Google) basta con anadir
 * .oauth2Login(...) en el filterChain SIN modificar la logica existente.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Endpoints publicos
                .requestMatchers("/api/auth/**").permitAll()
                // WebSocket endpoint: SockJS hace peticiones HTTP antes del handshake
                .requestMatchers("/ws/**").permitAll()
                // Frontend estatico (index.html de prueba)
                .requestMatchers("/", "/index.html", "/*.js", "/*.css").permitAll()
                // Todo lo demas requiere autenticacion
                .anyRequest().authenticated()
            )
            // JWT filter antes del filtro de usuario/password de Spring
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
            /*
             * EXTENSION FUTURA (no modifica lo anterior):
             * .oauth2Login(oauth2 -> oauth2
             *     .successHandler(oAuth2SuccessHandler)
             * )
             */

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
