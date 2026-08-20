-- ===========================================================
-- Crea la base de datos de tests automáticamente al iniciar
-- el contenedor MySQL por primera vez (o tras docker compose down -v).
-- Se ejecuta por /docker-entrypoint-initdb.d/ en orden alfabético.
-- ===========================================================
CREATE DATABASE IF NOT EXISTS envios_paraguay_cms_test
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
