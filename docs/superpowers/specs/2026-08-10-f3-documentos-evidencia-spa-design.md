# Spec — F3: Gestión de documentos y evidencias en la SPA React

Fecha: 2026-08-10
Rama: `main`

## Contexto

- Fase F3: exponer en la SPA React (`frontend-react`) la generación de documentos PDF y la gestión de evidencias que el backend ya ofrece.
- **Cero cambios en Java**: los endpoints, DTOs y servicios ya existen y están verificados (suite backend 317 tests). El trabajo es 100% frontend React (TDD con vitest).
- Puntos de anclaje backend ya existentes:
  - `DocumentosController` (`/api/v1/admin/documentos`, `ROLE_ADMIN`):
    - `GET /envios/{codigo}/etiqueta` → PDF 100×150 mm, `Content-Disposition: inline`.
    - `GET /lotes/{batchId}/etiquetas` → PDF multipágina en streaming, `attachment`.
    - `GET /lotes/{batchId}/manifiesto` → PDF A4, `attachment`.
    - `GET ?tipo=` → `List<DocumentoGeneradoDto>` (id, tipo, referenciaId, nombreArchivo, pesoBytes, usuarioGeneracion, fechaCreacion).
  - `AdminApiController.toTrackingDto` → el detalle de envío ya expone `evidencias[]` con `{titulo, descripcion, tipo, urlArchivo, visibleCliente}` (ruta directa `/uploads/evidencias/...`).
  - `SecurityConfig`: CSRF ignorado en `/api/**`; `/uploads/**` es `permitAll` (descarga directa de evidencias sin auth adicional, misma origin).
- Patrones SPA a reutilizar: `usePolling`, `NotificationContext`, `EmptyState`, tablas `.envios-table`, `StatusBadge`, estilos dark en `index.css`.

## Alcance (aprobado por usuario)

1. **Descarga de evidencias** en `EvidenciasGrid` (detalle de envío).
2. **Generación de documentos PDF**:
   - Etiqueta térmica de un envío desde `ShipmentDetailPage`.
   - Etiquetas + manifiesto de un lote desde `ImportBatchPage` (card "Lote #N").
3. **Vista de auditoría** de emisiones: página nueva `/dashboard/documentos` con enlace en el navbar y filtro por tipo.

Queda **fuera** de F3: registro de entrega (POD) con firma (endpoint `POST /api/v1/deliveries/{codigo}/pod` no se toca en esta fase).

## Tareas

### T1 — Descarga de evidencias (`EvidenciasGrid.jsx`)
- Añadir botón de descarga (⬇) por tarjeta de evidencia.
- Implementación: `<a href={ev.urlArchivo} download rel="noopener noreferrer">` (misma origin, `/uploads/**` permitido).
- Conservar el modal de preview existente y el estado vacío.
- Test (`EvidenciasGrid.test.jsx`, TDD): renderiza tarjetas, clic en descarga abre un enlace con `href` correcto y `download`; estado vacío muestra mensaje; preview se abre/cierra.

### T2 — Helper de URLs de documentos (`services/api.js`)
- Nueva función `getDocumentoUrl(tipo, referencia)` que devuelve las URLs de los 3 endpoints de PDF:
  - `etiqueta` → `/admin/documentos/envios/{codigo}/etiqueta` (inline → abrir en pestaña nueva con `window.open`).
  - `etiquetas-lote` → `/admin/documentos/lotes/{batchId}/etiquetas` (attachment → descarga vía anchor).
  - `manifiesto` → `/admin/documentos/lotes/{batchId}/manifiesto` (attachment → descarga vía anchor).
- Nueva función `getAdminDocumentos(tipo)` → `GET /admin/documentos` con `params: { tipo }` (opcional).
- Test si procede (los tests existentes de `api.js` mockean axios; añadir caso si hay `api.test.js`).

### T3 — Botones de PDF en `ShipmentDetailPage.jsx`
- Botón "Etiqueta térmica PDF" junto a la barra superior (topbar), junto a `RefreshIndicator`.
- `window.open(getDocumentoUrl('etiqueta', codigo), '_blank')` (endpoint devuelve `inline`).
- Test (`ShipmentDetailPage.test.jsx` si no existe, o ampliar): el botón invoca `window.open` con la URL correcta.

### T4 — Botones de PDF en `ImportBatchPage.jsx`
- En el card "Lote #N" activo (`loteActivo`): dos botones "Etiquetas del lote" y "Manifiesto".
- Descarga vía anchor temporal (`<a href download>` click programático) para `attachment`.
- Habilitados solo cuando `loteActivo.id` existe; deshabilitados si el lote no tiene registros.
- Test (`ImportBatchPage.test.jsx`, ampliar): renderiza los botones cuando hay lote activo y la descarga usa la URL correcta.

### T5 — Vista de auditoría (`DocumentosPage.jsx`, nueva)
- Página `DocumentosPage` en ruta protegida `/dashboard/documentos`.
- Carga `getAdminDocumentos()` al montar (sin polling; refresco manual con `RefreshIndicator` opcional).
- Filtro por tipo (`select`): todos / `ETIQUETA_TERMICA` / `ETIQUETAS_LOTE` / `MANIFIESTO_CARGA` → re-llamada al API con `params.tipo`.
- Tabla reutilizando `.envios-table` + `EmptyState`:
  - Tipo (badge con clase `lote-badge--*` o nueva), referencia (código/lote), nombre archivo, peso (KB formateado), usuario, fecha localizada (`parseLocalDateTime`).
- Test (`DocumentosPage.test.jsx`, TDD): renderiza tabla con emisiones; filtro por tipo re-consulta con el parámetro correcto; estado vacío.
- `App.jsx`: ruta `/dashboard/documentos` protegida. `MainLayout.jsx`: enlace navbar "Documentos" (`btn-nav-link`).

### T6 — Estilos (`index.css`)
- Clases para: botones de descarga de evidencia (`evidencia-download`), botones PDF en topbar y lote (`btn-pdf`, `btn-importar--small` reutilizado si encaja), tabla de auditoría (badges por tipo, alineación numérica, columna peso), select de filtro (`import-select` reutilizado).

## Archivos afectados

| Tipo | Ruta |
|---|---|
| + | `frontend-react/src/pages/DocumentosPage.jsx` |
| + | `frontend-react/src/pages/DocumentosPage.test.jsx` |
| + | `frontend-react/src/components/EvidenciasGrid.test.jsx` |
| ~ | `frontend-react/src/components/EvidenciasGrid.jsx` |
| ~ | `frontend-react/src/services/api.js` |
| ~ | `frontend-react/src/pages/ShipmentDetailPage.jsx` |
| ~ | `frontend-react/src/pages/ImportBatchPage.jsx` |
| ~ | `frontend-react/src/App.jsx` |
| ~ | `frontend-react/src/layouts/MainLayout.jsx` |
| ~ | `frontend-react/src/index.css` |
| ~ | `docs/handoff.md` (entrada F3) |

## Decisiones

- Los PDF `attachment` (lote etiquetas/manifiesto) se descargan con un anchor temporal (click programático); el `inline` (etiqueta) se abre en pestaña nueva con `window.open` (el backend ya fija `Content-Disposition`).
- El filtro de auditoría usa el parámetro `tipo` del backend (no filtrado en cliente).
- Sin cambios en Spring Boot ni en la suite backend; verificación de regresión: `npm test` (frontend 21+ nuevos) y `mvn clean test` (backend 317, sin tocar).

## Verificación final

- `npm test` → suite frontend en verde (21 + nuevos tests de F3).
- `npm run build` → OK.
- `mvn clean test` → BUILD SUCCESS (regresión backend, sin cambios).
- `docs/handoff.md` actualizado con la entrada F3.
- Commit en `main` tras confirmación del usuario (sin push automático).
