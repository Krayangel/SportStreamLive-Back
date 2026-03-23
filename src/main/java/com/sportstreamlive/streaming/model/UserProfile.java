package com.sportstreamlive.streaming.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Perfil de usuario: racha, XP, metas privadas.
 *
 * Nuevos campos:
 *  - ultimaActividad: fecha de la última vez que el usuario registró actividad
 *  - rachaActual: días consecutivos con actividad
 *  - xpTotal: XP acumulado (suma de todos los retos completados + logros)
 *  - metas: lista estructurada de metas privadas (solo visibles para el dueño)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user_profiles")
public class UserProfile {

    @Id
    private String id;

    private String userId;

    /** Días consecutivos de actividad — se actualiza automáticamente */
    private int rachaActual = 0;

    /** Fecha de la última actividad registrada (para calcular racha) */
    private LocalDate ultimaActividad;

    /** XP total acumulado */
    private int xpTotal = 0;

    /**
     * Metas privadas del usuario — nadie más puede verlas.
     * Lista de texto libre, cada entrada es una meta.
     */
    private List<String> metas = new ArrayList<>();

    // ── Compatibilidad con código anterior ──────────────────
    // Los campos racha y puntosTotales se mapean a los nuevos
    public int getRacha() { return rachaActual; }
    public void setRacha(int r) { this.rachaActual = r; }
    public int getPuntosTotales() { return xpTotal; }
    public void setPuntosTotales(int p) { this.xpTotal = p; }
}
