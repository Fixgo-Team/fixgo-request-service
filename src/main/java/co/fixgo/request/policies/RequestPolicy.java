package co.fixgo.request.policies;

import co.fixgo.request.clients.ServiceTypeInfo;
import co.fixgo.request.models.ServiceRequest;

/**
 * Regla que depende del subservicio pedido (por ejemplo, los datos del vehículo en desvare, RF3.1).
 * El servicio aplica todas las políticas registradas, así un subservicio nuevo con reglas propias
 * se agrega con otra clase sin tocar RequestServiceImpl (principio O de SOLID).
 */
public interface RequestPolicy {

    /**
     * Verifica la solicitud ya normalizada contra las reglas del subservicio.
     *
     * @throws co.fixgo.request.exceptions.InvalidRequestDataException si la solicitud no cumple la regla
     */
    void validate(ServiceRequest request, ServiceTypeInfo serviceType);
}
