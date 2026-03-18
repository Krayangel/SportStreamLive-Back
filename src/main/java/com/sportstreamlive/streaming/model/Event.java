package com.sportstreamlive.streaming.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Evento deportivo (E3 - Eventos).
 * Puede ser presencial o virtual.
 * El campo 'asistentes' es la lista de userIds inscritos.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "events")
public class Event {

    @Id
    private String id;

    private String titulo;
    private String descripcion;

    /** PRESENCIAL o VIRTUAL */
    private EventType tipo;

    private LocalDateTime fecha;
    private String creatorId;

    /** Lista de userIds inscritos */
    private List<String> asistentes = new ArrayList<>();

    public enum EventType {
        PRESENCIAL, VIRTUAL
    }
}
