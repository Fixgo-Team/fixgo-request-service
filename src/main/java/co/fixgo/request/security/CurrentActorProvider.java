package co.fixgo.request.security;

/**
 * Entrega la identidad de quien hace la llamada. Se usa a través de esta interfaz para que el servicio
 * y los controladores no dependan de Spring Security ni del formato del token.
 */
public interface CurrentActorProvider {

    /**
     * @return el actor autenticado
     * @throws org.springframework.security.core.AuthenticationException si no hay un JWT válido en la llamada
     * @throws co.fixgo.request.exceptions.ForbiddenOperationException   si el token no trae un rol o una cuenta válidos
     */
    CurrentActor current();
}
