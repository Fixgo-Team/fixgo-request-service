package co.fixgo.request.services;

import co.fixgo.request.dto.CancelRequestDTO;
import co.fixgo.request.dto.CreateRequestDTO;
import co.fixgo.request.dto.RequestResponseDTO;
import co.fixgo.request.dto.UpdateRequestDTO;
import co.fixgo.request.models.RequestStatus;
import java.util.List;

/**
 * Operaciones de negocio sobre las solicitudes (módulo 3 del SRS v3.1).
 */
public interface RequestService {

    /** RF3.1 y RF3.2: crea una solicitud urgente (PENDING) o programada (OPEN). */
    RequestResponseDTO create(CreateRequestDTO dto);

    /** RF4.1: lista solicitudes, con filtros opcionales por cliente y estado. */
    List<RequestResponseDTO> findAll(Long clientId, RequestStatus status);

    /** RF4.6: detalle de una solicitud. */
    RequestResponseDTO findById(Long id);

    /** RF3.3: modifica una solicitud antes de que sea aceptada. */
    RequestResponseDTO update(Long id, UpdateRequestDTO dto);

    /** RF3.4 y RN-08: cancela con motivo; nunca se borra físicamente. */
    RequestResponseDTO cancel(Long id, CancelRequestDTO dto);
}
