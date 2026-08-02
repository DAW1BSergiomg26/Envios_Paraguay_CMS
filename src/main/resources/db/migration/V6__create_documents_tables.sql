CREATE TABLE documentos_generados (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tipo VARCHAR(50) NOT NULL COMMENT 'ETIQUETA_TERMICA, ETIQUETAS_LOTE, MANIFIESTO_CARGA',
    referencia_id VARCHAR(100) NOT NULL COMMENT 'codigoUnico del envío o batch_id del lote',
    nombre_archivo VARCHAR(255) NOT NULL,
    peso_bytes INT NOT NULL,
    usuario_generacion VARCHAR(100) NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
