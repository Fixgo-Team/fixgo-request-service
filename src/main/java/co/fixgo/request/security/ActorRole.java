package co.fixgo.request.security;

/**
 * Roles con los que se llama a este servicio (RNF-16).
 */
public enum ActorRole {

    CLIENT,

    PROVIDER,

    ADMIN,

    /** Servicios internos (dispatch, technical-execution). Provisional hasta definir la autenticación entre servicios (Q-016). */
    SERVICE
}
