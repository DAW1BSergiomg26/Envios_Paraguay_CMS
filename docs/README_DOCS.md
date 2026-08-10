# Índice de Documentación — Envios_Paraguay_CMS

**Estado:** Vigente (creado el 2026-08-10, hito P3.1).
**Objetivo:** punto único de entrada para navegar la documentación del proyecto y reducir la
duplicidad documental. Este índice no sustituye a los documentos: los enlaza y los clasifica.

---

## 1. Documentos maestros (empezar aquí)

| Documento | Qué contiene |
|---|---|
| [ARQUITECTURA_INTERFACES.md](ARQUITECTURA_INTERFACES.md) | Arquitectura de interfaces (Thymeleaf + SPA React), matriz oficial/legacy, autenticación compartida, hoja de migración F1–F6 y reglas de convivencia. **Documento vigente de arquitectura.** |
| [HARDENING_BACKLOG_ENVIOS_CMS.md](HARDENING_BACKLOG_ENVIOS_CMS.md) | Backlog de hardening (P0–P3) con el estado y las decisiones de cada ítem. |
| [handoff.md](handoff.md) | Resumen de estado del proyecto: arquitectura, sprints recientes y mejoras de seguridad. |
| [PROJECT_MAP_ENVIOS_CMS.md](PROJECT_MAP_ENVIOS_CMS.md) | Mapa global del proyecto. |
| [ROUTE_AND_FLOW_MAP_ENVIOS_CMS.md](ROUTE_AND_FLOW_MAP_ENVIOS_CMS.md) | Mapa de rutas y flujos de la aplicación. |
| [CONTEXTO_COMPLETO_PROYECTO.md](CONTEXTO_COMPLETO_PROYECTO.md) | Contexto completo del proyecto (visión de negocio y técnica). |

## 2. Especificaciones y planes (superpowers)

Ruta: [`superpowers/`](superpowers/) — specs de diseño y planes de implementación por bloque.

| Área | Contenido |
|---|---|
| [`specs/`](superpowers/specs/) | Specs de diseño por bloque (arquitectura de interfaces, webhooks, batch ingestion, PDF/etiquetas, POD, CI/CD, etc.). |
| [`plans/`](superpowers/plans/) | Planes de implementación detallados por bloque. |

## 3. Auditorías y hardening

| Documento | Qué contiene |
|---|---|
| [HARDENING_FINAL_REPORT.md](HARDENING_FINAL_REPORT.md) | Informe final del plan de hardening. |
| [HARDENING_PHASE_1_CLOSURE.md](HARDENING_PHASE_1_CLOSURE.md) | Cierre de la fase 1 de hardening. |
| [AUDIT_INITIAL_ENVIOS_CMS.md](AUDIT_INITIAL_ENVIOS_CMS.md) | Auditoría inicial del CMS. |
| [AUDIT_PHASE_0_CLOSURE.md](AUDIT_PHASE_0_CLOSURE.md) | Cierre de la fase 0 de la auditoría. |
| [PREPRODUCTION_AUDIT_REPORT.md](PREPRODUCTION_AUDIT_REPORT.md) | Auditoría de preproducción. |
| [SENSITIVE_ENDPOINTS_AUDIT.md](SENSITIVE_ENDPOINTS_AUDIT.md) | Auditoría de endpoints sensibles. |
| [EVIDENCE_UPLOADS_AUDIT.md](EVIDENCE_UPLOADS_AUDIT.md) | Auditoría de subida de evidencias. |
| [PUBLIC_TRACKING_REVIEW.md](PUBLIC_TRACKING_REVIEW.md) | Revisión del tracking público. |
| [MONITORING_ACCESS_REVIEW.md](MONITORING_ACCESS_REVIEW.md) | Revisión de accesos de monitorización. |
| [OPERATIONAL_SCRIPTS_AUDIT.md](OPERATIONAL_SCRIPTS_AUDIT.md) | Auditoría de scripts operacionales. |
| [POST_DEPLOY_CHECKS_REVIEW.md](POST_DEPLOY_CHECKS_REVIEW.md) | Revisión post-despliegue. |
| [BACKUP_RESTORE_REVIEW.md](BACKUP_RESTORE_REVIEW.md) | Revisión del procedimiento de backup/restore. |
| [BACKUP_RETENTION_POLICY.md](BACKUP_RETENTION_POLICY.md) | Política de retención de backups. |

## 4. Despliegue, VPS y producción

### Planificación y decisiones de despliegue

| Documento | Qué contiene |
|---|---|
| [LIVE_DEPLOY_PLAN.md](LIVE_DEPLOY_PLAN.md) | Plan de despliegue en vivo. |
| [REAL_DEPLOY_DECISION_LOG.md](REAL_DEPLOY_DECISION_LOG.md) | Registro de decisiones del despliegue real. |
| [REAL_DEPLOY_TIMELINE.md](REAL_DEPLOY_TIMELINE.md) | Cronología del despliegue real. |
| [FIRST_DEPLOY_RISK_REGISTER.md](FIRST_DEPLOY_RISK_REGISTER.md) | Registro de riesgos del primer despliegue. |
| [DEPLOY_REAL_READY_CHECKLIST.md](DEPLOY_REAL_READY_CHECKLIST.md) | Checklist de preparación del despliegue real. |
| [FINAL_PRODUCTION_DEPLOY_CHECKLIST.md](FINAL_PRODUCTION_DEPLOY_CHECKLIST.md) | Checklist final de despliegue en producción. |

### Guías y runbooks de VPS

| Documento | Qué contiene |
|---|---|
| [VPS_DEPLOY_GUIDE.md](VPS_DEPLOY_GUIDE.md) | Guía de despliegue en VPS. |
| [VPS_DEPLOY_DAY_RUNBOOK.md](VPS_DEPLOY_DAY_RUNBOOK.md) | Runbook del día del despliegue en VPS. |
| [VPS_DEPLOY_EXECUTION_PLAN.md](VPS_DEPLOY_EXECUTION_PLAN.md) | Plan de ejecución del despliegue VPS. |
| [VPS_REAL_EXECUTION_GUIDE.md](VPS_REAL_EXECUTION_GUIDE.md) | Guía de ejecución real en VPS. |
| [VPS_REAL_NEXT_ACTIONS.md](VPS_REAL_NEXT_ACTIONS.md) | Siguientes acciones en el VPS real. |
| [VPS_HARDENING_CHECKLIST.md](VPS_HARDENING_CHECKLIST.md) | Checklist de hardening del VPS. |
| [HETZNER_VPS_PURCHASE_GUIDE.md](HETZNER_VPS_PURCHASE_GUIDE.md) | Guía de compra de VPS en Hetzner. |
| [FIRST_VPS_DEPLOY_CHECKLIST.md](FIRST_VPS_DEPLOY_CHECKLIST.md) | Checklist del primer despliegue en VPS. |
| [FIRST_REAL_DEPLOY_MASTER_CHECKLIST.md](FIRST_REAL_DEPLOY_MASTER_CHECKLIST.md) | Checklist maestra del primer despliegue real. |
| [FIRST_REAL_DEPLOY_COMMANDS.md](FIRST_REAL_DEPLOY_COMMANDS.md) | Comandos del primer despliegue real. |

### Entorno de producción

| Documento | Qué contiene |
|---|---|
| [PRODUCTION_ENV_GUIDE.md](PRODUCTION_ENV_GUIDE.md) | Guía del entorno de producción. |
| [PRODUCTION_VPS_RUNBOOK.md](PRODUCTION_VPS_RUNBOOK.md) | Runbook operativo del VPS de producción. |
| [PRODUCTION_SECRETS_TEMPLATE.md](PRODUCTION_SECRETS_TEMPLATE.md) | Plantilla de secretos de producción. |

## 5. Dominio, DNS, SSL y seguridad de infraestructura

| Documento | Qué contiene |
|---|---|
| [DOMAIN_DNS_SSL_SETUP.md](DOMAIN_DNS_SSL_SETUP.md) | Configuración de dominio, DNS y SSL. |
| [DOMAIN_PURCHASE_GUIDE.md](DOMAIN_PURCHASE_GUIDE.md) | Guía de compra de dominio. |
| [HTTPS_SETUP.md](HTTPS_SETUP.md) | Configuración de HTTPS. |
| [GITHUB_SECRETS_SSH_SETUP.md](GITHUB_SECRETS_SSH_SETUP.md) | Configuración de secretos de GitHub y SSH. |
| [CLOUDFLARE_TUNNEL_DEMO_GUIDE.md](CLOUDFLARE_TUNNEL_DEMO_GUIDE.md) | Guía de túnel de Cloudflare (demo). |

## 6. Calidad, QA y testing

| Documento | Qué contiene |
|---|---|
| [TESTING_STRATEGY.md](TESTING_STRATEGY.md) | Estrategia de testing del proyecto. |
| [QA_BROWSER_CHECKLIST.md](QA_BROWSER_CHECKLIST.md) | Checklist de QA en navegador. |
| [QA_E2E_NIVEL_DIOS.md](QA_E2E_NIVEL_DIOS.md) | Suite E2E de nivel avanzado. |
| [QA_ENDPOINTS_SENSIBLES_PLAN.md](QA_ENDPOINTS_SENSIBLES_PLAN.md) | Plan de QA de endpoints sensibles. |
| [QA_REAL_EXECUTION_LOG.md](QA_REAL_EXECUTION_LOG.md) | Registro de ejecución real de QA. |
| [SMOKE_TESTS_PRODUCTION.md](SMOKE_TESTS_PRODUCTION.md) | Smoke tests en producción. |
| [KNOWN_ISSUES_PREPROD.md](KNOWN_ISSUES_PREPROD.md) | Problemas conocidos en preproducción. |
| [E2E_CI_GUIDE.md](E2E_CI_GUIDE.md) | Guía de E2E en CI. |
| [CONTROLLED_EVIDENCE_DOWNLOAD_PLAN.md](CONTROLLED_EVIDENCE_DOWNLOAD_PLAN.md) | Plan de descarga controlada de evidencias. |

## 7. Backup, operaciones y monitorización

| Documento | Qué contiene |
|---|---|
| [BACKUP_RECOVERY.md](BACKUP_RECOVERY.md) | Procedimiento de backup y recuperación. |
| [UPTIME_MONITORING.md](UPTIME_MONITORING.md) | Monitorización de disponibilidad. |
| [OPERATIONAL_QUALITY_PHASE_2_CLOSURE.md](OPERATIONAL_QUALITY_PHASE_2_CLOSURE.md) | Cierre de la fase 2 de calidad operacional. |
| [PRODUCT_ROBUSTNESS_PHASE_3_CLOSURE.md](PRODUCT_ROBUSTNESS_PHASE_3_CLOSURE.md) | Cierre de la fase 3 de robustez del producto. |

## 8. Demo, preventa y lanzamiento

| Documento | Qué contiene |
|---|---|
| [FREE_DEMO_DEPLOY_OPTIONS.md](FREE_DEMO_DEPLOY_OPTIONS.md) | Opciones gratuitas de despliegue para demo. |
| [RECOMMENDED_FREE_DEMO_PLAN.md](RECOMMENDED_FREE_DEMO_PLAN.md) | Plan recomendado de demo gratuita. |
| [DEMO_SALES_PRESENTATION_SCRIPT.md](DEMO_SALES_PRESENTATION_SCRIPT.md) | Guion de presentación de ventas/demo. |
| [PROJECT_FREEZE_V20.md](PROJECT_FREEZE_V20.md) | Congelación del proyecto versión 2.0. |
| [RELEASE_V20_READY.md](RELEASE_V20_READY.md) | Estado de preparación del release 2.0. |
| [HANDOFF_Envios_Paraguay_CMS.md](HANDOFF_Envios_Paraguay_CMS.md) | Handoff del proyecto (copia histórica). |

## 9. Desarrollo y referencias

| Documento | Qué contiene |
|---|---|
| [LOCAL_DEV_COMMANDS.md](LOCAL_DEV_COMMANDS.md) | Comandos de desarrollo local. |
| [prompt_arranque_ai_studio.md](prompt_arranque_ai_studio.md) | Prompt de arranque para AI Studio. |

---

## Cómo mantener este índice

- Al añadir un documento nuevo, registrarlo aquí en la categoría correspondiente con una línea
  descriptiva de una sola frase.
- Los documentos obsoletos se mueven a una sección `Obsoleto/Histórico` en lugar de borrarse.
- Este índice es la única tabla de contenidos global; el resto de documentos no deben enlazar
  listados globales para evitar duplicidad.
