-- V2: alinea service_requests con el modelo de datos oficial (fixgo-docs, 06-data/models.md y modeling-conventions.md).
-- Cambios respecto a V1: ids BIGINT AUTO_INCREMENT (BD-02, no UUID), ENUM para mode y status, TEXT para textos largos,
-- TIMESTAMP en UTC con DEFAULT/ON UPDATE, columnas zone_id y agreed_value, y nombres chk_sr_* e idx_sr_*.
-- Las migraciones son forward-only: V1 no se edita. Se recrea la tabla porque cambia el tipo de la llave primaria y
-- el servicio aún no tiene datos de producción (los UUID de V1 no se pueden convertir a BIGINT).
-- client_id, service_type_id, zone_id y provider_id son llaves foráneas LÓGICAS (BD-03):
-- viven en client-service, catalog-provider-service y en su módulo provider-service, por eso no hay FOREIGN KEY.

DROP TABLE service_requests;

CREATE TABLE service_requests (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    client_id           BIGINT        NOT NULL,
    service_type_id     BIGINT        NOT NULL,
    zone_id             BIGINT        NULL,
    provider_id         BIGINT        NULL,
    mode                ENUM('URGENT', 'SCHEDULED') NOT NULL,
    status              ENUM('PENDING', 'OPEN', 'ACCEPTED', 'ON_THE_WAY',
                             'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'EXPIRED') NOT NULL DEFAULT 'PENDING',
    description         TEXT          NOT NULL,
    address             VARCHAR(255)  NULL,
    latitude            DECIMAL(9,6)  NULL,
    longitude           DECIMAL(9,6)  NULL,
    vehicle_plate       VARCHAR(10)   NULL,
    vehicle_brand       VARCHAR(50)   NULL,
    vehicle_type        VARCHAR(30)   NULL,
    agreed_value        DECIMAL(10,2) NULL,
    cancellation_reason TEXT          NULL,
    cancelled_at        TIMESTAMP     NULL DEFAULT NULL,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_service_requests PRIMARY KEY (id),

    -- RF3.1: una urgencia siempre tiene ubicación puntual
    CONSTRAINT chk_sr_urgent_location
        CHECK (mode <> 'URGENT' OR (latitude IS NOT NULL AND longitude IS NOT NULL)),

    -- RF3.2: una programada siempre tiene dirección
    CONSTRAINT chk_sr_scheduled_address
        CHECK (mode <> 'SCHEDULED' OR address IS NOT NULL),

    CONSTRAINT chk_sr_latitude
        CHECK (latitude IS NULL OR latitude BETWEEN -90 AND 90),

    CONSTRAINT chk_sr_longitude
        CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180),

    -- RN-09: el valor acordado nunca es negativo
    CONSTRAINT chk_sr_agreed_value
        CHECK (agreed_value IS NULL OR agreed_value >= 0),

    -- RF3.4: una solicitud cancelada siempre tiene motivo
    CONSTRAINT chk_sr_cancellation_reason
        CHECK (status <> 'CANCELLED' OR cancellation_reason IS NOT NULL)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- RF4.1 y RF4.5: solicitudes e historial del cliente
CREATE INDEX idx_sr_client_id ON service_requests (client_id);

-- RF4.1: solicitudes del profesional asignado
CREATE INDEX idx_sr_provider_id ON service_requests (provider_id);

-- RF4.1 y RF4.5: filtro por estado
CREATE INDEX idx_sr_status ON service_requests (status);
