package com.sportstreamlive.streaming.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Medalla/logro de un usuario (E6 - Logros).
 *
 * La logica de "atrapar" una medalla esta en BadgeService y usa
 * AtomicBoolean + compareAndSet para que solo UN usuario la obtenga
 * aunque multiples hagan click al mismo tiempo (race condition).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "badges")
public class Badge {

    @Id
    private String id;

    private String userId;

    /**
     * Tipo de medalla:
     * PRIMER_LOGRO, INSCRIPCION_EVENTO, UNIRSE_RETO, etc.
     */
    private String tipo;

    private String nombre;
    private String descripcion;
    private LocalDateTime fechaObtenida;
}
