# Plan — Entrega B2: Admin CMS + Logins Dark Premium

Fecha: 2026-08-03 · Rama: `main` (commits directos como en Entrega A) · Suite: 217 tests

## Objetivo

Re-tematizar el panel administrativo (`/admin/*`), el panel de cliente (`/cliente/*`) y los dos
logins (`/login` admin y `/cliente/login`) a **dark premium** estilo Linear/Stripe/Vercel con la
marca `#d4762a`, preservando **byte a byte** las expresiones Thymeleaf, `name`/`id`, handlers JS,
view names y los **217 tests**.

## Enlace

- Spec: `docs/superpowers/specs/2026-08-03-rediseno-ui-linear-stripe-vercel-design.md`
- Plan B1 (marketing): `docs/superpowers/plans/2026-08-03-entrega-b1-rediseno-ui-marketing.md`
- Inventario `css/admin/`: reporte del subagente `explore` (10 archivos, 3 vacíos: `admin-base.css`,
  `admin-components.css`, `admin-dashboard.css`)

## Arquitectura de re-tema (verificada en el código)

- `cms/*`, `cliente/*` y los logins cargan **SOLO** `/css/admin.css` (verificado en
  `cms/dashboard.html` y en `login.html`/`cliente/login.html`). NO cargan `luxury-core.css` → la
  capa override debe vivir en `admin.css`.
- `admin.css` @importa 10 hojas `css/admin/*` en orden (theme → base → sidebar → login → dashboard →
  tracking → components → evidencias → client-panel → responsive). `admin-theme.css` define las vars.
- `admin.css` ya es mayoritariamente oscuro (body con gradientes verde oscuro, `.card`/`.stat-item`/
  `.quick-action-card` glass). **Superficies claras residuales a voltear:**
  - `--fondo: #ece9e2`, `--texto: #222`, `--texto-sec: #555`, `--borde: #ddd` (`admin-theme.css`)
    → el texto global `body { color: var(--texto) }` renderiza **#222 sobre fondo oscuro** hoy.
  - Inputs/selects/textarea: `background: rgba(255,255,255,0.7)` (claro), focus `#ffffff`
    (`admin.css` líneas 94-107, 234-247, 499-527).
  - `.login-wrapper`: `rgba(255,255,255,0.88)` (claro) en `admin.css` y `admin-login.css`.
  - `.error-msg`/`.logout-msg`/`.toast-*`: fondos rojo/verde pálidos.
  - `.estado-pendiente`/`.estado-aprobada`/`.estado-cancelada`: fondos pálidos en
    `admin-tracking.css`.
  - `.vacio`: fondo claro.
- **Vars faltantes** (usadas por `admin-tracking.css` pero no definidas): `--verde-confirmada`
  (línea 114, texto de `.estado-aprobada`), `--rojo` (línea 125, texto de `.estado-cancelada`),
  `--blanco` (línea 246, borde del punto de timeline).
- **Gap de logins:** `login.html` y `cliente/login.html` usan `.glass-card`, `.btn-luxury`,
  `.focus-glow`, `.luxury-icon*` que **NO existen** en `admin.css` ni `css/admin/*` (verificado por
  grep) → hoy los botones renderizan como browser-default. La capa override debe definirlos.
  - `login.html` (admin): campos `name="username"` + `name="password"`.
  - `cliente/login.html`: campo `name="email"` + `name="password"` (login exitoso confirmado con
    email/password).
- **Sidebar**: `admin-sidebar.css` ya es oscuro (gradiente verde profundo, acentos naranja, fondos
  translúcidos) → **NO requiere cambios de template**. Solo se refina tipografía en la capa override.
- La franja cultural (`html::before/::after` en `admin.css`) se conserva (identidad).

## Constraints globales

- Regla de oro: NO se alteran expresiones Thymeleaf, `name`/`id`, handlers JS, view names.
- Solo cambian: `css/admin/admin-theme.css` (flip de vars) y `css/admin.css` (font @import +
  capa override final con `!important`).
- Verificación: los 217 tests como **regresión** (no hay TDD rojo-verde para CSS puro) + spot-check
  autenticado en el gate.

## Resumen de tareas

| # | Tarea | Archivos | Commit sugerido |
|---|-------|----------|-----------------|
| A1 | Flip dark de vars + vars faltantes | `css/admin/admin-theme.css` | `style(ui): admin-theme variables dark y vars faltantes` |
| A2 | Font Jakarta + capa dark re-skin | `css/admin.css` | `style(ui): admin.css dark re-skin - Jakarta Sans, logins luxury y overrides` |
| A3 | Gate Entrega B2 | — | `style(ui): ajustes Entrega B2` (si procede) |

---

## Task A1 — `admin-theme.css`: flip dark + vars faltantes

### Pasos

1. **Baseline:** `.\mvnw.cmd test "-Dtest=SecurityConfigTest"` → BUILD SUCCESS.
2. Reescribir `src/main/resources/static/css/admin/admin-theme.css` con el contenido COMPLETO de abajo.
3. **Regresión:** repetir el comando del paso 1 → BUILD SUCCESS.
4. Commit: `style(ui): admin-theme variables dark y vars faltantes`.

### Contenido completo de `admin-theme.css`

```css
:root {
    /* Identidad */
    --verde: #3f6338;
    --verde-oscuro: #2d4a28;
    --verde-gradient: linear-gradient(135deg, #3f6338, #557c45);
    --naranja: #d4762a;
    --naranja-hover: #e8893a;

    /* Textos claros sobre fondo oscuro */
    --blanco-principal: #f5f5f0;
    --blanco-secundario: #e8e8e2;
    --gris-texto-legible: #c8c8c0;

    /* Semáforo (legible sobre dark) */
    --verde-exito: #4caf82;
    --verde-aprobada: #2ecc71;
    --verde-confirmada: #4ade80;
    --naranja-advertencia: #f0a830;
    --rojo-error: #e05555;
    --rojo: #f87171;

    /* Superficies DARK (antes claras) */
    --fondo: #09090b;
    --texto: #f4f4f5;
    --texto-sec: #a1a1aa;
    --borde: rgba(255, 255, 255, 0.1);
    --sombra: 0 8px 25px rgba(0, 0, 0, 0.5);

    /* Glass (dark) */
    --glass-bg: rgba(255, 255, 255, 0.03);
    --glass-border: rgba(255, 255, 255, 0.1);
    --glass-shadow: 0 12px 36px rgba(0, 0, 0, 0.5);

    /* Texto en inputs / labels (ahora claro para legibilidad en dark) */
    --oscuro-texto-fondo-claro: #f4f4f5;
    --oscuro-texto-fondo-claro-sec: #d4d4d8;

    --texto-secundario: #cfd6c6;
    --texto-descriptivo: #f5f5f0;

    /* Borde del punto de timeline (fondo dark) */
    --blanco: #09090b;
}
```

### Notas de verificación (A1)

- `--texto`, `--texto-sec`, `--borde`, `--fondo`, `--sombra`, `--oscuro-texto-fondo-claro*` pasan a
  valores claros/oscuros coherentes; `--blanco-principal`/`--blanco-secundario`/`--gris-texto-legible`
  se mantienen.
- Se añaden `--verde-confirmada`, `--rojo`, `--blanco` (faltaban y `admin-tracking.css` los usa).

---

## Task A2 — `admin.css`: font @import + capa dark re-skin final

### Pasos

1. Añadir en la cabecera de `src/main/resources/static/css/admin.css`, **antes del primer `@import`
   de `css/admin/*`** (regla CSS: los `@import` deben preceder a cualquier otra regla):
   ```css
   @import url('https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap');
   ```
2. **Append al FINAL de `admin.css`** la capa "DARK RE-SKIN OVERRIDE" de abajo (con `!important`
   para vencer las reglas previas que también lo usan).
3. **Regresión:** `.\mvnw.cmd test "-Dtest=SecurityConfigTest"` → BUILD SUCCESS.
4. Commit: `style(ui): admin.css dark re-skin - Jakarta Sans, logins luxury y overrides`.

### Contenido a appendar en `admin.css`

```css
/* =============================================
   DARK RE-SKIN OVERRIDE — Entrega B (Admin/CMS)
   Al final de admin.css vence por orden de carga;
   con !important vence a reglas previas que usan
   el mismo !important. Define además las clases
   luxury usadas por los logins (antes inexistentes).
   ============================================= */

/* Tipografía global */
body, .card, .sidebar, .login-wrapper, input, select, textarea, button {
    font-family: 'Plus Jakarta Sans', 'Inter', system-ui, sans-serif !important;
}

/* ---- Login wrapper: claro → dark glass ---- */
.login-wrapper {
    background: rgba(18, 18, 21, 0.9) !important;
    border: 1px solid var(--glass-border) !important;
    box-shadow: 0 24px 80px rgba(0, 0, 0, 0.5), 0 0 0 1px rgba(255, 255, 255, 0.05) !important;
}
.login-wrapper h1, .login-header h1 {
    color: var(--naranja) !important;
}
.login-wrapper .subtitle,
.login-header p {
    color: var(--gris-texto-legible) !important;
}

/* ---- Clases luxury para logins (antes inexistentes) ---- */
.glass-card {
    border-radius: 16px;
    backdrop-filter: blur(16px);
    -webkit-backdrop-filter: blur(16px);
}

.btn-luxury {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 12px 28px;
    width: 100%;
    background: linear-gradient(135deg, var(--naranja), var(--naranja-hover)) !important;
    color: #ffffff !important;
    border: none;
    border-radius: 10px;
    font-weight: 700;
    font-size: 0.95rem;
    letter-spacing: 0.04em;
    cursor: pointer;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    box-shadow: 0 4px 16px rgba(212, 118, 42, 0.25) !important;
}
.btn-luxury:hover {
    background: linear-gradient(135deg, var(--naranja-hover), var(--naranja)) !important;
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(212, 118, 42, 0.4) !important;
}

.luxury-icon {
    stroke-width: 1.5px;
    stroke-linecap: round;
    stroke-linejoin: round;
    width: 24px;
    height: 24px;
    color: var(--naranja);
    vertical-align: middle;
    display: inline-block;
}
.luxury-icon-sm { width: 18px; height: 18px; }
.luxury-icon-lg { width: 32px; height: 32px; }

.focus-glow:focus {
    outline: none;
    border-color: var(--naranja) !important;
    box-shadow: 0 0 0 3px rgba(212, 118, 42, 0.25) !important;
}

/* ---- Inputs / selects / textareas: claro → dark ---- */
input, textarea, select,
.form-group input, .login-field input, .legal-editor textarea,
.evidencia-input, .evidencia-select, .evidencia-file {
    background: rgba(255, 255, 255, 0.05) !important;
    border: 1px solid rgba(255, 255, 255, 0.12) !important;
    color: var(--blanco-principal) !important;
}
input:focus, textarea:focus, select:focus,
.form-group input:focus, .login-field input:focus, .legal-editor textarea:focus {
    background: rgba(255, 255, 255, 0.08) !important;
    border-color: var(--naranja) !important;
    box-shadow: 0 0 0 3px rgba(212, 118, 42, 0.18) !important;
}
input::placeholder, textarea::placeholder, select::placeholder {
    color: var(--gris-texto-legible) !important;
    opacity: 0.7;
}

/* ---- Labels ---- */
.form-group label, .login-field label, .legal-editor label {
    color: var(--blanco-principal) !important;
}

/* ---- Mensajes error / éxito / toasts ---- */
.error-msg, .toast-error {
    background: rgba(224, 85, 85, 0.12) !important;
    border-left: 3px solid var(--rojo-error) !important;
    color: #fecaca !important;
}
.logout-msg, .toast-success {
    background: rgba(76, 175, 130, 0.12) !important;
    border-left: 3px solid var(--verde-exito) !important;
    color: #bbf7d0 !important;
}

/* ---- Badges de estados (tracking admin) ---- */
.estado-pendiente {
    background: rgba(240, 168, 48, 0.12) !important;
    color: var(--naranja-advertencia) !important;
}
.estado-aprobada {
    background: rgba(76, 175, 130, 0.12) !important;
    color: var(--verde-confirmada) !important;
}
.estado-cancelada {
    background: rgba(224, 85, 85, 0.12) !important;
    color: var(--rojo) !important;
}

/* ---- Estado vacío ---- */
.vacio {
    background: var(--glass-bg) !important;
    border: 2px dashed rgba(255, 255, 255, 0.12) !important;
    color: var(--gris-texto-legible) !important;
}

/* ---- Tablas ---- */
td, .dash-table td {
    color: var(--blanco-secundario) !important;
    border-bottom: 1px solid rgba(255, 255, 255, 0.06) !important;
}
tr:nth-child(even) td, .dash-table tr:nth-child(even) td {
    background: rgba(255, 255, 255, 0.03) !important;
}
tr:hover td, .dash-table tr:hover td {
    background: rgba(212, 118, 42, 0.08) !important;
}

/* ---- Sidebar: ya oscuro, refinamiento tipográfico ---- */
.sidebar {
    font-family: 'Plus Jakarta Sans', sans-serif !important;
}
```

### Notas de verificación (A2)

- `.btn-luxury` se define por primera vez en el admin → los botones de los dos logins pasan de
  browser-default a botón marca.
- La capa NO toca el cuerpo dark ya existente (gradientes), ni `html::before/::after` (franja
  cultural), ni el sidebar (ya oscuro) salvo tipografía.

---

## Task A3 — Gate Entrega B2

### Verificación

1. **Suite completa (217 tests) en contenedor** (comando canónico de `AGENTS.md`):
   ```powershell
   docker run --rm -v "${PWD}:/app" -w /app --network envios_paraguay_cms_backend `
     -e SPRING_DATASOURCE_URL="jdbc:mysql://db:3306/envios_paraguay_cms_test?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true" `
     -e DB_USERNAME=root -e DB_PASSWORD=root -e SPRING_DATA_REDIS_HOST=redis `
     -v "${HOME}\.m2:/root/.m2" maven:3.9-eclipse-temurin-17 mvn clean test
   ```
   → **BUILD SUCCESS, 217 tests**.
2. **Rebuild + redeploy:**
   ```powershell
   docker compose build app
   docker compose up -d app
   docker inspect --format "{{.State.Health.Status}}" monteastur-app   # → healthy
   ```
3. **Spot-check autenticado con curl** (puerto 8080):
   - `/login` → `.login-wrapper` dark glass + `.btn-luxury` definido + `Plus+Jakarta+Sans`.
   - `/cliente/login` → mismo + campo `name="email"` presente.
   - POST `/cliente/login` (email `cliente@monteastur.com`, password `demo2026`, `_csrf` obtenido de
     `/cliente/login`) → 302 a `/cliente/panel`; GET `/cliente/panel` → body dark, logout + CSRF.
   - `/admin/dashboard` con auth básica de formulario (`admin`/`admin123`) → HTML con `.card` glass
     dark y sidebar oscuro.
   - `/cms/*` con auth → tablas dark, inputs `rgba(255,255,255,0.05)`.
4. Actualizar ledger SDD: `.superpowers/sdd/progress.md`.
5. Si hubiera ajustes, commit `style(ui): ajustes Entrega B2`.

### Criterio de éxito

- **217/217 BUILD SUCCESS** en Docker.
- App healthy tras redeploy; logins y paneles muestran dark premium con marca `#d4762a`;
  cero cambios en Thymeleaf/JS/controladores.
