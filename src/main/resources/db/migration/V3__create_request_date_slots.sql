-- V3: franjas horarias que el cliente ofrece en una solicitud programada (RF3.2; models.md, request_date_slots).
-- Las fechas van en UTC (BD-02). Dentro del servicio la relación es una llave foránea real con ON DELETE RESTRICT (RN-08).

CREATE TABLE request_date_slots (
    id         BIGINT    NOT NULL AUTO_INCREMENT,
    request_id BIGINT    NOT NULL,
    starts_at  TIMESTAMP NOT NULL,
    ends_at    TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_request_date_slots PRIMARY KEY (id),

    -- Una franja siempre termina después de empezar
    CONSTRAINT chk_rds_range CHECK (ends_at > starts_at),

    CONSTRAINT fk_request_date_slots_service_requests
        FOREIGN KEY (request_id) REFERENCES service_requests (id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Franjas de una solicitud (RF3.2, RF3.7)
CREATE INDEX idx_rds_request_id ON request_date_slots (request_id);
