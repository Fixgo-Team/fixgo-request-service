package co.fixgo.request.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.fixgo.request.exceptions.ForbiddenOperationException;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class JwtCurrentActorProviderTest {

    private final JwtCurrentActorProvider provider =
            new JwtCurrentActorProvider(new FixgoSecurityProperties("userId", "role"));

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void readsTheActorFromTheClaims() {
        authenticate(Map.of("userId", 55, "role", "CLIENT"));

        assertThat(provider.current()).isEqualTo(new CurrentActor(55L, ActorRole.CLIENT));
    }

    @Test
    void acceptsTextualIdsAndLowercaseRoles() {
        authenticate(Map.of("userId", " 77 ", "role", "provider"));

        assertThat(provider.current()).isEqualTo(new CurrentActor(77L, ActorRole.PROVIDER));
    }

    @Test
    void usesTheConfiguredClaimNames() {
        JwtCurrentActorProvider custom = new JwtCurrentActorProvider(new FixgoSecurityProperties("uid", "rol"));
        authenticate(Map.of("uid", 3L, "rol", "ADMIN"));

        assertThat(custom.current()).isEqualTo(new CurrentActor(3L, ActorRole.ADMIN));
    }

    @Test
    void withoutAuthenticationItAsksForCredentials() {
        assertThatThrownBy(provider::current).isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    void missingOrInvalidUserIdIsForbidden() {
        authenticate(Map.of("role", "CLIENT"));
        assertThatThrownBy(provider::current).isInstanceOf(ForbiddenOperationException.class);

        authenticate(Map.of("userId", "abc", "role", "CLIENT"));
        assertThatThrownBy(provider::current).isInstanceOf(ForbiddenOperationException.class);

        authenticate(Map.of("userId", true, "role", "CLIENT"));
        assertThatThrownBy(provider::current).isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void missingOrUnknownRoleIsForbidden() {
        authenticate(Map.of("userId", 55));
        assertThatThrownBy(provider::current).isInstanceOf(ForbiddenOperationException.class);

        authenticate(Map.of("userId", 55, "role", "SUPERUSER"));
        assertThatThrownBy(provider::current).isInstanceOf(ForbiddenOperationException.class);
    }

    private void authenticate(Map<String, Object> claims) {
        Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(60), Map.of("alg", "none"), claims);
        SecurityContextHolder.getContext()
                .setAuthentication(new JwtAuthenticationToken(jwt, AuthorityUtils.NO_AUTHORITIES));
    }
}
