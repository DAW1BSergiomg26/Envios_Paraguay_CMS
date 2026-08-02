# Bloque 13 — Módulo de Evidencia Digital de Entrega (POD) con Firma Digital y GPS

- Fecha: 2026-08-02
- Estado: Aprobado (decisiones de diseño acordadas con el usuario)
- Base: `main` tras Bloques 11 y 12 (working tree limpio, 159 tests en verde, HEAD `fa3fc76`)

## Objetivo

Dotar al sistema de la capacidad de registrar la entrega final de los paquetes mediante
una API REST segura y validada, permitiendo a la red de distribución registrar:

1. **Identidad del Receptor:** nombre completo y número de documento (DNI/CI).
2. **Firma Digital:** captura de la firma manuscrita codificada en Base64/PNG.
3. **Geolocalización GPS:** latitud y longitud con validación estricta de rangos geodésicos.
4. **Metadatos Opcionales:** notas o incidencias de entrega.
5. **Transaccionalidad e integración de eventos:** al registrar un POD con éxito se
   persiste la evidencia, se actualiza de forma atómica el estado del envío a
   `ENTREGADO` y se propagan los eventos corporativos (invalidación de caché Redis,
   webhooks HMAC-SHA256 y notificaciones).

## Restricciones críticas

- **Prohibido Lombok:** entidad `EntregaEvidencia`, DTOs y validadores en Java puro
  (constructor vacío obligatorio para JPA, constructor parametrizado, getters/setters).
- **Inyección por constructor:** campos `private final` inicializados en el constructor.
- **Migración Flyway V8 exacta** en
  `src/main/resources/db/migration/V8__create_proof_of_delivery_tables.sql`
  (SQL del usuario, tabla `entregas_evidencia`).
- **TDD:** tests unitarios (`@WebMvcTest`) y de integración (`@SpringBootTest`), AssertJ
  y Awaitility para flujos asíncronos. Objetivo `mvn clean test` con BUILD SUCCESS.

## Decisiones de diseño aprobadas

1. **Seguridad:** los endpoints `/api/v1/deliveries/**` quedan bajo `authenticated()` y se
   habilita **HTTP Basic** (`http.httpBasic()`) para clientes REST (app móvil/distribución).
   Además, `@PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_OPERADOR')")` a nivel de clase.
   El login form/sesión del admin se mantiene intacto. `OpenApiConfig` ya documenta
   `basicAuth`, por lo que solo se toca `SecurityConfig`.
2. **Reuso del pipeline de eventos tal cual:** `registrarEntrega` llama a
   `EnvioTrackingService.actualizarEstado(codigo, "ENTREGADO")` dentro de la misma
   transacción; eso ya publica `EstadoEnvioActualizadoEvent`, cuyos listeners
   (`NotificacionEventListener`, `WebhookEventListener`) corren en `AFTER_COMMIT` de forma
   asíncrona. **No se modifica** el evento ni `WebhookPayloadBuilder`.
3. **POD duplicado → 409 Conflict:** `envio_id` es `UNIQUE`; si ya existe evidencia para el
   envío se lanza `ConflictException` (ya manejada por `GlobalExceptionHandler`).
4. **Validación rigurosa de la firma:** obligatoria, no vacía, **decodificación Base64** y
   **verificación de los magic bytes PNG** (`\x89PNG\r\n\x1a\n`); si no es un PNG válido → 400.
5. **Coordenadas opcionales pero validadas:** si se envían, latitud `[-90, 90]` y longitud
   `[-180, 180]` → 400 en caso contrario. `DECIMAL(10,8)` / `DECIMAL(11,8)` en la tabla,
   mapeadas como `Double` en la entidad (precisión suficiente para GPS).
6. **Invalidación de caché:** `actualizarEstado` ya evicta `envios.tracking`; como el estado
   cambia a `ENTREGADO`, `registrarEntrega` añade `@CacheEvict(value = "envios.dashboard",
   allEntries = true)` para que el dashboard no quede obsoleto.
7. **Timeline público:** se crea el evento de tracking (`EventoTrackingService.crearEvento`)
   con `ENTREGADO` para que el historial público muestre la entrega, igual que hace
   `AdminApiController.actualizarEstado`.

## Arquitectura

```
POST /api/v1/deliveries/{codigo}/pod   (ADMIN u OPERADOR) → 201 EntregaEvidenciaDto
GET  /api/v1/deliveries/{codigo}/pod   (ADMIN u OPERADOR) → 200 EntregaEvidenciaDto
        │
        ▼
EntregaEvidenciaController (authenticated + hasAnyRole ADMIN/OPERADOR)
        │  usuario = Authentication.getName()
        ▼
EntregaEvidenciaService
        │  - EntregaValidator (validación estricta → 400)
        │  - cargar envío por codigoUnico (→ 404)
        │  - existsByEnvioId (→ 409)
        │  - guardar EntregaEvidencia
        │  - actualizarEstado(codigo, "ENTREGADO")  → EstadoEnvioActualizadoEvent
        │      │                                       (webhooks + notificaciones async AFTER_COMMIT)
        │      └── @CacheEvict envios.tracking (interno) + envios.dashboard (nuevo)
        │  - eventoTrackingService.crearEvento(actualizado, estadoAnterior)
        ▼
EntregaEvidenciaRepository
  - findByEnvioId / existsByEnvioId
```

### Componentes (main)

| Clase | Paquete | Responsabilidad |
|---|---|---|
| `EntregaEvidencia` (entidad) | `model` | Evidencia de entrega (tabla `entregas_evidencia`, `@PrePersist` para `fechaEntrega`) |
| `EntregaEvidenciaRepository` | `repository` | `findByEnvioId`, `existsByEnvioId` |
| `EntregaValidator` | `service` | Validación estática: campos obligatorios, firma Base64+PNG, rangos GPS (métodos públicos, testable sin Spring) |
| `EntregaEvidenciaService` | `service` | Orquestación transaccional `registrarEntrega` / `obtenerEntrega` |
| `EntregaEvidenciaController` | `controller.api` | Endpoints REST `/api/v1/deliveries` |
| `RegistrarEntregaRequest` | `dto.api` | Payload de alta (mutable, Java puro) |
| `EntregaEvidenciaDto` | `dto.api` | Respuesta JSON (inmutable, `static from(...)`, estilo `DocumentoGeneradoDto`) |

### Modificaciones a código existente

- `SecurityConfig`: añadir `http.httpBasic()` y `requestMatchers("/api/v1/deliveries/**").authenticated()`.
- No se tocan: `EstadoEnvioActualizadoEvent`, `WebhookPayloadBuilder`, `GlobalExceptionHandler`
  (400/404/409 ya cubiertos), repos de webhooks/notificaciones.

## Migración

### V8 (exacta del usuario)

```sql
CREATE TABLE entregas_evidencia (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    envio_id BIGINT NOT NULL UNIQUE,
    receptor_nombre VARCHAR(150) NOT NULL,
    receptor_documento VARCHAR(50) NOT NULL,
    firma_base64 LONGTEXT NOT NULL,
    latitud DECIMAL(10, 8) NULL,
    longitud DECIMAL(11, 8) NULL,
    notas TEXT NULL,
    fecha_entrega DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_entregas_evidencia_envio FOREIGN KEY (envio_id) REFERENCES envios_tracking(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

## Validaciones (`EntregaValidator`)

| Regla | Resultado |
|---|---|
| `receptorNombre` blank o > 150 | 400 |
| `receptorDocumento` blank o > 50 | 400 |
| `firmaBase64` blank o > 5 MB de base64 | 400 |
| `firmaBase64` no decodifica a Base64 válido | 400 |
| `firmaBase64` decodificada sin magic bytes PNG (`89 50 4E 47`) | 400 |
| `latitud` presente fuera de `[-90.0, 90.0]` | 400 |
| `longitud` presente fuera de `[-180.0, 180.0]` | 400 |
| `notas` presente > 2000 caracteres | 400 |

Todas lanzan `BadRequestException` con mensaje descriptivo en español.

## DTOs

```java
// RegistrarEntregaRequest (mutable)
String receptorNombre;      // obligatorio
String receptorDocumento;   // obligatorio
String firmaBase64;         // obligatorio (Base64 PNG)
Double latitud;             // opcional
Double longitud;            // opcional
String notas;               // opcional

// EntregaEvidenciaDto (inmutable, static from(EntregaEvidencia))
Long id;
String codigoRastreo;
String receptorNombre;
String receptorDocumento;
String firmaBase64;
Double latitud;
Double longitud;
String notas;
String fechaEntrega;   // ISO_LOCAL_DATE_TIME
```

## Manejo de errores

- `ResourceNotFoundException` (404): envío por código inexistente; POD inexistente en el GET.
- `BadRequestException` (400): fallos de validación (`EntregaValidator`).
- `ConflictException` (409): ya existe POD para el envío.
- Sin autenticación en `/api/v1/deliveries/**` → 401 (HTTP Basic). Con rol distinto a
  ADMIN/OPERADOR → 403 (`AccessDeniedException` → `CustomAccessDeniedHandler`).

## Estrategia de tests (TDD)

| Test | Alcance |
|---|---|
| `EntregaValidatorTest` | firma vacía/no-Base64/no-PNG, campos obligatorios, límites lat/lon (incluye bordes ±90/±180 y fuera de rango), notas excesivas |
| `EntregaEvidenciaServiceTest` | mocks de repos/servicios: registro OK (estado→ENTREGADO, evidencia guardada, evento de tracking creado), 404 envío, 409 POD existente, 400 validación, GET 200 y 404 |
| `EntregaEvidenciaControllerTest` | `@WebMvcTest` + `@Import({SecurityConfig, GlobalExceptionHandler})`: 201 POST, 400, 404, 409, GET 200, sin auth → 401, `ROLE_CLIENTE` → 403, `ROLE_OPERADOR` → 201 |
| `EntregaEvidenciaIntegrationTest` | `@SpringBootTest` + perfil test: envío real → POD → estado `ENTREGADO` persistido + evidencia + evento de tracking + notificación/webhook disparados (Awaitility) + GET devuelve firma; limpieza `@AfterEach` |

## Criterio de finalización

- `mvn clean test` (contenedor Docker) con BUILD SUCCESS y suite completa en verde
  (159 base + ~18 nuevos ≈ 177 tests).
- `docs/handoff.md` actualizado con el Bloque 13 (migración V8, endpoints, seguridad HTTP Basic).
- Working tree limpio tras el commit final.
