# Problemas Conocidos — Preproducción

**Última actualización:** 2026-05-25

---

## Tabla de Issues

| # | Problema | Impacto | Prioridad | Solución Propuesta | Fase Recomendada |
|---|----------|---------|-----------|-------------------|------------------|
| 1 | **CasaRuralApplication naming legacy** | ✅ Resuelto en Fase 19.2 — renombrado a `MonteasturApplication` | — | — | — |
| 2 | **Package `com.grupb2.casarural` legacy** | ✅ Resuelto en Fase 19.2 — refactorizado a `com.monteastur.envios` | — | — | — |
| 3 | **Credenciales demo temporales** | `admin123` / `demo2026` son contraseñas inseguras para producción. Si se despliega sin cambiarlas, el sistema es vulnerable. | **ALTA** | Generar contraseñas seguras con `openssl rand -base64 32` y configurar vía variables de entorno en el VPS. | Antes del deploy real |
| 4 | **E2E requiere stack activo** | Los tests de Playwright (E2E) necesitan que el stack Docker esté corriendo. Actualmente están deshabilitados en CI (`if: false`). | Media | Implementar step de CI que levante los contenedores antes de E2E, o usar perfiles de test embedidos. | Fase 19.3 |
| 5 | **npm audit high vulnerabilities** | El frontend puede tener dependencias con vulnerabilidades conocidas. `npm audit` reporta hallazgos que requieren revisión. | Media | Ejecutar `npm audit fix` y revisar breaking changes. Si hay majors, evaluar actualización manual. | Fase 19.4 |
| 6 | **Puerto 80 ocupado por IIS en Windows** | En máquinas Windows con IIS activo, el puerto 80 está ocupado. Nginx local no puede usar `NGINX_PORT=80`. | Baja | Usar `NGINX_PORT=8090` (ya configurado en `.env`). Documentado en `LOCAL_DEV_COMMANDS.md`. | — |
| 7 | **docker compose down -v borra datos** | El flag `-v` elimina volúmenes MySQL, prometheus, grafana. Si hay datos reales, se pierden irreversiblemente. | Media | Documentar advertencia (ya hecho en README). Considerar backups periódicos de `mysql_data`. | Fase 19.5 |
| 8 | **Tabla `textos_legales` columna `slug` vs `clave`** | La entidad JPA usa `slug` pero `schema.sql` usa `clave`. Con `ddl-auto=update` Hibernate crea `slug`. Con `spring.sql.init.mode=always` se crea `clave`. | Baja | Unificar: cambiar `schema.sql` para usar `slug` o viceversa. | Fase 19.2 |
| 9 | **Reserva estado "aprobada" vs "confirmada"** | El `AdminController` usa `"aprobada"` en `aprobarReserva()` pero el `schema.sql` define CHECK constraint con `"confirmada"`. Con `ddl-auto=update` no hay problema. | Baja | Alinear el valor en el controller con el del schema, o eliminar el CHECK obsoleto de `schema.sql`. | Fase 19.2 |
| 10 | **Evidencia subida hardcodea directorio** | ✅ Resuelto — `AdminController.subirEvidencia()` usa `app.upload.dir` + subdirectorio `evidencias/`. | — | — | — |

---

## Leyenda

| Prioridad | Acción |
|-----------|--------|
| **ALTA** | Resolver antes del deploy real |
| Media | Planificar en próximas fases |
| Baja | Corregir cuando se toque el área |

## Seguimiento

Los issues marcados como "Fase 19.x" están planificados para las siguientes iteraciones dentro de la fase de estabilización. El resto deben resolverse siguiendo el orden de prioridad indicado.
