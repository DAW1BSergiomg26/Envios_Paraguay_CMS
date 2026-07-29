-- ============================================================
-- V1: Initial Schema
-- All tables + strategic indexes
-- Uses CREATE TABLE IF NOT EXISTS and conditional CREATE INDEX
-- for idempotency across fresh and existing databases.
-- ============================================================

-- -----------------------------------------------------------
-- 1. clientes
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS clientes (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    email       VARCHAR(255) NOT NULL,
    password    VARCHAR(255) NOT NULL,
    nombre      VARCHAR(255) NOT NULL,
    telefono    VARCHAR(255) NULL,
    CONSTRAINT uk_clientes_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------
-- 2. envios_tracking
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS envios_tracking (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo_unico          VARCHAR(255) NOT NULL,
    estado                VARCHAR(255) NOT NULL,
    destinatario          VARCHAR(255) NOT NULL,
    origen                VARCHAR(255) NULL,
    destino               VARCHAR(255) NULL,
    peso                  VARCHAR(255) NULL,
    contenido             VARCHAR(255) NULL,
    ubicacion_actual      VARCHAR(255) NULL,
    fecha_creacion        DATETIME NOT NULL,
    ultima_actualizacion  DATETIME NOT NULL,
    observaciones         TEXT NULL,
    cliente_id            BIGINT NULL,
    CONSTRAINT uk_envios_tracking_codigo UNIQUE (codigo_unico),
    CONSTRAINT fk_envios_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------
-- 3. eventos_tracking
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS eventos_tracking (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    envio_id        BIGINT NOT NULL,
    estado          VARCHAR(255) NOT NULL,
    titulo          VARCHAR(255) NOT NULL,
    descripcion     TEXT NULL,
    ubicacion       VARCHAR(255) NULL,
    icono           VARCHAR(255) NULL,
    color           VARCHAR(255) NULL,
    fecha_evento    DATETIME NOT NULL,
    creado_por      VARCHAR(255) NULL,
    visible_cliente BIT NOT NULL DEFAULT 1,
    CONSTRAINT fk_eventos_envio FOREIGN KEY (envio_id) REFERENCES envios_tracking(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------
-- 4. evidencias_envio
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS evidencias_envio (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    envio_id        BIGINT NOT NULL,
    titulo          VARCHAR(255) NOT NULL,
    descripcion     VARCHAR(255) NULL,
    tipo            VARCHAR(255) NOT NULL,
    url_archivo     VARCHAR(255) NOT NULL,
    fecha_subida    DATETIME NOT NULL,
    visible_cliente BIT NULL DEFAULT 1,
    CONSTRAINT fk_evidencias_envio FOREIGN KEY (envio_id) REFERENCES envios_tracking(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------
-- 5. reservas
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS reservas (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre_cliente    VARCHAR(255) NOT NULL,
    email             VARCHAR(255) NOT NULL,
    telefono          VARCHAR(255) NULL,
    fecha_entrada     DATE NOT NULL,
    fecha_salida      DATE NOT NULL,
    numero_huespedes  INT NOT NULL,
    comentarios       TEXT NULL,
    estado            VARCHAR(255) NOT NULL,
    created_at        DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------
-- 6. mensajes_contacto
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS mensajes_contacto (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre      VARCHAR(255) NOT NULL,
    email       VARCHAR(255) NOT NULL,
    telefono    VARCHAR(255) NULL,
    mensaje     VARCHAR(1000) NOT NULL,
    fecha_envio DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------
-- 7. imagenes
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS imagenes (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    titulo      VARCHAR(255) NOT NULL,
    descripcion TEXT NULL,
    url         VARCHAR(500) NOT NULL,
    categoria   VARCHAR(255) NULL,
    orden       INT NOT NULL,
    created_at  DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------
-- 8. textos_legales
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS textos_legales (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    slug        VARCHAR(255) NOT NULL,
    titulo      VARCHAR(255) NOT NULL,
    contenido   TEXT NULL,
    updated_at  DATETIME NULL,
    CONSTRAINT uk_textos_slug UNIQUE (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- STRATEGIC INDEXES
-- ============================================================
-- Helper: create index only if it doesn't already exist
DROP PROCEDURE IF EXISTS create_index_if_not_exists;
DELIMITER //
CREATE PROCEDURE create_index_if_not_exists(
    IN p_table_name VARCHAR(128),
    IN p_index_name VARCHAR(128),
    IN p_index_def  VARCHAR(512)
)
BEGIN
    DECLARE idx_count INT;
    SELECT COUNT(1) INTO idx_count
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = p_table_name
      AND index_name = p_index_name;
    IF idx_count = 0 THEN
        SET @ddl = CONCAT('CREATE INDEX ', p_index_name, ' ON ', p_table_name, ' ', p_index_def);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

-- envios_tracking
CALL create_index_if_not_exists('envios_tracking', 'idx_envios_estado', '(estado)');
CALL create_index_if_not_exists('envios_tracking', 'idx_envios_ultima_actualizacion', '(ultima_actualizacion)');
CALL create_index_if_not_exists('envios_tracking', 'idx_envios_cliente_id', '(cliente_id)');

-- eventos_tracking
CALL create_index_if_not_exists('eventos_tracking', 'idx_eventos_envio_id', '(envio_id)');

-- evidencias_envio
CALL create_index_if_not_exists('evidencias_envio', 'idx_evidencias_envio_id', '(envio_id)');
CALL create_index_if_not_exists('evidencias_envio', 'idx_evidencias_envio_visible', '(envio_id, visible_cliente)');

-- reservas
CALL create_index_if_not_exists('reservas', 'idx_reservas_estado', '(estado)');
CALL create_index_if_not_exists('reservas', 'idx_reservas_fechas', '(fecha_entrada, fecha_salida)');

-- mensajes_contacto
CALL create_index_if_not_exists('mensajes_contacto', 'idx_mensajes_fecha_envio', '(fecha_envio)');

-- Cleanup
DROP PROCEDURE IF EXISTS create_index_if_not_exists;
