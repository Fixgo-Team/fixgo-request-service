package co.fixgo.request.config;

import co.fixgo.request.security.ApiSecurityErrorHandler;
import co.fixgo.request.security.FixgoSecurityProperties;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Servicio de recursos OAuth2: valida el JWT de Firebase (firma, emisor, audiencia y vencimiento; RNF-15).
 * Todo endpoint exige token salvo Swagger y la salud. El permiso por rol y por propiedad de cada operación
 * se verifica en la capa Service (RNF-16); aquí solo se exige estar autenticado.
 * Sin estado en memoria entre llamadas (RNF-29): no hay sesión HTTP.
 */
@Configuration
@EnableConfigurationProperties(FixgoSecurityProperties.class)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ApiSecurityErrorHandler errorHandler,
                                                   FixgoSecurityProperties properties) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter(properties))));
        return http.build();
    }

    /** El rol del claim configurado se publica como autoridad ROLE_x, para reglas como hasRole("ADMIN"). */
    private JwtAuthenticationConverter jwtAuthenticationConverter(FixgoSecurityProperties properties) {
        Converter<Jwt, Collection<GrantedAuthority>> authorities = jwt -> {
            Object role = jwt.getClaims().get(properties.roleClaim());
            if (role instanceof String text && !text.isBlank()) {
                return List.of(new SimpleGrantedAuthority("ROLE_" + text.trim().toUpperCase(Locale.ROOT)));
            }
            return List.of();
        };
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}
