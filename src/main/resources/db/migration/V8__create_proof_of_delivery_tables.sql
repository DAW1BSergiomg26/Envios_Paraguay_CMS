CREATE TABLE entregas_evidencia (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    envio_id BIGINT NOT NULL UNIQUE,
    receptor_nombre VARCHAR(150) NOT NULL,
    receptor_documento VARCHAR(50) NOT NULL,
    firma_base64 LONGTEXT NOT NULL,
    latitud DECIMAL(10, 8) NULL,
    longitud DECIMAL(11, 8) NULL,
    notas TEXT NULL,
    fecha_entrega DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_entregas_evidencia_envio FOREIGN KEY (envio_id) REFERENCES envios_tracking(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
