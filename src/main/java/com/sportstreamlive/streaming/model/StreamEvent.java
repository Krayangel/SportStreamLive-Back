package com.sportstreamlive.streaming.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Map;

/**
 * Value Object para eventos de streaming en vivo (E5 - Transmitir).
 * NO se persiste en MongoDB: se transmite directamente via WebSocket.
 *
 * El campo 'payload' es flexible (Map) para soportar distintos tipos
 * de datos sin modificar el modelo:
 *   - Ejercicio en vivo: { lat, lng, bpm, velocidad }
 *   - Estado del stream: { status: "LIVE" | "ENDED" }
 *
 * Concurrencia: StreamSessionManager gestiona el thread del stream.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StreamEvent {

    /** ID unico del stream (vinculado a un Event o Challenge) */
    private String streamId;

    /** userId del usuario que transmite */
    private String userId;

    /** Tipo de evento: DATA, START, STOP */
    private String tipo;

    /** Datos del evento (lat, lng, bpm, etc.) */
    private Map<String, Object> payload;
}
