# Spec — F4: Reservas y mensajes de contacto en la SPA React

Fecha: 2026-08-10
Rama: `main`

## Contexto

- Fase F4 del roadmap (`docs/ARQUITECTURA_INTERFACES.md:90`): exponer en la SPA React (`frontend-react`) la gestión de reservas y los mensajes de contacto que el backend ya persiste.
- Parte backend ya existe y está verificada:
  - `ReservaApiController` (`/api/v1/admin/reservas`, Basic Auth admin): `GET` (filtro `estado`), `GET /{id}`, `PUT /{id}` (`ActualizarReservaRequest`), `PATCH /{id}/estado` (`ActualizarEstadoRequest`), `DELETE /{id}`. Devuelve `List<ReservaAdminDto>` / `ReservaAdminDto`.
  - `ReservaAdminDto`: `id`, `nombreCliente`, `email`, `telefono`, `fechaEntrada`, `fechaSalida`, `numeroHuespedes`, `comentarios`, `estado` (minúsculas), `createdAt`.
  - `MensajeContacto` (tabla `mensajes_contacto`, índice `idx_mensajes_fecha_envio`): `id`, `nombre`, `email`, `telefono`, `mensaje` (≤1000), `fechaEnvio` (`LocalDateTime`, columna `fecha_envio`), `leido` (bool, columna `leido`, default false). `MensajeContactoRepository` ya tiene `findAllByOrderByFechaEnvioDesc()` y `findTop5ByOrderByFechaEnvioDesc()`.
  - La única vista de mensajes hoy es Thymeleaf (`AdminController.mensajesRecibidos` → `cms/contactos`). No existe API REST admin de mensajes → **hay que añadir Java**.
  - `DataInitializer` siembra reservas en **minúsculas** (`pendiente` por constructor; `r2.setEstado("confirmada")`, `r3.setEstado("cancelada")`).
- **Bug de casing confirmado (rompe el filtro y las transiciones):**
  - `ReservaService.ESTADOS_VALIDOS` y `TRANSICIONES_PERMITIDAS` en MAYÚSCULAS (`ReservaService.java:20-26`).
  - `ReservaService.cambiarEstado` normaliza `nuevoEstado.trim().toUpperCase()` (`:114`) y guarda `r.setEstado(estadoNormalizado)` en mayúsculas (`:126`).
  - `ReservaApiController.listar` filtra contra `estado.trim().toUpperCase()` (`:42-44`).
  - El modelo guarda `pendiente` (`Reserva.java:56`), el repositorio consulta en minúsculas (`ReservaRepository.java:18,21,24`), las vistas Thymeleaf y `ReservaAdminDto` usan minúsculas.
  - Consecuencia: una reserva `pendiente` no matchea `?estado=PENDIENTE`; tras `cambiarEstado` a `APROBADA`, deja de matchear las consultas del repositorio y las vistas.
- Patrones SPA a reutilizar (F3): `useToast`, `EmptyState`, tablas `.envios-table`/`.table-section`, badges, `import-select`, `parseLocalDateTime` (`services/dateUtils.js`), mocks de axios devolviendo `{ data: ... }`, `vi.mock` de `services/api` y `NotificationContext`.

## Alcance (aprobado por usuario — Enfoque A)

1. **Fix de casing**: estandarizar estados de reserva a **minúsculas** en `ReservaService.cambiarEstado` y `ReservaApiController.listar`.
2. **Nueva API REST admin de mensajes**: `/api/v1/admin/mensajes` (listar con filtro `leido`, marcar leído, eliminar) con su service, DTOs y tests.
3. **`ReservasPage`** en `/dashboard/reservas`: tabla, filtro por estado, acciones de estado (aprobar/confirmar/cancelar), edición en modal, eliminar con confirmación.
4. **`MensajesPage`** en `/dashboard/mensajes`: tabla, filtro leído/no leído, marcar leído, eliminar.
5. **Routing y nav**: rutas protegidas en `App.jsx`, enlaces en `MainLayout.jsx`, helpers nuevos en `services/api.js`.

Fuera de F4: no se modifica el endpoint público de creación de reservas, ni `cms/contactos.html` Thymeleaf, ni la página pública de reservas (funcionan con minúsculas).

## Tareas

### T1 — Fix de casing de estados (backend, TDD)

- `ReservaService.cambiarEstado`: `String estadoNormalizado = nuevoEstado.trim().toLowerCase()`. `ESTADOS_VALIDOS` y `TRANSICIONES_PERMITIDAS` pasan a minúsculas (`"pendiente"`, `"aprobada"`, `"confirmada"`, `"cancelada"`). La transición compara `r.getEstado()` (minúsculas) contra el mapa en minúsculas → consistente.
- `ReservaApiController.listar`: `String estadoNormalizado = estado.trim().toLowerCase()`.
- Test TDD (`ReservaServiceTest`, ampliar):
  - `cambiarEstado_aceptaMinusculas`: reserva `pendiente` → `aprobada`, guarda `aprobada` (assert `r.getEstado()`).
  - `cambiarEstado_normalizaMayusculasEntrada`: entrada `"APROBADA"` se guarda como `"aprobada"`.
  - `cambiarEstado_estadoInvalido_lanzaBadRequest`.
  - `cambiarEstado_transicionIlegal_lanzaConflict` (p.ej. `confirmada` → `aprobada`).
- Sin migración V10: `DataInitializer` ya siembra en minúsculas y Flyway no define CHECK en `reservas.estado` (el CHECK vive solo en `data/schema.sql` legacy, no se usa con Flyway).

### T2 — API REST admin de mensajes (backend nuevo, TDD)

- **DTOs** (`com.monteastur.envios.dto.api`, Java puro, con `@Schema`):
  - `MensajeContactoAdminDto`: `id`, `nombre`, `email`, `telefono`, `mensaje`, `fechaEnvio` (`LocalDateTime`), `leido` (`boolean`). Getters/setters manuales.
  - `MarcarLeidoRequest`: `Boolean leido` (campo `estado`-style, getter/setter).
- **Service** (`MensajeContactoService`, nuevo, inyección por constructor):
  - `List<MensajeContacto> listar(Boolean leido)` → `repo.findAllByOrderByFechaEnvioDesc()`; si `leido != null`, filtra en memoria por `m.isLeido()`.
  - `Optional<MensajeContacto> marcarLeido(Long id, boolean leido)` → `repo.findById(id).map(m -> { m.setLeido(leido); return repo.save(m); })`.
  - `Optional<MensajeContacto> buscarPorId(Long id)` → `repo.findById(id)` (para el 404 del DELETE, patrón de `ReservaApiController.eliminar`).
  - `void eliminar(Long id)` → `repo.deleteById(id)`.
- **Controller** (`MensajeContactoApiController`, nuevo, `/api/v1/admin/mensajes`, patrón `ReservaApiController`: `@RestController`, `@Tag("Admin Mensajes")`, sin `@PreAuthorize` — la seguridad admin la aplica `SecurityConfig` por patrón de ruta; constructor):
  - `GET` → `ResponseEntity<List<MensajeContactoAdminDto>>`, param `@RequestParam(required = false) Boolean leido`, `@ApiResponses` 200.
  - `PATCH /{id}/leido` → body `@RequestBody MarcarLeidoRequest`, llama `marcarLeido(id, request.getLeido())`, `orElseThrow(ResourceNotFoundException("Mensaje no encontrado: " + id))`, 200 con DTO.
  - `DELETE /{id}` → `buscarPorId` + `orElseThrow(...404)`, `eliminar(id)`, `ResponseEntity.noContent().build()`.
  - `toDto(MensajeContacto)` privado.
- **Tests TDD**:
  - `MensajeContactoServiceTest` (Mockito, patrón `ReservaServiceTest`): listar sin filtro / con `leido=true` / con `leido=false`; marcarLeido ok (guarda `leido`); marcarLeido inexistente → empty; eliminar ok; eliminar inexistente no lanza (verifica).
  - `MensajeContactoApiControllerTest` (`@WebMvcTest`, patrón `ReservaPublicApiControllerTest`: `@Import({GlobalExceptionHandler.class, SecurityConfig.class})`, `@MockBean MensajeContactoService`, `DataSource`, `RBACAccessLogger`, `CustomAccessDeniedHandler`): GET listar 200 (con y sin filtro), PATCH leido 200, PATCH leido inexistente 404, DELETE 204, DELETE inexistente 404.

### T3 — Helpers de API (frontend, TDD)

- `services/api.js`, nuevos:
  - `getAdminReservas(estado)` → `GET /admin/reservas`, `params: { estado }` (solo si `estado` no vacío).
  - `getAdminReservaDetalle(id)` → `GET /admin/reservas/${id}`.
  - `putAdminReserva(id, body)` → `PUT /admin/reservas/${id}`.
  - `patchAdminReservaEstado(id, estado)` → `PATCH /admin/reservas/${id}/estado`, body `{ estado }`.
  - `deleteAdminReserva(id)` → `DELETE /admin/reservas/${id}`.
  - `getAdminMensajes(leido)` → `GET /admin/mensajes`, `params: { leido }` (solo si definido).
  - `patchAdminMensajeLeido(id, leido)` → `PATCH /admin/mensajes/${id}/leido`, body `{ leido }`.
  - `deleteAdminMensaje(id)` → `DELETE /admin/mensajes/${id}`.
- Tests en el patrón existente de `api.test.js` si existe (verificar en implementación; los mocks de axios devuelven `{ data: ... }`).

### T4 — `ReservasPage` (frontend nuevo, TDD)

- Ruta protegida `/dashboard/reservas` en `App.jsx` (patrón rutas `/dashboard/*` existentes).
- **Tabla** (`.envios-table`, `.table-section`): Cliente, Fechas (`fechaEntrada` → `fechaSalida`, `LocalDate` directas), Huéspedes, Email, Comentarios, Estado (badge), Creada (`parseLocalDateTime` + `toLocaleString`).
- **Filtro estado** (`import-select`): Todos / Pendiente / Aprobada / Confirmada / Cancelada → `getAdminReservas(estado)` con **minúsculas**.
- **Acciones por fila** (estilo `btn-nav-link`/F3):
  - Aprobar (si `estado === 'pendiente'`) → `patchAdminReservaEstado(id, 'aprobada')`.
  - Confirmar (si `'aprobada'`) → `'confirmada'`.
  - Cancelar (si `'pendiente'|'aprobada'|'confirmada'`) → `'cancelada'`.
  - Editar → modal con formulario (nombre, email, teléfono, fechas, huéspedes, comentarios) → `putAdminReserva(id, body)`.
  - Eliminar → `window.confirm` → `deleteAdminReserva(id)`.
- Badges: `pendiente→warning`, `aprobada→info`, `confirmada→success`, `cancelada→danger` (patrón `lote-badge--*`/StatusBadge).
- `useToast` éxito/error; `EmptyState` para vacío/carga; refresco tras mutación.
- **Tests** (`ReservasPage.test.jsx`, TDD, patrón `DocumentosPage.test.jsx`):
  1. Carga y renderiza reservas en la tabla.
  2. Cambio de filtro → `getAdminReservas('aprobada')`.
  3. Aprobar → PATCH + toast éxito + refresco.
  4. Cancelar/Confirmar análogos.
  5. Editar abre modal, guarda con PUT + toast.
  6. Eliminar con confirmación → DELETE.
  7. EmptyState y error de carga.

### T5 — `MensajesPage` (frontend nuevo, TDD)

- Ruta protegida `/dashboard/mensajes` en `App.jsx`.
- **Tabla**: Nombre, Email, Teléfono, Mensaje, Estado (badge Leído/No leído), Fecha (`fechaEnvio` vía `parseLocalDateTime`).
- **Filtro estado** (`import-select`): Todos / No leídos / Leídos → `getAdminMensajes(leido)` (`'false'`/`'true'`).
- **Acciones por fila**: Marcar leído (si `!leido`) → `patchAdminMensajeLeido(id, true)` + toast + refresco; Eliminar → `window.confirm` → `deleteAdminMensaje(id)`.
- Badges: `leido → success`, `!leido → warning`.
- **Tests** (`MensajesPage.test.jsx`, TDD):
  1. Carga y renderiza mensajes.
  2. Filtro leído/no leído → `getAdminMensajes(true/false)`.
  3. Marcar leído → PATCH + toast + refresco.
  4. Eliminar con confirmación → DELETE.
  5. EmptyState y error de carga.

### T6 — Routing, nav y estilos

- `App.jsx`: rutas `/dashboard/reservas` y `/dashboard/mensajes` protegidas.
- `MainLayout.jsx`: enlaces nav "Reservas" y "Mensajes" (`btn-nav-link`), junto a "Importar envíos" y "Documentos".
- `index.css`: badges de estado de reserva y de mensaje si no reutilizables (`lote-badge--*`), y ajustes menores del modal de edición (reutilizar modal de detalle si existe).

## Archivos afectados

| Tipo | Ruta |
|---|---|
| + | `src/main/java/com/monteastur/envios/service/MensajeContactoService.java` |
| + | `src/main/java/com/monteastur/envios/controller/api/MensajeContactoApiController.java` |
| + | `src/main/java/com/monteastur/envios/dto/api/MensajeContactoAdminDto.java` |
| + | `src/main/java/com/monteastur/envios/dto/api/MarcarLeidoRequest.java` |
| + | `src/test/java/com/monteastur/envios/service/MensajeContactoServiceTest.java` |
| + | `src/test/java/com/monteastur/envios/controller/api/MensajeContactoApiControllerTest.java` |
| ~ | `src/main/java/com/monteastur/envios/service/ReservaService.java` |
| ~ | `src/main/java/com/monteastur/envios/controller/api/ReservaApiController.java` |
| ~ | `src/test/java/com/monteastur/envios/service/ReservaServiceTest.java` |
| + | `frontend-react/src/pages/ReservasPage.jsx` |
| + | `frontend-react/src/pages/ReservasPage.test.jsx` |
| + | `frontend-react/src/pages/MensajesPage.jsx` |
| + | `frontend-react/src/pages/MensajesPage.test.jsx` |
| ~ | `frontend-react/src/services/api.js` |
| ~ | `frontend-react/src/App.jsx` |
| ~ | `frontend-react/src/layouts/MainLayout.jsx` |
| ~ | `frontend-react/src/index.css` (si hacen falta badges) |
| ~ | `docs/handoff.md` (entrada F4) |

## Decisiones

- **Estados de reserva siempre en minúsculas** en payloads y UI; el backend normaliza cualquier entrada (`toLowerCase()`). Alinea service, controller, repositorio, vistas y `ReservaAdminDto`.
- La API de mensajes es **nueva** (la vista Thymeleaf existente no se toca; la app React consume la REST).
- El filtro `leido` se hace en memoria en el service (datos pequeños), igual que el filtro de estado de reservas.
- `MarcarLeidoRequest` reutiliza el patrón de `ActualizarEstadoRequest` (sin `@NotNull` en el DTO; `null` se trata como `false` vía auto-unboxing en el service — verificar en tests para evitar NPE).
- Las fechas `LocalDate` de reservas se muestran directas (`2026-06-01`); `createdAt`/`fechaEnvio` se localizan con `parseLocalDateTime`.

## Verificación final

- Backend: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd clean test` → **BUILD SUCCESS** (317 tests existentes + nuevos de F4).
- Frontend: `npx vitest run` (workdir `frontend-react`) → suite en verde (36 + nuevos); `npm run build` → OK.
- `docs/handoff.md` actualizado con la entrada F4.
- Commits en `main` tras confirmación del usuario, sin push automático.
