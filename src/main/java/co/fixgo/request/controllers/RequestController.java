package co.fixgo.request.controllers;

import co.fixgo.request.dto.ApiResponse;
import co.fixgo.request.dto.CancelRequestDTO;
import co.fixgo.request.dto.CreateRequestDTO;
import co.fixgo.request.dto.RequestResponseDTO;
import co.fixgo.request.dto.UpdateRequestDTO;
import co.fixgo.request.models.RequestStatus;
import co.fixgo.request.services.RequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Endpoints de solicitudes. Solo recibe DTO y responde ApiResponse; no tiene try-catch:
 * los errores los atrapa GlobalExceptionHandler.
 */
@RestController
@RequestMapping("/api/v1/requests")
@Tag(name = "Requests", description = "Solicitudes urgentes y programadas (módulo 3)")
public class RequestController {

    private final RequestService requestService;

    public RequestController(RequestService requestService) {
        this.requestService = requestService;
    }

    @GetMapping
    @Operation(summary = "Listar solicitudes", description = "RF4.1. Filtros opcionales por cliente y estado.")
    public ResponseEntity<ApiResponse<List<RequestResponseDTO>>> findAll(
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) RequestStatus status) {
        return ResponseEntity.ok(new ApiResponse<>(requestService.findAll(clientId, status)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar una solicitud", description = "RF4.6.")
    public ResponseEntity<ApiResponse<RequestResponseDTO>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(requestService.findById(id)));
    }

    @PostMapping
    @Operation(summary = "Crear una solicitud",
            description = "RF3.1 (URGENT, requiere latitud y longitud) y RF3.2 (SCHEDULED, requiere dirección y zona). "
                    + "Responde 201 con la cabecera Location de la solicitud creada.")
    public ResponseEntity<ApiResponse<RequestResponseDTO>> create(@Valid @RequestBody CreateRequestDTO dto) {
        RequestResponseDTO created = requestService.create(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(new ApiResponse<>(created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar una solicitud", description = "RF3.3. Solo en estado PENDING u OPEN.")
    public ResponseEntity<ApiResponse<RequestResponseDTO>> update(@PathVariable Long id,
                                                                  @Valid @RequestBody UpdateRequestDTO dto) {
        return ResponseEntity.ok(new ApiResponse<>(requestService.update(id, dto)));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancelar una solicitud",
            description = "RF3.4 y RN-08. Borrado lógico: pasa a CANCELLED con motivo; nunca se elimina.")
    public ResponseEntity<ApiResponse<RequestResponseDTO>> cancel(@PathVariable Long id,
                                                                  @Valid @RequestBody CancelRequestDTO dto) {
        return ResponseEntity.ok(new ApiResponse<>(requestService.cancel(id, dto)));
    }
}
