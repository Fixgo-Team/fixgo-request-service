# fixgo-request-service

Microservicio de solicitudes de FixGo: solicitudes urgentes y programadas, cotizaciones y mensajes.

| Dato | Valor |
|---|---|
| Módulos del SRS v3.0 | 3 (RF3.1–RF3.9) y RF5.4 (mensajes) |
| Puerto | `8084` |
| Base de datos | MySQL 8 · `fixgo_request` |
| Stack | Java 17, Spring Boot 4.1, Spring Data JPA, Flyway, springdoc-openapi |
| Responsable | Mateo Esteban Ramirez Garzon |

## Requisitos

- JDK 17 o superior
- Maven 3.9 o superior
- Docker y Docker Compose

## Ejecutar con Docker

```bash
cp .env.example .env      # completa las contraseñas
docker compose up --build
```

## Ejecutar en local (sin Docker para la API)

```bash
docker compose up -d request-db
mvn spring-boot:run
```

## Pruebas

```bash
mvn verify
```

## Endpoints de solicitudes

| Método | Ruta | Requisito | Respuesta OK |
|---|---|---|---|
| `GET` | `/api/v1/requests?clientId=&status=` | RF4.1 | 200 |
| `GET` | `/api/v1/requests/{id}` | RF4.6 | 200 |
| `POST` | `/api/v1/requests` | RF3.1, RF3.2 | 201 |
| `PUT` | `/api/v1/requests/{id}` | RF3.3 (solo `PENDING` u `OPEN`) | 200 |
| `PATCH` | `/api/v1/requests/{id}/cancel` | RF3.4, RN-08 (borrado lógico) | 200 |

Ejemplo para probar en Swagger (`POST`, solicitud urgente):

```json
{
  "clientId": "3f1c2a4e-8b7d-4c6a-9e21-5d4b3a2c1f00",
  "serviceTypeId": "7a9e1b2c-3d4f-4a5b-8c6d-1e2f3a4b5c6d",
  "mode": "URGENT",
  "description": "El carro no enciende",
  "latitude": 2.9273,
  "longitude": -75.2819,
  "vehiclePlate": "ABC123",
  "vehicleBrand": "Chevrolet",
  "vehicleType": "Automóvil"
}
```

> Mientras `catalog-service` y `notification-service` publican su API, se usan `FakeCatalogClient` y `FakeNotificationClient` (paquete `clients`). Se reemplazan sin tocar el servicio ni el controlador.

## Endpoints de soporte

| Recurso | URL |
|---|---|
| Swagger UI | http://localhost:8084/swagger-ui.html |
| Contrato OpenAPI | http://localhost:8084/v3/api-docs |
| Salud | http://localhost:8084/actuator/health |

## Estructura de paquetes

Se sigue la misma convención de los proyectos anteriores del equipo (paquetes en plural):

```
co.fixgo.request
├── controllers    REST: solo recibe DTO y responde DTO       → RequestController
├── dto            objetos de entrada y salida                → ApiResponse, CreateRequestDTO
├── mappers        DTO ↔ entidad                              → RequestMapper
├── services       interfaces de negocio                      → RequestService
│   └── impl       implementaciones                           → RequestServiceImpl
├── policies       estrategias por subservicio                → VehicleDataPolicy
├── clients        clientes REST hacia otros servicios        → CatalogRestClient
├── repositories   Spring Data JPA                            → ServiceRequestRepository
├── models         entidades propias                          → ServiceRequest, Quote
├── exceptions     errores de negocio y manejador global      → GlobalExceptionHandler, BusinessRuleException
└── config         configuración transversal                  → OpenApiConfig
```

## Formato de respuesta (wrapper `ApiResponse<T>`)

Todos los endpoints responden con la misma raíz: `success`, `data`, `error` y `meta`. El código HTTP de la cabecera siempre coincide con el contenido del JSON.

Éxito (`201 Created`):

```json
{
  "success": true,
  "data": { "...": "recurso creado" },
  "error": null,
  "meta": { "timestamp": "2026-11-12T01:30:00Z", "apiVersion": "v1.0" }
}
```

Error de validación (`400 Bad Request`):

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Los datos enviados son inválidos.",
    "details": { "description": "La descripción es obligatoria." }
  },
  "meta": { "timestamp": "2026-11-12T01:31:00Z", "apiVersion": "v1.0" }
}
```

| HTTP | `error.code` | Cuándo |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Uno o más campos no cumplen las validaciones del DTO (cada campo en `details`) |
| 400 | `MALFORMED_REQUEST` | JSON mal formado, tipo de dato incorrecto o parámetro ausente |
| 401 / 403 | — | Sin token válido o sin permiso (pendiente: configuración de seguridad) |
| 404 | `RESOURCE_NOT_FOUND` / `ROUTE_NOT_FOUND` | El recurso o la ruta no existen |
| 405 | `METHOD_NOT_ALLOWED` | Método HTTP no permitido en la ruta |
| 409 | `BUSINESS_RULE_VIOLATION` o el código de la regla (ej. `QUOTE_ALREADY_ACCEPTED`) | Se incumple una regla de negocio o una transición de estado |
| 500 | `INTERNAL_ERROR` | Error inesperado (el detalle solo queda en el log, nunca el stack trace) |

## Convenciones

- Código, contratos y commits en inglés; mensajes al usuario y documentación en español.
- Commits con *Conventional Commits* (`feat:`, `fix:`, `refactor:`, `docs:`, `chore:`).
- Fechas en ISO 8601 y UTC en la API.
- Cambios de esquema solo con migraciones Flyway en `src/main/resources/db/migration` (`V1__`, `V2__`…).
