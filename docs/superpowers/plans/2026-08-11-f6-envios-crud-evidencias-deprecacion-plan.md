# F6 — CRUD de envíos, evidencias admin y deprecación total del CMS Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Tasks use checkbox (`- [ ]`) syntax for tracking. Each task is an independent TDD cycle (RED → GREEN → REFACTOR) and is sized to be a meaningful reviewer gate.

**Goal:** Close the CMS migration: full admin CRUD for envíos + evidencias in the React SPA against a dedicated REST controller (`/api/v1/admin/envios`), fix three latent bugs (non-transactional delete, broken `getDocumentoUrl`, `EvidenciaDto` without id/date), and delete the legacy Thymeleaf admin entirely.

**Architecture:** Follows F4/F5 per-resource convention — new `EnvioApiController` (`/api/v1/admin/envios`, `@PreAuthorize("hasRole('ROLE_ADMIN')")`) for all write ops; `AdminApiController` keeps reads + `PUT .../estado` unchanged (except migrating manual `EvidenciaDto` construction to `EvidenciaDto.from`). Domain logic lives in `EnvioTrackingService` (`crear`, transactional `eliminar`); the controller orchestrates request→entity→service→`TrackingDto`. Evidencia uploads reuse F5's `UploadService` with a custom extension allowlist. Deprecation = delete `AdminController`, `cms/*.html` templates, `admin-sidebar.html`, legacy tests, plus a `/admin/**` → `/dashboard` redirect.

**Tech Stack:** Spring Boot 3.5+, Java 25, Spring Data JPA, MySQL 8, Flyway V1 (zero schema changes — `envios_tracking`, `evidencias_envio`, `eventos_tracking` already exist); React 18 + Vite + Vitest + @testing-library/react + jest-dom; no Lombok; constructor injection only.

## Global Constraints

- Java toolchain: `$env:JAVA_HOME="$env:USERPROFILE\.jdks\openjdk-25.0.2"`; Maven `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd`.
- Backend test: `mvn test` (full) or `mvn test -Dtest=<Class>` (focused). Must end `BUILD SUCCESS`.
- Frontend test: `cd frontend-react; npx vitest run` (full) or `npx vitest run <file>` (focused). All green.
- Frontend build: `cd frontend-react; npm run build` → OK.
- No Lombok; entities/models/DTOs are pure Java (private fields, no-arg ctor, ctor-with-args, manual getters/setters).
- API conventions: DTOs in `com.monteastur.envios.dto.api`; exceptions reuse `GlobalExceptionHandler` + `com.monteastur.envios.exception.*` (`ResourceNotFoundException`→404, `BadRequestException`→400, `ForbiddenException`→403).
- **Measured baselines (verified 2026-08-11):** backend 323 `@Test`; frontend 77 `test()`/`it()`. Delta targets below are estimates; the Task 9 gate confirms real totals.
- Frontend conventions: helpers in `frontend-react/src/services/api.js` (axios `baseURL='/api/v1'`, `withCredentials:true`); pages mock `../services/api` + `../context/NotificationContext`; use `useToast`, `EmptyState`, `parseLocalDateTime` where dates appear.
- Commits atomic on `main`, **no push** unless explicitly authorized.

## File Structure

```
src/main/java/com/monteastur/envios/
  service/EnvioTrackingService.java       [MODIFY — crear(), eliminar() @Transactional, +3 ctor deps]
  service/UploadService.java              [MODIFY — subirArchivo overload w/ custom ext allowlist]
  repository/EventoTrackingRepository.java [MODIFY — + deleteByEnvioTrackingId(Long)]
  repository/EvidenciaEnvioRepository.java [MODIFY — + deleteByEnvioTrackingId(Long)]
  controller/api/EnvioApiController.java  [CREATE — /api/v1/admin/envios CRUD + evidencias]
  dto/api/CrearEnvioRequest.java          [CREATE]
  dto/api/ActualizarEnvioRequest.java     [CREATE]
  dto/api/ActualizarVisibilidadRequest.java [CREATE]
  dto/api/EvidenciaDto.java               [MODIFY — +id, +fechaSubida, +static from()]
  controller/api/AdminApiController.java  [MODIFY — use EvidenciaDto.from]
  controller/api/ClienteApiController.java [MODIFY — use EvidenciaDto.from + client URL override]
  controller/AdminController.java         [DELETE — legacy CMS]
  config/SpaForwardController.java        [MODIFY — + /admin, /admin/** → redirect:/dashboard]
src/main/resources/templates/cms/*.html   [DELETE — 9 templates]
src/main/resources/templates/fragments/admin-sidebar.html [DELETE]
src/test/java/com/monteastur/envios/
  service/EnvioTrackingServiceTest.java   [CREATE]
  service/UploadServiceTest.java          [MODIFY — +overload tests]
  dto/api/EvidenciaDtoTest.java           [CREATE — from() mapping]
  controller/api/EnvioApiControllerTest.java [CREATE]
  controller/AdminControllerTest.java     [DELETE]
  controller/AdminThemeAssetsTest.java    [DELETE]
  integration/AdminDocumentosPageIntegrationTest.java [DELETE]
  config/SpaForwardControllerTest.java    [MODIFY — + /admin/** redirect test]
frontend-react/src/
  services/api.js                         [MODIFY — fix getDocumentoUrl, +6 helpers]
  services/api.test.js                    [MODIFY — +helper asserts, fix URL assert]
  pages/EnvioFormPage.jsx                 [CREATE]
  pages/EnvioFormPage.test.jsx            [CREATE]
  pages/AdminDashboard.jsx                [MODIFY — Nuevo envio button + row actions]
  pages/AdminDashboard.test.jsx           [CREATE — actions]
  pages/ShipmentDetailPage.jsx            [MODIFY — edit/delete buttons + evidencia admin panel]
  pages/ShipmentDetailPage.test.jsx       [MODIFY — +tests]
  components/EvidenciasGrid.jsx           [MODIFY — key=ev.id, modoAdmin props]
  components/EvidenciasGrid.test.jsx      [MODIFY — +tests]
  App.jsx                                 [MODIFY — 2 routes]
  index.css                               [MODIFY — F6 styles]
docs/ARQUITECTURA_INTERFACES.md           [MODIFY — F6 → Completa, rewrite §4]
docs/handoff.md                           [MODIFY — F6 entry]
```

## Global Test Targets (F6 additions — deltas confirmed at Task 9)

| Layer | File | Tests (≈) |
|---|---|---|
| backend | `EnvioTrackingServiceTest` (new) | 6 |
| backend | `EnvioApiControllerTest` (new) | 10 |
| backend | `UploadServiceTest` (+overload) | +2 |
| backend | `EvidenciaDtoTest` (new) | 1 |
| backend | `SpaForwardControllerTest` (+redirect) | +1 |
| backend | **deleted** (AdminControllerTest 6, AdminThemeAssetsTest 2, AdminDocumentosPageIntegrationTest 1) | −9 |
| backend **net** | — | **~+11** → 323 → **~334** |
| frontend | `api.test.js` (+6 helpers, URL fix) | +7 |
| frontend | `EnvioFormPage.test.jsx` (new) | 6 |
| frontend | `AdminDashboard.test.jsx` (new) | 3 |
| frontend | `ShipmentDetailPage.test.jsx` (+3) | +3 |
| frontend | `EvidenciasGrid.test.jsx` (+2) | +2 |
| frontend **net** | — | **+21** → 77 → **~98** |

---

### Task 1: EnvioTrackingService — `crear()` + transactional `eliminar()` — TDD

**Files:** Modify `src/main/java/.../service/EnvioTrackingService.java`; Modify `repository/EventoTrackingRepository.java`, `repository/EvidenciaEnvioRepository.java`; Create `src/test/java/.../service/EnvioTrackingServiceTest.java`.

**Consumes:** `EventoTrackingService` (verified: depends only on `EventoTrackingRepository` — **no cycle**), `EventoTrackingRepository`, `EvidenciaEnvioRepository`. **Produces:** service API consumed by Task 2.

**Repository additions (derived delete, no `@Modifying` needed):**
```java
void deleteByEnvioTrackingId(Long envioId);
```
in both `EventoTrackingRepository` and `EvidenciaEnvioRepository`.

**Service changes (constructor grows):**
```java
public EnvioTrackingService(EnvioTrackingRepository repo,
                            ApplicationEventPublisher eventPublisher,
                            EventoTrackingService eventoTrackingService,
                            EventoTrackingRepository eventoRepo,
                            EvidenciaEnvioRepository evidenciaRepo)

@Transactional
public EnvioTracking crear(EnvioTracking envio) {
    EnvioTracking guardado = guardar(envio);            // @CacheEvict + fechaCreacion/ultimaActualizacion
    eventoTrackingService.crearEventoInicial(guardado); // "Envío registrado en MONTEASTUR"
    return guardado;
}

@Transactional
@CacheEvict(value = {"envios.tracking", "envios.tracking.pagina", "envios.cliente.dashboard", "envios.analytics"}, allEntries = true)
public void eliminar(Long id) {
    evidenciaRepo.deleteByEnvioTrackingId(id);
    eventoRepo.deleteByEnvioTrackingId(id);
    repo.deleteById(id);
}
```

- [ ] **Step 1: Failing test (RED).** `EnvioTrackingServiceTest` — `@ExtendWith(MockitoExtension.class)`, `@Mock` the 5 deps, `@InjectMocks` service. Tests (6):
  1. `crear_guardaYRegistraEventoInicial` — `crear(envio)` → `verify(repo).save(envio)` then `verify(eventoTrackingService).crearEventoInicial(any())`; returns saved entity.
  2. `crear_registraEventoInicialTrasGuardar` — `InOrder` asserts `repo.save` before `eventoTrackingService.crearEventoInicial`.
  3. `eliminar_borraEvidenciasEventosYEnvio` — `eliminar(7L)` → `verify(evidenciaRepo).deleteByEnvioTrackingId(7L)`, `verify(eventoRepo).deleteByEnvioTrackingId(7L)`, `verify(repo).deleteById(7L)`.
  4. `eliminar_ordenaHijosAntesQueEnvio` — `InOrder`: evidencias → eventos → deleteById.
  5. `generarCodigo_devuelveFormatoMT` — `when(repo.count()).thenReturn(41L)` → `crear...generarCodigo()` matches `MT-\d{4}-0042`.
  6. `guardar_fijaFechaCreacionSiNula` — `save` sets `fechaCreacion`/`ultimaActualizacion`.

  Run: `& C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd test -Dtest=EnvioTrackingServiceTest` → **FAIL** (test file compiles? class exists but `crear` missing → compile error on test, which is a valid RED; note: tests that only verify existing methods may PASS — that's acceptable, the new-symbol tests fail first).

- [ ] **Step 2: Verify RED.** At minimum tests 1,2,3,4 fail to compile (`cannot find symbol crear / cannot resolve constructor`).

- [ ] **Step 3: Minimal GREEN impl.** Add repos' `deleteByEnvioTrackingId`, grow the constructor, add `crear` + `@Transactional` `eliminar` per the snippet above. Keep `buscarPorId`, `generarCodigo`, `count`, `listarTodos`, `buscarPorCodigo`, `actualizarEstado` untouched.

- [ ] **Step 4: Verify GREEN.** Run `EnvioTrackingServiceTest` → **PASS (6)**. Then full `mvn test` → BUILD SUCCESS (all existing service tests still compile against the new constructor — no other caller constructs it manually; verify via `mvn test -Dtest='EnvioTrackingService*,EventoTrackingService*'` first if in doubt).

- [ ] **Step 5: Commit.** `git add src/main/java/com/monteastur/envios/service/EnvioTrackingService.java src/main/java/com/monteastur/envios/repository/EventoTrackingRepository.java src/main/java/com/monteastur/envios/repository/EvidenciaEnvioRepository.java src/test/java/com/monteastur/envios/service/EnvioTrackingServiceTest.java` → `fix: borrado transaccional de envíos con hijos y alta con evento inicial (EnvioTrackingService)`.

---

### Task 2: EnvioApiController + request DTOs — TDD

**Files:** Create `dto/api/CrearEnvioRequest.java`, `dto/api/ActualizarEnvioRequest.java`, `dto/api/ActualizarVisibilidadRequest.java`; Create `controller/api/EnvioApiController.java`; Create `controller/api/EnvioApiControllerTest.java`.

**Consumes:** `EnvioTrackingService` (Task 1: `crear`, `eliminar`, `generarCodigo`, `guardar`, `buscarPorId`), `EnvioTrackingRepository` (`findWithClienteByCodigoUnico`, `existsByCodigoUnico`), `ClienteRepository` (`findById`), `EventoTrackingService` (`crearEvento`), `EvidenciaEnvioService` (`guardar`, `listarPorEnvio`, `toggleVisibilidad`, `buscar`, `eliminar`), `UploadService` (Task 3 overload). **Produces:** endpoints §3.1.

**DTO contracts (pure POJOs, getters/setters):**
```java
public class CrearEnvioRequest {
    private String codigoUnico; // optional — blank ⇒ generarCodigo()
    private String estado;      // required
    private String destinatario;// required
    private String origen, destino, peso, contenido, observaciones;
    private Long clienteId;     // optional
}
public class ActualizarEnvioRequest { /* same minus codigoUnico */ }
public class ActualizarVisibilidadRequest { private Boolean visibleCliente; }
```

**Controller shape (mirror ImagenApiController/AdminApiController conventions):**
```java
@Tag(name = "Admin Envíos")
@RestController @RequestMapping("/api/v1/admin/envios")
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class EnvioApiController {
    // ctor: EnvioTrackingRepository, ClienteRepository, EnvioTrackingService,
    //       EventoTrackingService, EvidenciaEnvioService, UploadService
    @PostMapping            → 201 TrackingDto  (validar: destinatario/estado required→400; codigo blank→generarCodigo; existsByCodigoUnico→400)
    @PutMapping("/{codigo}") → 200 TrackingDto  (findWithClienteByCodigoUnico→404; actualiza campos; si estado cambia → eventoTrackingService.crearEvento)
    @DeleteMapping("/{codigo}") → 204           (findWithClienteByCodigoUnico→404; envioTrackingService.eliminar(id))
    @PostMapping("/{codigo}/evidencias") consumes=MULTIPART → 201 EvidenciaDto
        (findByCodigo→404; validar titulo/tipo(FOTO|DOCUMENTO)/archivo→400; relPath=uploadService.subirArchivo(archivo,"evidencias","jpg","jpeg","png","webp","pdf"); evidencia.setUrlArchivo("/uploads/evidencias/"+relPath); evidenciaService.guardar)
    @PatchMapping("/evidencias/{id}/visibilidad") → 200 EvidenciaDto (evidenciaService.toggleVisibilidad→404)
    @DeleteMapping("/evidencias/{id}") → 204      (evidenciaService.buscar→404; uploadService.eliminarArchivo(url sin prefijo /uploads/); evidenciaService.eliminar)
    private TrackingDto toTrackingDto(EnvioTracking) // espejo del de AdminApiController (169-200)
}
```
`EvidenciaDto.from(...)` is implemented in Task 4 — **for this task** build `EvidenciaDto` inline (fields id/titulo/descripcion/tipo/urlArchivo/visibleCliente/fechaSubida) exactly as Task 4's `from` will, then Task 4 swaps it to `from(...)`.

- [ ] **Step 1: RED — EnvioApiControllerTest.** `@WebMvcTest(EnvioApiController.class)` + `@Import({GlobalExceptionHandler.class, SecurityConfig.class})` + `@WithMockUser(username="admin", roles="ADMIN")` + `@MockBean` `EnvioTrackingRepository`, `EventoTrackingRepository`, `EvidenciaEnvioRepository`, `ClienteRepository`, `EventoTrackingService`, `EnvioTrackingService`, `EvidenciaEnvioService`, `UploadService`, `DataSource`, `RBACAccessLogger`, `CustomAccessDeniedHandler`. Tests (10):
  1. `crear_201_retornaTrackingDto` — POST `/api/v1/admin/envios` body `{estado:"RECIBIDO",destinatario:"María"}`, `envioTrackingService.crear(any())` returns saved envio (id, codigoUnico "MT-2026-0001", estado, destinatario) → 201, `jsonPath("$.codigoUnico").value("MT-2026-0001")`.
  2. `crear_400_destinatarioVacio` — blank destinatario → 400.
  3. `crear_400_codigoDuplicado` — `when(repo.existsByCodigoUnico(...)).thenReturn(true)` → 400.
  4. `crear_generaCodigoCuandoVieneVacio` — codigoUnico blank → `verify(envioTrackingService).generarCodigo()`.
  5. `actualizar_200` — PUT `/envios/MT-2026-0001`, repo.findWithClienteByCodigoUnico→envio, save returns updated → 200, `jsonPath("$.destinatario")`.
  6. `actualizar_404` — repo empty → 404.
  7. `eliminar_204` — DELETE `/envios/MT-2026-0001`, envio presente → 204; `verify(envioTrackingService).eliminar(anyLong())`.
  8. `eliminar_404` — repo empty → 404.
  9. `subirEvidencia_201` — multipart POST `/envios/MT-2026-0001/evidencias` (file "evidencia.pdf", param titulo+tipo=DOCUMENTO), repo→envio, `uploadService.subirArchivo→"evidencias/uuid.pdf"`, `evidenciaService.guardar` returns evidencia → 201, `jsonPath("$.tipo").value("DOCUMENTO")`, `jsonPath("$.urlArchivo").value("/uploads/evidencias/uuid.pdf")`.
  10. `sinAutenticacion_401` — `@WithAnonymousUser` → 401.
  Run: `mvn test -Dtest=EnvioApiControllerTest` → **FAIL** (controller/DTOs missing).
- [ ] **Step 2: Verify RED** (compile fails on missing symbols).
- [ ] **Step 3: GREEN impl** per the controller shape. Use `evidenciaService.toggleVisibilidad(id)` (already throws `ResourceNotFoundException` → 404). `subirEvidencia` maps `EvidenciaEnvio` inline for now (id/titulo/descripcion/tipo/urlArchivo/visibleCliente/fechaSubida).
- [ ] **Step 4: Verify GREEN** → PASS (10). Full `mvn test` → BUILD SUCCESS (~334 − pending Task 4/9 deltas).
- [ ] **Step 5: Commit.** `git add src/main/java/com/monteastur/envios/controller/api/EnvioApiController.java src/main/java/com/monteastur/envios/dto/api/CrearEnvioRequest.java src/main/java/com/monteastur/envios/dto/api/ActualizarEnvioRequest.java src/main/java/com/monteastur/envios/dto/api/ActualizarVisibilidadRequest.java src/test/java/com/monteastur/envios/controller/api/EnvioApiControllerTest.java` → `feat: API REST de CRUD de envíos y evidencias admin con tests`.

---

### Task 3: UploadService — extension allowlist overload — TDD

**Files:** Modify `service/UploadService.java`; Modify `test/.../service/UploadServiceTest.java`.

**Produces (consumed by Task 2's subirEvidencia):**
```java
public String subirArchivo(MultipartFile archivo, String subDir, String... extensionesPermitidas) throws IOException {
    // si extensionesPermitidas vacías ⇒ EXT_PERMITIDAS (imágenes, F5)
    // extraerExtension + validación contra el set recibido (sin puntos, lowercase)
    // resto idéntico: UUID + subDir + Files.write
}
public String subirArchivo(MultipartFile archivo, String subDir) { return subirArchivo(archivo, subDir, EXT_PERMITIDAS.toArray(String[]::new)); }
```
`EXT_PERMITIDAS` stays `jpg|jpeg|png|webp|gif|svg`; evidencias pass `"jpg","jpeg","png","webp","pdf"` (matches `AdminController.java:347`, minus the obsolete `.` prefix).

- [ ] **Step 1: RED.** In `UploadServiceTest` add 2 tests:
  - `subirArchivo_customAllowlist_aceptaPdf` — `new MockMultipartFile("archivo","doc.pdf","application/pdf","pdf".getBytes())` via 3-arg overload with `"jpg","pdf"` → returns `"evidencias/<uuid>.pdf"`, file exists.
  - `subirArchivo_customAllowlist_rechazaSvg` — "img.svg" via 3-arg overload with `"jpg","pdf"` → `BadRequestException`.
  Run: `mvn test -Dtest=UploadServiceTest` → **FAIL** (no such overload).
- [ ] **Step 2: Verify RED.**
- [ ] **Step 3: GREEN impl** (overload delegating to varargs; default delegate keeps F5 behavior).
- [ ] **Step 4: Verify GREEN** → PASS (5+2=7). `mvn test` BUILD SUCCESS.
- [ ] **Step 5: Commit.** `git add src/main/java/com/monteastur/envios/service/UploadService.java src/test/java/com/monteastur/envios/service/UploadServiceTest.java` → `feat: UploadService admite lista personalizada de extensiones para evidencias`.

---

### Task 4: EvidenciaDto — id, fechaSubida, from() — TDD

**Files:** Modify `dto/api/EvidenciaDto.java`; Create `test/.../dto/api/EvidenciaDtoTest.java`; Modify `controller/api/AdminApiController.java` (190-198), `controller/api/ClienteApiController.java` (114-122); adjust affected existing tests.

**Current state verified:** `EvidenciaDto` (22 lines) has only `titulo`, `descripcion`, `tipo`, `urlArchivo`, `visibleCliente`. `AdminApiController.toTrackingDto` builds it manually (190-198); `ClienteApiController` builds it manually (114-122) with client-scoped URL `/api/v1/cliente/evidencias/{id}/archivo`.

**Dto contract:**
```java
public class EvidenciaDto {
    private Long id; private String titulo; private String descripcion;
    private String tipo; private String urlArchivo; private Boolean visibleCliente;
    private LocalDateTime fechaSubida;
    public static EvidenciaDto from(EvidenciaEnvio ev) { /* id, titulo, descripcion, tipo, urlArchivo, visibleCliente, fechaSubida */ }
    // + getters/setters
}
```
`from` keeps the **raw stored `urlArchivo`** (admin URL). The client controller overrides the URL after `from(...)` (existing behavior, `ClienteApiController.java:119`).

- [ ] **Step 1: RED — EvidenciaDtoTest.** `from` maps id, titulo, descripcion, tipo, urlArchivo, visibleCliente, fechaSubida from a constructed `EvidenciaEnvio`. Run `mvn test -Dtest=EvidenciaDtoTest` → FAIL (no `from`).
- [ ] **Step 2: Verify RED.**
- [ ] **Step 3: GREEN impl.** Add fields + `from` + getters/setters to `EvidenciaDto`. Replace manual constructions:
  - `AdminApiController.toTrackingDto`: `.map(EvidenciaDto::from)`.
  - `ClienteApiController.detalleEnvio`: `.map(ev -> { EvidenciaDto d = EvidenciaDto.from(ev); d.setUrlArchivo("/api/v1/cliente/evidencias/" + ev.getId() + "/archivo"); return d; })`.
  - **Task 2's `subirEvidencia`** now uses `EvidenciaDto.from(saved)` too (swap inline mapping).
- [ ] **Step 4: Verify GREEN.** `EvidenciaDtoTest` PASS (1). Find existing tests asserting EvidenciaDto shape: `grep -rn "urlArchivo\|EvidenciaDto\|fechaSubida" src/test/java/com/monteastur/envios/controller/api/` and add `id`/`fechaSubida` assertions where a full detail is asserted (AdminApiControllerTest / ClienteApiControllerTest). Full `mvn test` → BUILD SUCCESS.
- [ ] **Step 5: Commit.** `git add src/main/java/com/monteastur/envios/dto/api/EvidenciaDto.java src/main/java/com/monteastur/envios/controller/api/AdminApiController.java src/main/java/com/monteastur/envios/controller/api/ClienteApiController.java src/test/java/com/monteastur/envios/dto/api/EvidenciaDtoTest.java` → `refactor: EvidenciaDto con id/fechaSubida y mapeo from() en APIs admin y cliente`.

---

### Task 5: Fix getDocumentoUrl + api.js helpers — TDD

**Files:** Modify `frontend-react/src/services/api.js`; Modify `frontend-react/src/services/api.test.js`.

**Verified bug:** `getDocumentoUrl` (api.js:103-111) returns `/admin/documentos/...` — legacy path with no endpoint. Real endpoint: `DocumentosController` → `/api/v1/admin/documentos/...` (same-origin, `JSESSIONID` cookie travels). `api.test.js:21-31` asserts the old path.

**Helpers (exact signatures/URLs, style F4/F5):**
```js
export function postAdminEnvio(data)           { return api.post('/admin/envios', data); }
export function putAdminEnvio(codigo, data)    { return api.put(`/admin/envios/${codigo}`, data); }
export function deleteAdminEnvio(codigo)       { return api.delete(`/admin/envios/${codigo}`); }
export function uploadAdminEvidencia(codigo, formData) { return api.post(`/admin/envios/${codigo}/evidencias`, formData); }
export function patchAdminEvidenciaVisibilidad(id, visibleCliente) { return api.patch(`/admin/envios/evidencias/${id}/visibilidad`, { visibleCliente }); }
export function deleteAdminEvidencia(id)       { return api.delete(`/admin/envios/evidencias/${id}`); }
// getDocumentoUrl(id/codigo): return `/api/v1/admin/documentos/...` (same relative call path the old one returned)
```
- [ ] **Step 1: RED — api.test.js.** Fix the `getDocumentoUrl` assert (line ~21-31) to expect `/api/v1/admin/documentos/...`; add `expect(postAdminEnvio).toBeDefined()` etc. for all 6 (+7 assertions). Run `cd frontend-react; npx vitest run api.test.js` → FAIL (helpers undefined / old URL).
- [ ] **Step 2: Verify RED.**
- [ ] **Step 3: GREEN — api.js.** Fix `getDocumentoUrl` to `/api/v1/admin/documentos/...`; add 6 helpers (place after envios/mensajes helpers, before `export default api`).
- [ ] **Step 4: Verify GREEN** → `npx vitest run api.test.js` PASS. Then update downstream mocks/asserts in `ShipmentDetailPage.test.jsx` (~53-61) and `ImportBatchPage.test.jsx` (~138-148) that assert `/admin/documentos/` → run `npx vitest run ShipmentDetailPage ImportBatchPage`.
- [ ] **Step 5: Commit.** `git add frontend-react/src/services/api.js frontend-react/src/services/api.test.js frontend-react/src/pages/ShipmentDetailPage.test.jsx frontend-react/src/pages/ImportBatchPage.test.jsx` → `fix: getDocumentoUrl apunta a /api/v1/admin/documentos con tests actualizados`.

---

### Task 6: EnvioFormPage.jsx + tests — TDD

**Files:** Create `pages/EnvioFormPage.jsx`, `pages/EnvioFormPage.test.jsx`.

**Consumes (api.js):** `postAdminEnvio`, `putAdminEnvio`, `getAdminEnvioDetalle`, `getAdminClientes` (mock in tests). **Modes:** `/dashboard/envios/nuevo` (crear) and `/dashboard/envios/:codigo/editar` (editar: load detail, prefill).

**UX (per spec §6):** form with código (auto-suggestion placeholder when blank, disabled in edit), estado (select of the 6 states), destinatario (required), origen, destino, peso, contenido, observaciones (textarea), cliente (select from `getAdminClientes`, optional). Submit → `postAdminEnvio`/`putAdminEnvio` → toast success → `navigate('/dashboard/envio/${codigo}')`. Uses `useToast`, `useNavigate`, `EmptyState` on load error.

- [ ] **Step 1: RED.** 6 tests in `EnvioFormPage.test.jsx` (structure mirrors `AdminImagesPage.test.jsx`/`AdminLegalTextsPage.test.jsx`: `vi.mock('../services/api')`, `vi.mock('../context/NotificationContext')`, mock `react-router-dom` useParams/useNavigate):
  1. `modoCrear_muestraFormulario` — render `/dashboard/envios/nuevo` shape: título "Nuevo envío", inputs for destinatario/estado.
  2. `modoEditar_cargaDetalle` — useParams codigo → `getAdminEnvioDetalle` → inputs prefilled.
  3. `creaEnvioConExito` — fill fields, submit → `postAdminEnvio(data)`, `showSuccess`, navigate to `/dashboard/envio/{codigo}`.
  4. `editaEnvioConExito` — edit mode → `putAdminEnvio(codigo,data)`.
  5. `validaDestinatarioObligatorio` — empty → error, no API call.
  6. `cargaClientesParaSelector` — `getAdminClientes` called, options rendered.
  Run: `cd frontend-react; npx vitest run EnvioFormPage` → FAIL.
- [ ] **Step 2: Verify RED.**
- [ ] **Step 3: GREEN impl.** (Tests authoritative; component follows F4/F5 form patterns.)
- [ ] **Step 4: Verify GREEN** → PASS (6).
- [ ] **Step 5: Commit.** `git add frontend-react/src/pages/EnvioFormPage.jsx frontend-react/src/pages/EnvioFormPage.test.jsx` → `feat: página de alta/edición de envíos en la SPA React`.

---

### Task 7: Dashboard + detail actions + EvidenciasGrid admin mode — TDD

**Files:** Modify `pages/AdminDashboard.jsx`; Create `pages/AdminDashboard.test.jsx`; Modify `pages/ShipmentDetailPage.jsx`; Modify `pages/ShipmentDetailPage.test.jsx`; Modify `components/EvidenciasGrid.jsx`; Modify `components/EvidenciasGrid.test.jsx`.

**UX (spec §6):** AdminDashboard: "Nuevo envío" button (→ `/dashboard/envios/nuevo`) in header; row actions "Editar" (→ `/dashboard/envios/${codigo}/editar`) and "Eliminar" (confirm → `deleteAdminEnvio` → reload/remove row). ShipmentDetailPage: "Editar" + "Eliminar" buttons (same pattern); admin evidencias panel (upload `uploadAdminEvidencia` FormData, toggle `patchAdminEvidenciaVisibilidad`, delete `deleteAdminEvidencia`) reusing `EvidenciasGrid` with `modoAdmin`. EvidenciasGrid: `key={i}` → `key={ev.id}` (line 20), new optional props `modoAdmin`, `onToggleVisibilidad`, `onEliminar`.

- [ ] **Step 1: RED.** New/changed tests:
  - `AdminDashboard.test.jsx` (3): `botonNuevoEnvio_navega`, `accionEditar_navega`, `eliminar_confirmaYRecarga` (mock `deleteAdminEnvio`, `vi.spyOn(window,'confirm')`).
  - `ShipmentDetailPage.test.jsx` (+3): `editar_navegaAFormulario`, `eliminar_confirmaYElimina`, `panelEvidenciasAdmin_subirYAlternar` (upload + visibility toggle call helpers).
  - `EvidenciasGrid.test.jsx` (+2): `modoAdmin_muestraBotonToggle`, `modoAdmin_eliminarLlamaCallback`.
  Run: `npx vitest run AdminDashboard ShipmentDetailPage EvidenciasGrid` → FAIL.
- [ ] **Step 2: Verify RED.**
- [ ] **Step 3: GREEN impl.** (Tests authoritative.)
- [ ] **Step 4: Verify GREEN** → PASS (3+3+2 new). Full `npx vitest run` still green (existing ShipmentDetailPage tests now mock the new buttons' helpers — verify no stale asserts).
- [ ] **Step 5: Commit.** `git add frontend-react/src/pages/AdminDashboard.jsx frontend-react/src/pages/AdminDashboard.test.jsx frontend-react/src/pages/ShipmentDetailPage.jsx frontend-react/src/pages/ShipmentDetailPage.test.jsx frontend-react/src/components/EvidenciasGrid.jsx frontend-react/src/components/EvidenciasGrid.test.jsx` → `feat: acciones CRUD de envíos y panel de evidencias admin en la SPA React`.

---

### Task 8: Deprecación del CMS — borrado + redirect — tests

**Files:** Delete `controller/AdminController.java`, `templates/cms/*.html` (9), `templates/fragments/admin-sidebar.html`, `test/.../controller/AdminControllerTest.java`, `test/.../controller/AdminThemeAssetsTest.java`, `test/.../integration/AdminDocumentosPageIntegrationTest.java`; Modify `config/SpaForwardController.java`; Modify `test/.../config/SpaForwardControllerTest.java`; re-check `SecurityConfigTest`, `LoginControllerTest`.

**Verified:** `SpaForwardController` forwards `/dashboard` + `/dashboard/**` → `index.html` (line 9). `SecurityConfig` keeps `/admin/**` authenticated (anonymous → `/login`); admin → SPA. `LoginController.defaultSuccessUrl` keeps `/react-dashboard/` (both roots served by the SPA via `ReactConfig`) — no security-scope change in F6.

**Redirect (add to SpaForwardController):**
```java
@GetMapping({"/admin", "/admin/**"})
public String legacyAdminRedirect() { return "redirect:/dashboard"; }
```
- [ ] **Step 1: RED — SpaForwardControllerTest.** Add test `adminRedirectsToDashboard` — GET `/admin` and GET `/admin/whatever` → 302, header `Location=/dashboard`. Run `mvn test -Dtest=SpaForwardControllerTest` → FAIL (no mapping).
- [ ] **Step 2: Verify RED.**
- [ ] **Step 3: GREEN impl.** Add redirect mappings. Then delete the legacy files listed above. Sweep for dangling references before/after delete:
  - `grep -rn "AdminController\|/admin/tracking\|/admin/textos\|/admin/imagenes\|cms/" src/test src/main` — only `SecurityConfig` `/admin/**` (kept) and the redirect mapping may remain.
  - `TemplateAssetIntegrityTest` walks **all** `templates/**/*.html` for CSS refs — deleting cms templates can only remove refs; verify it still passes.
  - `LoginControllerTest` (line ~55) + `SecurityConfigTest` (line ~144): only touch if they reference deleted pages; spec says keep `/react-dashboard/` — expected no change.
- [ ] **Step 4: Verify GREEN.** `mvn test -Dtest=SpaForwardControllerTest,TemplateAssetIntegrityTest,SecurityConfigTest,LoginControllerTest,PublicControllerTest` PASS. Full `mvn test` → BUILD SUCCESS (~334 net after Task 1-4; legacy test deletions already subtracted).
- [ ] **Step 5: Commit.** `git add -A src/main/java/com/monteastur/envios/controller/AdminController.java src/main/java/com/monteastur/envios/config/SpaForwardController.java src/main/resources/templates src/test/java/com/monteastur/envios/controller/AdminControllerTest.java src/test/java/com/monteastur/envios/controller/AdminThemeAssetsTest.java src/test/java/com/monteastur/envios/integration/AdminDocumentosPageIntegrationTest.java src/test/java/com/monteastur/envios/config/SpaForwardControllerTest.java` → `refactor: depreca AdminController y templates cms/*.html con redirect a /dashboard`.

---

### Task 9: Routing + Styles + Docs + closing verification

**Files:** Modify `App.jsx`, `index.css`; Modify `docs/ARQUITECTURA_INTERFACES.md`, `docs/handoff.md`.

- [ ] **Step 1: App.jsx** — import `EnvioFormPage`; add two routes (inside `MainLayout` + `ProtectedRoute`, mirror F4/F5 shape):
  ```jsx
  <Route path="/dashboard/envios/nuevo" element={<ProtectedRoute><EnvioFormPage /></ProtectedRoute>} />
  <Route path="/dashboard/envios/:codigo/editar" element={<ProtectedRoute><EnvioFormPage /></ProtectedRoute>} />
  ```
- [ ] **Step 2: index.css** — append F6 classes scoped to modules (`.envio-form`, `.evidencias-admin-panel`, reuse `.acciones-fila`, `.cell-*`, `.upload-form` from F4/F5). Match corporate identity `#d4762a`; no hardcoded brand overrides.
- [ ] **Step 3: ARQUITECTURA_INTERFACES.md** — F6 row (line ~92) → "Completa"; update lines ~70, 80-81 if they describe legacy admin; rewrite §4 to describe current state (envíos CRUD REST, no CMS Thymeleaf).
- [ ] **Step 4: handoff.md** — append F6 entry (commits + verificación final) following the F5 block format.
- [ ] **Step 5: Verify routing** — `cd frontend-react && npx vitest run` (all green, ~98 tests), `npm run build` → OK.
- [ ] **Step 6: Full backend gate** — `$env:JAVA_HOME="$env:USERPROFILE\.jdks\openjdk-25.0.2"; & C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd clean test` → **BUILD SUCCESS**, ~334 `@Test`, 0 failures, 0 errors.
- [ ] **Step 7: Commit.** `git add frontend-react/src/App.jsx frontend-react/src/index.css docs/ARQUITECTURA_INTERFACES.md docs/handoff.md` → `docs: rutas, estilos, roadmap F6 completa y handoff`.

---

### Task 10: Final verification + closing gate

- [ ] `mvn clean test` (JDK 25) → **BUILD SUCCESS**, ~334 tests, 0 failures, 0 errors.
- [ ] `cd frontend-react && npx vitest run` → **~98 tests**, 0 failures.
- [ ] `cd frontend-react && npm run build` → OK.
- [ ] Confirm deletions: `Test-Path src/main/java/com/monteastur/envios/controller/AdminController.java` → False; `Test-Path src/main/resources/templates/cms` → False; `Test-Path src/main/resources/templates/fragments/admin-sidebar.html` → False. `/admin` y `/admin/**` → redirect a `/dashboard` (test en SpaForwardControllerTest).
- [ ] `git log --oneline <base>..HEAD` → 9 commits atómicos F6; `git diff --stat` sane. `git status` working tree clean (solo `node_modules/` pre-existente).
- [ ] Nada hecho `git push`.

---

## TDD Rhythm Notes (per task)

- Every new method/class gets a failing test FIRST; run to confirm RED, then implement MINIMAL code to GREEN; refactor; run; commit.
- Backend controller tests: `mvn test -Dtest=<Class>` (fast). Full `mvn test` per task after GREEN; full `mvn clean test` at Tasks 8/10 gates.
- Frontend: `npx vitest run <file>` per task; full `npx vitest run` at Tasks 7/9/10.
- Task ordering matters: Task 1 → 2 → 3 (UploadService overload consumed by `subirEvidencia`; if Task 3 lands first, Task 2's `subirEvidencia` uses the 3-arg overload directly — either order works, the snippet in Task 2 assumes the overload exists by implementation time).
- `MockMultipartFile`: `org.springframework.mock.web.MockMultipartFile`.
- `@MockBean` DataSource/RBACAccessLogger/CustomAccessDeniedHandler are required by the SecurityConfig import in `@WebMvcTest` — mirror `ImagenApiControllerTest`/`TextoLegalApiControllerTest` exactly.
- `EnvioTrackingServiceTest`: pure mocks, `InOrder` for `crear` and `eliminar`; no Spring context (fast).
- Route to detail after save: `/dashboard/envio/${codigo}` (singular — matches `App.jsx` existing route).
