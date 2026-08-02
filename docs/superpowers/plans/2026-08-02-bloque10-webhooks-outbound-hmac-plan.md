# Bloque 10 — Plan de Implementación: Webhooks Outbound y Firma HMAC-SHA256

- Spec: `docs/superpowers/specs/2026-08-02-bloque10-webhooks-outbound-hmac-design.md` (commit `b5735fb`)
- Base: `main`
- Ejecución directa (aprobada por el usuario)

## Tasks

1. **Migración Flyway V4** — `src/main/resources/db/migration/V4__create_webhooks_tables.sql`
   (SQL aprobado: `webhooks_config` + `webhook_logs`).
2. **Entidades + repos** — `model/WebhookConfig`, `model/WebhookLog`,
   `repository/WebhookConfigRepository`, `repository/WebhookLogRepository`. Java puro.
3. **Firma + payload** — `service/WebhookSignature` (HMAC-SHA256 + `HexFormat`),
   `service/WebhookPayloadBuilder` (JSON normalizado, serializa una vez).
4. **Despacho + listener + config** — `service/WebhookDispatchService`,
   `listener/WebhookEventListener` (`@Async("webhookTaskExecutor")` + `REQUIRES_NEW` +
   `AFTER_COMMIT`), `config/WebhookHttpConfig` (RestClient 2s/5s + executor dedicado),
   props `app.webhook.*` en base (y test si aplica).
5. **CRUD admin** — `controller/api/WebhookConfigController` + `dto/api/WebhookConfigRequest`
   + `dto/api/WebhookConfigDto`. `secretToken` nunca en respuestas.
6. **Tests unitarios** — Signature (vector RFC 4231), PayloadBuilder, DispatchService
   (Mockito), EventListener, Controller (WebMvcTest + `@WithMockUser(roles="ADMIN")`).
7. **Test integración** — `integration/WebhookDispatchIntegrationTest` con sink HTTP local
   (`HttpServer` puerto efímero), flujo `actualizarEstado` → AFTER_COMMIT → @Async → POST →
   `webhook_logs` (200 y 500), firma verificada.
8. **Verificación + handoff** — `mvn clean test` en contenedor Maven (red
   `envios_paraguay_cms_backend`), actualizar `docs/handoff.md`, commits finales.

## Patrón de ejecución de tests de integración

```powershell
docker run --rm -v "${PWD}:/app" -w /app --network envios_paraguay_cms_backend `
  -e SPRING_DATASOURCE_URL="jdbc:mysql://db:3306/envios_paraguay_cms_test?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true" `
  -e DB_USERNAME=root -e DB_PASSWORD=root -e SPRING_DATA_REDIS_HOST=redis `
  -v "${HOME}\.m2:/root/.m2" maven:3.9-eclipse-temurin-17 mvn test -Dtest=WebhookDispatchIntegrationTest
```

## Riesgo conocido

`ddl-auto=validate` + columnas `BOOLEAN` en MySQL: verificar empíricamente que Hibernate 6.5
acepta `TINYINT(1)` para `boolean`. Si falla la validación de esquema, cambiar `BOOLEAN` → `BIT`
en la migración (patrón existente de V1) y documentar la desviación.

## Commits esperados

- `docs: add Bloque 10 webhooks outbound HMAC design spec` (hecho: `b5735fb`)
- `docs: add Bloque 10 webhooks implementation plan`
- `feat(webhooks): add Flyway V4 migration for webhooks tables`
- `feat(webhooks): add WebhookConfig/WebhookLog entities and repositories`
- `feat(webhooks): add HMAC-SHA256 signature and normalized payload builder`
- `feat(webhooks): add async dispatch service, listener and HTTP config`
- `feat(webhooks): add admin CRUD API for webhook configs`
- `test(webhooks): add unit tests for signature, payload, dispatch, listener, controller`
- `test(webhooks): add end-to-end integration test with local HTTP sink`
- `docs: update handoff with Bloque 10 webhooks module`
