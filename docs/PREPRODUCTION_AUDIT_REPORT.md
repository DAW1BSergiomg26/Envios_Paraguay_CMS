# Auditoría Técnica de Preproducción

**Fecha:** 2026-05-25
**Rama:** feature/fase-19-estabilizacion-preproduccion
**Origen:** develop (merge desde feature/fase-18-primer-deploy-real)

---

## Estado General

| Componente | Estado |
|------------|--------|
| Docker Compose (6 servicios) | Todos UP |
| Backend Spring Boot | BUILD SUCCESS, 11 tests OK |
| Frontend React | BUILD OK, 15 tests OK |
| Base de datos MySQL | Healthy |
| Nginx reverse proxy | Operativo (local.conf activo) |
| Prometheus | UP |
| Grafana | UP |
| Uptime Kuma | UP |

---

## Servicios Docker

| Contenedor | Puerto | Health |
|------------|--------|--------|
| monteastur-app | :8080 | healthy |
| monteastur-nginx | :8090 / :443 | UP |
| monteastur-mysql | — (interno) | healthy |
| monteastur-prometheus | :9090 | UP |
| monteastur-grafana | :3001 | UP |
| monteastur-uptime-kuma | :3002 | healthy |

---

## Rutas Críticas Probadas

| Ruta | Código | Contenido |
|------|--------|-----------|
| `http://localhost:8090/` | 200 | Home page |
| `http://localhost:8090/login` | 200 | Admin login form |
| `http://localhost:8090/admin/login` | 302 → `/login` | Redirect |
| `http://localhost:8090/admin/dashboard` | 200 | Panel admin con datos |
| `http://localhost:8090/admin/reservas` | 200 | 4 reservas demo |
| `http://localhost:8090/admin/mensajesrecibidos` | 200 | 4 mensajes demo |
| `http://localhost:8090/admin/imagenes` | 200 | 4 imágenes SVG demo |
| `http://localhost:8090/cliente/login` | 200 | Cliente login form |
| `http://localhost:8090/cliente/panel` | 200 | Panel cliente con envíos |
| `http://localhost:8090/login-react` | 200 | React SPA login |
| `http://localhost:8090/tracking` | 200 | Tracking público |
| `http://localhost:8080/actuator/health` | 200 | `{"status":"UP"}` |
| `/api/v1/tracking/MT-2026-0001` | 200 | JSON demo data |
| `/api/v1/tracking/MT-2026-0004` | 200 | JSON demo data |

---

## Credenciales Locales

| Rol | URL | Usuario | Contraseña |
|-----|-----|---------|------------|
| Admin | `/login` | `admin` | `admin123` |
| Cliente | `/cliente/login` | `cliente@monteastur.com` | `demo2026` |
| React SPA | `/login-react` | `admin` | `admin123` |
| Grafana | `http://localhost:3001` | `admin` | `admin123` |

---

## Datos Demo

| Tipo | Cantidad | Verificado |
|------|----------|------------|
| Clientes | 1 | 1 en BD |
| Envíos tracking | 4 | API 200 para todos |
| Eventos tracking | 18 | Visibles en panel admin |
| Evidencias | 3 | Visibles en detalle envío |
| Mensajes contacto | 4 | Listados en admin |
| Reservas | 4 | Listadas en admin |
| Imágenes galería | 4 | SVGs servidos correctamente |
| Textos legales | 2 | Aviso legal + cookies |

---

## Tests Ejecutados

### Backend (Java + Maven)

| Test | Tests | Resultado |
|------|-------|-----------|
| SecurityConfigTest | 3 | ✅ 0 fallos |
| PushSubscriptionControllerTest | 3 | ✅ 0 fallos |
| TrackingApiControllerTest | 2 | ✅ 0 fallos |
| ReservaServiceTest | 3 | ✅ 0 fallos |
| **Total backend** | **11** | **BUILD SUCCESS** |

### Frontend (React + Vitest)

| Test | Tests | Resultado |
|------|-------|-----------|
| StatsCard | 2 | ✅ pasado |
| EmptyState | 2 | ✅ pasado |
| StatusBadge | 3 | ✅ pasado |
| SearchBar | 4 | ✅ pasado |
| LoginPage | 4 | ✅ pasado |
| **Total frontend** | **15** | **✅ pasado** |

---

## Seguridad Básica

| Aspecto | Estado |
|---------|--------|
| Spring Security CSRF | ✅ Activado (excepto `/api/**`) |
| HTTPS en local | ❌ Solo HTTP (local dev intencional) |
| HTTPS en producción | 🟡 Pendiente (certificados SSL) |
| Security headers (nginx) | ✅ HSTS, CSP, X-Frame-Options, etc. |
| Contraseñas por defecto | ⚠️ Temporales (admin123, demo2026) |
| CORS | ✅ No expuesto |
| Session HttpOnly | ✅ Activado |
| Session SameSite | ✅ Lax |
| SQL Injection | ✅ Protegido por JPA/Hibernate |
| XSS | ✅ Thymeleaf escapa por defecto |

---

## Riesgos Pendientes

| Riesgo | Impacto | Prioridad |
|--------|---------|-----------|
| Naming legacy CasaRural | Confusión en branding | Media |
| package com.grupb2.casarural legacy | Refactor costoso | Baja |
| admin123 / demo2026 en producción | Seguridad | **ALTA** |
| E2E requiere stack activo | CI/CD parcial | Media |
| npm audit vulnerabilities | Dependencias | Media |
| Puerto 80 ocupado por IIS en Windows | Dev local Windows | Baja |
| docker compose down -v borra datos | Pérdida accidental | Media |

---

## Decisión Final

### ✅ LISTO PARA PRODUCCIÓN (con condiciones)

**Bloqueantes:** Ninguno detectado.

**Condiciones:**
1. Cambiar `ADMIN_PASSWORD` y contraseñas BD antes de deploy real
2. Configurar certificados SSL (Let's Encrypt) y cambiar a HTTPS
3. Asegurar que `APP_DEMO_DATA=false` en producción (ya configurado en `application-prod.properties`)
4. Verificar variables de entorno en el VPS

**Siguiente fase recomendada:** Fase 19.2 — Refactor naming (CasaRural → Monteastur)
