package co.fixgo.request.services;

import co.fixgo.request.dto.CancelRequestDTO;
import co.fixgo.request.dto.CreateRequestDTO;
import co.fixgo.request.dto.RequestResponseDTO;
import co.fixgo.request.dto.UpdateRequestDTO;
import co.fixgo.request.models.RequestStatus;
import co.fixgo.request.security.CurrentActor;
import java.util.List;

/**
 * Operaciones de negocio sobre las solicitudes (módulo 3 del SRS v3.1).
 * Cada operación recibe al actor y verifica su rol y su relación con la solicitud (RNF-16).
 */
public interface RequestService {

    /** RF3.1 y RF3.2: un cliente crea una solicitud urgente (PENDING) o programada (OPEN); el dueño es el actor. */
    RequestResponseDTO create(CreateRequestDTO dto, CurrentActor actor);

    /**
     * RF4.1: lista solicitudes con filtros opcionales. El cliente ve solo las suyas, el profesional solo las
     * que tiene asignadas y el administrador todas (puede filtrar por cliente).
     */
    List<RequestResponseDTO> findAll(Long clientId, RequestStatus status, CurrentActor actor);

    /** RF4.6: detalle de una solicitud; solo para su dueño, su profesional asignado o un administrador. */
    RequestResponseDTO findById(Long id, CurrentActor actor);

    /** RF3.3: el cliente dueño modifica una solicitud antes de que sea aceptada. */
    RequestResponseDTO update(Long id, UpdateRequestDTO dto, CurrentActor actor);

    /** RF3.4 y RN-08: cancela con motivo; nunca se borra físicamente. */
    RequestResponseDTO cancel(Long id, CancelRequestDTO dto, CurrentActor actor);
}
