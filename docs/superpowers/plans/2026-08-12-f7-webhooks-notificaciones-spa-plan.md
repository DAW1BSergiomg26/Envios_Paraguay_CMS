# F7 — UI de gestión de Webhooks y Notificaciones en la SPA React (Implementation Plan)

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Tasks use checkbox (`- [ ]`) syntax for tracking. Each task is an independent TDD cycle (RED → GREEN → REFACTOR) and is sized to be a meaningful reviewer gate.

**Goal:** Expose webhook + notification management in the React SPA: new `NotificacionApiController` (`/api/v1/admin/notificaciones`: list filtered by estado, detail, synchronous retry), extend `WebhookConfigController` (PUT edit, GET logs), and build `WebhooksPage` (full CRUD + collapsible dispatch history) + `NotificacionesPage` (estado filter, expandable detail, retry button) in the SPA with routes/nav/styles.

**Architecture:** Follows the F4/F5 per-resource convention — one dedicated API controller per resource, DTOs with `from()`, `@PreAuthorize("hasRole('ROLE_ADMIN')")` at class level, Swagger `@Tag`. `NotificacionApiController` is **new**; `WebhookConfigController` is **extended** (existing GET/POST/DELETE untouched). Controllers stay repository-direct (mirror of the existing `WebhookConfigController` — no new service layer in F7); the retry mutates the entity and saves it. The SPA adds two pages that mock `../services/api` + `../context/NotificationContext` in tests (F4/F5 pattern) and two nav buttons in `MainLayout`.

**Tech Stack:** Spring Boot 3.5+, Java 25, Spring Data JPA, MySQL 8, Flyway V1 (zero schema changes — `webhooks_config`, `webhook_logs`, `notificaciones` already exist); React 18 + Vite + Vitest + @testing-library/react + jest-dom; no Lombok; constructor injection only.

## Global Constraints

- Java toolchain: `$env:JAVA_HOME="$env:USERPROFILE\.jdks\openjdk-25.0.2"`; Maven `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd`.
- Backend test: `mvn test` (full, excl. Docker `*IntegrationTest`) or `mvn test -Dtest=<Class>` (focused). Must end `BUILD SUCCESS`.
- Frontend test: `cd frontend-react; npx vitest run` (full) or `npx vitest run <file>` (focused). All green.
- Frontend build: `cd frontend-react; npm run build` → OK.
- No Lombok; entities/DTOs are pure Java (private fields, no-arg ctor, ctor-with-args, manual getters/setters).
- API conventions: DTOs in `com.monteastur.envios.dto.api`; exceptions reuse `GlobalExceptionHandler` + `com.monteastur.envios.exception.*` (`ResourceNotFoundException`→404, `BadRequestException`→400, `ConflictException`→409). **Note:** `GlobalExceptionHandler` maps `IllegalStateException`→409 and `Exception`→500 with a generic message — the retry endpoint must therefore **catch** the email exception itself and respond 500 explicitly (so the record is updated to `FALLIDO` before responding).
- `secret_token` never serialized (mirror `WebhookConfigDto`/`WebhookConfigControllerTest`); `WebhookLog.payload` never exposed (not in `WebhookLogDto`).
- **Measured baselines (verified 2026-08-12, F6 close):** backend **325** `@Test` (BUILD SUCCESS `mvn clean test` excl. integración Docker); frontend **97** `test()`/`it()` in **16** files; `npm run build` OK. Delta targets below are estimates; the closing gate confirms real totals.
- Frontend conventions: helpers in `frontend-react/src/services/api.js` (axios `baseURL='/api/v1'`, `withCredentials:true`); page tests `vi.mock('../services/api')` + `vi.mock('../context/NotificationContext')` with `useToast` returning `{ showSuccess, showError }`; `EmptyState` for empty lists; `parseLocalDateTime` where dates appear.
- Commits atomic on `main`, **no push** unless explicitly authorized.

## File Structure

```
src/main/java/com/monteastur/envios/
  controller/api/NotificacionApiController.java  [CREATE — /api/v1/admin/notificaciones]
  controller/api/WebhookConfigController.java    [MODIFY — + PUT /{id}, + GET /{id}/logs]
  dto/api/NotificacionDto.java                   [CREATE — id, envioId, destinatario, asunto, mensaje, estado, errorMensaje, fechaCreacion; from()]
  dto/api/WebhookLogDto.java                     [CREATE — id, webhookId, envioId, responseStatus, exitoso, errorMensaje, fechaCreacion; from()]
  dto/api/ActualizarWebhookRequest.java          [CREATE — url, secretToken, activo]
  repository/NotificacionRepository.java         [MODIFY — + findAllByOrderByFechaCreacionDesc, + findByEstadoOrderByFechaCreacionDesc]
src/test/java/com/monteastur/envios/
  controller/api/NotificacionApiControllerTest.java [CREATE — @WebMvcTest, mirror WebhookConfigControllerTest]
  controller/api/WebhookConfigControllerTest.java   [MODIFY — + 7 tests]
frontend-react/src/
  services/api.js                                [MODIFY — + 7 helpers]
  services/api.test.js                           [MODIFY — + describe "webhooks y notificaciones"]
  pages/WebhooksPage.jsx                         [CREATE]
  pages/WebhooksPage.test.jsx                    [CREATE]
  pages/NotificacionesPage.jsx                   [CREATE]
  pages/NotificacionesPage.test.jsx              [CREATE]
  App.jsx                                        [MODIFY — + 2 routes]
  layouts/MainLayout.jsx                         [MODIFY — + 2 nav buttons]
  index.css                                      [MODIFY — F7 classes]
docs/ARQUITECTURA_INTERFACES.md                  [MODIFY — F7 row → Completa]
docs/handoff.md                                  [MODIFY — F7 entry]
```

## Global Test Targets (F7 additions — deltas confirmed at closing gate)

| Layer | File | Tests (≈) |
|---|---|---|
| backend | `NotificacionApiControllerTest` (new) | 11 |
| backend | `WebhookConfigControllerTest` (+7) | +7 → 16 |
| backend **net** | — | **+18** → 325 → **~343** |
| frontend | `api.test.js` (+7 helpers) | +7 |
| frontend | `WebhooksPage.test.jsx` (new) | 8 |
| frontend | `NotificacionesPage.test.jsx` (new) | 5 |
| frontend **net** | — | **+20** → 97 → **~117** |

---

### Task 1: NotificacionApiController + DTOs + repo methods — TDD

**Files:** Create `dto/api/NotificacionDto.java`; Create `controller/api/NotificacionApiController.java`; Modify `repository/NotificacionRepository.java`; Create `src/test/java/.../controller/api/NotificacionApiControllerTest.java`.

**Consumes:** `NotificacionRepository`, `EmailService` (`enviarCorreoSimple(para, asunto, texto)` — throws `IllegalStateException` if no `JavaMailSender`). **Produces:** endpoints consumed by Task 4's page.

**Repository additions (derived queries):**
```java
List<Notificacion> findAllByOrderByFechaCreacionDesc();
List<Notificacion> findByEstadoOrderByFechaCreacionDesc(Notificacion.EstadoNotificacion estado);
```

**DTO contract (`NotificacionDto`):** `id`, `envioId`, `destinatario`, `asunto`, `mensaje`, `estado` (String = enum name), `errorMensaje`, `fechaCreacion`; `static from(Notificacion)`. Reused as the response of `POST /{id}/reintentar` (no dedicated response DTO).

**Controller shape (mirror `WebhookConfigController`):**
```java
@Tag(name = "Admin Notificaciones")
@RestController @RequestMapping("/api/v1/admin/notificaciones")
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class NotificacionApiController {
    // ctor: NotificacionRepository, EmailService
    @GetMapping → List<NotificacionDto>            (filtro @RequestParam(required=false) String estado; parse a EstadoNotificacion → BadRequestException si inválido; sin filtro → findAllByOrderByFechaCreacionDesc)
    @GetMapping("/{id}") → NotificacionDto          (findById → ResourceNotFoundException 404)
    @PostMapping("/{id}/reintentar") → ResponseEntity<NotificacionDto>
        // 1) findById → 404
        // 2) estado != FALLIDO → ConflictException 409
        // 3) destinatario null/blank → BadRequestException 400
        // 4) try { emailService.enviarCorreoSimple(destinatario, asunto, mensaje); notif.setEstado(ENVIADO); notif.setErrorMensaje(null); }
        //    catch (Exception e) { notif.setEstado(FALLIDO); notif.setErrorMensaje(e.getMessage()); save; return 500 ErrorDto(timestamp,500,"..."); }
        // 5) save + return ResponseEntity.ok(NotificacionDto.from(saved))
}
```
Estado filter parse helper: `try { EstadoNotificacion.valueOf(estado.toUpperCase()) } catch (IllegalArgumentException) { throw new BadRequestException("estado inválido: " + estado); }`. The retry's catch returns `ResponseEntity.status(INTERNAL_SERVER_ERROR).body(new ErrorDto(Instant.now().toString(), 500, "No se pudo reenviar la notificación"))` — must catch (do **not** let `IllegalStateException` propagate, or it becomes 409).

- [ ] **Step 1: RED — NotificacionApiControllerTest.** `@WebMvcTest(NotificacionApiController.class)` + `@Import({GlobalExceptionHandler.class, SecurityConfig.class})` + `@WithMockUser(username="admin", roles="ADMIN")` + `@MockBean` `NotificacionRepository`, `EmailService`, `DataSource`, `RBACAccessLogger`, `CustomAccessDeniedHandler` (mirror `WebhookConfigControllerTest`). Helper `notificacion(id, estado, destinatario)` builds the entity. Tests (11):
  1. `listar_retornaNotificaciones` — GET `/api/v1/admin/notificaciones`, repo returns list → 200, `$[0].estado` value, `$[0].destinatario`.
  2. `listar_filtraPorEstadoValido` — GET `?estado=FALLIDO` → `verify(repo).findByEstadoOrderByFechaCreacionDesc(FALLIDO)`.
  3. `listar_estadoInvalido_retorna400` — GET `?estado=FOO` → 400, `$.status` 400.
  4. `detalle_retornaNotificacion` — GET `/{id}` → 200.
  5. `detalle_noEncontrado_retorna404` — GET `/{id}` empty → 404.
  6. `reintentar_fallidaEmailOk_retorna200YEstadoEnviado` — POST `/{id}/reintentar`, estado FALLIDO, `emailService.enviarCorreoSimple` no-throw, `save` returns updated (ENVIADO, errorMensaje null) → 200, `$.estado` `"ENVIADO"`, `$.errorMensaje` null.
  7. `reintentar_enviada_retorna409` — estado ENVIADO → 409.
  8. `reintentar_noEncontrada_retorna404` — empty repo → 404.
  9. `reintentar_sinDestinatario_retorna400` — destinatario null → 400.
  10. `reintentar_fallaEmail_retorna500YEstadoFallido` — estado FALLIDO, `enviarCorreoSimple` throws `IllegalStateException("JavaMailSender no configurado")` → 500, `$.status` 500, `verify(repo).save` with estado FALLIDO + errorMensaje set.
  11. `sinAutenticacion_retorna401` — `@WithAnonymousUser` GET → 401.
  Run: `mvn test -Dtest=NotificacionApiControllerTest` → **FAIL** (controller/DTO missing).
- [ ] **Step 2: Verify RED** (compile fails on missing symbols).
- [ ] **Step 3: GREEN impl** per the controller shape. Keep `NotificacionDto` in `dto/api` with `from()`; `estado` serialized as the enum name string.
- [ ] **Step 4: Verify GREEN** → PASS (11). Full `mvn test` → BUILD SUCCESS (existing tests unaffected — repo additions are additive).
- [ ] **Step 5: Commit.** `git add src/main/java/com/monteastur/envios/controller/api/NotificacionApiController.java src/main/java/com/monteastur/envios/dto/api/NotificacionDto.java src/main/java/com/monteastur/envios/repository/NotificacionRepository.java src/test/java/com/monteastur/envios/controller/api/NotificacionApiControllerTest.java` → `feat: API REST de notificaciones con listado, detalle y reintento con tests`.

---

### Task 2: WebhookConfigController — PUT + GET logs — TDD

**Files:** Create `dto/api/WebhookLogDto.java`, `dto/api/ActualizarWebhookRequest.java`; Modify `controller/api/WebhookConfigController.java`; Modify `src/test/java/.../controller/api/WebhookConfigControllerTest.java`.

**Consumes:** `WebhookConfigRepository` (`findById`), `WebhookLogRepository` (`findByWebhookIdOrderByFechaCreacionDesc`). **Produces:** endpoints consumed by Task 4's page.

**DTO contracts:**
```java
public class WebhookLogDto { // id, webhookId, envioId, responseStatus, exitoso, errorMensaje, fechaCreacion — SIN payload
    public static WebhookLogDto from(WebhookLog log);
}
public class ActualizarWebhookRequest { // url, secretToken, activo (todos opcionales)
    // + getters/setters
}
```

**Controller additions (existing GET/POST/DELETE untouched, reuse private `validarUrl`):**
```java
@PutMapping("/{id}") → WebhookConfigDto
    // findById → ResourceNotFoundException 404
    // url no blank → validarUrl(request.getUrl()); config.setUrl(...)
    // secretToken no blank → config.setSecretToken(...)   (blanco ⇒ se ignora, no se borra el secreto)
    // activo != null → config.setActivo(...)
    // save → WebhookConfigDto.from (sin secret)
@GetMapping("/{id}/logs") → List<WebhookLogDto>
    // !webhookConfigRepository.existsById(id) → 404
    // logs = webhookLogRepository.findByWebhookIdOrderByFechaCreacionDesc(id)
    // filtro @RequestParam(required=false) Boolean exitoso → logs.stream().filter(l -> l.isExitoso() == exitoso)
    // map WebhookLogDto::from
```
`WebhookConfigController` ctor gains `WebhookLogRepository` (3rd dep). Swagger `@Operation`/`@ApiResponses` mirror existing methods.

- [ ] **Step 1: RED — add 7 tests to WebhookConfigControllerTest.** Add `@MockBean WebhookLogRepository webhookLogRepository`. Existing tests must stay untouched/green. New tests:
  1. `actualizar_retorna200ActualizaUrlYActivo` — repo has config id 10; PUT body `{"url":"https://new.example.com","activo":false}` → 200, `$.url` new, `$.activo` false; `verify(config).setSecretToken` **not** called (no secretToken in body).
  2. `actualizar_secretTokenEnBlanco_noBorraSecreto` — body `{"secretToken":"  "}` → `verify(config, never()).setSecretToken(...)`.
  3. `actualizar_secretTokenNuevo_loActualiza` — body `{"secretToken":"nuevo-secreto"}` → `verify(config).setSecretToken("nuevo-secreto")`.
  4. `actualizar_urlInvalida_retorna400` — body `{"url":"ftp://x/y"}` → 400.
  5. `actualizar_noEncontrado_retorna404` — empty repo → 404.
  6. `logs_retornaListaSinPayload` — repo logs → 200, `$[0].exitoso`, `$[0].responseStatus`, `$[0].payload` doesNotExist.
  7. `logs_filtraPorExitoso` — param `exitoso=true` → only exitosos in response.
  8. `logs_webhookNoEncontrado_retorna404` — `existsById` false → 404.
  Run: `mvn test -Dtest=WebhookConfigControllerTest` → **FAIL** (PUT/GET logs missing).
- [ ] **Step 2: Verify RED.**
- [ ] **Step 3: GREEN impl** per additions above.
- [ ] **Step 4: Verify GREEN** → PASS (16). Full `mvn test` → BUILD SUCCESS.
- [ ] **Step 5: Commit.** `git add src/main/java/com/monteastur/envios/controller/api/WebhookConfigController.java src/main/java/com/monteastur/envios/dto/api/WebhookLogDto.java src/main/java/com/monteastur/envios/dto/api/ActualizarWebhookRequest.java src/test/java/com/monteastur/envios/controller/api/WebhookConfigControllerTest.java` → `feat: edición de webhooks e historial de despachos en la API admin con tests`.

---

### Task 3: api.js helpers + tests — TDD

**Files:** Modify `frontend-react/src/services/api.js`; Modify `frontend-react/src/services/api.test.js`.

**Helpers (exact signatures/URLs, style of existing blocks):**
```js
export function listarWebhooks(clienteId)                    { return api.get('/admin/webhooks', { params: clienteId ? { clienteId } : {} }); }
export function crearWebhook(data)                           { return api.post('/admin/webhooks', data); }
export function actualizarWebhook(id, data)                  { return api.put(`/admin/webhooks/${id}`, data); }
export function eliminarWebhook(id)                          { return api.delete(`/admin/webhooks/${id}`); }
export function listarWebhookLogs(id, exitoso)               { const params = {}; if (exitoso !== undefined && exitoso !== null) params.exitoso = exitoso; return api.get(`/admin/webhooks/${id}/logs`, { params }); }
export function listarNotificaciones(estado)                 { const params = {}; if (estado) params.estado = estado; return api.get('/admin/notificaciones', { params }); }
export function reintentarNotificacion(id)                   { return api.post(`/admin/notificaciones/${id}/reintentar`); }
```
- [ ] **Step 1: RED — api.test.js.** New `describe('api helpers de webhooks y notificaciones')` with `vi.spyOn(api, 'get'|'post'|'put'|'delete')` asserts (mirror `getAdminReservas`/`putTextoLegal`/`deleteAdminMensaje` blocks), 7 tests: `listarWebhooks` (params vacío), `crearWebhook` POST body, `actualizarWebhook` PUT `/:id`, `eliminarWebhook` DELETE, `listarWebhookLogs` (with/without exitoso → params), `listarNotificaciones` (with/without estado), `reintentarNotificacion` POST `/:id/reintentar`. Run `cd frontend-react; npx vitest run api.test.js` → FAIL (undefined).
- [ ] **Step 2: Verify RED.**
- [ ] **Step 3: GREEN — api.js.** Add 7 helpers (place after `deleteAdminMensaje`, before `getAdminImagenes` or grouped before `export default api`).
- [ ] **Step 4: Verify GREEN** → `npx vitest run api.test.js` PASS (97+7=104). Full `npx vitest run` still green.
- [ ] **Step 5: Commit.** `git add frontend-react/src/services/api.js frontend-react/src/services/api.test.js` → `feat: helpers de API para webhooks y notificaciones con tests`.

---

### Task 4: WebhooksPage + tests — TDD

**Files:** Create `pages/WebhooksPage.jsx`, `pages/WebhooksPage.test.jsx`.

**Consumes (api.js):** `listarWebhooks`, `crearWebhook`, `actualizarWebhook`, `eliminarWebhook`, `listarWebhookLogs`. **UX (spec §3.2):** list of config cards (url, clienteId, activo, fechaCreacion) + create form (clienteId, url, secretToken, activo) + inline edit form (url, secretToken optional, activo) + delete with `window.confirm`; per webhook a collapsible dispatch-history section calling `listarWebhookLogs(id)` (badge exitoso/failure, responseStatus, fecha) with an "exitoso" filter. `secretToken` **never** rendered. Uses `useToast` (`showSuccess`/`showError`) and `EmptyState`.

- [ ] **Step 1: RED.** 8 tests in `WebhooksPage.test.jsx` (structure mirrors `AdminImagesPage.test.jsx`: `vi.mock('../services/api')` with the 5 helpers, `vi.mock('../context/NotificationContext')` with `useToast`, `vi.spyOn(window,'confirm')`):
  1. `cargaYListaWebhooks` — `listarWebhooks` called; url rendered.
  2. `muestraEmptyStateSinWebhooks` — empty → EmptyState text.
  3. `creaWebhookYRecarga` — fill create form, submit → `crearWebhook` called, `showSuccess`, reload.
  4. `editaWebhookYRecarga` — click Editar, change url, save → `actualizarWebhook(id, body)`.
  5. `noMuestraSecretToken` — rendered list has no `secretToken` text.
  6. `eliminaConConfirmacion` — confirm true → `eliminarWebhook(id)`, `showSuccess`.
  7. `noEliminaSiCancela` — confirm false → not called.
  8. `expandeHistorialYListaLogs` — click toggle → `listarWebhookLogs(id)` called, logs rendered, `$.payload` never shown.
  Run: `cd frontend-react; npx vitest run WebhooksPage` → FAIL.
- [ ] **Step 2: Verify RED.**
- [ ] **Step 3: GREEN impl.** (Tests authoritative; component follows F4/F5 card/form patterns, `EmptyState`, `.acciones-fila`, `#d4762a` tokens.)
- [ ] **Step 4: Verify GREEN** → PASS (8). Full `npx vitest run` still green.
- [ ] **Step 5: Commit.** `git add frontend-react/src/pages/WebhooksPage.jsx frontend-react/src/pages/WebhooksPage.test.jsx` → `feat: página de gestión de webhooks con CRUD e historial de despachos`.

---

### Task 5: NotificacionesPage + tests — TDD

**Files:** Create `pages/NotificacionesPage.jsx`, `pages/NotificacionesPage.test.jsx`.

**Consumes (api.js):** `listarNotificaciones`, `reintentarNotificacion`. **UX (spec §3.2):** list with estado filter (select ENVIADO/FALLIDO/OMITIDO_SIN_DESTINATARIO + all), expandable row detail (destinatario, asunto, mensaje, errorMensaje, fecha), "Reintentar" button only on `FALLIDO` rows → `reintentarNotificacion(id)` → on success `showSuccess` + reload list; on failure `showError` + reload list. Uses `useToast`, `EmptyState`.

- [ ] **Step 1: RED.** 5 tests (mirror `AdminImagesPage.test.jsx` + filter pattern from `ReservasPage`):
  1. `cargaYListaNotificaciones` — `listarNotificaciones()` called; asunto rendered.
  2. `filtraPorEstado` — select FALLIDO → `listarNotificaciones('FALLIDO')`.
  3. `expandeDetalle` — click row → asunto/mensaje/destinatario visible.
  4. `reintentaConExito` — click Reintentar on FALLIDO row → `reintentarNotificacion(id)`, `showSuccess`, `listarNotificaciones` called again.
  5. `reintentaConError` — `reintentarNotificacion` rejects → `showError`, list reloaded.
  Run: `cd frontend-react; npx vitest run NotificacionesPage` → FAIL.
- [ ] **Step 2: Verify RED.**
- [ ] **Step 3: GREEN impl.** (Tests authoritative.)
- [ ] **Step 4: Verify GREEN** → PASS (5). Full `npx vitest run` still green.
- [ ] **Step 5: Commit.** `git add frontend-react/src/pages/NotificacionesPage.jsx frontend-react/src/pages/NotificacionesPage.test.jsx` → `feat: página de notificaciones con filtro, detalle y reintento`.

---

### Task 6: Routing + Nav + Styles + Docs + closing verification

**Files:** Modify `App.jsx`, `layouts/MainLayout.jsx`, `index.css`; Modify `docs/ARQUITECTURA_INTERFACES.md`, `docs/handoff.md`.

- [ ] **Step 1: App.jsx** — import `WebhooksPage`, `NotificacionesPage`; add 2 routes inside `MainLayout` + `ProtectedRoute` (mirror F4/F5):
  ```jsx
  <Route path="/dashboard/webhooks" element={<ProtectedRoute><WebhooksPage /></ProtectedRoute>} />
  <Route path="/dashboard/notificaciones" element={<ProtectedRoute><NotificacionesPage /></ProtectedRoute>} />
  ```
- [ ] **Step 2: MainLayout.jsx** — add 2 `btn-nav-link` buttons after "Textos legales": `Webhooks` → `/dashboard/webhooks`, `Notificaciones` → `/dashboard/notificaciones`.
- [ ] **Step 3: index.css** — append F7 classes (`WebhooksPage`/`NotificacionesPage` scoped: `.webhook-card`, `.webhook-logs`, `.notificaciones-list`, reuse `.acciones-fila`, `.badge-*`, `.empty-state`, `--accent-color` #d4762a). No brand override.
- [ ] **Step 4: ARQUITECTURA_INTERFACES.md** — F7 row → "Completa" + short paragraph (webhooks + notificaciones gestionados en SPA).
- [ ] **Step 5: handoff.md** — append F7 entry (commits + verificación final) following the F6 block format.
- [ ] **Step 6: Frontend gates** — `cd frontend-react && npx vitest run` (all green, ~117), `npm run build` → OK.
- [ ] **Step 7: Full backend gate** — `$env:JAVA_HOME="$env:USERPROFILE\.jdks\openjdk-25.0.2"; & C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd clean test "-Dtest=!*IntegrationTest"` → **BUILD SUCCESS**, ~343 `@Test`, 0 failures, 0 errors.
- [ ] **Step 8: Commit.** `git add frontend-react/src/App.jsx frontend-react/src/layouts/MainLayout.jsx frontend-react/src/index.css docs/ARQUITECTURA_INTERFACES.md docs/handoff.md` → `docs: rutas, navegación, estilos F7 y handoff`.

---

### Task 7: Final verification + closing gate

- [ ] `mvn clean test "-Dtest=!*IntegrationTest"` (JDK 25) → **BUILD SUCCESS**, ~343 tests, 0 failures, 0 errors.
- [ ] `cd frontend-react && npx vitest run` → **~117 tests**, 0 failures.
- [ ] `cd frontend-react && npm run build` → OK.
- [ ] `git log --oneline <base>..HEAD` → 6 commits atómicos F7; `git diff --stat` sane; `git status` clean (solo `node_modules/` pre-existente).
- [ ] Confirm none of the pre-existing `WebhookConfigController` tests broke: `mvn test -Dtest=WebhookConfigControllerTest` → 16 PASS.
- [ ] Nada hecho `git push`.

---

## TDD Rhythm Notes (per task)

- Every new method/class gets a failing test FIRST; run to confirm RED, then implement MINIMAL code to GREEN; refactor; run; commit.
- Backend controller tests: `mvn test -Dtest=<Class>` (fast). Full `mvn test` per task after GREEN; full `mvn clean test` at Tasks 6/7 gates.
- Frontend: `npx vitest run <file>` per task; full `npx vitest run` at Tasks 4/5/6/7.
- Task ordering: Task 1 (controller+Dtos) → Task 2 (webhook extension) → Task 3 (api helpers) → Task 4 (WebhooksPage) → Task 5 (NotificacionesPage) → Task 6 (routing/docs) → Task 7 (gate). Tasks 1/2 are independent; Tasks 3–5 depend on 1/2 only at runtime, not compile time.
- `@MockBean` DataSource/RBACAccessLogger/CustomAccessDeniedHandler required by the SecurityConfig import in `@WebMvcTest` — mirror `WebhookConfigControllerTest`/`ImagenApiControllerTest` exactly.
- **Retry 500 nuance:** `GlobalExceptionHandler` maps `IllegalStateException`→409 and generic `Exception`→500 with a generic body. The controller must catch around `enviarCorreoSimple`, persist `FALLIDO` + `errorMensaje`, then respond 500 explicitly — never let the exception propagate before saving.
- Frontend helper signature style follows existing blocks (`getAdminReservas(estado)` params-object pattern for optional filters).
- `WebhookLogDto` never includes `payload` (client data leakage guard, spec §3.3).
