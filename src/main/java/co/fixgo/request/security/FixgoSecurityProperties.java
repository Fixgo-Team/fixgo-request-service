package co.fixgo.request.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Nombres de los claims del JWT. Cómo el sujeto de Firebase se asocia a la cuenta numérica y dónde viaja el rol
 * está pendiente (Q-004), por eso son configurables; los valores por defecto son provisionales.
 *
 * @param userIdClaim claim con el id numérico de la cuenta
 * @param roleClaim   claim con el rol: CLIENT, PROVIDER, ADMIN o SERVICE
 */
@ConfigurationProperties(prefix = "fixgo.security")
public record FixgoSecurityProperties(@DefaultValue("userId") String userIdClaim,
                                      @DefaultValue("role") String roleClaim) {
}
