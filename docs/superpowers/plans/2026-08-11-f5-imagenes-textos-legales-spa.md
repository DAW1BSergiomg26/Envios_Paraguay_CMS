# F5 — Imágenes y textos legales (SPA + REST) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Tasks use checkbox (`- [ ]`) syntax for tracking. Each task is an independent TDD cycle (RED → GREEN → REFACTOR) and is sized to be a meaningful reviewer gate.

**Goal:** Provide the React admin SPA with a managed image gallery and a legal-texts editor, backed by dedicated Spring Boot REST admin controllers, full test coverage, and zero regressions.

**Architecture:** Backend follows F4's per-resource convention — dedicated `ImagenApiController` (`/api/v1/admin/imagenes`) + `TextoLegalApiController` (`/api/v1/admin/textos`), both `@PreAuthorize("hasRole('ROLE_ADMIN')")`. New `UploadService` handles file I/O against `app.upload.dir` (images → `/uploads/<uuid>`, reusing existing `/uploads/**` serving). Frontend mirrors F2/F3/F4 pages: `api.js` helpers → `AdminImagesPage`/`AdminLegalTextsPage` → routes in `App.jsx` + nav in `MainLayout.jsx` + scoped CSS.

**Tech Stack:** Spring Boot 3.5+, Java 25, Spring Data JPA, MySQL 8, Flyway V1 (no schema changes); React 18 + Vite + Vitest + @testing-library/react + jest-dom; no Lombok; constructor injection only.

## Global Constraints

- Java toolchain: `$env:JAVA_HOME="$env:USERPROFILE\.jdks\openjdk-25.0.2"`; Maven `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd`.
- Backend test: `mvn test` (full) or `mvn test -Dtest=<Class>` (focused). Must end `BUILD SUCCESS`.
- Frontend test: `cd frontend-react; npx vitest run` (full) or `npx vitest run <file>` (focused). All green.
- Frontend build: `cd frontend-react; npm run build` → OK.
- No Lombok; entities/models/DTOs are pure Java (private fields, no-arg ctor, ctor-with-args, manual getters/setters).
- API conventions: DTOs in `com.monteastur.envios.dto.api`; REST exceptions reuse `GlobalExceptionHandler` + `com.monteastur.envios.exception.*` (`ResourceNotFoundException`→404, `BadRequestException`→400).
- Frontend conventions: helpers in `frontend-react/src/services/api.js` (axios `baseURL='/api/v1'`, `withCredentials:true`); pages mock `../services/api` + `../context/NotificationContext`; use `useToast`, `EmptyState`, `parseLocalDateTime` where dates appear.
- Commits atomic on `main`, **no push** unless explicitly authorized.

## File Structure

```
src/main/java/com/monteastur/envios/
  service/UploadService.java                  [CREATE — file I/O, UUID naming, ext allowlist]
  service/ImagenService.java                [optional, inline in controller if trivial — see note]*
  controller/api/ImagenApiController.java   [CREATE]
  controller/api/TextoLegalApiController.java [CREATE]
  dto/api/ImagenDto.java                    [CREATE]
  dto/api/TextoLegalDto.java                [CREATE]
  dto/api/ActualizarOrdenImagenRequest.java [CREATE]
  dto/api/ActualizarTextoRequest.java       [CREATE]
frontend-react/src/
  services/api.js                            [MODIFY — 7 helpers]
  services/api.test.js                       [MODIFY — +tests]
  pages/AdminImagesPage.jsx                  [CREATE]
  pages/AdminImagesPage.test.jsx             [CREATE]
  pages/AdminLegalTextsPage.jsx              [CREATE]
  pages/AdminLegalTextsPage.test.jsx         [CREATE]
  App.jsx                                    [MODIFY — 2 routes]
  layouts/MainLayout.jsx                     [MODIFY — 2 nav buttons]
  index.css                                  [MODIFY — F5 styles]
docs/ARQUITECTURA_INTERFACES.md              [MODIFY — F5 → Completa]
docs/handoff.md                              [MODIFY — F5 entry]
src/test/.../UploadServiceTest.java           [CREATE]
src/test/.../controller/api/ImagenApiControllerTest.java [CREATE]
src/test/.../controller/api/TextoLegalApiControllerTest.java [CREATE]
```

>*Servicio de imágenes*: F5 mantiene la lógica de negocio mínima (orden, borrado asociado a repo). Se decide inline en el controlador para evitar capa extra sin valor (YAGNI); si crece, extraer a `ImagenService`.

## Global Test Targets (F5 additions)

| Layer | File | Tests (≈) |
|---|---|---|
| backend | `UploadServiceTest` | 5 |
| backend | `ImagenApiControllerTest` | 8 |
| backend | `TextoLegalApiControllerTest` | 7 |
| backend **new total** | — | **20** → repo 333 → **353** |
| frontend | `api.test.js` (+helpers) | +10 |
| frontend | `AdminImagesPage.test.jsx` | 7 |
| frontend | `AdminLegalTextsPage.test.jsx` | 6 |
| frontend **new total** | — | **+23** → repo 57 → **80** |

---

### Task 1: UploadService (file I/O) — TDD

**Files:** Create `src/main/java/.../service/UploadService.java`; Create `src/test/java/.../UploadServiceTest.java`.

**Produces (consumed by Task 2):**
```java
public String subirArchivo(MultipartFile archivo, String subDir) throws IOException;  // returns relPath, e.g. "uuid.jpg" or "sub/uuid.jpg"
public void eliminarArchivo(String pathRelativo);      // idempotent, no throw
```

**Constants:** `EXT_PERMITIDAS = jpg|jpeg|png|webp|gif|svg` (lowercase).

- [ ] **Step 1: Failing test (RED).** `UploadServiceTest` — plain JUnit 5 (`@TempDir Path tmp`). Construct `new UploadService(tmp.toString())`. Tests:
  - `subirArchivo_escribeArchivoYDevuelveRutaRelativa`: `new MockMultipartFile("archivo","foto.jpg","image/jpeg","img".getBytes())`, `subDir="imagenes"` → returns `"imagenes/<uuid>.jpg"`, file exists under `tmp/imagenes/`.
  - `subirArchivo_raiz_cuandoSubDirVacio`: `subirArchivo(foto,"")` → returns `"<uuid>.jpg"` (matches existing `/uploads/<uuid>` for images).
  - `subirArchivo_extensionNoPermitida_lanzaBadRequest`: `"foto.exe"` → `BadRequestException`.
  - `subirArchivo_archivoVacio_lanzaBadRequest`: empty `MockMultipartFile` → `BadRequestException`.
  - `eliminarArchivo_borraYIdempotente`: eliminaArchivo(relPath) borra; llamar de nuevo no lanza.

  Run: `& mvn -q test -Dtest=UploadServiceTest` → **FAIL** (`UploadService` class missing / methods undefined).

- [ ] **Step 2: Verify RED.** Expected: tests fail to compile (`cannot find symbol UploadService`) or `NullPointerException`. Confirmed failing for the right reason.

- [ ] **Step 3: Minimal GREEN impl.** `UploadService.java`:
  ```java
  @Service
  public class UploadService {
      private final Path uploadDir;
      private static final List<String> EXT_PERMITIDAS = List.of("jpg","jpeg","png","webp","gif","svg");
      public UploadService(@Value("${app.upload.dir}") String uploadDir) {
          this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
      }
      public String subirArchivo(MultipartFile archivo, String subDir) throws IOException {
          if (archivo == null || archivo.isEmpty()) throw new BadRequestException("Archivo vacío");
          String nombre = archivo.getOriginalFilename();
          String ext = "";
          if (nombre != null && nombre.contains(".")) ext = nombre.substring(nombre.lastIndexOf(".")+1).toLowerCase();
          if (!EXT_PERMITIDAS.contains(ext)) throw new BadRequestException("Tipo de archivo no permitido");
          String uuid = UUID.randomUUID().toString() + "." + ext;
          Path base = uploadDir;
          if (subDir != null && !subDir.isBlank()) base = uploadDir.resolve(subDir);
          Files.createDirectories(base);
          Path destino = base.resolve(uuid);
          Files.write(destino, archivo.getBytes());
          return (subDir != null && !subDir.isBlank() ? subDir + "/" : "") + uuid;
      }
      public void eliminarArchivo(String pathRelativo) {
          if (pathRelativo == null || pathRelativo.isBlank()) return;
          Path target = uploadDir.resolve(pathRelativo).normalize();
          try { Files.deleteIfExists(target); } catch (IOException ignored) {}
      }
  }
  ```

- [ ] **Step 4: Verify GREEN.** Run `& mvn -q test -Dtest=UploadServiceTest` → **PASS** (5 tests). `Files` = `java.nio.file.*`, `UUID`/`List` imported, `BadRequestException` from `com.monteastur.envios.exception`.

- [ ] **Step 5: Commit.** `git add service/UploadService.java test/.../UploadServiceTest.java && git commit -q -m "refactor: extrae UploadService reutilizable para subida de archivos"`.

---

### Task 2: ImagenApiController + ImagenDto (+ ActualizarOrdenImagenRequest) — TDD

**Files:** Create `dto/api/ImagenDto.java`, `dto/api/ActualizarOrdenImagenRequest.java`; Create `controller/api/ImagenApiController.java`; Create `controller/api/ImagenApiControllerTest.java`.

**Consumes:** `UploadService` (Task 1), `ImagenRepository` (existing: `findAllByOrderByOrdenAsc`), `Imagen` model. **Produces:** endpoints in §3.1.

**Dto contracts (exact):**
```java
public class ImagenDto {  // no-arg ctor, getters/setters
    private Long id; private String titulo; private String descripcion;
    private String url; private String categoria; private Integer orden;
    private LocalDateTime createdAt;
}
public class ActualizarOrdenImagenRequest { Integer orden; } // +from, +getOrden/setOrden
private static ImagenDto toDto(Imagen i) { d.setUrl(i.getUrl()); ... }
```

- [ ] **Step 1: RED — ImagenApiControllerTest.** `@WebMvcTest(ImagenApiController.class)` + `@Import({GlobalExceptionHandler.class, SecurityConfig.class})` + `@WithMockUser(username="admin", roles="ADMIN")` + `@MockBean ImagenRepository imagenRepo, UploadService uploadService, DataSource dataSource, RBACAccessLogger rbacAccessLogger, CustomAccessDeniedHandler accessDenied`. Mock `Imagen` → `ImagenDto`. Tests (8):
  1. `listar_retornaNombreOrdenado` — mock `findAllByOrderByOrdenAsc` returns `[img1,img2]` → 200, `jsonPath("$[0].titulo")`.
  2. `subir_retornaCreado` — `multipart("/api/v1/admin/imagenes").file("archivo",...).param("titulo","Foto").param("orden","1")`, mock `uploadService.subirArchivo→"abc.jpg"`, `imagenRepo.save(any)` returns img → 201, `jsonPath("$.url").value("/uploads/abc.jpg")`.
  3. `subir_archivoVacio_400` — empty file → 400.
  4. `subir_extensionInvalida_400` — "foto.exe" → 400.
  5. `cambiarOrden_retornado` — `PATCH /imagenes/1/orden`, body `{"orden":2}`, repo.findById(1)=img, mock save → 200, `jsonPath("$.orden").value(2)`.
  6. `cambiarOrden_noExiste_404` — repo.findById(1) empty → 404.
  7. `eliminar_204` — `DELETE /imagenes/1`, repositorio.findById presente (deleta url + repo.delete), → 204.
  8. `sinAutenticacion_401` — `@WithAnonymousUser` en un test → 401.
  Run: `& mvn -q test -Dtest=ImagenApiControllerTest` → **FAIL** (controller/class missing, methods undefined).

- [ ] **Step 2: Verify RED** (compile fails on `ImagenApiController`/`ImagenDto` missing).

- [ ] **Step 3: GREEN impl.** `ImagenApiController`:
  ```java
  @Tag(name="Admin Galería") @RestController @RequestMapping("/api/v1/admin/imagenes") @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ImagenApiController(ImagenRepository repo, UploadService uploadService) { ... }
  @GetMapping List<ImagenDto> = repo.findAllByOrderByOrdenAsc().stream().map(this::toDto).collect(toList());
  @PostMapping(consumes=MULTIPART_FORM_DATA_VALUE) ResponseEntity<ImagenDto> subir(@RequestParam String titulo, @RequestParam(required=false) String descripcion, @RequestParam(required=false) String categoria, @RequestParam Integer orden, @RequestPart("archivo") MultipartFile archivo) { String rel=uploadService.subirArchivo(archivo,""); Imagen i=new Imagen(titulo,descripcion,"/uploads/"+rel,categoria,orden); return ResponseEntity.status(201).body(toDto(repo.save(i))); }
  @PatchMapping("/{id}/orden") ResponseEntity<ImagenDto> cambiar(@PathVariable Long id, @RequestBody ActualizarOrdenImagenRequest r){ Imagen i=repo.findById(id).orElseThrow(()->new ResourceNotFoundException("Imagen "+id)); i.setOrden(r.getOrden()); return ResponseEntity.ok(toDto(repo.save(i))); }
  @DeleteMapping("/{id}") @ResponseStatus(NO_CONTENT) void eliminar(@PathVariable Long id){ Optional<Imagen> opt=repo.findById(id); if(opt.isPresent()){ Imagen i=opt.get(); uploadService.eliminarArchivo(i.getUrl().replaceFirst("^/uploads/","")); repo.delete(i); } }
  ```
  `url.replaceFirst("^/uploads/","")` → rel path for UploadService.eliminarArchivo. Images use root upload dir (relPath = "uuid.ext"), matching existing demo images served at `/uploads/<uuid>`.

- [ ] **Step 4: Verify GREEN** → `& mvn -q test -Dtest=ImagenApiControllerTest` PASS (8). Run full `mvn test` → BUILD SUCCESS (333 + 5 UploadService +8 = 346).

- [ ] **Step 5: Commit.** `git add dto/api/* controller/api/Imagen*` → `feat: API REST de gestión de imágenes (galería) con tests`.

---

### Task 3: TextoLegalApiController + TextoLegalDto (+ ActualizarTextoRequest) — TDD

**Files:** Create `dto/api/TextoLegalDto.java`, `dto/api/ActualizarTextoRequest.java`; Create `controller/api/TextoLegalApiController.java`; Create `controller/api/TextoLegalApiControllerTest.java`.

**Consumes:** `TextoLegalRepository` (existing: `findBySlug`, `findAll`). **Produces:** §3.2.

**Dto:** `TextoLegalDto{id,slug,titulo,contenido,updatedAt}`. `listar` maps WITHOUT contenido (null) — list excludes large bodies.

- [ ] **Step 1: RED.** `@WebMvcTest(TextoLegalApiController.class)` + security import + `@MockBean TextoLegalRepository textoRepo, DataSource, RBACAccessLogger, CustomAccessDeniedHandler`. Tests (7):
  1. `listar_sinContenido` — `findAll()` returns 2 textos → 200, `jsonPath("$[0].contenido").value((String)null)` (or `doesNotExist` if we omit). Decision: include field but null in list (simpler mapping). Assert `jsonPath("$[0].slug")`, `jsonPath("$[0].contenido").value((String)null)`.
  2. `porSlug_conContenido` — `findBySlug("aviso-legal")` → 200, `jsonPath("$.contenido").value("...")`.
  3. `porSlug_noExiste_404` — empty → 404 `ResourceNotFoundException`.
  4. `actualizar_ok` — find present, PUT body `{titulo,contenido}`, `save` returns, → 200, `jsonPath("$.titulo")`.
  5. `actualizar_noExiste_404` — slug not found → 404.
  6. `actualizar_contenidoVacio_400` — contenido blank → `BadRequestException` 400.
  7. `sinAutenticacion_401` — `@WithAnonymousUser`.
  Run: `& mvn -q test -Dtest=TextoLegalApiControllerTest` → FAIL.

- [ ] **Step 2: Verify RED.**

- [ ] **Step 3: GREEN impl.**
  ```java
  @Tag(name="Admin Textos Legales") @RestController @RequestMapping("/api/v1/admin/textos") @PreAuthorize("hasRole('ROLE_ADMIN')")
  @GetMapping List<textoDto sin contenido> = textoRepo.findAll().stream().map(i->toDto(i,false)).collect(toList());
  @GetMapping("/{slug}") ResponseEntity<TextoLegalDto> porSlug(@PathVariable String slug){ TextoLegal t=textoRepo.findBySlug(slug).orElseThrow(()->new ResourceNotFoundException("Texto "+slug)); return ok(toDto(t,true)); }
  @PutMapping("/{slug}") ResponseEntity<TextoLegalDto> actualizar(@PathVariable String slug, @RequestBody ActualizarTextoRequest r){ if(r.getTitulo()==null||r.getTitulo().isBlank()||r.getContenido()==null||r.getContenido().isBlank()) throw new BadRequestException("Título y contenido requeridos"); TextoLegal t=textoRepo.findBySlug(slug).orElseThrow(...); t.setTitulo(r.getTitulo()); t.setContenido(r.getContenido()); t.setUpdatedAt(LocalDateTime.now()); return ok(toDto(textoRepo.save(t),true)); }
  static toDto(TextoLegal t,boolean conContenido){ d.setContenido(conContenido?t.getContenido():null); ... }
  ```

- [ ] **Step 4: Verify GREEN** → PASS (7). Full `mvn test` → BUILD SUCCESS (~351).

- [ ] **Step 5: Commit.** `git add dto/api/* controller/api/TextoLegal*` → `feat: API REST de textos legales con tests`.

---

### Task 4: api.js helpers + api.test.js — TDD

**Files:** Modify `frontend-react/src/services/api.js`; Modify `frontend-react/src/services/api.test.js`.

**Helpers (exact signatures/URLs):**
```js
export function getAdminImagenes()        { return api.get('/admin/imagenes'); }
export function uploadAdminImagen(formData){ return api.post('/admin/imagenes', formData); }
export function patchAdminImagenOrden(id, orden){ return api.patch(`/admin/imagenes/${id}/orden`, { orden }); }
export function deleteAdminImagen(id)     { return api.delete(`/admin/imagenes/${id}`); }
export function getAdminTextos()          { return api.get('/admin/textos'); }
export function getTextoLegal(slug)       { return api.get(`/admin/textos/${slug}`); }
export function putTextoLegal(slug, { titulo, contenido }){ return api.put(`/admin/textos/${slug}`, { titulo, contenido }); }
```
Convention: `uploadAdminImagen` posts `FormData` (axios sets multipart, like `uploadImportCsv`).

- [ ] **Step 1: RED — add failing assertion** in `api.test.js`: `expect(getAdminImagenes).toBeDefined()` etc. for each of the 7; mock axios via `vi.mock('axios')` returning `{data}`. Run `cd frontend-react; npx vitest run api.test.js` → FAIL (helpers undefined).
- [ ] **Step 2: Verify RED.**
- [ ] **Step 3: GREEN — add 7 helpers to api.js** (place after mensajes helpers, before `export default api`).
- [ ] **Step 4: Verify GREEN** → `npx vitest run api.test.js` PASS (+10 assertions).
- [ ] **Step 5: Commit.** `git add services/api.js services/api.test.js` → `feat: helpers de API para imágenes y textos legales`.

---

### Task 5: AdminImagesPage.jsx + tests — TDD

**Files:** Create `pages/AdminImagesPage.jsx`, `pages/AdminImagesPage.test.jsx`.

**Consumes (api.js):** `getAdminImagenes`, `uploadAdminImagen`, `patchAdminImagenOrden`, `deleteAdminImagen` (mock in tests — same pattern as F4 ReservasPage.test.jsx).

**UX:** top upload form (file input `archivo`, inputs `titulo`/`categoria`/`orden`, textarea `descripcion`); below a `.galeria-grid` of `.imagen-card` (img thumbnail from `url`, `titulo`, chip `categoria`, number input `orden` persisted on change, delete button w/ `confirm`). Uses `useToast`, `EmptyState`. Title "Imágenes de la galería"; subtitle "Gestión de imágenes de la web y documentación.".

- [ ] **Step 1: RED.** 7 failing tests in `AdminImagesPage.test.jsx` (structure mirrors F4 MensajesPage/ReservasPage: `vi.mock('../services/api')`, `vi.mock('../context/NotificationContext')`):
  1. `carga y muestra galería` — render, `findByAltText`/`img` with titulo.
  2. `muestra EmptyState sin imágenes`.
  3. `sube imagen y recarga` — fill form fields + `fireEvent.change` file input, submit, assert `uploadAdminImagen` called w/ FormData, then `getAdminImagenes` recargado, toast success.  4. `subida fallida muestra error` — mock reject → `showError`.
  5. `cambia orden y persiste` — userEvent spin number → `patchAdminImagenOrden(id, orden)`.
  6. `elimina con confirmación` — `vi.spyOn(window,'confirm').mockReturnValue(true)` → `deleteAdminImagen` + recarga.
  7. `no elimina sin confirmar` — confirm false → `deleteAdminImagen` not called.
  Run: `cd frontend-react; npx vitest run AdminImagesPage` → FAIL (component missing).

- [ ] **Step 2: Verify RED.**

- [ ] **Step 3: GREEN impl.** (Representative structure — component holds its own TDD; tests above are authoritative.)

- [ ] **Step 4: Verify GREEN** → PASS (7).

- [ ] **Step 5: Commit.** `git add pages/AdminImagesPage.jsx pages/AdminImagesPage.test.jsx` → `feat: página de gestión de imágenes en la SPA React`.

---

### Task 6: AdminLegalTextsPage.jsx + tests — TDD

**Files:** Create `pages/AdminLegalTextsPage.jsx`, `pages/AdminLegalTextsPage.test.jsx`.

**Consumes:** `getAdminTextos`, `getTextoLegal`, `putTextoLegal`.

**UX:** split layout — left master list (slug + titulo, selected highlighted); right detail editor (titulo `<input>`, contenido `<textarea>`, "Guardar" disabled until change, PUT on save). Title "Textos legales"; subtitle "Gestión de aviso legal y política de cookies.".

- [ ] **Step 1: RED.** 6 tests:
  1. `carga y lista textos en el master` — `getAdminTextos` → master rows w/ slug+titulo.
  2. `abre detalle al hacer click` — click row → `getTextoLegal(slug)` → editor shows titulo+contenido.
  3. `guarda cambios con PUT` — edit textarea, click Guardar → `putTextoLegal(slug,{titulo,contenido})`, toast success.
  4. `recarga lista tras guardar` — `getAdminTextos` llamado de nuevo.
  5. `muestra error si el servicio falla` — reject → `showError`.
  6. `deshabilita Guardar hasta cambios` — (o `Guardar` no llama PUT sin cambios).
  Run: `npx vitest run AdminLegalTextsPage` → FAIL.

- [ ] **Step 2: Verify RED.**

- [ ] **Step 3: GREEN impl.**

- [ ] **Step 4: Verify GREEN** → PASS (6).

- [ ] **Step 5: Commit.** `git add pages/AdminLegalTextsPage.jsx pages/AdminLegalTextsPage.test.jsx` → `feat: página de gestión de textos legales en la SPA React`.

---

### Task 7: Routing + Nav + Styles + Docs

**Files:** Modify `App.jsx`, `layouts/MainLayout.jsx`, `index.css`; Modify `docs/ARQUITECTURA_INTERFACES.md`; Modify `docs/handoff.md`.

- [ ] **Step 1: App.jsx** — add `import AdminImagesPage from './pages/AdminImagesPage'; import AdminLegalTextsPage from './pages/AdminLegalTextsPage';` and two routes (mirror F4 shape, inside `<ProtectedRoute>`):
  ```jsx
  <Route path="/dashboard/imagenes" element={<ProtectedRoute><AdminImagesPage /></ProtectedRoute>} />
  <Route path="/dashboard/textos" element={<ProtectedRoute><AdminLegalTextsPage /></ProtectedRoute>} />
  ```
- [ ] **Step 2: MainLayout.jsx** — add two nav buttons (after Mensajes, before InstallPWAButton):
  ```jsx
  <button className="btn-nav-link" onClick={() => navigate('/dashboard/imagenes')}>Imágenes</button>
  <button className="btn-nav-link" onClick={() => navigate('/dashboard/textos')}>Textos legales</button>
  ```
- [ ] **Step 3: index.css** — append F5 classes scoped to modules (`.galeria-grid`, `.imagen-card`, `.upload-form`, `.texto-editor`, reuse `.acciones-fila`/`.cell-*`/`.lote-badge` from F4). Match corporate identity; no hardcoded brand overrides.
- [ ] **Step 4: ARQUITECTURA_INTERFACES.md** — line 91 F5 row → "Completa".
- [ ] **Step 5: handoff.md** — append F5 entry (commits + verificación final) following the F4 block format.
- [ ] **Step 6: Commit.** `git add App.jsx layouts/MainLayout.jsx index.css docs/ARQUITECTURA_INTERFACES.md docs/handoff.md` → `docs: rutas, nav, estilos e handoff F5`.

---

### Task 8: Final verification + closing commit

- [ ] `mvn test` (JDK 25) → **BUILD SUCCESS**, count ~353, 0 failures, 0 errors.
- [ ] `cd frontend-react && npx vitest run` → **80 tests / 12 files, 0 failures**.
- [ ] `cd frontend-react && npm run build` → OK.
- [ ] `git log --oneline <base>..HEAD` → 7 commits atómicos F5; `git diff --stat` = 19 files +~1300. `git status` working tree clean (solo `node_modules/` pre-existing no ignorado).
- [ ] Nada hecho `git push`.

---

## TDD Rhythm Notes (per task)

- Every new method/class gets a failing test FIRST; run to confirm RED, then implement MINIMAL code to GREEN; refactor; run; commit.
- Backend controller tests: `mvn test -Dtest=<Class>` (fast). Full `mvn test` only at Task 8 gate (and optionally after T3).
- Frontend: `npx vitest run <file>` per task; full `npx vitest run` at Task 8.
- `BadRequestException` import: `com.monteastur.envios.exception.BadRequestException` (exists per bloque3 refactor). `ResourceNotFoundException`: `com.monteastur.envios.exception.ResourceNotFoundException`.
- `MultipartFile` mock in tests: `org.springframework.mock.web.MockMultipartFile`.
