# F6 — CRUD de envíos, evidencias admin y deprecación total del CMS — Design Spec

- **Fecha:** 2026-08-11
- **Fase:** F6 (ver `docs/ARQUITECTURA_INTERFACES.md:92` — "Deprecación: `/admin/**` → redirect total a `/dashboard` + borrado de templates `cms/*.html`")
- **Estado del spec:** propuesto / aprobado en brainstorming
- **Stack:** Spring Boot 3.5+ (backend) + React 18 + Vite + Vitest (SPA); MySQL 8; **Java 25**; cero Lombok; inyección por constructor.
- **Decisión de controlador:** **Opción A** — nuevo `EnvioApiController` dedicado (`/api/v1/admin/envios`) para operaciones de escritura (alta, edición, borrado y evidencias). `AdminApiController` conserva lecturas (`GET /envios`, `GET /envios/{codigo}`, `GET /clientes`) y `PUT /envios/{codigo}/estado`, como está hoy (verificado en `AdminApiController.java:201`). Se evita el monolito y se sigue la convención de F4/F5 (controladores API por recurso).

## 1. Contexto y alcance

La SPA React del panel admin (`frontend-react`) ya cubre F1–F5. F6 cierra la migración con tres bloques:

1. **CRUD de envíos admin:** alta y edición de `EnvioTracking` (hoy solo existen `GET`s y `PUT estado` en la API), con borrado seguro de hijos.
2. **Evidencias admin:** subida, visibilidad y borrado de `EvidenciaEnvio` desde la SPA (hoy solo existe el detalle de lectura).
3. **Deprecación total del CMS legacy:** borrado de `AdminController` (417 líneas), de los 9 templates `cms/*.html` y de `fragments/admin-sidebar.html`; `/admin/**` → redirect a la SPA (`/dashboard`).

### Bugs latentes que F6 arregla (verificados en código)

- **`EnvioTrackingService.eliminar(Long)`** (`EnvioTrackingService.java:66-69`): `repo.deleteById(id)` sin `@Transactional` ni limpieza de hijos. Las FKs `fk_eventos_envio` / `fk_evidencias_envio` no tienen CASCADE → eliminar un envío con eventos/evidencias lanza `DataIntegrityViolationException`. **Fix:** `@Transactional` + `deleteByEnvioTrackingId(...)` en `EventoTrackingRepository` y `EvidenciaEnvioRepository` (derived delete de Spring Data).
- **`getDocumentoUrl`** (`frontend-react/src/services/api.js:103-111`): devuelve `/admin/documentos/...`, ruta legacy que ya **no existe** como endpoint (`AdminController` solo tiene `GET /documentos` de página; los PDFs reales viven en `DocumentosController` → `/api/v1/admin/documentos/...`). En la SPA, `window.open('/admin/documentos/...')` no abre el PDF. **Fix:** apuntar a `/api/v1/admin/documentos/...` (misma-origen → la cookie `JSESSIONID` viaja sola). Actualizar asserts en `api.test.js:21-31`, `ShipmentDetailPage.test.jsx:53-61`, `ImportBatchPage.test.jsx:138-148`.
- **`EvidenciaDto`** (`dto/api/EvidenciaDto.java:22`): sin `id` ni `fechaSubida` → la SPA no puede hacer toggle/borrado (necesita el id) ni mostrar la fecha. **Fix:** añadir `Long id` + `LocalDateTime fechaSubida` + `static from(EvidenciaEnvio)`; migrar la construcción manual en `AdminApiController.java:190-198` y `ClienteApiController.java:114-122`.

### Fuera de alcance
- Portar la Zona cliente (`/cliente`, `ClientDashboardController`) — es Oficial, no legacy.
- Refactorizar el flujo público de tracking (`/tracking`) o web pública Thymeleaf — Oficial.
- Migraciones Flyway nuevas: el esquema ya existe (V1). CERO migraciones en F6.
- Autenticación/cliente de sesión: sin cambios en `SecurityConfig` (mantiene `/admin/**` autenticado y CSRF eximido en `/api/**`).

## 2. Decisiones de arquitectura

1. **`EnvioApiController`** (`/api/v1/admin/envios`, `@PreAuthorize("hasRole('ROLE_ADMIN')")`) con `@Tag` OpenAPI, espejando `ImagenApiController`/`TextoLegalApiController` de F5. `AdminApiController` **no se toca** salvo el mapeo de `EvidenciaDto` a `from(...)`.
2. **Servicio de dominio sobre DTOs:** `EnvioTrackingService` no conoce DTOs de request. Añade `crear(EnvioTracking)` y repara `eliminar(Long)`. El controlador orquesta: construye la entidad desde el request, valida (clientes, códigos), llama al servicio. La creación del evento inicial va **dentro de `crear`** vía `EventoTrackingService` inyectado (sin ciclos: `EventoTrackingService` no depende de `EnvioTrackingService`), replicando el orden de `AdminController.guardarTracking` (`AdminController.java:291-296`).
3. **`eliminar` transaccional con orden de borrado:** evidencias → eventos → envío, bajo `@Transactional` en el servicio. Los repos ganan `deleteByEnvioTrackingId(Long)` (derived delete). Test unitario con `InOrder` de Mockito que verifica el orden.
4. **Subida de evidencias:** reutiliza `UploadService` (creado en F5) con sobrecarga `subirArchivo(archivo, subDir, extensionesPermitidas...)`; evidencias → `subirArchivo(archivo, "evidencias", ".jpg", ".jpeg", ".png", ".webp", ".pdf")` (mismo set que `AdminController.java:347`). `urlArchivo = "/uploads/evidencias/" + relPath`. Se elimina así la lógica inline de `AdminController.subirEvidencia`. El borrado de la evidencia también borra el fichero del disco via `UploadService.eliminarArchivo`.
5. **DTOs:** en `com.monteastur.envios.dto.api`, POJOs puros sin Lombok, getters/setters manuales (regla global). `EvidenciaDto.from(EvidenciaEnvio)` único punto de mapeo. Ningún DTO expone `secret_token`/credenciales.
6. **Manejo de errores:** `GlobalExceptionHandler` existente — `ResourceNotFoundException`→404, `BadRequestException`→400. Validaciones de request (destinatario obligatorio, extensión no permitida, código duplicado) → 400.
7. **Código de envío:** si `CrearEnvioRequest.codigoUnico` viene vacío, el controlador usa `envioTrackingService.generarCodigo()` (mismo formato `MT-{year}-{seq}` de `EnvioTrackingService.java:75-78`). El servicio valida unicidad (existe `existsByCodigoUnico`) → 400 si duplicado.
8. **Deprecación:** al borrar `AdminController`, `/admin/**` queda sin handler. Se añade un redirect mínimo (`/admin` y `/admin/**` → `redirect:/dashboard`) para rutas legacy no cubiertas por la SPA. `SpaForwardController` ya sirve `/dashboard` y `/dashboard/**` → `index.html` (verificado `SpaForwardController.java:9`). `SecurityConfig` sin cambios: anónimo en `/admin/**` → `/login`; admin → SPA. `LoginController` y `defaultSuccessUrl` mantienen `/react-dashboard/` por ahora (la SPA también responde ahí vía `ReactConfig`); se anota como limpieza opcional en el mismo commit si los tests de redirect se ajustan.

## 3. Inventario de endpoints

### 3.1 Envíos — `EnvioApiController` (nuevo, `/api/v1/admin/envios`)

| Método | Ruta | Request | Respuesta | Códigos |
|---|---|---|---|---|
| POST | `/envios` | `CrearEnvioRequest{codigoUnico?, estado, destinatario, origen?, destino?, peso?, contenido?, observaciones?, clienteId?}` | `TrackingDto` | 201 / 400 / 401 |
| PUT | `/envios/{codigo}` | `ActualizarEnvioRequest{estado, destinatario, origen?, destino?, peso?, contenido?, observaciones?, clienteId?}` | `TrackingDto` | 200 / 400 / 404 |
| DELETE | `/envios/{codigo}` | — | — | 204 / 404 |
| POST | `/envios/{codigo}/evidencias` | multipart: `titulo`(req), `descripcion`, `tipo`(req: `FOTO`\|`DOCUMENTO`), `archivo`(req) | `EvidenciaDto` | 201 / 400 / 404 |
| PATCH | `/envios/evidencias/{id}/visibilidad` | `ActualizarVisibilidadRequest{visibleCliente}` | `EvidenciaDto` | 200 / 404 |
| DELETE | `/envios/evidencias/{id}` | — | — | 204 / 404 |

### 3.2 Conservados (sin cambios de contrato)

| Controller | Endpoints |
|---|---|
| `AdminApiController` | `GET /envios` (paginado/filtros), `GET /envios/{codigo}`, `GET /clientes`, `PUT /envios/{codigo}/estado` |
| `DocumentosController` | `GET /documentos`, `GET /documentos/envios/{codigo}/etiqueta`, `GET /documentos/lotes/{id}/etiquetas`, `GET /documentos/lotes/{id}/manifiesto` |

## 4. DTOs

- `CrearEnvioRequest`: `codigoUnico`, `estado`, `destinatario`, `origen`, `destino`, `peso`, `contenido`, `observaciones`, `clienteId`.
- `ActualizarEnvioRequest`: igual sin `codigoUnico`.
- `EvidenciaDto` (extendido): `id`, `titulo`, `descripcion`, `tipo`, `urlArchivo`, `visibleCliente`, `fechaSubida` + `static from(EvidenciaEnvio)`.
- `ActualizarVisibilidadRequest`: `visibleCliente` (Boolean).

## 5. Cambios de servicio y repositorio

### `EnvioTrackingService` (se amplía)
- Nuevas dependencias por constructor: `EventoTrackingService`, `EventoTrackingRepository`, `EvidenciaEnvioRepository` (todas inyectables, sin ciclos).
- `@Transactional EnvioTracking crear(EnvioTracking envio)` → `guardar(envio)` + `eventoTrackingService.crearEventoInicial(guardado)`.
- `@Transactional void eliminar(Long id)` → `evidenciaRepo.deleteByEnvioTrackingId(id)` → `eventoRepo.deleteByEnvioTrackingId(id)` → `repo.deleteById(id)`.

### Repositorios
- `EventoTrackingRepository`: añadir `void deleteByEnvioTrackingId(Long envioId)`.
- `EvidenciaEnvioRepository`: añadir `void deleteByEnvioTrackingId(Long envioId)`.

### `UploadService` (F5, se amplía)
- Sobrecarga `String subirArchivo(MultipartFile archivo, String subDir, String... extensionesPermitidas)`; la firma de 2 args delega en el set por defecto de imágenes (no rompe `ImagenApiController`). Test ampliado en `UploadServiceTest`.

### `EvidenciaEnvioService` — sin cambios (guarda/toggle/elimina/busca ya existen).

## 6. Flujo de datos y UX (frontend)

- **`EnvioFormPage.jsx`** (rutas `/dashboard/envios/nuevo` y `/dashboard/envios/:codigo/editar`): formulario de alta/edición (código con sugerencia automática, estado, destinatario, origen, destino, peso, contenido, observaciones, cliente desde `getAdminClientes`). Modo edición carga `getAdminEnvioDetalle(codigo)`. Guarda con `postAdminEnvio`/`putAdminEnvio`; navega a `/dashboard/envio/:codigo` al terminar. Usa `useToast`, `useNavigate`, `EmptyState`.
- **`AdminDashboard.jsx`**: botón "Nuevo envío" (→ `/dashboard/envios/nuevo`) en la cabecera; acciones por fila: "Editar" (→ `/dashboard/envios/${codigo}/editar`) y "Eliminar" (confirm + `deleteAdminEnvio` + recarga/remueve fila).
- **`ShipmentDetailPage.jsx`**: botones "Editar" y "Eliminar" (mismo patrón); panel admin de evidencias (subida `uploadAdminEvidencia`, toggle `patchAdminEvidenciaVisibilidad`, borrado `deleteAdminEvidencia`) reutilizando `EvidenciasGrid` en modo admin.
- **`EvidenciasGrid.jsx`**: `key={i}` → `key={ev.id}`; props opcionales `modoAdmin`, `onToggleVisibilidad`, `onEliminar`.
- **`api.js`** +6 helpers (estilo F4/F5): `postAdminEnvio`, `putAdminEnvio`, `deleteAdminEnvio`, `uploadAdminEvidencia` (FormData), `patchAdminEvidenciaVisibilidad`, `deleteAdminEvidencia`. `getDocumentoUrl` corregido a `/api/v1/admin/documentos/...`.
- **`App.jsx`**: 2 `<Route>` nuevas bajo `MainLayout` + `ProtectedRoute`.
- **`MainLayout.jsx`**: sin botón nav nuevo — `/` ya es el dashboard de envíos (decisión: no duplicar navegación).
- **`index.css`**: `.envio-form`, `.evidencias-admin-panel` (scoped; reusan `.acciones-fila`, `.cell-*`, `.upload-form` de F4/F5).

## 7. Estrategia TDD (RED → GREEN → REFACTOR, por tarea)

Orden de implementación respetando TDD dentro de cada tarea (tests primero):

1. `EnvioTrackingServiceTest` (nuevo, `@ExtendWith(MockitoExtension.class)`) — `crear` (orden guardar→evento inicial con `InOrder`) y `eliminar` (orden evidencias→eventos→envío, `@Transactional`).
2. `EnvioApiController` — `EnvioApiControllerTest` (`@WebMvcTest`) → impl controller + DTOs.
3. `EvidenciaDto.from` + migración de mapeos en `AdminApiController`/`ClienteApiController` — ajustar tests existentes de detalle (añadir `id`/`fechaSubida` en asserts).
4. `UploadService` sobrecarga + test ampliado.
5. Fix `getDocumentoUrl` + actualizar `api.test.js`, `ShipmentDetailPage.test.jsx`, `ImportBatchPage.test.jsx`.
6. `api.js` helpers + `api.test.js` → impl helpers.
7. `EnvioFormPage.jsx` + `.test.jsx` → impl componente.
8. Acciones en `AdminDashboard.jsx` + test; `ShipmentDetailPage.jsx` + test; `EvidenciasGrid.jsx` + test.
9. **Deprecación:** borrar `AdminController.java`, 9 templates `cms/*.html`, `fragments/admin-sidebar.html`, tests `AdminControllerTest`, `AdminThemeAssetsTest`, `AdminDocumentosPageIntegrationTest`; añadir redirect `/admin/**` → `/dashboard` con test; ajustar `SecurityConfigTest` (línea 144) si procede.
10. Docs: `ARQUITECTURA_INTERFACES.md` F6 → "Completa" (líneas 70, 80-81, 92, y reescribir §4), `handoff.md`, plan `docs/superpowers/plans/`.
11. Verificación final: `mvn clean test` (JDK 25) + `npx vitest run` + `npm run build`.

### Patrón de test de controladores (confirma con F4/F5)
- `@WebMvcTest(EnvioApiController.class)` / `@Import({GlobalExceptionHandler.class, SecurityConfig.class})`.
- `@WithMockUser(username="admin", roles="ADMIN")`; caso `@WithAnonymousUser` → 401.
- `@MockBean EnvioTrackingRepository / EventoTrackingRepository / EvidenciaEnvioRepository / ClienteRepository / EventoTrackingService / EnvioTrackingService / EvidenciaEnvioService / UploadService / DataSource / RBACAccessLogger / CustomAccessDeniedHandler`.
- `multipart` para upload de evidencia via `MockMvc`.
- `EnvioTrackingServiceTest`: mocks puros, `InOrder` para `eliminar` y `crear`; `verifyNoMoreInteractions` opcional.

## 8. Criterios de aceptación

- `mvn clean test` → BUILD SUCCESS, **354 base + ~15-18 nuevos − ~(tests borrados de AdminController/Theme/Documentos)** , 0 fallos, 0 errores (JDK 25). El delta exacto se confirma en ejecución (los 3 tests legacy borrados restan del total).
- `npx vitest run` (`frontend-react`) → **77 base + ~10-12 nuevos**, 0 fallos. `npm run build` → OK.
- `AdminController`, `templates/cms/*.html` y `fragments/admin-sidebar.html` **eliminados**; `/admin/**` autenticado → redirect a `/dashboard` (SPA).
- Ningún `console.log`/excepción silenciada nueva; los borrados de ficheros de evidencia usan `deleteIfExists` sin fallar.
- `docs/ARQUITECTURA_INTERFACES.md` F6 → "Completa"; `docs/handoff.md` actualizado; plan de implementación en `docs/superpowers/plans/`.
- Commits atómicos en `main`, sin push (regla AGENTS.md).

## 9. Deuda técnica / riesgos

- `defaultSuccessUrl("/react-dashboard/")` y `LoginController` mantienen `/react-dashboard/` (la SPA sirve en ambas raíces vía `ReactConfig`). Limpieza opcional: unificar en `/dashboard`; requiere tocar `SecurityConfigTest:144`, `LoginControllerTest:55`. Se decide durante el commit 9 sin ampliar el alcance de seguridad.
- `checkSession` de `api.js:92-101` usa `GET /admin/envios?page=0&size=1` como heartbeat de sesión; sigue válido (endpoint conservado).
- `UploadService` valida extensión por defecto de imágenes; evidencias pasan su propio set (`.pdf` incluido). Si mañana se permiten más formatos, se amplía en el servicio, no en el controlador.
- El redirect catch-all `/admin/**` enmascara cualquier ruta legacy olvidada (vuelve a la SPA). El frontend deja de generar URLs `/admin/*` tras el fix de `getDocumentoUrl`.

## 10. Commits atómicos (sobre `main`, sin push)

1. `fix: borrado transaccional de envíos con hijos y alta con evento inicial (EnvioTrackingService)`
2. `feat: API REST de CRUD de envíos y evidencias admin con tests`
3. `refactor: EvidenciaDto con id/fechaSubida y mapeo from() en APIs admin y cliente`
4. `fix: getDocumentoUrl apunta a /api/v1/admin/documentos con tests actualizados`
5. `feat: helpers de API para envíos y evidencias`
6. `feat: página de alta/edición de envíos en la SPA React`
7. `feat: acciones CRUD de envíos y panel de evidencias admin en la SPA React`
8. `refactor: depreca AdminController y templates cms/*.html con redirect a /dashboard`
9. `docs: roadmap F6 completa, convivencia y handoff`
