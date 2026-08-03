# Diseño — Theme Switcher Claro/Oscuro Dinámico + Pulido Visual (Sprint DIOS)

Fecha: 2026-08-03
Repo: `Envios_Paraguay_CMS` (Spring Boot 3.3.5, Thymeleaf, JPA, MySQL 8, Redis)
Rama: `main` (commit base `fe14e4f`)

## 1. Objetivo

Añadir un selector de tema **Modo Oscuro (Obsidian, default) / Modo Claro (Pristine Quartz)** en todo el ecosistema
(público, tracking/cliente Tailwind y admin), con anti-FOUC, persistencia en `localStorage`, animaciones a 60 FPS y
cero regresiones (suite 219/219 → nueva suite verde). Incluye pulido visual de vistas clave y **dos páginas admin
nuevas funcionales** (`/admin/imports`, `/admin/documentos`) conectadas a los APIs existentes.

## 2. Decisiones tomadas (brainstorming)

| Decisión | Valor |
|---|---|
| Tema por defecto (sin preferencia guardada) | **Siempre Oscuro** (`theme = saved \|\| 'dark'`; NO seguir `prefers-color-scheme`) |
| Estrategia de tokens | **Enfoque A — Token Bridge**: `design-system.css` canónico + remap de vars existentes + overlay Tailwind |
| Atajo de teclado | **Ninguno** (Ctrl/Cmd+Shift+T es "reabrir pestaña" del navegador; no fiable) |
| Páginas `/admin/imports` y `/admin/documentos` | **Crear funcionales completas** conectando controllers/APIs existentes |
| Mensaje de commit | `feat(ui): add dynamic dark/light theme switcher with anti-FOUC and motion polish` |

## 3. Contexto real del repositorio (auditoría)

- **3 ecosistemas de tema distintos:**
  - **A — Marketing** (fragmentos `header`/`header-en`): `home`, `contacto`, `lacasa`, `reservas`, `operaciones`, `entorno`, legales, `en/*`. Usan `style.css` + CSS premium + `luxury-core.css` (tokens dark en `:root`, hardcodes `#09090b !important`, `#ffffff !important`).
  - **B — Tailwind CDN** (fragmento `public-head`): `tracking-search`, `tracking-result`, `tracking-404`, `cliente/panel`. Colores dark **hardcodeados en clases** (`bg-[#09090b]/80`, `text-zinc-400`, `border-white/10`, `bg-white/[0.03]`).
  - **C — Admin/Login** (`admin.css` → imports): `cms/*`, `login.html`, `cliente/login.html`. Tokens dark en `admin/admin-theme.css` (`--fondo`, `--texto`, `--texto-sec`, `--borde`, `--blanco-principal`, `--naranja`, glass…). Capa "DARK FORM FOCUS OVERRIDE" al final de `admin.css`.
- **Sin theme switcher ni script anti-FOUC existente** (solo `localStorage('cookies_aceptadas')`).
- **Rutas reales**: `/tracking/{codigo}` existe (`TrackingWebController:48`); `tracking-result.html` ya tiene timeline, viewer POD (`view.entrega.firmaBase64`) y evidencias. `/admin/imports` y `/admin/documentos` **NO existen** como páginas HTML.
- **Backends existentes para las páginas nuevas:**
  - `api/BatchImportController` (`/api/v1/admin/imports`): `POST /csv` (multipart + clienteId → 202 batch_id), `GET /{id}` (estado), `GET /{id}/errors`. Exento de CSRF (`/api/**` ignorado en `SecurityConfig:57`), protegido por sesión (`authenticated`).
  - `api/DocumentosController` (`/api/v1/admin/documentos`): `GET /envios/{codigo}/etiqueta` (PDF 100×150 inline), `GET /lotes/{batchId}/etiquetas`, `GET /lotes/{batchId}/manifiesto` (A4 attachment), `GET /` (auditoría `listarEmisiones`).
  - `service/batch/BatchImportPersistenceService`: **no tiene `listarLotes()`** → añadir método + test.
- **`mvnw` NO existe** → verificación con `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd clean test` y suite Docker (AGENTS.md).

## 4. Diseño — Sección 1: Sistema de tema

### 4.1 `design-system.css` (nuevo, canónico, cargado primero)

Tokens bajo `:root, :root[data-theme="dark"]` (Obsidian, default) y `:root[data-theme="light"]` (Pristine Quartz):

- Superficies: `--bg-body`, `--bg-surface`, `--bg-card`, `--bg-card-glass`
- Texto: `--text-primary`, `--text-secondary`, `--text-muted`
- Bordes/acento: `--border-subtle`, `--accent-color` (`#d4762a` ambos modos), `--accent-color-hover`, `--accent-glow`
- Semáforo: `--status-success` (dark `#4ade80` / light `#16a34a`), `--status-warning`, `--status-danger`, `--status-info`
- Glass: `--glass-bg`, `--glass-bg-hover`, `--glass-border`, `--glass-shadow`, `--glass-blur`
- Transiciones: `--transition-default`, `--transition-slow`
- Bloque global `@media (prefers-reduced-motion: reduce)` que desactiva animaciones/transiciones.

Carga: antes de `luxury-core.css` (ecosistema A), antes de `admin.css` (ecosistema C) y en `public-head` (ecosistema B).

### 4.2 Remap (Token Bridge)

- `luxury-core.css :root`: `--color-accent: var(--accent-color)`, `--color-accent-hover: var(--accent-color-hover)`, `--text-primary`, `--text-secondary`, `--text-muted` → canónicos; `--glass-*` → canónicos. `body { background: var(--bg-body) !important; color: var(--text-primary) !important }` (sustituir `#09090b`).
- `admin/admin-theme.css :root`: `--fondo: var(--bg-body)`, `--texto: var(--text-primary)`, `--texto-sec: var(--text-secondary)`, `--borde: var(--border-subtle)`, `--blanco-principal: var(--text-primary)`, `--blanco-secundario: var(--text-primary)`, `--gris-texto-legible: var(--text-secondary)`, `--naranja: var(--accent-color)`, `--naranja-hover: var(--accent-color-hover)`, glass → canónicos.
- Hardcodes residuales a vencer con capa `:root[data-theme="light"]` al final de `admin.css` (mismo patrón que la capa "DARK FORM FOCUS OVERRIDE") y ajustes en `luxury-core.css`/premium según auditoría del plan.

### 4.3 Anti-FOUC (inline en `<head>`, antes del body)

```html
<script>
(function() {
  try {
    var theme = localStorage.getItem('theme') || 'dark';
    document.documentElement.setAttribute('data-theme', theme);
  } catch (e) {
    document.documentElement.setAttribute('data-theme', 'dark');
  }
})();
</script>
```

Se añade a: `fragments/public-head.html`, `fragments/header.html`, `fragments/header-en.html`, y a cada `<head>` admin
(`login.html`, `cliente/login.html`, `cms/dashboard.html`, `cms/contactos.html`, `cms/textos.html`, `cms/reservas.html`,
`cms/tracking.html`, `cms/tracking-form.html`, `cms/imagenes.html`).

### 4.4 Toggle `.btn-theme-toggle`

- Markup: botón circular glass, icono Lucide `sun`/`moon`, `aria-label` y `title` dinámicos, `type="button"`.
- JS `/js/theme-toggle.js` (defer, tras lucide): lee `data-theme` actual, alterna, persiste `localStorage.setItem('theme', nuevo)`, recarga iconos Lucide y aplica clase `theme-rotating` (`transform: rotate(180deg)` + transición; solo transform/opacity).
- Ubicación: navbar `public-head` y `header`/`header-en`; sidebar admin (junto a logout/volver-web); login admin y cliente.
- Sin atajo de teclado.

### 4.5 Overlay Tailwind (ecosistema B)

Sustituir en `tracking-search`, `tracking-result`, `tracking-404`, `cliente/panel` las clases dark hardcodeadas por
valores arbitrarios con var: `bg-[color:var(--bg-body)]`, `bg-[color:var(--bg-card-glass)]`, `text-[color:var(--text-primary)]`,
`border-[color:var(--border-subtle)]`, etc. No se tocan atributos Thymeleaf (`${...}`, `th:*`).

## 5. Diseño — Sección 2: Páginas admin nuevas + pulido

### 5.1 `/admin/imports` — Carga Masiva CSV (nueva)

- `AdminController`: `GET /admin/imports` → `cms/imports.html` con model `clientes` (`clienteRepo.findAll()`) y `lotes`.
- `BatchImportPersistenceService.listarLotes()` (nuevo, usa `BatchImportRepository.findAllByOrderByIdDesc()`) + test.
- Dropzone drag&drop (fallback input file) → `fetch` `POST /api/v1/admin/imports/csv` (FormData + clienteId) → polling
  `GET /{id}` cada 2 s hasta estado terminal → modal errores por línea (`GET /{id}/errors`).
- Tabla historial de lotes: estado, contadores, barra de progreso, "ver errores".
- Enlace en `admin-sidebar.html`.

### 5.2 `/admin/documentos` — Etiquetas y Manifiestos (nueva, server-rendered)

- `AdminController`: `GET /admin/documentos` → `cms/documentos.html` con model `envios` recientes, `lotes` y `emisiones` (`documentoPdfService.listarEmisiones(null)`).
- Cards de acción: por envío → "Etiqueta térmica 100×150" (link al endpoint existente); por lote → "Etiquetas del lote" + "Manifiesto A4"; tabla de auditoría con filtro por tipo.
- Enlace en `admin-sidebar.html`.

### 5.3 Pulido `tracking-result` (público)

- Stepper con **pulso de luz** en el estado activo (keyframes solo `transform`/`opacity`).
- Cards glass adaptativas vía tokens (sustituir hardcodes Tailwind por vars).
- Refinamiento visual del visor POD y evidencias (existentes).

### 5.4 Pulido admin `dashboard`/`tracking`

- KPI/quick-action cards con hover flotante.
- Badges de estado redondeados (reusar `estado-*` existentes).
- Menú contextual de acciones por fila en `cms/tracking.html` (dropdown ligero JS).

### 5.5 Pulido logins (admin + cliente)

- Glow sutil con `--accent-glow`; focus adaptativo a ambos temas (reusar capa de foco existente + ajustes light).

### 5.6 Motion

- Transiciones en `.btn-luxury`, `.glass-card`, `.card`, `.quick-action-card`, `.dropzone`, dropdowns.
- Animaciones solo `transform`+`opacity` (60 fps); bloque `prefers-reduced-motion: reduce` global en `design-system.css`.

## 6. Diseño — Sección 3: Verificación, Git, Handoff

### 6.1 Tests (TDD)

- RED→GREEN por capa:
  - Render: vistas públicas y admin contienen script anti-FOUC y `.btn-theme-toggle`.
  - `AdminController`: `GET /admin/imports` y `GET /admin/documentos` → 200 + vista + atributos de modelo.
  - `BatchImportPersistenceService.listarLotes()`.
- Suite previa (219) intacta + nueva suite completa **BUILD SUCCESS** con `mvn.cmd clean test` y suite Docker (MySQL+Redis).

### 6.2 Redis/sesiones

- Cero cambios en auth/session (toggle 100% client-side). La suite Docker + `SecurityConfigTest` + spot-check
  login→dashboard validan `SESSION` y flujos Spring Security.

### 6.3 Git

- Staging de archivos intencionados; commit `feat(ui): add dynamic dark/light theme switcher with anti-FOUC and motion polish`; `git push origin main`; verificar run GitHub Actions.

### 6.4 Handoff

- Actualizar `docs/handoff.md`: feature del toggle, tokens/anti-FOUC, páginas nuevas, suite en verde, sincronización.

## 7. No-go / fuera de alcance

- No atajo de teclado para el toggle.
- No seguir `prefers-color-scheme` como default (siempre oscuro hasta guardar).
- No alterar atributos Thymeleaf ni rutas de controladores existentes.
- No refactor de sesiones/seguridad (solo consumo de APIs existentes).
