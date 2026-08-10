# F5 — Imágenes (galería) y textos legales — Design Spec

- **Fecha:** 2026-08-11
- **Fase:** F5 (ver `docs/ARQUITECTURA_INTERFACES.md:91` — "Imágenes (galería) y textos legales | Nueva página React sobre `AdminApiController`")
- **Estado del spec:** propuesto / aprobado en brainstorming
- **Stack:** Spring Boot 3.5+ (backend) + React 18 + Vite + Vitest (SPA); MySQL 8; **Java 25**; cero Lombok; inyección por constructor.
- **Decisión de controlador:** **Opción A** — controladores API dedicados (`ImagenApiController`, `TextoLegalApiController`), siguiendo la convención establecida en F4 (`ReservaApiController`, `MensajeContactoApiController`). El literal "sobre `AdminApiController`" del roadmap se interpreta como "sobre los controllers API correspondientes", por lo que se prefiere la cohesión por recurso sobre un monolito.

## 1. Contexto y alcance

La SPA React del panel de administración (`frontend-react`) sustituye progresivamente al Thymeleaf legacy (`AdminController` `/admin`). F4 completó reservas y contactos con controladores API dedicados por recurso. F5 continúa el mismo patrón para:

- **Galería de imágenes:** listado, subida, reorden, borrado. Imágenes servidas estáticamente por `/uploads/**` (sin cambios en el serving).
- **Textos legales:** listado, lectura por `slug`, edición de título + contenido.

### Fuera de alcance
- Serving público de `/uploads/**` (Thymeleaf + `app.upload.dir`), sin tocar.
- Nuevas migraciones: el esquema `imagenes` y `textos_legales` ya existen (Flyway V1). Ver `KNOWN_ISSUES_PREPROD.md:18` (columna `slug` vs `clave` — **NO** se modifica en F5; la entidad usa `slug`, que coincide con V1).
- Almacenamiento base64 o CDN: se guarda fichero en disco con nombre UUID y se almacena `url` relativa, igual que hoy.
- Galería pública / página de producto: Fuera de alcance F5 (roadmap futuro).

## 2. Decisiones de arquitectura

1. **Organización de controladores:** dedicados, por recurso y por responsabilidad. `AdminApiController` se mantiene enfocado en tracking + clientes.
2. **Upload de imágenes:** **Opción A de subidas** — un nuevo bean `UploadService` (probado unitariamente con `@TempDir`) que `ImagenApiController` consume por REST multipart. `AdminController` (legacy Thymeleaf) **NO** se refactoriza en F5 (evita tocar un controlador sin tests unitarios y protege el flujo `/admin/imagenes` en producción). La duplicación mínima (~4 líneas de UUID) se acepta; se anota como `TECHDEBT` para una futura limpieza.
   - Alternativas descartadas: B) refactorizar `AdminController` sobre `UploadService` (más limpio, pero mayor radio de explosión y sin tests de `AdminController`); C) base64 en JSON (33% de overhead, inadecuado para galería).
3. **DTOs:** en `com.monteastur.envios.dto.api`, POJOs puros (sin Lombok), getters/setters manuales, como F4 (`MensajeContactoAdminDto`, `MarcarLeidoRequest`).
4. **Seguridad:** `SecurityConfig` ya exime `/api/**` de CSRF (línea 66) y exige autenticación en `/api/v1/admin/**` (línea 35). Los controladores usan `@PreAuthorize("hasRole('ROLE_ADMIN')")`. Login de SPA es form-login (`fetch('/login')`), cookie `JSESSIONID` enviada con `withCredentials`.
5. **Manejo de errores:** reutiliza `GlobalExceptionHandler` existente — `ResourceNotFoundException`→404, `BadRequestException`→400. Subidas: archivo vacío / extensión no permitida → 400; texto no encontrado → 404.
6. **Slug canonical de textos legales:** `aviso-legal`, `politica-cookies` (coherente con `AdminController.textos` y datos demo). La colección `listar` devuelve `List<TextoLegalDto>` sin `contenido`.

## 3. Inventario de endpoints

### 3.1 Imágenes — `ImagenApiController` (`/api/v1/admin/imagenes`)

| Método | Ruta | Request | Respuesta | Códigos |
|---|---|---|---|---|
| GET | `/imagenes` | — | `List<ImagenDto>` (ordenado por `orden` asc) | 200 |
| POST | `/imagenes` | multipart: `titulo`(req), `descripcion`, `categoria`, `orden`, `archivo`(req, `MultipartFile`) | `ImagenDto` | 201 / 400 |
| PATCH | `/imagenes/{id}/orden` | `ActualizarOrdenImagenRequest{orden}` | `ImagenDto` | 200 / 404 |
| DELETE | `/imagenes/{id}` | — | — | 204 / 404 |

### 3.2 Textos legales — `TextoLegalApiController` (`/api/v1/admin/textos`)

| Método | Ruta | Request | Respuesta | Códigos |
|---|---|---|---|---|
| GET | `/textos` | — | `List<TextoLegalDto>` (sin `contenido`) | 200 |
| GET | `/textos/{slug}` | — | `TextoLegalDto` (con `contenido`) | 200 / 404 |
| PUT | `/textos/{slug}` | `ActualizarTextoRequest{titulo, contenido}` | `TextoLegalDto` | 200 / 404 |

## 4. DTOs

- `ImagenDto`: `id`, `titulo`, `descripcion`, `url`, `categoria`, `orden`, `createdAt`.
- `TextoLegalDto`: `id`, `slug`, `titulo`, `contenido`, `updatedAt`.
- `ActualizarOrdenImagenRequest`: `orden` (Integer).
- `ActualizarTextoRequest`: `titulo`, `contenido`.

Ningún DTO expone `secret_token`/credenciales (regla global de AGENTS.md §2). `contenido` solo se expone en el detalle por slug, no en el listado.

## 5. `UploadService` (nuevo bean)

Responsabilidad única: escritura/borrado de ficheros en `app.upload.dir`. Se inyecta `@Value("${app.upload.dir}")` por constructor.

- `String subirArchivo(MultipartFile archivo, String subDir)` → valida vacío + extensión (`jpg|jpeg|png|webp|gif|svg`), genera `UUID + ext`, escribe bytes bajo `uploadDir/subDir/` (subDir="" → raíz), devuelve la **ruta relativa** (`uuid.ext` o `subDir/uuid.ext`). El controlador arma `url = "/uploads/" + relPath`.
- `void eliminarArchivo(String pathRelativo)` → borra bajo `uploadDir` de forma `deleteIfExists` (no falla si no existe).

Imágenes se suben a la **raíz** de `app.upload.dir` → `url="/uploads/<uuid.ext>"`, coherente con el `AdminController.subirImagen` existente (línea 166) y con las imágenes demo servidas en `/uploads/<uuid>`. El serving `/uploads/**` ya funciona (verificado en `EVIDENCE_UPLOADS_AUDIT.md:158`). No se introduce subdirectorio por imágenes; las evidencias siguen `/uploads/evidencias/` (out-of-F5, sin tocar).

> Nota: `AdminController` sigue con su lógica inline (no se toca). TECHDEBT apuntado en `docs/handoff.md`.

## 6. Flujo de datos y UX (frontend)

- **`AdminImagesPage.jsx`** (ruta `/dashboard/imagenes`): formulario de alta (file picker + titulo/descripcion/categoria/orden) arriba; debajo, rejilla `.galeria-grid` de tarjetas `.imagen-card` con miniatura (`<img src={url}>`), chip de categoría, input numérico de orden (persistido on blur) y botón "Eliminar" con `confirm()`. Usa `useToast`, `EmptyState`.
- **`AdminLegalTextsPage.jsx`** (ruta `/dashboard/textos`): master/detail. Columna izquierda: lista de textos (`slug` + `titulo` + `updatedAt`). Al click, columna derecha: `<input>` de título + `<textarea>` de contenido + "Guardar" (PUT). Listado no incluye contenido.
- **`api.js`** +7 helpers (mirror del estilo F4): `getAdminImagenes`, `uploadAdminImagen` (FormData, sin headers explícitos), `patchAdminImagenOrden`, `deleteAdminImagen`, `getAdminTextos`, `getTextoLegal`, `putTextoLegal`. Todos sobre `baseURL='/api/v1'`.
- **`App.jsx`**: 2 `<Route>` bajo `MainLayout` + `ProtectedRoute`.
- **`MainLayout.jsx`**: 2 botones nav ("Imágenes", "Textos legales").
- **`index.css`**: `.galeria-grid`, `.imagen-card`, `.upload-form`, `.texto-editor` (scoped a módulos; reusan `.acciones-fila`/`.cell-*` de F4).

## 7. Estrategia TDD (RED → GREEN → REFACTOR, por tarea)

Orden de implementación respetando TDD dentro de cada tarea (tests primero):

1. `UploadService` — test unitario (`@TempDir`) → impl.
2. `ImagenApiController` — `ImagenApiControllerTest` (`@WebMvcTest`) → impl controller + DTOs.
3. `TextoLegalApiController` — `TextoLegalApiControllerTest` (`@WebMvcTest`) → impl controller + DTOs.
4. `api.js` helpers + `api.test.js` → impl helpers.
5. `AdminImagesPage.jsx` + `.test.jsx` → impl componente.
6. `AdminLegalTextsPage.jsx` + `.test.jsx` → impl componente.
7. Routing (`App.jsx`), nav (`MainLayout.jsx`), estilos (`index.css`), `handoff.md`.
8. Verificación final: `mvn clean test` (JDK 25) + `npx vitest run` + `npm run build`.

### Patrón de test de controladores (confirma con F4)
- `@WebMvcTest(ImagenApiController.class)` / `@Import({GlobalExceptionHandler.class, SecurityConfig.class})`.
- `@WithMockUser(username="admin", roles="ADMIN")`.
- `@MockBean ImagenRepository / TextoLegalRepository / UploadService / DataSource / RBACAccessLogger / CustomAccessDeniedHandler`.
- Caso `@WithAnonymousUser` → 401.
- `multipart` para upload vía `MockMvc` (`file(...)`).
- Frontend: axios mockeado como en F4 (`getAdminMensajes` → `{ data: ... }`); `useToast` no se dispara de forma real.

## 8. Criterios de aceptación

- `mvn clean test` → BUILD SUCCESS, **333 + ~20 = ~353** tests, 0 fallos, 0 errores (JDK 25).
- `npx vitest run` (`frontend-react`) → **77** tests, 0 fallos. `npm run build` → OK.
- 19 archivos de diff netos (backend + frontend + docs), commits atómicos en `main`, sin push.
- `docs/ARQUITECTURA_INTERFACES.md` F5 → "Completa"; `docs/handoff.md` actualizado; `docs/superpowers/plans/2026-08-11-f5-imagenes-textos-legales-spa.md` como hoja de ruta.

## 9. Deuda técnica / riesgos

- `node_modules/` aparece no ignorado en `.gitignore` raíz (observar tras F4 build). No afecta a F5.
- `AdminController` (Thymeleaf) mantiene lógica inline de upload; posible refacto futuro sobre `UploadService` (fuera de F5).
- `KNOWN_ISSUES_PREPROD.md:18` (`slug` vs `clave` en `textos_legales`) queda como está; la entidad y V1 usan `slug`, por lo que no hay conflicto runtime.

## 10. Commits atómicos (sobre `main`, sin push)

1. `refactor: extrae UploadService reutilizable para subida de archivos`
2. `feat: API REST de gestión de imágenes (galería) con tests`
3. `feat: API REST de textos legales con tests`
4. `feat: helpers de API para imágenes y textos legales`
5. `feat: página de gestión de imágenes en la SPA React`
6. `feat: página de gestión de textos legales en la SPA React`
7. `docs: estilos F5 y actualización de handoff`
