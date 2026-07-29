-- =============================================
-- ESQUEMA COMPLETO: MONTEASTUR ENVIOS
-- Motor: MySQL 8+
-- Proyecto: MONTEASTUR ENVIOS - Grupo B_2
-- =============================================

CREATE DATABASE IF NOT EXISTS envios_paraguay_cms
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE envios_paraguay_cms;

-- =============================================
-- TABLA: imagenes
-- Galería de imágenes para página "La Casa"
-- =============================================
CREATE TABLE IF NOT EXISTS imagenes (
    id          BIGINT          AUTO_INCREMENT PRIMARY KEY,
    titulo      VARCHAR(255)    NOT NULL,
    descripcion TEXT,
    url         VARCHAR(500)    NOT NULL,
    categoria   VARCHAR(100),
    orden       INT             DEFAULT 0,
    created_at  DATETIME        DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- TABLA: reservas
-- Solicitudes de reserva de los clientes
-- =============================================
CREATE TABLE IF NOT EXISTS reservas (
    id               BIGINT          AUTO_INCREMENT PRIMARY KEY,
    nombre_cliente   VARCHAR(255)    NOT NULL,
    email            VARCHAR(255)    NOT NULL,
    telefono         VARCHAR(20),
    fecha_entrada    DATE            NOT NULL,
    fecha_salida     DATE            NOT NULL,
    numero_huespedes INT             NOT NULL,
    comentarios      TEXT,
    estado           VARCHAR(50)     DEFAULT 'pendiente',
    created_at       DATETIME        DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_estado CHECK (estado IN ('pendiente', 'confirmada', 'cancelada'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- TABLA: mensajes_contacto
-- Mensajes enviados desde el formulario de contacto
-- =============================================
CREATE TABLE IF NOT EXISTS mensajes_contacto (
    id          BIGINT          AUTO_INCREMENT PRIMARY KEY,
    nombre      VARCHAR(255)    NOT NULL,
    email       VARCHAR(255)    NOT NULL,
    telefono    VARCHAR(20),
    mensaje     TEXT            NOT NULL,
    fecha_envio DATETIME        NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- TABLA: textos_legales
-- Contenido de aviso legal, política de cookies, etc.
-- =============================================
CREATE TABLE IF NOT EXISTS textos_legales (
    id          BIGINT          AUTO_INCREMENT PRIMARY KEY,
    clave       VARCHAR(100)    NOT NULL UNIQUE,
    titulo      VARCHAR(255)    NOT NULL,
    contenido   TEXT            NOT NULL,
    actualizado DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- DATOS INICIALES: textos legales
-- =============================================
INSERT INTO textos_legales (clave, titulo, contenido) VALUES
('aviso-legal', 'Aviso Legal',
 '<p>En cumplimiento con el deber de informaci&oacute;n recogido en el art&iacute;culo 10 de la Ley 34/2002, de 11 de julio, de Servicios de la Sociedad de la Informaci&oacute;n y del Comercio Electr&oacute;nico, a continuaci&oacute;n se exponen los datos identificativos de la empresa titular de la presente p&aacute;gina web.</p>'
),
('politica-cookies', 'Pol&iacute;tica de Cookies',
 '<p>Este sitio web utiliza cookies propias y de terceros para garantizar el correcto funcionamiento del portal, as&iacute; como para mejorar la experiencia del usuario.</p>'
);
