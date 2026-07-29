# HARDENING_PHASE_1_CLOSURE

## Estado

```text
Proyecto: Envios_Paraguay_CMS
Rama: feature/cierre-hardening-fase-1
Fase: 1 — Hardening secretos y perfil prod
Estado: cerrada documentalmente
```

---

## Proposito

Este documento cierra la Fase 1 de hardening de `Envios_Paraguay_CMS`.

La fase tuvo como objetivo reducir riesgos de despliegue, secretos, perfil de produccion y exposicion de servicios internos.

---

## Documentos creados durante la fase

```text
docs/SECRET_HISTORY_AUDIT.md
docs/PROD_PROFILE_HARDENING_PLAN.md
docs/DEPLOY_SECURITY_REVIEW.md
docs/SAFE_DEPLOY_DECISION.md
docs/MONITORING_ACCESS_REVIEW.md
```

---

## Cambios tecnicos integrados

### 1. Deploy VPS manual

Archivo:

```text
.github/workflows/deploy.yml
```

Cambio:

```text
Se elimino el trigger automatico por push a develop.
El workflow queda manual con workflow_dispatch.
```

Riesgo cerrado:

```text
Deploy accidental al hacer merge o push a develop.
```

---

### 2. Grafana sin fallback inseguro

Archivo:

```text
docker-compose.yml
```

Cambio:

```text
Se eliminaron los defaults admin/admin123 en Grafana.
Grafana ahora requiere GRAFANA_ADMIN_USER y GRAFANA_ADMIN_PASSWORD desde entorno.
```

Riesgo cerrado:

```text
Arranque de Grafana con credenciales predecibles por defecto.
```

---

### 3. Monitoring limitado a localhost

Archivo:

```text
docker-compose.yml
```

Cambio:

```text
Prometheus, Grafana y Uptime Kuma se publican solo en 127.0.0.1.
```

Servicios:

```text
127.0.0.1:${PROMETHEUS_PORT:-9090}:9090
127.0.0.1:${GRAFANA_PORT:-3000}:3000
127.0.0.1:${UPTIME_KUMA_PORT:-3001}:3001
```

Riesgo cerrado:

```text
Exposicion directa de monitoring a Internet.
```

---

## Riesgos cerrados

```text
[x] Deploy automatico desde develop.
[x] Grafana con admin123 por defecto.
[x] Monitoring expuesto directamente por puertos publicos.
[x] Decision de deploy seguro documentada.
[x] Perfil prod documentado como obligatorio.
[x] Historial de secretos pendiente de revision operativa documentado.
```

---

## Riesgos pendientes

```text
[ ] Confirmar en local resultado final de busqueda de secretos.
[ ] Revisar scripts/deploy-prod.sh.
[ ] Revisar scripts/rollback-prod.sh.
[ ] Revisar backups reales.
[ ] Revisar smoke tests post-deploy.
[ ] Confirmar firewall real del VPS.
[ ] Revisar si /api/v1/push/test debe desactivarse en produccion.
[ ] Revisar uploads/evidencias para unificar rutas.
```

---

## Estado final de la fase

```text
Fase 1 completada a nivel documental y con hardening tecnico inicial aplicado.
```

No se considera cierre absoluto de seguridad, pero si un avance fuerte y ordenado.

---

## Siguiente fase recomendada

```text
FASE 2 — Calidad operativa
```

Foco:

```text
scripts/deploy-prod.sh
scripts/rollback-prod.sh
backups
healthchecks
smoke tests
runbooks
validacion de despliegue
recuperacion ante fallo
```

Primera rama recomendada:

```text
feature/auditoria-scripts-deploy-rollback
```

Objetivo:

```text
Auditar scripts de despliegue y rollback antes de modificarlos.
```

---

## Checklist de cierre

```text
[x] SECRET_HISTORY_AUDIT creado.
[x] PROD_PROFILE_HARDENING_PLAN creado.
[x] DEPLOY_SECURITY_REVIEW creado.
[x] SAFE_DEPLOY_DECISION creado.
[x] MONITORING_ACCESS_REVIEW creado.
[x] deploy.yml convertido a manual.
[x] Grafana sin fallback admin123.
[x] Monitoring limitado a localhost.
[x] Cierre de fase documentado.
```

---

## Frase guia

La seguridad no se improvisa al final.

Se va sembrando paso a paso, como quien refuerza una casa antes de la tormenta.
