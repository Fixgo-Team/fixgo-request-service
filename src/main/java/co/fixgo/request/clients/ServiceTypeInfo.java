package co.fixgo.request.clients;

import java.util.UUID;

/**
 * Datos del subservicio que este servicio necesita del catálogo.
 *
 * @param id            identificador del subservicio en fixgo-catalog-service
 * @param active        false si el administrador lo desactivó (RF2.4)
 * @param urgentAllowed true si admite modo urgente (RN-03)
 */
public record ServiceTypeInfo(UUID id, boolean active, boolean urgentAllowed) {
}
