package com.sportstreamlive.streaming.controller;

import com.sportstreamlive.streaming.model.StreamEvent;
import com.sportstreamlive.streaming.service.StreamSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * E5 - Streaming en Vivo via WebSocket (CORE).
 *
 * CONCURRENCIA:
 * - Delega a StreamSessionManager que maneja:
 *   - Un Thread dedicado por stream (ciclo de vida + heartbeat)
 *   - AtomicBoolean para inicio/parada segura del thread
 *   - ConcurrentHashMap para registro de sesiones activas
 *
 * Flujo WebSocket:
 *   /app/stream/{streamId}/start  -> inicia thread del stream
 *   /app/stream/{streamId}/data   -> envia datos en vivo (GPS, bpm, etc.)
 *   /app/stream/{streamId}/stop   -> detiene thread del stream con AtomicBoolean
 *
 * Suscripcion del cliente: /topic/stream/{streamId}
 *
 * Endpoints REST auxiliares:
 *   POST /api/streaming/{streamId}/start?userId=...
 *   POST /api/streaming/{streamId}/stop
 *   GET  /api/streaming/{streamId}/active
 */
@Slf4j
@RestController
@RequestMapping("/api/streaming")
@RequiredArgsConstructor
public class StreamingController {

    private final StreamSessionManager sessionManager;

    // --- WebSocket handlers ---

    /** Inicia el stream via WebSocket: /app/stream/{streamId}/start */
    @MessageMapping("/stream/{streamId}/start")
    public void startStreamWs(@DestinationVariable String streamId,
                              @Payload StreamEvent event) {
        if (!StringUtils.hasText(streamId) || event == null || !StringUtils.hasText(event.getUserId())) {
            log.warn("Solicitud WS start invalida. streamId='{}' userId='{}'", streamId, event == null ? null : event.getUserId());
            return;
        }
        sessionManager.startStream(streamId, event.getUserId());
    }

    /** Envia datos en vivo via WebSocket: /app/stream/{streamId}/data */
    @MessageMapping("/stream/{streamId}/data")
    public void sendDataWs(@DestinationVariable String streamId,
                           @Payload StreamEvent event) {
        if (!StringUtils.hasText(streamId) || event == null) {
            return;
        }
        event.setStreamId(streamId);
        sessionManager.broadcastData(event);
    }

    /** Detiene el stream via WebSocket: /app/stream/{streamId}/stop */
    @MessageMapping("/stream/{streamId}/stop")
    public void stopStreamWs(@DestinationVariable String streamId,
                             @Payload StreamEvent event) {
        if (!StringUtils.hasText(streamId) || event == null || !StringUtils.hasText(event.getUserId())) {
            log.warn("Solicitud WS stop invalida. streamId='{}' userId='{}'", streamId, event == null ? null : event.getUserId());
            return;
        }
        sessionManager.stopStream(streamId, event.getUserId());
    }

    // --- REST endpoints auxiliares ---

    /** POST /api/streaming/{streamId}/start?userId=... */
    @PostMapping("/{streamId}/start")
    public ResponseEntity<Map<String, String>> startStream(@PathVariable String streamId,
                                                           @RequestParam String userId) {
        if (!StringUtils.hasText(streamId)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "INVALID_STREAM_ID",
                    "message", "streamId no puede estar vacio"
            ));
        }
        if (!StringUtils.hasText(userId)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "INVALID_USER_ID",
                    "message", "userId no puede estar vacio"
            ));
        }

        boolean started = sessionManager.startStream(streamId, userId);
        if (!started) {
            return ResponseEntity.ok(Map.of(
                "status", "ALREADY_ACTIVE",
                "streamId", streamId,
                "message", "El stream ya estaba activo"
            ));
        }
        return ResponseEntity.ok(Map.of(
            "status", "STARTED",
            "streamId", streamId,
            "message", "Stream iniciado"
        ));
    }

    /** POST /api/streaming/{streamId}/stop?userId=... */
    @PostMapping("/{streamId}/stop")
    public ResponseEntity<Map<String, String>> stopStream(@PathVariable String streamId,
                                                          @RequestParam String userId) {
        if (!StringUtils.hasText(streamId)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "INVALID_STREAM_ID",
                    "message", "streamId no puede estar vacio"
            ));
        }
        if (!StringUtils.hasText(userId)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "INVALID_USER_ID",
                    "message", "userId no puede estar vacio"
            ));
        }

        StreamSessionManager.StopResult result = sessionManager.stopStream(streamId, userId);
        if (result == StreamSessionManager.StopResult.NOT_ACTIVE) {
            return ResponseEntity.ok(Map.of(
                    "status", "NOT_ACTIVE",
                    "streamId", streamId,
                    "message", "No habia stream activo para detener"
            ));
        }
        if (result == StreamSessionManager.StopResult.FORBIDDEN_OWNER_MISMATCH) {
            return ResponseEntity.status(403).body(Map.of(
                    "status", "FORBIDDEN_OWNER_MISMATCH",
                    "streamId", streamId,
                    "message", "Solo el usuario que inicio el stream puede detenerlo"
            ));
        }
        return ResponseEntity.ok(Map.of(
                "status", "STOP_REQUESTED",
                "streamId", streamId,
                "message", "Solicitud de parada enviada"
        ));
    }

    /** GET /api/streaming/{streamId}/active */
    @GetMapping("/{streamId}/active")
    public ResponseEntity<Map<String, Object>> isActive(@PathVariable String streamId) {
        if (!StringUtils.hasText(streamId)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "INVALID_STREAM_ID",
                    "message", "streamId no puede estar vacio"
            ));
        }
        return ResponseEntity.ok(Map.of("streamId", streamId, "active", sessionManager.isActive(streamId)));
    }

    /** GET /api/streaming/active */
    @GetMapping("/active")
    public ResponseEntity<Map<String, Object>> listActiveStreams() {
        return ResponseEntity.ok(Map.of(
                "count", sessionManager.getActiveStreams().size(),
                "streams", sessionManager.getActiveStreams()
        ));
    }
}
