# HANDOFF - Envios_Paraguay_CMS

## 📋 Resumen del Proyecto

**Envios_Paraguay_CMS** es una aplicación full-stack desarrollada en **Spring Boot** (Backend) y **Thymeleaf** (Frontend server-side) para gestionar envíos y operaciones logísticas entre Asturias/España y Paraguay: perfil de administración seguro, gestión de envíos, tracking público, notificaciones y control por roles.

---

## 🏗️ Arquitectura y Tecnologías

- **Backend:** Java 17, Spring Boot 3.3.5, Spring Security, Spring Data JPA, Hibernate, Flyway.
- **Base de Datos:** MySQL 8 (perfil de producción apunta a TiDB Cloud y valida el esquema con `ddl-auto=validate`).
- **Caché / Sesiones:** Redis (sesiones distribuidas y caché del tracking público).
- **Servidor Web / Reverse Proxy:** Nginx (caché estático agresivo, cabeceras de seguridad y Let's Encrypt).
- **Email:** JavaMailSender (SMTP) con soporte para Mailpit en desarrollo.
- **Observabilidad:** Prometheus, Grafana y uptime-kuma (definidos en `docker-compose.yml`).
- **Contenedores:** Docker y Docker Compose (`docker-compose.yml`, `start-all.ps1`).

Servicios del compose: `db` (MySQL), `app`, `nginx`, `certbot`, `prometheus`, `grafana`, `uptime-kuma`, `redis`.

---

## 🔒 Mejoras de Hardening y Seguridad Recientes (Sprint Actual)

1. **Endurecimiento de Producción (`application-prod.properties`)** — commit `595818e`:
   - Eliminados los fallbacks inseguros por defecto para las credenciales de base de datos (`DB_USERNAME`, `DB_PASSWORD`) y del administrador (`ADMIN_USERNAME`, `ADMIN_PASSWORD`). Ahora son variables obligatorias sin valor por defecto.
   - Forzado de `spring.jpa.hibernate.ddl-auto=validate` para evitar alteraciones automáticas de esquemas en producción.
2. **Validación de Entorno al Arranque** — commit `d87c7da`:
   - `MonteasturApplication.java` inyecta `Environment` y ejecuta `@PostConstruct validateEnvironment()`: cuando el perfil activo es `prod`, valida que `DB_USERNAME` y `DB_PASSWORD` existan; si falta alguna, registra el error y aborta el arranque lanzando `IllegalStateException`. En desarrollo, la validación se omite y se loguea un aviso informativo.
3. **Optimización Nginx** — commit `efd2bd9`:
   - Caché estático agresivo (`expires 30d;`, `add_header Cache-Control "public, immutable";`) para CSS, JS, fuentes e imágenes en `nginx/conf.d/local.conf` y `nginx/conf.d/monteastur.conf`, antes del bloque `location /`.
4. **Módulo de Notificaciones Automáticas (en curso)**:
   - Spec de diseño: commit `841fb2b`.
   - Migración Flyway `V3` (tabla `notificaciones`), entidad `Notificacion` y repositorio: commit `d46a08c`.
   - Evento `EstadoEnvioActualizadoEvent`, `@EnableAsync` y propiedades SMTP/notificaciones: commit `5dc9a4f`.
   - `EmailService.enviarCorreoSimple(...)`: commit `7bc3182`.
   - Pendiente: listener asíncrono, `actualizarEstado` en servicio/controller, tests de integración y wiring Mailpit/compose.

---

## 🚀 Guía de Arranque Rápido para Desarrolladores

### 1. Requisitos previos

- Tener instalado **Docker** y **Docker Compose**.
- Java 17 + Maven (solo si se compila localmente; también se puede compilar con el contenedor `maven:3.9-eclipse-temurin-17`).

### 2. Variables de Entorno

Asegúrate de definir las variables de entorno críticas antes de levantar el perfil de producción (`prod`), especialmente:

- `DB_USERNAME`
- `DB_PASSWORD`
- `ADMIN_USERNAME`
- `ADMIN_PASSWORD`

En local, la mayoría están en `.env` (no versionado). El arranque valida su presencia en el perfil `prod`.

### 3. Comprobación y Arranque

- **Validar sintaxis de Nginx:**

  ```powershell
  docker compose run --rm nginx nginx -t
  ```

  Debe devolver `syntax is ok` y `test is successful`.

- **Levantar el stack completo:**

  ```powershell
  docker compose up -d --build
  ```

  o usar el script `start-all.ps1`.

- **Compilar (sin JDK local):**

  ```powershell
  docker run --rm -v "${PWD}:/app" -w /app -v "${HOME}\.m2:/root/.m2" maven:3.9-eclipse-temurin-17 mvn clean compile -q
  ```

---

## 📌 Estado Git Actual

- **Rama:** `main` (estable).
- **HEAD:** `7bc3182` (`refactor: update email service configurations and startup script`).
- Flujo de ramas: `main` = estable, `develop` = integración, `feature/*` = mejoras concretas.
- No hacer push ni merge sin confirmación explícita del usuario.

---

## 📝 Reglas de Trabajo

1. No empezar el proyecto desde cero.
2. No cambiar arquitectura sin explicar riesgos.
3. No mezclar demasiadas mejoras en una sola tarea.
4. Antes de modificar archivos: `git status` y `git branch`.
5. Cambios pequeños y revisables.
6. Mantener coherencia español/inglés.
7. Probar antes de sugerir commit.
8. No hacer push ni merge sin confirmación.
