package com.sportstreamlive.streaming.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reto deportivo.
 *
 * Nuevos campos:
 *  - dificultad: FACIL | MEDIA | DIFICIL
 *  - xpRecompensa: calculado según dificultad al crear
 *  - evidencias: Map<userId, List<String>> — historial de evidencias
 *  - progresoDiario: Map<userId, Map<fecha(yyyy-MM-dd), String>> — 1 entrada por día por usuario
 *  - diasCompletados: Map<userId, Integer> — días marcados por usuario
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "challenges")
public class Challenge {

    @Id
    private String id;

    private String nombre;
    private String descripcion;

    /** Duración en días del reto, ej: 30 */
    private int duracionDias;

    private String creatorId;

    /**
     * Dificultad del reto.
     * FACIL  → 50 XP
     * MEDIA  → 150 XP
     * DIFICIL → 300 XP
     */
    private Dificultad dificultad = Dificultad.MEDIA;

    /** XP que otorga al completarlo — se fija al crear según dificultad */
    private int xpRecompensa = 150;

    /** Lista de userIds participantes */
    private List<String> participantes = new ArrayList<>();

    /**
     * Evidencias libres por usuario (texto + fecha, sin restricción de 1/día).
     * Key: userId → Value: lista de entradas "[fecha] texto"
     */
    private Map<String, List<String>> evidencias = new HashMap<>();

    /**
     * Progreso diario estructurado: 1 entrada editable por día por usuario.
     * Key: userId → Value: Map<"yyyy-MM-dd", texto>
     * Si el reto dura 30 días, el usuario puede tener máximo 30 entradas.
     */
    private Map<String, Map<String, String>> progresoDiario = new HashMap<>();

    public enum Dificultad {
        FACIL, MEDIA, DIFICIL;

        public int getXp() {
            return switch (this) {
                case FACIL   -> 50;
                case MEDIA   -> 150;
                case DIFICIL -> 300;
            };
        }

        public String getLabel() {
            return switch (this) {
                case FACIL   -> "Fácil";
                case MEDIA   -> "Media";
                case DIFICIL -> "Difícil";
            };
        }
    }
}
