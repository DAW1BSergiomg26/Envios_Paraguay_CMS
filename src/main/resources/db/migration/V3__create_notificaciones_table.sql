-- ============================================================
-- V3: Notificaciones automáticas
-- Registro de emails enviados al cliente al cambiar el estado
-- de un envío. Flyway la aplica una sola vez por base de datos.
-- ============================================================

CREATE TABLE notificaciones (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    envio_id BIGINT NOT NULL,
    destinatario VARCHAR(150) NULL,
    asunto VARCHAR(255) NOT NULL,
    mensaje TEXT NOT NULL,
    estado VARCHAR(30) NOT NULL COMMENT 'ENVIADO, FALLIDO, OMITIDO_SIN_DESTINATARIO',
    error_mensaje TEXT NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notificaciones_envio_tracking
        FOREIGN KEY (envio_id) REFERENCES envios_tracking(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_notificaciones_envio_id ON notificaciones (envio_id);
CREATE INDEX idx_notificaciones_estado ON notificaciones (estado);
