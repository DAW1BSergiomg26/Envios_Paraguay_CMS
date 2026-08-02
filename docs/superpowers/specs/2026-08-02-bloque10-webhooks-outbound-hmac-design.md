# Bloque 10 — Módulo de Webhooks Outbound y Firma Digital HMAC-SHA256

- Fecha: 2026-08-02
- Estado: Aprobado (decisiones de diseño acordadas con el usuario)
- Base: `main` tras Sprint de Optimización y Resiliencia (commit `9dc18b8`)

## Objetivo

Aprovechar la arquitectura basada en eventos: cada vez que se emita
`EstadoEnvioActualizadoEvent` (publicado por `EnvioTrackingService.actualizarEstado`),
el sistema debe:

1. Validar si el cliente asociado al envío posee webhooks activos configurados.
2. Construir un payload JSON normalizado.
3. Calcular la firma criptográfica HMAC-SHA256 (cabecera `X-Signature-256`) usando el
   `secret_token` del webhook sobre el cuerpo bruto exacto que se envía.
4. Despachar el POST de forma asíncrona tras el commit (`AFTER_COMMIT`).
5. Auditar cada entrega en la tabla `webhook_logs`.

## Restricciones críticas

- **Prohibido Lombok:** entidades y DTOs en Java puro (constructores explícitos,
  getters/setters, constructor vacío obligatorio).
- **Migración Flyway V4** en `src/main/resources/db/migration/V4__create_webhooks_tables.sql`.
- **Criptografía:** HMAC-SHA256 sobre el String bruto del payload; resultado en hex
  lowercase (Java 17 `HexFormat.of().formatHex(...)`).

## Decisiones de diseño aprobadas

1. **Modelo de despacho:** `RestClient` (Spring 6.1, sin dependencias nuevas) ejecutado en
   un hilo dedicado `@Async("webhookTaskExecutor")`, en transacción independiente
   `@Transactional(propagation = REQUIRES_NEW)`, disparado tras commit
   `@TransactionalEventListener(phase = AFTER_COMMIT)`. Mismo patrón que el módulo de
   notificaciones (Bloque 9).
2. **CRUD API admin:** sí, bajo `/api/v1/admin/webhooks` (autenticado + `ROLE_ADMIN`):
   GET listar (por cliente o todos), POST crear, DELETE eliminar. **`secretToken` nunca se
   expone en respuestas.**
3. **Resiliencia:** timeouts de conexión/lectura en el RestClient (2s/5s) para que el hilo
   nunca se cuelgue; **sin reintentos automáticos** en v1. Cada intento se audita.
4. **Múltiples webhooks:** se despachan **todos** los `activo = TRUE` del cliente, con un
   registro `webhook_logs` por webhook.

## Arquitectura

```
EstadoEnvioActualizadoEvent
        │ (AFTER_COMMIT)
        ▼
WebhookEventListener (@Async "webhookTaskExecutor" + REQUIRES_NEW)
        │
        ▼
WebhookDispatchService ──▶ EnvioTrackingRepository.findWithClienteByCodigoUnico
        │                   WebhookConfigRepository.findByClienteIdAndActivoTrue
        │  por cada config: WebhookPayloadBuilder.construir → WebhookSignature.hmacSha256
        │                   → POST RestClient (X-Signature-256) → WebhookLogRepository.save
        ▼
webhook_logs
```

### Componentes (main)

- `db/migration/V4__create_webhooks_tables.sql` — tablas `webhooks_config` y `webhook_logs`
  (SQL aprobado por el usuario; FKs con `ON DELETE CASCADE`).
- `model/WebhookConfig.java` — entidad: `id, clienteId, url, secretToken, activo,
  fechaCreacion`. FK por ID simple (patrón `Notificacion.envioId`).
- `model/WebhookLog.java` — entidad: `id, webhookId, envioId, payload(TEXT),
  responseStatus(Integer nullable), exitoso(boolean), errorMensaje(TEXT nullable),
  fechaCreacion`.
- `repository/WebhookConfigRepository.java` — `findByClienteId`, `findByClienteIdAndActivoTrue`.
- `repository/WebhookLogRepository.java` — `findByEnvioIdOrderByFechaCreacionDesc`,
  `findByWebhookIdOrderByFechaCreacionDesc`.
- `service/WebhookSignature.java` — utilidad estática pura: `hmacSha256(secret, body)`.
- `service/WebhookPayloadBuilder.java` — serializa el payload normalizado (Jackson); el
  String devuelto es exactamente el que se firma y se envía.
- `service/WebhookDispatchService.java` — orquestación por evento (fetch envio → configs →
  despacho+auditoría por config).
- `listener/WebhookEventListener.java` — `@Async("webhookTaskExecutor")` + `REQUIRES_NEW` +
  `AFTER_COMMIT`; traga excepciones (nunca rompe el flujo principal).
- `config/WebhookHttpConfig.java` — bean `RestClient` con timeouts y executor dedicado
  `webhookTaskExecutor` (core 4 / max 8 / queue 100).
- `controller/api/WebhookConfigController.java` + `dto/api/WebhookConfigRequest.java` +
  `dto/api/WebhookConfigDto.java` — CRUD admin sin exponer `secretToken`.

### Propiedades

```
app.webhook.enabled=true
app.webhook.connect-timeout=2000
app.webhook.read-timeout=5000
app.webhook.tracking.base-url=${APP_TRACKING_BASE_URL:http://localhost:8080/tracking}
app.webhook.executor.core-size=4
app.webhook.executor.max-size=8
app.webhook.executor.queue-capacity=100
```

## Payload JSON normalizado

```json
{
  "event_id": "<UUID>",
  "envio_id": 42,
  "codigo_rastreo": "MT-2026-1",
  "estado_anterior": "RECIBIDO",
  "estado_nuevo": "EN_TRANSITO",
  "timestamp": "2026-08-02T10:30:00",
  "url_seguimiento": "http://localhost:8080/tracking/MT-2026-1",
  "destinatario": "Ana García"
}
```

La firma `X-Signature-256: <hex lowercase>` se calcula sobre la serialización JSON exacta
que se envía como cuerpo.

## Manejo de errores y auditoría

| Caso | `exitoso` | `response_status` | `error_mensaje` |
|---|---|---|---|
| Respuesta 2xx | true | código | null |
| Respuesta 4xx/5xx (`RestClientResponseException`) | false | código | `HTTP <codigo>` |
| Error de red/timeout | false | null | mensaje de la excepción |

El listener envuelve `dispatchService.despachar` en try/catch: cualquier excepción se loguea
y no propaga. Con `app.webhook.enabled=false` el listener no despacha.

## Testing

- **Unitarios:** `WebhookSignatureTest` (vector RFC 4231 + formato hex lowercase + dependencia
  secret/body); `WebhookPayloadBuilderTest` (campos normalizados, timestamp ISO);
  `WebhookDispatchServiceTest` (Mockito: sin cliente → no despacha; cliente sin webhooks →
  no audita; múltiples activos → N POST + N logs; firma correcta en cabecera; 500 → fallo con
  status; timeout → fallo sin status); `WebhookEventListenerTest` (disabled → no despacha;
  excepción tragada); `WebhookConfigControllerTest` (WebMvcTest: listar/crear/eliminar,
  404s, 400s, `secretToken` no expuesto, sin auth → redirect).
- **Integración:** `WebhookDispatchIntegrationTest` (`@SpringBootTest`, perfil `test`, MySQL/
  Redis reales en red docker): sink HTTP local (`com.sun.net.httpserver.HttpServer` en puerto
  efímero) que captura body + `X-Signature-256`; flujo completo `actualizarEstado` →
  `AFTER_COMMIT` → `@Async` → POST → `webhook_logs` (casos 200 y 500). Uso de Awaitility
  (patrón de `EnvioNotificacionIntegrationTest`).

## Criterios de aceptación

1. `mvn clean test` en verde (suite completa + nuevos tests).
2. El POST solo ocurre tras commit (no con rollback) y no bloquea la request.
3. Firma HMAC-SHA256 hex lowercase correcta sobre el payload bruto.
4. `webhook_logs` audita cada entrega (éxito/fallo, status, error, payload).
5. `secretToken` nunca aparece en respuestas del API admin.
6. Entidades sin Lombok; migración V4 exacta.
