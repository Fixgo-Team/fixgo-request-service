-- V1: tabla principal del servicio de solicitudes (módulo 3 del SRS v3.0).
-- client_id, service_type_id y provider_id son llaves foráneas LÓGICAS (BD-03):
-- esos datos viven en las bases de access-service y catalog-service, por eso no hay FOREIGN KEY.

CREATE TABLE service_requests (
    id                  CHAR(36)      NOT NULL,
    client_id           CHAR(36)      NOT NULL,
    service_type_id     CHAR(36)      NOT NULL,
    provider_id         CHAR(36)      NULL,
    mode                VARCHAR(20)   NOT NULL,
    status              VARCHAR(20)   NOT NULL,
    description         VARCHAR(1000) NOT NULL,
    address             VARCHAR(255)  NULL,
    latitude            DECIMAL(9,6)  NULL,
    longitude           DECIMAL(9,6)  NULL,
    vehicle_plate       VARCHAR(10)   NULL,
    vehicle_brand       VARCHAR(50)   NULL,
    vehicle_type        VARCHAR(30)   NULL,
    cancellation_reason VARCHAR(500)  NULL,
    cancelled_at        DATETIME(6)   NULL,
    created_at          DATETIME(6)   NOT NULL,
    updated_at          DATETIME(6)   NOT NULL,

    CONSTRAINT pk_service_requests PRIMARY KEY (id),

    -- Modo de la solicitud: urgente (RF3.1) o programada (RF3.2)
    CONSTRAINT ck_service_requests_mode
        CHECK (mode IN ('URGENT', 'SCHEDULED')),

    -- Los 8 estados del Anexo A.3
    CONSTRAINT ck_service_requests_status
        CHECK (status IN ('PENDING', 'OPEN', 'ACCEPTED', 'ON_THE_WAY',
                          'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'EXPIRED')),

    -- RF3.1: una urgencia siempre tiene ubicación puntual
    CONSTRAINT ck_service_requests_urgent_location
        CHECK (mode <> 'URGENT' OR (latitude IS NOT NULL AND longitude IS NOT NULL)),

    -- RF3.2: una programada siempre tiene dirección
    CONSTRAINT ck_service_requests_scheduled_address
        CHECK (mode <> 'SCHEDULED' OR address IS NOT NULL),

    CONSTRAINT ck_service_requests_latitude
        CHECK (latitude IS NULL OR latitude BETWEEN -90 AND 90),

    CONSTRAINT ck_service_requests_longitude
        CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180),

    -- RF3.4: una solicitud cancelada siempre tiene motivo
    CONSTRAINT ck_service_requests_cancellation
        CHECK (status <> 'CANCELLED' OR cancellation_reason IS NOT NULL)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- RF4.1: solicitudes del cliente
CREATE INDEX ix_service_requests_client ON service_requests (client_id, created_at);

-- RF3.5 y RN-02: solicitudes abiertas por subservicio
CREATE INDEX ix_service_requests_status_type ON service_requests (status, service_type_id);

-- RF4.1: solicitudes del profesional asignado
CREATE INDEX ix_service_requests_provider ON service_requests (provider_id);
