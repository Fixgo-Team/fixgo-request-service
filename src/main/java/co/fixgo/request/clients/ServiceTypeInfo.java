package co.fixgo.request.clients;

import java.math.BigDecimal;

/**
 * Datos del subservicio que este servicio necesita del catálogo.
 *
 * @param id                            identificador del subservicio en catalog-provider-service
 * @param active                        false si el administrador lo desactivó (RF2.4)
 * @param urgentAllowed                 true si admite modo urgente (RN-03)
 * @param referencePrice                precio de referencia en COP; en urgente es el valor acordado (RN-09)
 * @param requiresOwnershipConfirmation true si el profesional debe confirmar la propiedad antes de empezar (RN-07)
 * @param requiresVehicleData           true si la solicitud debe traer placa, marca y tipo de vehículo (RF3.1)
 */
public record ServiceTypeInfo(Long id, boolean active, boolean urgentAllowed, BigDecimal referencePrice,
                              boolean requiresOwnershipConfirmation, boolean requiresVehicleData) {
}
