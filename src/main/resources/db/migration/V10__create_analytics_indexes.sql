-- ============================================================
-- V10: Índices de agregación para el Dashboard BI
-- Optimiza las consultas de AnalyticsQueryService (JdbcTemplate):
--   * envíos por estado y tendencia por fecha (fecha_creacion, estado)
--   * top de rutas origen -> destino
--   * tasa de éxito de webhooks por día (exitoso, fecha_creacion)
-- ============================================================

CREATE INDEX idx_envios_fecha_estado ON envios_tracking(fecha_creacion, estado);
CREATE INDEX idx_envios_origen_destino ON envios_tracking(origen, destino);
CREATE INDEX idx_webhook_logs_exitoso ON webhook_logs(exitoso, fecha_creacion);
