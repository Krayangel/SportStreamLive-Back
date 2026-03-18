package com.sportstreamlive.streaming.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Perfil de usuario: racha, puntos y metas (E2 - Dashboard).
 * Separado de User para permitir crecimiento independiente
 * sin modificar la entidad principal.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user_profiles")
public class UserProfile {

    @Id
    private String id;

    private String userId;

    /** Dias consecutivos de actividad registrada */
    private int racha;

    /** Puntos acumulados por completar retos y eventos */
    private int puntosTotales;

    /** Descripcion libre de las metas del usuario */
    private String metas;
}
