CREATE TABLE batch_imports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id BIGINT NULL,
    nombre_archivo VARCHAR(255) NOT NULL,
    total_registros INT NOT NULL DEFAULT 0,
    procesados INT NOT NULL DEFAULT 0,
    exitosos INT NOT NULL DEFAULT 0,
    fallidos INT NOT NULL DEFAULT 0,
    estado VARCHAR(30) NOT NULL COMMENT 'PENDIENTE, EN_PROCESO, COMPLETADO, COMPLETADO_CON_ERRORES, FALLIDO',
    error_resumen TEXT NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_fin DATETIME NULL,
    CONSTRAINT fk_batch_imports_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE batch_import_errors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id BIGINT NOT NULL,
    linea_numero INT NOT NULL,
    codigo_rastreo VARCHAR(100) NULL,
    error_mensaje TEXT NOT NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_batch_import_errors_batch FOREIGN KEY (batch_id) REFERENCES batch_imports(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
