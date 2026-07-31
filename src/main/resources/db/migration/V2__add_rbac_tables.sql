-- ============================================================
-- V2: RBAC Tables (Usuarios, Roles, Auditoría)
-- Agregar usuarios, roles y auditoría de seguridad para el sistema RBAC
-- ============================================================

-- -----------------------------------------------------------
-- 1. users
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------
-- 2. roles
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS roles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------
-- 3. user_roles (relación many-to-many)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id INT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------
-- 4. auditoria_accesos (seguridad + auditoría)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS auditoria_accesos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL,
    username VARCHAR(255) NOT NULL,
    accion VARCHAR(100) NOT NULL,
    recurso VARCHAR(255) NULL,
    ip_origen VARCHAR(45) NULL,
    user_agent TEXT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    exitoso BOOLEAN NOT NULL,
    descripcion TEXT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------
-- ÍNDICES ESTRATÉGICOS PARA RBAC
-- -----------------------------------------------------------
-- V1 elimina el procedimiento al final, por lo que hay que
-- recrearlo aquí antes de los CALL.
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

-- Índice en user_roles.role_id para búsquedas rápidas por rol
CALL create_index_if_not_exists('user_roles', 'idx_user_roles_role_id', '(role_id)');

-- Índice en auditoria_accesos.timestamp para consultas de auditoría por tiempo
CALL create_index_if_not_exists('auditoria_accesos', 'idx_auditoria_timestamp', '(timestamp)');

-- Índice en auditoria_accesos.username para búsquedas por usuario
CALL create_index_if_not_exists('auditoria_accesos', 'idx_auditoria_username', '(username)');

-- Índice en auditoria_accesos.exitoso para filtración por éxito/fallo
CALL create_index_if_not_exists('auditoria_accesos', 'idx_auditoria_exitoso', '(exitoso)');

-- -----------------------------------------------------------
-- DATOS DE COMÚN PARA DEFAULTS (datos por defecto)
-- -----------------------------------------------------------

-- Insertar roles básicos (IGNORE para ser idempotente)
INSERT IGNORE INTO roles (nombre) VALUES 
    ('ROLE_ADMIN'),
    ('ROLE_OPERADOR'),
    ('ROLE_CLIENTE');

-- Insertar usuarios por defecto (Las passwords deben ser inyectadas desde .env usando $env.var o un script)
-- El usuario admin se crea utilizando la contraseña del entorno app.admin.password
-- El usuario operador se crea utilizando la misma contraseña de app.admin.password
-- Los clientes endpoints (clientes reales de la base de datos) mantienen su autenticación existente

-- Nota: Insertar usuarios reales a través de un script o API después del despliegue
-- para mantener contraseñas seguras en el controlador, no en SQL

-- -----------------------------------------------------------
-- NOTA SOBRE LA SEGURIDAD DE LA CONTRASEÑA
-- -----------------------------------------------------------
-- Contraseñas almacenadas como texto plano en este archivo. En producción:
-- 1. Usar pswd encriptadas (bcrypt) para usuarios por defecto.
-- 2. O mejor, crear usuarios admin/operador a través de un endpoint solo para admins
--    en el arranque de la aplicación o por servicios externos.
-- 3. Mantener contraseñas seguras en archivos .env o variables del sistema, NO en migraciones committeadas.
-- Ejemplo de patrón (NOT COMITTEADO) para referencia:
-- password='$2a$12$VIqC7GvW6YtQG7v5jY.qv.e2f8jKq6L8F8H7M9K2P1N3R4T5U6V7W8X'

-- -----------------------------------------------------------
-- Limpiar el procedimiento
-- -----------------------------------------------------------
DROP PROCEDURE IF EXISTS create_index_if_not_exists;