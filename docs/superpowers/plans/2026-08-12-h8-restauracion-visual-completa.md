# H8 — Restauración Visual Completa (Pulido Post-e72def6)

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Tasks use checkbox (`- [ ]`) syntax for tracking. Each task is an independent TDD cycle (RED → GREEN → REFACTOR) and is sized to be a meaningful reviewer gate.

**Goal:** Restaurar los **234 selectores CSS ausentes** en `design-system.css` que el commit `e72def6` borró (353 selectores perdidos), afectando a **31 plantillas** (tracking público, zona cliente, login admin, páginas públicas ES/EN). Implementar con **Enfoque C Híbrido**: recuperar selectores de `e72def6^` y modernizarlos a **tokens actuales** (`--color-*`, `--glass-*`, `--radius-*`, `--spacing-*`, `--shadow-*`) sin hardcodear colores hex.

**Contexto:** El hallazgo H8 original (sidebar/nav-links del CMS) quedó obsoleto tras F6 (CMS eliminado). La regresión real es `e72def6` (2026-08-06) que reescribió masivamente el CSS eliminando la capa de componentes visuales. Plantillas actuales usan clases sin definir → páginas visualmente rotas.

**Arquitectura:** Un solo archivo `src/main/resources/static/css/design-system.css` — añadir sección comentada `/* ---- H8: Restauración visual post-e72def6 ---- */` al final. Agrupación incremental por dominio: **Tracking → Zona Cliente → Login Admin → Páginas Públicas**. Sin cambios en plantillas ni Java; solo CSS.

**Tech Stack:** Spring Boot 3.3.5, Java 25, Thymeleaf, `design-system.css` (tokens dark/light, glass, theme switcher). No Lombok. Tests: `DesignSystemCssTest`, `TemplateAssetIntegrityTest`, `PublicControllerTest`, suite completa backend/frontend.

---

## Global Constraints

- Java toolchain: `$env:JAVA_HOME="$env:USERPROFILE\.jdks\openjdk-25.0.2"`; Maven `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd`.
- Backend test: `mvn test` (full, excl. Docker `*IntegrationTest`) o `mvn test -Dtest=<Class>`. Debe terminar `BUILD SUCCESS`.
- Frontend test: `cd frontend-react; npx vitest run` (full). Todos en verde.
- Frontend build: `cd frontend-react; npm run build` → OK.
- Commits atómicos en `main`, **no push** salvo autorización explícita.
- **Regla de tokens**: cero colores hex en selectores nuevos; todo vía `var(--token)` para coherencia con theme switcher (dark/light). Tokens base ya existen en `design-system.css` (ver líneas 1-80: `--color-success`, `--color-warning`, `--color-danger`, `--color-info`, `--glass-bg`, `--glass-border`, `--radius-card`, `--radius-btn`, `--accent-color`, `--text-*`, `--bg-*`).

---

## File Structure (único archivo modificado)

```
src/main/resources/static/css/design-system.css  [MODIFY — + ~234 selectores en sección H8]
src/test/java/com/monteastur/envios/staticassets/DesignSystemCssTest.java  [MODIFY — + aserciones de selectores clave]
docs/handoff.md  [MODIFY — entrada H8]
```

---

## Global Test Targets (estimados)

| Layer | Target |
|---|---|
| backend | `DesignSystemCssTest` + selectores clave (`.status-badge`, `.tracking-result-page`, `.panel-stats`, `.login-wrapper`, `.brand-banner`, `.operations-hero`) → PASS |
| backend | Suite completa `mvn clean test "-Dtest=!*IntegrationTest"` → **BUILD SUCCESS**, ~344 tests, 0 failures |
| frontend | `npx vitest run` → **120 tests**, 0 failures (sin regresiones) |
| frontend | `npm run build` → OK |

---

## Task 1: Mapeo de selectores (legacy e72def6^ → tokens modernos)

**Objetivo:** Extraer los 353 selectores perdidos de `e72def6^`, filtrar los **234 que usan plantillas actuales** (ver tabla abajo), y mapear cada uno a tokens modernos. Generar tabla de equivalencias antes de codificar.

**Fuentes:**
- `git show e72def6^:src/main/resources/static/css/design-system.css` — CSS previo a la regresión.
- Escaneo de plantillas actuales (31 archivos, 234 clases únicas ausentes) — ya realizado en brainstorming.

**Agrupación de selectores por dominio (para implementación incremental):**

| Grupo | Plantillas | Selectores representativos (subconjunto) | Tokens a usar |
|---|---|---|---|
| **Tracking público** | tracking-search, tracking-result, tracking-404 (ES/EN) | `.tracking-hero`, `.tracking-form-glass`, `.tracking-help-card`, `.tracking-result-page`, `.tracking-result-header`, `.tracking-result-head`, `.tracking-code`, `.tracking-code-label`, `.tracking-last-update`, `.tracking-last-update-value`, `.tracking-grid`, `.tracking-info-card`, `.tracking-step`, `.tracking-step-icon`, `.tracking-event`, `.tracking-event-meta`, `.tracking-event-time`, `.tracking-event-toggle`, `.tracking-event-details`, `.tracking-event-icon`, `.tracking-event-title`, `.tracking-event-date`, `.tracking-event-location`, `.tracking-event-chevron`, `.tracking-timeline`, `.tracking-history-list`, `.tracking-empty`, `.tracking-back-link`, `.tracking-pod-*`, `.tracking-evidencia-*`, `.status-badge` + 6 variantes (--recibido, --en_aduan_orig, --en_transito, --en_aduan_dest, --en_reparto, --entregado, --default), `.tracking-search-form`, `.tracking-result-link`, `.tracking-step-card`, `.tracking-step-label`, `.tracking-step-text`, `.tracking-step-title`, `.tracking-step-icon`, `.tracking-steps-grid`, `.tracking-404-*`, `.tracking-pill`, `.tracking-help-*` | `--color-success/warning/danger/info`, `--glass-bg/border`, `--radius-card`, `--radius-btn`, `--spacing-sm/md/lg`, `--shadow-card`, `--text-muted`, `--accent-color` |
| **Zona cliente** | cliente/panel, cliente/login | `.panel-toolbar`, `.panel-greeting`, `.panel-logout`, `.panel-stats`, `.stat-card`, `.stat-label`, `.stat-value`, `.stat-value--accent`, `.stat-value--success`, `.panel-table`, `.panel-table-wrap`, `.panel-table-card`, `.panel-table-code`, `.panel-table-empty`, `.panel-table-actions`, `.panel-table-action`, `.panel-table-action--primary`, `.panel-table-action--secondary`, `.page-title`, `.public-container`, `.login-wrapper`, `.login-header`, `.login-body`, `.login-field`, `.login-footer`, `.toast`, `.toast-error`, `.btn-logout` (distinto del SPA) | `--color-accent`, `--glass-bg`, `--radius-btn`, `--shadow-card`, `--color-success/warning/danger`, `--text-primary`, `--text-secondary`, `--bg-card`, `--bg-surface` |
| **Login admin** | login.html | `.login-wrapper`, `.login-header`, `.login-body`, `.login-field`, `.login-footer`, `.toast`, `.toast-error` | mismo que zona cliente |
| **Páginas públicas** | home, contacto, reservas, operaciones, lacasa, entorno, error, aviso-legal, politica-cookies (ES/EN) | `.brand-banner`, `.brand-banner-bg`, `.brand-banner-content`, `.brand-banner-overlay`, `.como-funciona`, `.como-funciona-cta`, `.como-funciona-grid`, `.destacados`, `.final-cta-actions`, `.final-cta-eyebrow`, `.final-cta-premium`, `.home-phone`, `.paso-numero`, `.tracking-badge`, `.tracking-badge-done`, `.tracking-cta`, `.tracking-dot`, `.tracking-icon`, `.tracking-info`, `.tracking-label`, `.tracking-section`, `.tracking-step`, `.tracking-timeline`, `.tracking-title`, `.operation-card`, `.operation-icon`, `.operations-btn`, `.operations-cta-row`, `.operations-eyebrow`, `.operations-final`, `.operations-gallery`, `.operations-gallery-grid`, `.operations-gallery-header`, `.operations-grid`, `.operations-hero`, `.operations-page`, `.operations-timeline`, `.gallery-card`, `.gallery-visual`, `.lightbox`, `.lightbox-cerrar`, `.lightbox-contenido`, `.lightbox-nav`, `.lightbox-nav-next`, `.lightbox-nav-prev`, `.lightbox-titulo`, `.calendario-*`, `.dia-*`, `.leyenda`, `.reserva-calendario`, `.reservas-metric-card`, `.error-card`, `.error-code`, `.error-message`, `.error-title`, `.contenido`, `.contenido-legal`, `.pagina-titulo`, `.page-title`, `.page-subtitle`, `.section-title`, `.public-container`, `.mt-4`, `.mt-6`, `.mt-12`, `.font-bold`, `.hidden`, `.alert`, `.alert-error`, `.modal`, `.modal-content`, `.modal-header`, `.secondary`, `.grid-public`, `.qr-modal`, `.qr-modal-header`, `.tracking-form-glass`, `.tracking-help-card`, `.tracking-hero`, `.tracking-search-form`, `.tracking-result-link` | tokens existentes + definir `--spacing-*` (`--spacing-xs/sm/md/lg/xl`) si faltan; `--glass-*`, `--radius-*`, `--shadow-*`, `--color-*` |

**Entregable de Task 1:** Archivo `docs/superpowers/specs/2026-08-12-h8-mapeo-selectores.md` con tabla completa `selector_legacy → selector_moderno + tokens_usados` para los 234 selectores. Revisión humana antes de Task 2.

- [ ] **Step 1:** Extraer selectores de `e72def6^` (script o manual) y filtrar contra las 234 clases usadas.
- [ ] **Step 2:** Para cada selector, decidir token moderno (ej: `background: #1B4D3B` → `background: var(--bg-card-green)`; `color: #E67E22` → `color: var(--accent-color)`; `border-radius: 16px` → `border-radius: var(--radius-card)`).
- [ ] **Step 3:** Documentar mapeo en spec. **Gate:** usuario aprueba tabla antes de Task 2.

---

## Task 2: Implementación incremental en design-system.css

**Objetivo:** Añadir los 234 selectores mapeados a `design-system.css` en una sección comentada `/* ---- H8: Restauración visual post-e72def6 ---- */`, agrupados por dominio y en orden de dependencia visual (Tracking → Cliente → Login → Públicas). Cada grupo = commit atómico.

**Convenciones de código:**
- Usar `var(--token)` exclusivamente. Si falta un token (ej. `--spacing-md`), definirlo en la sección de tokens (líneas 1-80) antes de usarlo.
- Agrupar selectores relacionados (BEM) bajo un bloque comentado: `/* .tracking-result-page */`.
- Mantener especificidad baja (una clase, no selectores anidados profundos).
- Compatibilidad dark/light: tokens ya cambian con `data-theme` — no definir media queries nuevas salvo necesidad comprobada.

**Grupos y commits:**

- [ ] **Grupo 1 — Tracking público** (6 plantillas ES/EN, ~60 selectores): `.tracking-*`, `.status-badge*`, `.tracking-404-*`, `.tracking-search-form`, `.tracking-pill`, `.tracking-help-*`.
  - Commit: `feat(css): H8 grupo 1 — estilos tracking público (status-badge, timeline, result, search, 404)`
- [ ] **Grupo 2 — Zona cliente** (2 plantillas, ~40 selectores): `.panel-*`, `.stat-*`, `.login-*`, `.toast*`, `.btn-logout`, `.public-container`, `.page-title`.
  - Commit: `feat(css): H8 grupo 2 — estilos zona cliente (panel, login, stats, tabla, toast)`
- [ ] **Grupo 3 — Login admin** (1 plantilla, ~7 selectores): `.login-*`, `.toast*` (reutiliza tokens de Grupo 2).
  - Commit: `feat(css): H8 grupo 3 — estilos login admin`
- [ ] **Grupo 4 — Páginas públicas** (22 plantillas ES/EN, ~127 selectores): `.brand-banner*`, `.operation-*`, `.operations-*`, `.gallery-*`, `.lightbox*`, `.calendario-*`, `.dia-*`, `.leyenda`, `.reserva-*`, `.error-*`, `.contenido*`, `.pagina-titulo`, `.page-*`, `.section-title`, `.mt-*`, `.font-bold`, `.hidden`, `.alert*`, `.modal*`, `.secondary`, `.qr-modal*`, `.grid-public`, `.tracking-form-glass`, `.tracking-help-card`, `.tracking-hero`, `.tracking-search-form`, `.tracking-result-link`.
  - Commit: `feat(css): H8 grupo 4 — estilos páginas públicas (home, contacto, reservas, operaciones, lacasa, entorno, legales, error)`

**Verificación por grupo:**
- [ ] `mvn test -Dtest=DesignSystemCssTest` → PASS (ver Task 3 para aserciones).
- [ ] `mvn clean test "-Dtest=!*IntegrationTest"` → BUILD SUCCESS.
- [ ] Visual smoke manual: abrir plantillas del grupo en navegador (dark mode default), verificar que no hay elementos sin estilo obvio.

---

## Task 3: Verificación de integridad en plantillas y tests

**Objetivo:** Asegurar que todos los 234 selectores están definidos, usan tokens, y no hay regresiones en tests.

- [x] **A1 — DesignSystemCssTest extendido:** Añadir aserciones de presencia para selectores canónicos por grupo:
  ```java
  @Test void h8_definesTrackingSelectors() { assertThat(css()).contains(".status-badge", ".tracking-result-page", ".tracking-step", ".tracking-event"); }
  @Test void h8_definesClienteSelectors() { assertThat(css()).contains(".panel-stats", ".stat-card", ".login-wrapper", ".toast"); }
  @Test void h8_definesPublicSelectors() { assertThat(css()).contains(".brand-banner", ".operation-card", ".operations-hero", ".lightbox"); }
  @Test void h8_noHardcodedHexInNewSection() { // regex que busca #[0-9a-fA-F]{3,8} en la sección H8 → falla si encuentra }
  ```
- [x] **A2 — TemplateAssetIntegrityTest:** Verifica que `design-system.css` se sirve y las plantillas cargan sin error 500.
- [x] **A3 — PublicControllerTest:** Endpoints públicos (`/`, `/casa`, `/contacto`, `/reservas`, `/operaciones`, `/tracking`, `/tracking/{codigo}`, `/cliente/panel`, `/cliente/login`, `/login`) devuelven 200 y contienen link a CSS.
- [x] **A4 — Suite completa backend:** `mvn clean test "-Dtest=!*IntegrationTest"` → **BUILD SUCCESS**, ~349 tests, 0 failures.
- [x] **A5 — Suite completa frontend:** `cd frontend-react && npx vitest run` → **120 tests**, 0 failures; `npm run build` → OK.
- [ ] **A6 — Smoke visual manual:** Abrir en navegador (dark mode): `/tracking` (búsqueda), `/tracking/MT-2026-0001` (resultado), `/tracking/inexistente` (404), `/cliente/panel`, `/cliente/login`, `/login`, `/`, `/casa`, `/contacto`, `/reservas`, `/operaciones`, `/entorno`, `/aviso-legal`, `/politica-cookies`. Verificar que elementos clave tienen estilos (badges, cards, formularios, tablas, botones, modales, lightbox).

---

## Task 4: Docs + Handoff + Closing Gate

- [x] **D1 — handoff.md:** Añadir entrada H8 (formato F6/F7) con commits, verificación final, totales de tests.
- [x] **D2 — ARQUITECTURA_INTERFACES.md** (opcional): Nota en §5 "Observaciones de pulido visual" → "H8 completado: 234 selectores restaurados con tokens modernos".
- [x] **Closing Gate:**
  - [x] `mvn clean test "-Dtest=!*IntegrationTest"` → BUILD SUCCESS, 349 tests.
  - [x] `cd frontend-react && npx vitest run` → 120 tests, 0 failures.
  - [x] `cd frontend-react && npm run build` → OK.
  - [ ] `git log --oneline <base>..HEAD` → 4 commits atómicos H8 + 1 docs. *(4/4 commits de código presentes: `37e6a78`, `b0c504f`, `4fcac6d`, `5b8748b`; falta el commit de docs de cierre.)*
  - [ ] `git status` limpio (solo `node_modules/` pre-existente). *(pendiente: `docs/handoff.md` y `docs/ARQUITECTURA_INTERFACES.md` modificados sin commitear.)*
  - [x] **No push** sin autorización.

---

## TDD Rhythm Notes

- Task 1 es **investigación/documentación** (no código) — gate humano en la tabla de mapeo.
- Tasks 2 (4 sub-tasks/grupos) son ciclos TDD ligeros: RED = test `DesignSystemCssTest` falla por selector ausente; GREEN = añadir selector con tokens; REFACTOR = agrupar, comentar, verificar visual.
- Tests de verificación (Task 3) se escriben **antes** de cada grupo (RED) y pasan tras el commit (GREEN).
- Scope cerrado: **solo los 234 selectores mapeados**. No añadir clases que no usan plantillas actuales.
- Tokens: si falta uno, definir en sección `:root` **antes** de usarlo (commit aparte o en el mismo grupo).
- No tocar plantillas ni Java — riesgo cero de regresión funcional.

---

## Riesgos y mitigaciones

| Riesgo | Mitigación |
|---|---|
| Mapeo legacy→token impreciso → visual distinto al original | Tabla de mapeo revisada por humano (Task 1 gate); smoke visual por grupo. |
| Falta token para caso edge (ej. gradiente específico) | Definir token nuevo en `:root` con nombre semántico; documentar en mapeo. |
| Conflicto con theme switcher (light mode roto) | Usar solo tokens `--color-*`/`--bg-*`/`--text-*` que ya cambian con `data-theme`; probar light mode en smoke. |
| Scope creep (añadir selectores "por si acaso") | Lista cerrada de 234 del escaneo; no añadir extras. |
| Regresión en `DesignSystemCssTest` existente | Añadir aserciones, no modificar existentes; ejecutar test tras cada grupo. |