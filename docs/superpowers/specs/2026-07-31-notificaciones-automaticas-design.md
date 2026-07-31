# Módulo de Notificaciones Automáticas (Eventos de Dominio) — Diseño Técnico

**Fecha:** 2026-07-31
**Rol:** Arquitecto de Software. Diseño e implementación del módulo de notificaciones por email ante cambios de estado de envíos.

## Objetivo

Implementar una Arquitectura de Eventos de Dominio desacoplada: cuando se actualice el estado de un envío, `EnvioTrackingService` emitirá `EstadoEnvioActualizadoEvent`. Un listener asíncrono escuchará el evento solo tras la confirmación de la transacción (`AFTER_COMMIT`), despachará un correo al cliente asociado vía SMTP y guardará el histórico en la tabla `notificaciones` (MySQL/Flyway).

## Contexto del proyecto (hallazgos de exploración)

- Proyecto Spring Boot 3.3.5, Java 17, Maven, MySQL 8, Flyway (V1+V2 existentes), Redis (caché + sesiones).
- **No hay Lombok** ni `mvnw`. Todos los modelos usan getters/setters manuales por convención explícita.
- **No existe `EnvioService`**: el servicio real es `EnvioTrackingService`; la tabla es `envios_tracking` (no `envios`). `estado` es `String` plano (no enum `EstadoEnvio`). Los estados conocidos: `RECIBIDO`, `EN_ADUANA_ORIGEN`, `EN_TRANSITO`, `EN_ADUANA_DESTINO`, `EN_REPARTO`, `ENTREGADO`.
- `spring-boot-starter-mail` **ya está** en el pom. `EmailService` existente envía a destinatario fijo `spring.mail.to` (admin).
- `spring.mail.*` está comentado en `application.properties`. Config en `.properties`, no `.yml`.
- **No existe `@EnableAsync`**, ni `AsyncConfig`, ni listeners de eventos de dominio.
- `AdminApiController.actualizarEstado` (PUT `/api/v1/admin/envios/{codigo}/estado`) es el único punto que cambia el estado (hoy inline: `findWithClienteByCodigoUnico` → `setEstado` → `guardar` → `crearEvento`).
- `EnvioTrackingRepository.findWithClienteByCodigoUnico` existe (EntityGraph con `cliente`).
- Tests actuales: 49, todos slice tests (`@WebMvcTest`, `@ExtendWith(SpringExtension.class)` con configs mockeadas). No existe `src/test/resources/application-test.properties`. CI usa MySQL service pero **no Redis**. No hay `awaitility` en el pom.
- No hay servicio SMTP/mailpit en `docker-compose.yml`.

## Decisiones de arquitectura (confirmadas con el stakeholder)

1. **Enfoque: evento liviano (recomendado).** `EstadoEnvioActualizadoEvent` es un record con solo datos primitivos (`envioId`, `codigoRastreo`, `estadoAnterior`, `estadoNuevo`, `timestamp`). El listener re-consulta el envío con cliente fresco tras el commit. Evita entidades detached / `LazyInitializationException` en el hilo async. (Descartados: evento con entidad completa y llamada directa en el controller.)
2. **Disparo:** solo en transiciones reales de estado (el estado cambia respecto al anterior). No en creación/edición.
3. **Destinatario:** el cliente registrado asociado al envío (`clientes.email`). No admin.
4. **Envío sin cliente/email:** se registra `OMITIDO_SIN_DESTINATARIO`, sin intento de envío ni error.
5. **Reintentos:** ninguno. Fallo SMTP → estado `FALLIDO` con `error_mensaje` (auditable). Sin jobs/scheduler.
6. **Contenido del correo:** texto plano con datos del envío (código, estado nuevo, ubicación) + enlace de seguimiento `app.notification.tracking.base-url/{codigo}`. Reutiliza `SimpleMailMessage`.
7. **SMTP dev:** se añade servicio **Mailpit** al `docker-compose.yml` (SMTP `localhost:1025`, UI `http://localhost:8025`).
8. **Redundancia/guard:** `app.notification.mail.enabled` (default `true`) permite apagar el módulo sin deploy. Si está `false`, el listener no envía ni registra.

## Adaptaciones al prompt original (obligatorias para el código real)

1. **Sin Lombok:** `Notificacion` usa getters/setters manuales, igual que el resto de entidades.
2. **Refactor del controller:** `AdminApiController.actualizarEstado` debe delegar en `EnvioTrackingService.actualizarEstado(codigo, nuevoEstado)` (que publica el evento). Conserva `crearEvento` (timeline) y `toTrackingDto`. El controller actualiza el DTO con el envío devuelto.
3. **Redis para tests:** el `@SpringBootTest` carga el contexto completo (incluido `RedisConfig` + `@CacheEvict` en `guardar`). Se necesita:
   - `src/test/resources/application-test.properties` (perfil `test`): apunta a MySQL + Redis de test; `spring.cache.type=redis` no aplica (el `cacheManager` lo define `RedisConfig`).
   - Añadir servicio `redis` al `ci.yml` para que el test de integración pase en CI.
4. **`awaitility`** (test scope) al pom — no existe.
5. **`EmailService.enviarCorreoSimple(String para, String asunto, String texto)`:** nuevo método público que envía a destinatario variable usando `app.notification.mail.from` como remitente. Los métodos existentes no cambian.
6. **Migración `V3`** (V1 y V2 existen).

## Componentes

### 1. Migración Flyway — `src/main/resources/db/migration/V3__create_notificaciones_table.sql`

```sql
CREATE TABLE notificaciones (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    envio_id BIGINT NOT NULL,
    destinatario VARCHAR(150) NULL,
    asunto VARCHAR(255) NOT NULL,
    mensaje TEXT NOT NULL,
    estado VARCHAR(30) NOT NULL COMMENT 'ENVIADO, FALLIDO, OMITIDO_SIN_DESTINATARIO',
    error_mensaje TEXT NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notificaciones_envio_tracking
        FOREIGN KEY (envio_id) REFERENCES envios_tracking(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_notificaciones_envio_id ON notificaciones (envio_id);
CREATE INDEX idx_notificaciones_estado ON notificaciones (estado);
```

### 2. Entidad — `src/main/java/com/monteastur/envios/model/Notificacion.java`

- `@Entity @Table(name = "notificaciones")`, getters/setters manuales (sin Lombok).
- Campos: `id`, `envioId`, `destinatario`, `asunto`, `mensaje`, `errorMensaje`, `fechaCreacion`.
- Enum anidado `EstadoNotificacion { ENVIADO, FALLIDO, OMITIDO_SIN_DESTINATARIO }` con `@Enumerated(EnumType.STRING)`.
- `@PrePersist`: asigna `LocalDateTime.now()` si `fechaCreacion` es null.

### 3. Repositorio — `src/main/java/com/monteastur/envios/repository/NotificacionRepository.java`

- `extends JpaRepository<Notificacion, Long>`.
- `List<Notificacion> findByEnvioIdOrderByFechaCreacionDesc(Long envioId);`

### 4. Evento — `src/main/java/com/monteastur/envios/event/EstadoEnvioActualizadoEvent.java`

```java
public record EstadoEnvioActualizadoEvent(
    Long envioId,
    String codigoRastreo,
    String estadoAnterior,
    String estadoNuevo,
    LocalDateTime timestamp
) {
    public EstadoEnvioActualizadoEvent(Long envioId, String codigoRastreo, String estadoAnterior, String estadoNuevo) {
        this(envioId, codigoRastreo, estadoAnterior, estadoNuevo, LocalDateTime.now());
    }
}
```

### 5. Async — `src/main/java/com/monteastur/envios/config/AsyncConfig.java`

- `@Configuration @EnableAsync`. Sin TaskExecutor custom (default suficiente para el volumen).

### 6. Propiedades — final de `src/main/resources/application.properties`

```properties
# =========================
# SMTP (Mailpit/dev)
# =========================
spring.mail.host=${SPRING_MAIL_HOST:localhost}
spring.mail.port=${SPRING_MAIL_PORT:1025}
spring.mail.username=${SPRING_MAIL_USERNAME:}
spring.mail.password=${SPRING_MAIL_PASSWORD:}
spring.mail.properties.mail.smtp.auth=${SPRING_MAIL_SMTP_AUTH:false}
spring.mail.properties.mail.smtp.starttls.enable=${SPRING_MAIL_STARTTLS_ENABLE:false}

# Notificaciones de negocio
app.notification.mail.from=no-reply@enviosparaguay.com.py
app.notification.mail.enabled=${APP_NOTIFICATION_MAIL_ENABLED:true}
app.notification.tracking.base-url=${APP_TRACKING_BASE_URL:http://localhost:8080/tracking}
```

### 7. Listener — `src/main/java/com/monteastur/envios/listener/NotificacionEventListener.java`

- `@Component`, inyecta `EnvioTrackingRepository`, `NotificacionRepository`, `EmailService` y `app.notification.mail.enabled` / `base-url`.
- Método `@Async @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) @Transactional(readOnly = true)`:
  1. Si `enabled == false` → log + return.
  2. Busca el envío con cliente: `envioTrackingRepository.findWithClienteByCodigoUnico(event.codigoRastreo())`.
  3. Si no existe envío, cliente o email en blanco → guarda `Notificacion(estado=OMITIDO_SIN_DESTINATARIO, destinatario=null)` y return.
  4. Si hay email → construye asunto/mensaje (datos del envío + enlace `base-url/codigo`), `emailService.enviarCorreoSimple(...)` en try/catch:
     - éxito → `ENVIADO`.
     - excepción → `FALLIDO` con `error_mensaje = e.getMessage()`.
  5. Guarda la `Notificacion` (destinatario = email del cliente).

**Nota de diseño:** `@Transactional(readOnly = true)` no bloquea el `save()` de una entidad nueva (Hibernate 6 no fuerza `FlushMode.MANUAL`); el registro persiste al cierre del hilo async.

### 8. EmailService — método nuevo

```java
public void enviarCorreoSimple(String para, String asunto, String texto) {
    // usa app.notification.mail.from como remitente
    // si mailSender == null → lanza IllegalStateException (el listener
    //   captura y registra FALLIDO; un return silencioso falsearía el registro ENVIADO)
    // try/catch similar al de enviar() existente
}
```

### 9. `EnvioTrackingService.actualizarEstado(String codigo, String nuevoEstado)`

- Inyecta `ApplicationEventPublisher`.
- Busca por `findWithClienteByCodigoUnico(codigo.trim().toUpperCase())`; si no existe → `ResourceNotFoundException`.
- Si `nuevoEstado.equals(estado)` → devuelve el envío sin publicar (no hay transición).
- Si cambia → `setEstado`, `guardar(envio)` (conserva `@CacheEvict` de `envios.tracking`), publica `EstadoEnvioActualizadoEvent(envioGuardado.getId(), codigo, estadoAnterior, nuevoEstado)` y devuelve el envío guardado.

### 10. Refactor — `AdminApiController.actualizarEstado`

- Reemplaza el bloque inline (líneas 142-148) por:
  `EnvioTracking actualizado = envioTrackingService.actualizarEstado(codigo, request.getEstado());`
  `eventoTrackingService.crearEvento(actualizado, estadoAnterior);`
  `return ResponseEntity.ok(toTrackingDto(actualizado));`
- `estadoAnterior` se obtiene de una carga previa del envío (el controller ya lo necesita para el DTO). Alternativa aceptada: `actualizarEstado` devuelve el envío y `estadoAnterior` se captura ANTES de delegar, o el método del servicio expone el estado anterior vía el evento. **Decisión final:** el controller conserva `trackingRepo.findWithClienteByCodigoUnico(...)` para capturar `estadoAnterior` y DTO, y delega la mutación + publicación al servicio con el código (re-carga aceptada por simplicidad y bajo tráfico admin).

> **Nota de coherencia:** `guardar()` (con `@CacheEvict`) es quien invalida la caché pública; como `actualizarEstado` lo invoca, el flujo cacheado se mantiene consistente.

### 11. Infra de dev — `docker-compose.yml`

```yaml
  mailpit:
    image: axllent/mailpit:latest
    container_name: monteastur-mailpit
    ports:
      - "${MAILPIT_UI_PORT:-8025}:8025"
      - "1025:1025"
    restart: unless-stopped
    networks:
      - backend
    mem_limit: 128m
```

### 12. Test de integración — `src/test/java/com/monteastur/envios/integration/EnvioNotificacionIntegrationTest.java`

- `@SpringBootTest`, `@ActiveProfiles("test")`, `@MockBean EmailService`.
- Inyecta `EnvioTrackingService`, `NotificacionRepository`, `ClienteRepository`, `EnvioTrackingRepository`, `TransactionTemplate`.
- Setup: crea `Cliente` (email único de test) + `EnvioTracking` (código `TEST-...` único, estado inicial `RECIBIDO`) y persiste ambos (asociados). Cleanup en `@AfterEach`: borra envío (CASCADE borra notificaciones) y cliente.
- Test: dentro de `transactionTemplate.executeWithoutResult(...)` llama `envioTrackingService.actualizarEstado(codigo, "EN_TRANSITO")` (fuerza commit → AFTER_COMMIT).
- `awaitility.await().atMost(5s)` → `notificacionRepository.findByEnvioId...` tiene exactamente 1 registro con estado `ENVIADO`.
- Segundo test: envío sin cliente → 1 registro `OMITIDO_SIN_DESTINATARIO`.

### 13. Perfil de test — `src/test/resources/application-test.properties`

```properties
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:mysql://localhost:3307/envios_paraguay_cms_test?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true}
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:}
spring.jpa.hibernate.ddl-auto=validate
spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=${REDIS_PORT:6379}
spring.mail.host=localhost
spring.mail.port=1025
app.notification.mail.from=no-reply@test.local
app.notification.tracking.base-url=http://localhost:8080/tracking
app.demo-data=false
```

**Nota local:** la base `envios_paraguay_cms_test` debe existir en el MySQL local (CI la crea vía `MYSQL_DATABASE`); en local crearla manualmente (p.ej. `CREATE DATABASE envios_paraguay_cms_test`) antes de `mvn test`, o sobrescribir con `SPRING_DATASOURCE_URL` apuntando a la base dev. `app.demo-data=false` evita que `DataInitializer` siembre datos demo dentro del test.

### 14. CI — `.github/workflows/ci.yml`

- Añadir servicio `redis` (imagen `redis:7-alpine`, ports 6379) junto al service `mysql`.
- No requiere credenciales; el perfil `test` toma `SPRING_DATASOURCE_URL` (ya existente) y `REDIS_HOST=localhost`.

### 15. pom.xml

- Añadir `org.awaitility:awaitility` (test scope).

## Flujo de datos

```
PUT /api/v1/admin/envios/{codigo}/estado
  → AdminApiController.actualizarEstado
    → EnvioTrackingService.actualizarEstado(codigo, nuevoEstado)
        ├─ guardar() → @CacheEvict(envios.tracking)   [evict tras commit]
        └─ publishEvent(EstadoEnvioActualizadoEvent)  [dentro de la tx]
  → [commit]
AFTER_COMMIT + @Async → NotificacionEventListener
  ├─ findWithClienteByCodigoUnico(codigo)      [tx nueva read-only]
  ├─ ¿cliente + email?
  │    ├─ no  → Notificacion(OMITIDO_SIN_DESTINATARIO)
  │    └─ sí  → EmailService.enviarCorreoSimple(email, asunto, texto+link)
  │              ├─ ok    → Notificacion(ENVIADO)
  │              └─ error → Notificacion(FALLIDO, error_mensaje)
  └─ save(Notificacion)
```

## Manejo de errores

- **Envío inexistente al transicionar:** `ResourceNotFoundException` (ya gestionada por `GlobalExceptionHandler` → 404).
- **Sin cliente/email:** `OMITIDO_SIN_DESTINATARIO`, sin excepción.
- **Fallo SMTP (mailSender null o `MailSendException`):** `FALLIDO` + `error_mensaje`, sin reintento, sin propagar la excepción fuera del listener (no rompe el request).
- **Listener lanza excepción no capturada:** el `@Async` la registra en logs del hilo; no afecta al request original (ya commiteado). Queda auditada en `notificaciones` si se alcanzó el `save`.
- **Redis caído en test/CI:** no aplica — el test de integración corre con Redis disponible (servicio CI + compose local).

## Testing

1. **Test de integración nuevo** (sección 12): transición con cliente → `ENVIADO`; sin cliente → `OMITIDO_SIN_DESTINATARIO`; corre con `@SpringBootTest` contra MySQL+Redis de test.
2. **Suite existente (49):** no se modifica; sigue pasando (slice tests aislados).
3. **Verificación runtime (post-suite):** stack local con Mailpit — `PUT .../estado` → correo visible en `http://localhost:8025` y fila en `notificaciones`.
4. **Gate:** `mvn test` con contenedor `maven:3.9-eclipse-temurin-17` (no existe `mvnw`) → `BUILD SUCCESS` con **50+ tests** (49 existentes + nuevos). En local, sobre el perfil `test` con MySQL/Redis levantados.

## Fuera de alcance

- Plantillas HTML/Thymeleaf para correos (solo texto plano).
- Reintentos/scheduler/outbox pattern.
- Notificaciones push/web (existe `PushSubscriptionController` independiente).
- Modificación del `EmailService` existente (solo se añade método).

## Riesgos y mitigaciones

- **`@Transactional(readOnly=true)` + save en hilo async:** verificado en runtime con el test de integración; si Spring forzara `FlushMode.MANUAL`, se retira el `readOnly`.
- **Doble carga del envío en el controller:** aceptada (bajo tráfico admin); alternativa futura: devolver resultado con estado anterior.
- **`@MockBean` deprecado en Boot 3.4+:** el proyecto está en 3.3.5; si se actualiza, migrar a `@MockitoBean`.
- **Mailpit en prod:** el servicio es solo para dev; en prod se sobreescribe vía `SPRING_MAIL_*`/`APP_TRACKING_BASE_URL` en `.env`.
