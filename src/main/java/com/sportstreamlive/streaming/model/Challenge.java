package com.sportstreamlive.streaming.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * Reto deportivo (E4 - Retos).
 * Ejemplo: "30 dias de ejercicio".
 * Contiene la lista de userIds participantes.
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

    /** Duracion en dias del reto, ej: 30 */
    private int duracionDias;

    private String creatorId;

    /** Lista de userIds que se han unido al reto */
    private List<String> participantes = new ArrayList<>();
}
