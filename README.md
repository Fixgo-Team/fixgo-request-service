# fixgo-request-service

Microservicio de solicitudes de FixGo: solicitudes urgentes y programadas, cotizaciones y mensajes.

| Dato | Valor |
|---|---|
| Módulos del SRS v3.1 | 3 (RF3.1–RF3.4 y RF3.6–RF3.9; el emparejamiento RF3.5 es de `dispatch-matchmaking`) y RF5.4 (mensajes) |
| Puerto | `8084` |
| Base de datos | MySQL 8 · `fixgo_request` |
| Stack | Java 17, Spring Boot 4.1, Spring Data JPA, Flyway, springdoc-openapi |
| Responsable | Mateo Esteban Ramirez Garzon |

## Requisitos

- JDK 17 o superior
- Maven: no hace falta instalarlo, el repo incluye Maven Wrapper (`mvnw` / `mvnw.cmd`, Maven 3.9)
- Docker y Docker Compose (también lo usan las pruebas de integración con Testcontainers)

## Ejecutar con Docker

```bash
cp .env.example .env      # completa las contraseñas
docker compose up --build
```

## Ejecutar en local (sin Docker para la API)

```bash
docker compose up -d request-db
./mvnw spring-boot:run      # en Windows: .\mvnw.cmd spring-boot:run
```

## Pruebas

```bash
./mvnw verify               # en Windows: .\mvnw.cmd verify
```

`verify` ejecuta las pruebas unitarias (`*Test`), las de integración (`*IT`, con un MySQL 8 real en Testcontainers, requieren Docker) y el control de cobertura de JaCoCo: mínimo 80 % de líneas en todo el servicio y 90 % en `services`, `policies` y `models`. Si no se alcanza, el build falla. El informe queda en `target/site/jacoco/index.html`.

## Seguridad

Todos los endpoints (salvo Swagger y `/actuator/health`) exigen un JWT de Firebase Authentication en la cabecera `Authorization: Bearer <token>` (RNF-15). El servicio solo valida el token (firma, emisor, audiencia y vencimiento); el permiso de cada operación se verifica en la capa `services` según el rol y la relación con la solicitud (RNF-16).

| Variable | Para qué sirve | Valor por defecto |
|---|---|---|
| `FIREBASE_PROJECT_ID` | Proyecto de Firebase: define el emisor `https://securetoken.google.com/<id>` y la audiencia | `fixgo-dev` |
| `JWT_ISSUER_URI` | Otro emisor de tokens (opcional) | emisor de Firebase |
| `JWT_USER_ID_CLAIM` | Claim con el id numérico de la cuenta | `userId` |
| `JWT_ROLE_CLAIM` | Claim con el rol: `CLIENT`, `PROVIDER`, `ADMIN` o `SERVICE` | `role` |

> Los nombres de los claims son **provisionales**: cómo el sujeto de Firebase se asocia al id numérico de la cuenta está pendiente (Q-004). El rol `SERVICE` (llamadas internas entre servicios) también es provisional (Q-016).

| Operación | Quién puede |
|---|---|
| `POST` crear, `PUT` actualizar | Solo el `CLIENT` dueño (el cliente sale del token, no del cuerpo) |
| `GET /{id}` | Dueño, profesional asignado o `ADMIN`; si no, 403 |
| `GET` lista | Cada quien ve solo lo propio (cliente: las suyas; profesional: las asignadas); `ADMIN` ve todo y puede filtrar por cliente |
| `PATCH /{id}/cancel` | Dueño, profesional asignado, `ADMIN` o el sistema (`SERVICE`); ver estados abajo |
| `/actuator/**` (salvo `health`) | Solo `ADMIN` |

Sin token o con token inválido responde `401 UNAUTHORIZED`; con token válido pero sin permiso, `403 FORBIDDEN`, ambos con el wrapper `ApiResponse`.

## Endpoints de solicitudes

| Método | Ruta | Requisito | Respuesta OK |
|---|---|---|---|
| `GET` | `/api/v1/requests?clientId=&status=` | RF4.1 | 200 |
| `GET` | `/api/v1/requests/{id}` | RF4.6 | 200 |
| `POST` | `/api/v1/requests` | RF3.1, RF3.2 (programada: dirección y `zoneId` obligatorios) | 201 + cabecera `Location` |
| `PUT` | `/api/v1/requests/{id}` | RF3.3 (solo `PENDING` u `OPEN`) | 200 |
| `PATCH` | `/api/v1/requests/{id}/cancel` | RF3.4, RN-08 (borrado lógico) | 200 |

### Estados y cancelación

Los cambios de estado siguen la tabla 26 del SRS v3.1 y los decide `RequestTransitionPolicy` (paquete `policies`); `RequestStatus.allowedTargets()` define a qué estados se puede pasar desde cada uno:

| Desde | Hacia |
|---|---|
| `PENDING` | `ACCEPTED`, `OPEN`, `CANCELLED` |
| `OPEN` | `ACCEPTED`, `EXPIRED`, `CANCELLED` |
| `ACCEPTED` | `ON_THE_WAY`, `CANCELLED` |
| `ON_THE_WAY` | `IN_PROGRESS`, `CANCELLED` |
| `IN_PROGRESS` | `COMPLETED`, `CANCELLED` |
| `COMPLETED`, `CANCELLED`, `EXPIRED` | ninguno (la reapertura por garantía, RF4.9, está bloqueada por Q-033) |

Quién puede cancelar (RF3.4, siempre con motivo): el cliente dueño en `PENDING`, `OPEN`, `ACCEPTED` y `ON_THE_WAY`; el profesional asignado en `ACCEPTED` y `ON_THE_WAY`; el `ADMIN` y el sistema en cualquier estado no final, incluido `IN_PROGRESS`. Si el actor ya no puede cancelar responde `409 REQUEST_NOT_CANCELLABLE`; una transición que la tabla no permite, `409 INVALID_STATUS_TRANSITION`; un rol que no le corresponde, `403`.

Los subservicios que exigen datos del vehículo (`requiresVehicleData`) validan placa, marca y tipo con `VehicleDataPolicy`: `400 VEHICLE_DATA_REQUIRED` con el detalle de cada campo faltante.

Ejemplo para probar en Swagger (`POST`, solicitud urgente; el cliente se toma del token):

```json
{
  "serviceTypeId": 7,
  "mode": "URGENT",
  "description": "El carro no enciende",
  "latitude": 2.9273,
  "longitude": -75.2819,
  "vehiclePlate": "ABC123",
  "vehicleBrand": "Chevrolet",
  "vehicleType": "Automóvil"
}
```

Los identificadores son numéricos (`BIGINT`, BD-02). En una solicitud urgente, `agreedValue` guarda el precio de referencia del subservicio al crearla (RN-09).

> Mientras `catalog-provider-service` y `notification-service` publican su API, se usan `FakeCatalogClient` y `FakeNotificationClient` (paquete `clients`). Se reemplazan sin tocar el servicio ni el controlador.

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
├── security       identidad del JWT y errores 401/403        → CurrentActor, JwtCurrentActorProvider
├── exceptions     errores de negocio y manejador global      → GlobalExceptionHandler, BusinessRuleException
└── config         configuración transversal                  → OpenApiConfig, SecurityConfig, ClockConfig
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
| 400 | `VEHICLE_DATA_REQUIRED` | El subservicio exige placa, marca y tipo de vehículo y falta alguno |
| 400 | `MALFORMED_REQUEST` | JSON mal formado, tipo de dato incorrecto o parámetro ausente |
| 401 | `UNAUTHORIZED` | Sin token o con token inválido o vencido |
| 403 | `FORBIDDEN` | Token válido, pero el rol o la propiedad del recurso no permiten la operación |
| 404 | `RESOURCE_NOT_FOUND` / `ROUTE_NOT_FOUND` | El recurso o la ruta no existen |
| 405 | `METHOD_NOT_ALLOWED` | Método HTTP no permitido en la ruta |
| 409 | `BUSINESS_RULE_VIOLATION` o el código de la regla (ej. `QUOTE_ALREADY_ACCEPTED`) | Se incumple una regla de negocio o una transición de estado |
| 409 | `INVALID_STATUS_TRANSITION` / `REQUEST_NOT_CANCELLABLE` | La tabla de estados no permite el cambio, o el actor ya no puede cancelar en ese estado |
| 409 | `DATA_INTEGRITY_VIOLATION` | La base de datos rechazó la operación por una restricción (BD-05) |
| 500 | `INTERNAL_ERROR` | Error inesperado (el detalle solo queda en el log, nunca el stack trace) |

## Convenciones

- Código, contratos y commits en inglés; mensajes al usuario y documentación en español.
- Commits con *Conventional Commits* (`feat:`, `fix:`, `refactor:`, `docs:`, `chore:`).
- Fechas en ISO 8601 y UTC en la API; el tiempo se lee de un `Clock` inyectado, nunca de `Instant.now()` en la lógica de negocio.
- Cambios de esquema solo con migraciones Flyway en `src/main/resources/db/migration` (`V1__`, `V2__`…).
