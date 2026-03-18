package com.sportstreamlive.streaming.service;

import com.sportstreamlive.streaming.model.StreamEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Gestiona el ciclo de vida de los streams en vivo (E5 - Transmitir).
 *
 * CONCURRENCIA:
 * - ConcurrentHashMap mantiene las sesiones activas sin race conditions.
 * - AtomicBoolean controla el inicio/fin del thread de cada stream de forma
 *   segura: set(false) detiene el loop del thread sin necesidad de interrupt().
 * - Cada stream tiene su propio Thread dedicado.
 */
@Slf4j
@Component
public class StreamSessionManager {

    /** Mapa de sesiones activas: streamId -> sesion */
    private final ConcurrentHashMap<String, StreamSession> activeSessions = new ConcurrentHashMap<>();

    private final SimpMessagingTemplate messagingTemplate;

    public StreamSessionManager(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Inicia un nuevo stream.
     * Crea un StreamSession con un thread dedicado que mantiene el stream vivo
     * y hace un heartbeat cada segundo para notificar a los suscriptores.
     *
     * @param streamId ID del stream (vinculado a evento o reto)
     * @param userId   ID del usuario que transmite
     */
    public void startStream(String streamId, String userId) {
        if (activeSessions.containsKey(streamId)) {
            log.warn("Stream {} ya esta activo, ignorando solicitud de inicio", streamId);
            return;
        }

        AtomicBoolean active = new AtomicBoolean(true);

        Thread streamThread = new Thread(() -> {
            log.info("Stream {} iniciado por usuario {}", streamId, userId);

            // Notificar a los suscriptores que el stream comenzo
            broadcastStatus(streamId, userId, "STARTED");

            // Loop de vida del stream: corre mientras 'active' sea true
            while (active.get()) {
                try {
                    // Heartbeat cada 2 segundos para que el frontend sepa que el stream sigue vivo
                    Thread.sleep(2000);
                    if (active.get()) {
                        broadcastStatus(streamId, userId, "ALIVE");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            log.info("Stream {} terminado para usuario {}", streamId, userId);
            broadcastStatus(streamId, userId, "ENDED");
            activeSessions.remove(streamId);
        });

        streamThread.setName("stream-" + streamId);
        streamThread.setDaemon(true); // No bloquea el shutdown del servidor

        StreamSession session = new StreamSession(streamId, userId, active, streamThread);
        activeSessions.put(streamId, session);
        streamThread.start();
    }

    /**
     * Detiene un stream activo de forma segura.
     * Cambia AtomicBoolean a false, el thread termina su proximo ciclo.
     *
     * @param streamId ID del stream a detener
     */
    public void stopStream(String streamId) {
        StreamSession session = activeSessions.get(streamId);
        if (session != null) {
            session.getActive().set(false); // Senal de parada atomica
            log.info("Solicitud de parada enviada al stream {}", streamId);
        } else {
            log.warn("Intento de detener stream inexistente: {}", streamId);
        }
    }

    /**
     * Transmite un evento de datos (GPS, bpm, etc.) a todos los suscriptores del stream.
     *
     * @param event Evento con el payload de datos
     */
    public void broadcastData(StreamEvent event) {
        if (!activeSessions.containsKey(event.getStreamId())) {
            log.warn("Intento de broadcast en stream inactivo: {}", event.getStreamId());
            return;
        }
        messagingTemplate.convertAndSend(
                "/topic/stream/" + event.getStreamId(), event);
    }

    public boolean isActive(String streamId) {
        return activeSessions.containsKey(streamId);
    }

    // --- Interno ---

    private void broadcastStatus(String streamId, String userId, String status) {
        StreamEvent statusEvent = new StreamEvent(streamId, userId, status, null);
        messagingTemplate.convertAndSend("/topic/stream/" + streamId, statusEvent);
    }

    // --- Inner class que representa una sesion ---
    public static class StreamSession {
        private final String streamId;
        private final String userId;
        private final AtomicBoolean active;
        private final Thread streamThread;

        public StreamSession(String streamId, String userId, AtomicBoolean active, Thread streamThread) {
            this.streamId = streamId;
            this.userId = userId;
            this.active = active;
            this.streamThread = streamThread;
        }

        public AtomicBoolean getActive() { return active; }
        public String getStreamId() { return streamId; }
        public String getUserId() { return userId; }
    }
}
