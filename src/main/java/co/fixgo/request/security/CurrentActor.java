package co.fixgo.request.security;

/**
 * Quién hace la llamada, tomado del JWT ya validado; nunca del cuerpo de la petición (A01 de OWASP).
 *
 * @param userId identificador numérico de la cuenta (BD-02)
 * @param role   rol de la cuenta
 */
public record CurrentActor(Long userId, ActorRole role) {

    public boolean is(ActorRole expected) {
        return role == expected;
    }
}
