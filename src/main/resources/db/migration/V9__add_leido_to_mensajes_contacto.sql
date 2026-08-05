-- =============================================
-- V9: Añade columna leido a mensajes_contacto
-- (sincroniza la entidad MensajeContacto con el esquema)
-- =============================================

ALTER TABLE mensajes_contacto
    ADD COLUMN leido BOOLEAN NOT NULL DEFAULT FALSE;
