# 📦 Envios Paraguay CMS

Sistema integral de gestión de paquetería y envíos, desarrollado como proyecto avanzado con una arquitectura robusta de microservicios y contenedores, diseñado para ofrecer trazabilidad en tiempo real, alta disponibilidad y observabilidad completa.

---

## 🚀 Tecnologías Principales

- **Backend:** Java 25, Spring Boot 3.4+, Spring Data JPA, Flyway (Control de versiones de BD), Spring Security, Actuator, Micrometer.
- **Frontend:** React (Vite), Tailwind CSS, WebSockets en tiempo real.
- **Base de Datos y Persistencia:** MySQL 8, Redis (Caché y sesiones).
- **Infraestructura & Observabilidad:** Docker Compose, Nginx, Prometheus, Grafana, Uptime Kuma, Mailpit.
- **Testing:** JUnit 5, Mockito, Testcontainers (445 tests automatizados con 0 fallos).

---

## 🛠️ Arquitectura y Servicios (Docker Compose)

El ecosistema se levanta de forma completamente isolada mediante contenedores interconectados:

1. **`monteastur-app`**: Aplicación backend Spring Boot (Java 25).
2. **`db`**: Base de datos MySQL persistente (`envios_paraguay_cms`).
3. **`redis`**: Sistema de caché y soporte para tiempo real.
4. **`nginx`**: Servidor web y reverse proxy con soporte SSL/Certbot.
5. **`prometheus` & `grafana`**: Monitorización de métricas de negocio y rendimiento.
6. **`mailpit`**: Servidor SMTP de pruebas para intercepción de correos.

---

## ⚙️ Puesta en Marcha Rápida

Para levantar todo el entorno de producción/desarrollo local con un solo comando:

```bash
docker compose up -d --build
```
