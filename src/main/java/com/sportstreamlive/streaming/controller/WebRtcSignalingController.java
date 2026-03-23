package com.sportstreamlive.streaming.controller;

import com.sportstreamlive.streaming.model.WebRtcSignal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

/**
 * Signaling WebRTC sobre STOMP — soporta N viewers simultáneos.
 *
 * Flujo para múltiples viewers:
 * 1. Viewer envía JOIN → el dueño recibe JOIN y manda OFFER dirigido a ese viewer
 * 2. Viewer recibe OFFER (con targetUserId = su userId) → manda ANSWER
 * 3. Dueño recibe ANSWER → conexión establecida con ese viewer
 * 4. Cada par dueño↔viewer intercambia ICE candidates etiquetados con targetUserId
 *
 * El campo targetUserId en WebRtcSignal permite rutear señales punto a punto
 * incluso a través del broadcast del topic.
 */
@Slf4j
@Controller
public class WebRtcSignalingController {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Registro de viewers activos por stream.
     * Key: streamId → Value: lista de userIds de viewers conectados
     */
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<String>> streamViewers =
            new ConcurrentHashMap<>();

    public WebRtcSignalingController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Recibe señales WebRTC de cualquier participante y las broadcast
     * al topic del stream. Cada cliente filtra por targetUserId.
     *
     * Cliente envía:  /app/webrtc/{streamId}/signal
     * Todos escuchan: /topic/webrtc/{streamId}
     */
    @MessageMapping("/webrtc/{streamId}/signal")
    public void signal(@DestinationVariable String streamId,
                       @Payload WebRtcSignal signal) {

        if (!StringUtils.hasText(streamId) || signal == null
                || !StringUtils.hasText(signal.getType())) {
            return;
        }

        signal.setStreamId(streamId);

        // Registrar/desregistrar viewers
        if ("JOIN".equals(signal.getType()) && StringUtils.hasText(signal.getSenderUserId())) {
            streamViewers.computeIfAbsent(streamId, k -> new CopyOnWriteArrayList<>())
                    .add(signal.getSenderUserId());
            log.debug("Viewer {} se unió al stream {}", signal.getSenderUserId(), streamId);
        }

        if ("LEAVE".equals(signal.getType()) && StringUtils.hasText(signal.getSenderUserId())) {
            List<String> viewers = streamViewers.get(streamId);
            if (viewers != null) viewers.remove(signal.getSenderUserId());
            log.debug("Viewer {} salió del stream {}", signal.getSenderUserId(), streamId);
        }

        // Broadcast al topic — todos los suscritos reciben la señal
        // El front filtra: ignora señales que no van dirigidas a él (targetUserId)
        messagingTemplate.convertAndSend("/topic/webrtc/" + streamId, signal);

        log.debug("WebRTC [{}] stream={} from={} to={}",
                signal.getType(), streamId,
                signal.getSenderUserId(), signal.getTargetUserId());
    }

    /** Devuelve los viewers activos de un stream (para debug/admin) */
    public List<String> getViewers(String streamId) {
        return streamViewers.getOrDefault(streamId, new CopyOnWriteArrayList<>());
    }
}
