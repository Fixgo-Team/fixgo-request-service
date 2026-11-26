package co.fixgo.request.security;

import co.fixgo.request.exceptions.ForbiddenOperationException;
import java.util.Locale;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Lee el actor de los claims del JWT que Spring Security ya validó (firma, emisor y vencimiento).
 */
@Component
public class JwtCurrentActorProvider implements CurrentActorProvider {

    private final FixgoSecurityProperties properties;

    public JwtCurrentActorProvider(FixgoSecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    public CurrentActor current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken token)) {
            throw new AuthenticationCredentialsNotFoundException("No hay un token válido en la llamada.");
        }
        Jwt jwt = token.getToken();
        return new CurrentActor(userId(jwt), role(jwt));
    }

    private Long userId(Jwt jwt) {
        Object claim = jwt.getClaims().get(properties.userIdClaim());
        try {
            if (claim instanceof Number number) {
                return number.longValue();
            }
            if (claim instanceof String text) {
                return Long.valueOf(text.trim());
            }
        } catch (NumberFormatException ignored) {
            // se trata igual que un claim ausente
        }
        throw new ForbiddenOperationException("El token no identifica una cuenta de FixGo.");
    }

    private ActorRole role(Jwt jwt) {
        Object claim = jwt.getClaims().get(properties.roleClaim());
        if (claim instanceof String text) {
            try {
                return ActorRole.valueOf(text.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                // rol desconocido: se rechaza abajo
            }
        }
        throw new ForbiddenOperationException("El token no trae un rol válido.");
    }
}
