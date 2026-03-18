package com.sportstreamlive.streaming.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.util.List;

/**
 * Entidad principal de usuario (E1 - Autenticacion).
 *
 * El campo 'provider' permite distinguir usuarios locales de los que
 * iniciaran sesion con OAuth2 (Microsoft/Google) en futuras iteraciones,
 * sin necesidad de modificar esta clase (principio Abierto/Cerrado).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    @Indexed(unique = true)
    private String email;

    /** Hash bcrypt de la contrasena (solo para provider LOCAL) */
    private String passwordHash;

    /** Roles del usuario, ej: ["ROLE_USER", "ROLE_ADMIN"] */
    private List<String> roles;

    /**
     * Proveedor de autenticacion.
     * LOCAL  -> registro manual con email/password
     * GOOGLE -> OAuth2 Google (futuro)
     * MICROSOFT -> OAuth2 Azure AD (futuro)
     */
    private AuthProvider provider;

    public enum AuthProvider {
        LOCAL, GOOGLE, MICROSOFT
    }
}
