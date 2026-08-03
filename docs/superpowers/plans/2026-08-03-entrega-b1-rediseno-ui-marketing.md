# Plan — Entrega B1: Marketing Público Dark Premium (ES/EN)

Fecha: 2026-08-03 · Rama: `main` (commits directos como en Entrega A) · Suite: 217 tests

## Objetivo

Re-tematizar el sitio público (home, entorno, operaciones, lacasa, reservas, contacto, aviso-legal,
política de cookies — ES y EN) a **dark premium** estilo Linear/Stripe/Vercel con la marca
`#d4762a`, preservando **byte a byte** todas las expresiones Thymeleaf, `name`/`id`, funciones JS
(`aceptarCookies`, `app.js`), view names y los **217 tests**.

## Enlaces

- Spec: `docs/superpowers/specs/2026-08-03-rediseno-ui-linear-stripe-vercel-design.md`
- Plan A: `docs/superpowers/plans/2026-08-03-entrega-a-rediseno-ui.md`
- Inventario de superficies light marketing: reporte del subagente `explore` (en el hilo de la sesión)

## Arquitectura de re-tema (verificada en el código)

- Todas las páginas marketing ES/EN cargan su `<head>`/`<header>`/`<footer>` vía fragmentos:
  - ES → `fragments/header.html` + `fragments/footer.html`
  - EN → `fragments/header-en.html` + `fragments/footer-en.html`
  - Los templates de página NO tienen CSS propio; solo `th:replace` de esos fragmentos.
- **`luxury-core.css` ya es la ÚLTIMA hoja del `<head>`** (`header.html` línea 16, `header-en.html`
  idem) → **append de una capa "dark override" al final de `luxury-core.css`** re-tematiza todo el
  sitio público por orden de carga; con `!important` vence a las reglas `!important` de
  `tracking-logistica.css`, `tracking-resultado.css`, `contacto-premium.css`, `reservas-premium*.css`.
- **Flip de vars de los premium CSS** (mismo `:root`, hoja posterior gana): redefinir en luxury-core
  `--casa-dark`, `--contacto-green-dark`, `--reservas-green-dark`, `--tracking-logistic-dark`
  (texto `#172114` → `#f4f4f5`) y `--casa-paper`, `--contacto-paper`, `--reservas-paper`,
  `--tracking-logistic-paper` (superficies crema `#fff8e1` → glass dark). Verificado: los `*-dark`
  solo se usan como `color`; los `*-paper` solo como `background` del trust-strip.
- `error.html` y `en/error.html` usan vars de luxury-core (`--glass-bg`, `--color-accent`,
  `--color-text`, `--color-muted`, `--glass-blur`, `--radius-xl`) → se re-tematizan **gratis** al
  voltear las vars (se añaden los alias `--color-text`/`--color-muted`).
- **El hero-over-imagen NO se toca**: `.hero-premium`, `.hero-premium-overlay`, `.hero-tracking-card`,
  `.hero-tracking-actions input` y su botón naranja se conservan intactos (decisión: el input blanco
  del hero es affordance principal de la sección). Las secciones KPI (`.kpi-premium`, `.kpi-card`) y
  ruta (`.route-premium-card`, `.route-point`, `.trust-logo-pill`) ya son oscuras → se conservan.
- **Secciones home premium que SÍ son claras** y deben re-tematizarse: `.trust-premium`,
  `.testimonials-premium`, `.trust-logos-premium` (fondo blanco 0.58-0.88), `.trust-card`,
  `.testimonial-card`, más sus títulos `#24451f` y textos `#5d6258`/`#3f463d`.
- El header verde corporativo (`style.css:43` `linear-gradient(135deg,#3f6338,#557c45)`) se
  re-tematiza vía CSS (sticky glass dark) **sin cambiar el markup** (`logo`, `telefono`, `idiomas`,
  `nav a` son blancos → legibles sobre glass dark). Footer verde oscuro (`style.css:188`) → `#0c0c0e`.
- `.banner-reserva` (naranja, dentro del footer) → **dark glass con borde naranja** (decisión
  aprobada: eliminar superficies saturadas, mantener el acento `#d4762a`).
- Franjas culturales Paraguay/Asturias (`style.css:2865/2878`) se **conservan** (identidad).

## Constraints globales

- Regla de oro: NO se alteran expresiones Thymeleaf, `name`/`id`, handlers JS, view names.
- Solo cambian: `luxury-core.css` (rewrite + capa override) y 4 fragmentos de chrome (preconnect +
  limpieza mínima).
- Verificación: los tests existentes actúan como **regresión** (no hay TDD rojo-verde para CSS puro)
  + **spot-check visual/curl** en el gate. Compilar: `.\mvnw.cmd test`.

## Resumen de tareas

| # | Tarea | Archivos | Commit sugerido |
|---|-------|----------|-----------------|
| M1 | Rewrite dark de `luxury-core.css` (+ capa override) | `static/css/luxury-core.css` | `style(ui): luxury-core dark theme - tokens, Jakarta Sans y capa dark override` |
| M2 | Chrome: preconnect fuentes + limpieza footer | 4 fragmentos | `style(ui): preconnect fuentes y limpieza de markup footer` |
| M3 | Gate Entrega B1 | — | `style(ui): ajustes Entrega B1` (si procede) |

---

## Task M1 — `luxury-core.css` dark rewrite (+ capa override)

### Pasos

1. **Baseline:** `.\mvnw.cmd test "-Dtest=PublicControllerTest,TrackingWebControllerTest,ClientDashboardControllerTest"` → BUILD SUCCESS.
2. Verificar por grep que los selectores del override existen en los CSS objetivo:
   ```powershell
   Select-String -Path src/main/resources/static/css/{casa,contacto,reservas,tracking}*.css -Pattern 'trust-premium|testimonials-premium|casa-route-node|contacto-info-card|reserva-calendario|tracking-form-glass|tr-badge\.estado|tl-step-icon'
   ```
3. Reescribir `src/main/resources/static/css/luxury-core.css` con el contenido COMPLETO de abajo.
4. **Regresión:** repetir el comando del paso 1 → BUILD SUCCESS.
5. Commit: `style(ui): luxury-core dark theme - tokens, Jakarta Sans y capa dark override`.

### Contenido completo de `luxury-core.css`

```css
@import url('https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap');

/* =============================================
   LUXURY CORE — Monteastur Envios Design System
   Dark theme | Marca: #d4762a | Plus Jakarta Sans
   ============================================= */

:root {
  --color-accent: #d4762a;
  --color-accent-dark: #b8661f;
  --color-accent-hover: #e08c3f;
  --color-dark: #f4f4f5;
  --color-charcoal: #18181b;
  --color-stone: #18181b;
  --color-parchment: #121215;
  --color-white: #f4f4f5;
  --text-primary: #f4f4f5;
  --text-secondary: #a1a1aa;
  --text-muted: #71717a;
  --text-on-accent: #ffffff;
  --color-text: var(--text-primary);
  --color-muted: var(--text-muted);
  --icon-stroke-width: 1.5px;
  --icon-size: 24px;
  --icon-color: var(--color-accent);
  --transition-default: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  --transition-slow: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);
  --glass-bg: rgba(255, 255, 255, 0.03);
  --glass-bg-hover: rgba(255, 255, 255, 0.06);
  --glass-border: rgba(255, 255, 255, 0.1);
  --glass-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
  --glass-blur: 12px;
  --brand-glow: 0 0 40px rgba(212, 118, 42, 0.15);

  /* Flip de vars de los premium CSS (mismo :root, hoja posterior gana) */
  --casa-dark: #f4f4f5;
  --contacto-green-dark: #f4f4f5;
  --reservas-green-dark: #f4f4f5;
  --tracking-logistic-dark: #f4f4f5;
  --casa-paper: rgba(255, 255, 255, 0.05);
  --contacto-paper: rgba(255, 255, 255, 0.05);
  --reservas-paper: rgba(255, 255, 255, 0.05);
  --tracking-logistic-paper: rgba(255, 255, 255, 0.05);
}

/* ---- Base ---- */
html {
  scroll-behavior: smooth;
}

body {
  background-color: #09090b !important;
  background-image: radial-gradient(ellipse at 50% 0%, rgba(212, 118, 42, 0.05) 0%, transparent 70%);
  color: var(--text-primary) !important;
  font-family: 'Plus Jakarta Sans', 'Inter', system-ui, sans-serif;
  -webkit-font-smoothing: antialiased;
}

/* ---- Iconos ---- */
.luxury-icon {
  stroke-width: var(--icon-stroke-width);
  stroke-linecap: round;
  stroke-linejoin: round;
  width: var(--icon-size);
  height: var(--icon-size);
  color: var(--icon-color);
  vertical-align: middle;
  display: inline-block;
}

.luxury-icon-sm { width: 18px; height: 18px; }
.luxury-icon-lg { width: 32px; height: 32px; }
.luxury-icon-xl { width: 48px; height: 48px; }

.luxury-icon-accent { color: var(--color-accent); }
.luxury-icon-light { color: var(--color-white); }
.luxury-icon-muted { color: var(--text-muted); }

/* ---- Superficies glass ---- */
.glass-card {
  background: var(--glass-bg);
  backdrop-filter: blur(var(--glass-blur));
  -webkit-backdrop-filter: blur(var(--glass-blur));
  border: 1px solid var(--glass-border);
  box-shadow: var(--glass-shadow);
  border-radius: 16px;
  transition: var(--transition-default);
}

.glass-card:hover {
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.6);
  transform: translateY(-2px);
}

/* ---- Botones ---- */
.btn-luxury {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 28px;
  background: var(--color-accent);
  color: var(--text-on-accent);
  border: none;
  border-radius: 999px;
  font-size: 0.95rem;
  font-weight: 700;
  letter-spacing: 0.02em;
  cursor: pointer;
  transition: var(--transition-default);
  box-shadow: 0 4px 16px rgba(212, 118, 42, 0.25);
}

.btn-luxury:hover {
  background: var(--color-accent-hover);
  box-shadow: 0 8px 24px rgba(212, 118, 42, 0.4);
  transform: translateY(-2px);
}

.btn-luxury-outline {
  background: transparent;
  border: 1.5px solid var(--color-accent);
  color: var(--color-accent);
}

.btn-luxury-outline:hover {
  background: var(--color-accent);
  color: var(--text-on-accent);
}

.btn-luxury-secondary {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 28px;
  background: var(--glass-bg);
  color: var(--text-primary);
  border: 1.5px solid var(--glass-border);
  border-radius: 999px;
  font-size: 0.95rem;
  font-weight: 600;
  letter-spacing: 0.02em;
  cursor: pointer;
  transition: var(--transition-default);
}

.btn-luxury-secondary:hover {
  background: var(--glass-bg-hover);
  transform: translateY(-1px);
}

.btn-luxury-ghost {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 28px;
  background: transparent;
  color: var(--text-secondary);
  border: none;
  border-radius: 8px;
  font-size: 0.95rem;
  font-weight: 500;
  letter-spacing: 0.03em;
  cursor: pointer;
  transition: var(--transition-default);
}

.btn-luxury-ghost:hover {
  color: var(--color-accent);
  background: rgba(212, 118, 42, 0.08);
}

/* ---- Títulos ---- */
.luxury-heading {
  letter-spacing: 0.04em;
  font-weight: 700;
  color: var(--color-dark);
}

.luxury-heading-lg {
  letter-spacing: 0.05em;
  font-weight: 800;
}

.luxury-heading-xl {
  letter-spacing: 0.08em;
  font-weight: 800;
  text-transform: uppercase;
}

/* ---- Foco accesible ---- */
.focus-glow:focus {
  outline: none;
  border-color: var(--color-accent);
  box-shadow: 0 0 0 3px rgba(212, 118, 42, 0.2);
}

/* ---- Utilidades ---- */
.card-hover {
  transition: var(--transition-default);
}

.card-hover:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.5);
}

.border-accent-left {
  border-left: 3px solid var(--color-accent);
}

/* ---- Estados ---- */
.status-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-right: 6px;
  vertical-align: middle;
}

.status-dot-en-transito { background: #f59e0b; }
.status-dot-entregado { background: #22c55e; }
.status-dot-pendiente { background: #94a3b8; }
.status-dot-cancelado { background: #ef4444; }
.status-dot-confirmada { background: #22c55e; }
.status-dot-leido { background: #3b82f6; }
.status-dot-no-leido { background: #d4762a; }

/* =============================================
   DARK OVERRIDE — Entrega B (Marketing ES/EN)
   Última hoja del <head>: por orden de carga
   vence a style.css y a los premium CSS; con
   !important vence a sus reglas !important.
   El hero-over-imagen y las secciones KPI/ruta
   (ya oscuras) NO se tocan.
   ============================================= */

/* ---- Chrome: header sticky glass, footer, cookie banner ---- */
header {
  position: sticky;
  top: 0;
  z-index: 50;
  background: rgba(9, 9, 11, 0.92) !important;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1) !important;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.35) !important;
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
}

footer {
  background: #0c0c0e !important;
  border-top: 1px solid rgba(255, 255, 255, 0.1) !important;
}

#cookie-banner {
  background: rgba(18, 18, 21, 0.96) !important;
  border-top: 1px solid rgba(255, 255, 255, 0.1) !important;
  color: var(--text-secondary) !important;
}

.banner-reserva {
  background: linear-gradient(135deg, rgba(212, 118, 42, 0.16), rgba(212, 118, 42, 0.05)) !important;
  border: 1px solid rgba(212, 118, 42, 0.35) !important;
  box-shadow: 0 6px 30px rgba(212, 118, 42, 0.18) !important;
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

/* ---- Secciones home que heredaban fondo claro ---- */
.home-welcome {
  background-color: transparent !important;
}

/* ---- Secciones premium del home que SON claras ---- */
.trust-premium,
.testimonials-premium,
.trust-logos-premium {
  background: rgba(18, 18, 21, 0.85) !important;
  border: 1px solid rgba(255, 255, 255, 0.1) !important;
  box-shadow: var(--glass-shadow) !important;
}

.trust-card,
.testimonial-card {
  background: var(--glass-bg) !important;
  border: 1px solid var(--glass-border) !important;
  border-radius: 26px !important;
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

.trust-card:hover,
.testimonial-card:hover {
  background: var(--glass-bg-hover) !important;
  border-color: rgba(212, 118, 42, 0.32) !important;
  transform: translateY(-4px);
}

/* Textos de las secciones premium del home (eran verde oscuro #24451f / #5d6258 / #3f463d) */
.trust-premium h2,
.testimonials-header h2,
.trust-card h3,
.testimonial-card strong {
  color: var(--text-primary) !important;
}

.trust-premium-header p,
.testimonials-header p,
.trust-card p,
.testimonial-card p {
  color: var(--text-secondary) !important;
}

.trust-eyebrow,
.testimonial-card span,
.trust-logos-title {
  color: var(--color-accent-hover) !important;
}

.trust-icon {
  background: linear-gradient(135deg, rgba(63, 99, 56, 0.2), rgba(212, 118, 42, 0.25)) !important;
}

/* ---- Superficies claras hardcoded (casa / contacto / reservas) ---- */
.casa-trust-strip div,
.contacto-trust-strip div,
.reservas-trust-strip div,
.tracking-trust-strip div {
  background: var(--glass-bg) !important;
  border: 1px solid var(--glass-border) !important;
  color: var(--text-secondary) !important;
}

.casa-route-node,
.reservas-route-node,
.tracking-route {
  background: rgba(18, 18, 21, 0.6) !important;
  border: 1px solid var(--glass-border) !important;
}

.casa-services-premium .casa-card,
.casa-process-premium,
.casa-process-step,
.contacto-info-card,
.contacto-info-item,
.reserva-calendario,
.calendario-mes,
.reserva-confianza,
.reservas-metric-card,
.reservas-estimator-grid div,
.reserva-success-code,
.reserva-success-next div,
.tracking-form-glass,
.tracking-help-card,
.tracking-result-premium,
.tracking-mini-resume .tr-item,
.tracking-info-item,
.tracking-observaciones,
.tracking-timeline-premium,
.evento-timeline-public,
.evento-card {
  background: var(--glass-bg) !important;
  border: 1px solid var(--glass-border) !important;
  border-radius: 16px !important;
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

/* Texto hardcoded de pasos (casa) */
.casa-process-step span {
  color: var(--text-secondary) !important;
}

/* Iconos / puntos de ruta / pasos */
.contacto-info-icono,
.reservas-route-node span,
.tl-step-icon,
.tl-step-label,
.evento-icon,
.route-dot {
  background: rgba(212, 118, 42, 0.15) !important;
  border: 1px solid rgba(212, 118, 42, 0.3) !important;
  color: var(--color-accent) !important;
}

/* CTA blanco (casa) -> botón marca */
.casa-cta-premium a {
  background: linear-gradient(135deg, var(--color-accent), var(--color-accent-hover)) !important;
  color: #ffffff !important;
  box-shadow: 0 8px 24px rgba(212, 118, 42, 0.3) !important;
}

/* ---- Texto hardcoded sobre superficies claras (tracking-resultado / reservas-v2) ---- */
.tr-label,
.tracking-info-label {
  color: var(--text-muted) !important;
}

.tr-value,
.tr-code,
.tracking-info-value {
  color: var(--text-primary) !important;
}

/* Badges de estado de tracking-resultado -> tokens Entrega A (dark glass) */
.tr-badge.estado-RECIBIDO {
  background: rgba(129, 140, 248, 0.1) !important;
  color: #a5b4fc !important;
  border-color: rgba(129, 140, 248, 0.3) !important;
}

.tr-badge.estado-EN_ADUANA_ORIGEN,
.tr-badge.estado-EN_ADUANA_DESTINO {
  background: rgba(167, 139, 250, 0.1) !important;
  color: #c4b5fd !important;
  border-color: rgba(167, 139, 250, 0.3) !important;
}

.tr-badge.estado-EN_TRANSITO {
  background: rgba(251, 191, 36, 0.1) !important;
  color: #fcd34d !important;
  border-color: rgba(251, 191, 36, 0.4) !important;
}

.tr-badge.estado-EN_REPARTO {
  background: rgba(56, 189, 248, 0.1) !important;
  color: #7dd3fc !important;
  border-color: rgba(56, 189, 248, 0.3) !important;
}

.tr-badge.estado-ENTREGADO {
  background: rgba(52, 211, 153, 0.1) !important;
  color: #6ee7b7 !important;
  border-color: rgba(52, 211, 153, 0.4) !important;
}

/* ---- Formularios y controles ---- */
input[type="text"],
input[type="email"],
input[type="password"],
input[type="tel"],
input[type="date"],
input[type="number"],
select,
textarea,
.rf-control {
  background-color: rgba(255, 255, 255, 0.05) !important;
  border: 1px solid var(--glass-border) !important;
  color: var(--text-primary) !important;
  border-radius: 10px !important;
}

input::placeholder,
textarea::placeholder {
  color: var(--text-muted) !important;
}

input:focus,
select:focus,
textarea:focus {
  border-color: rgba(212, 118, 42, 0.6) !important;
  box-shadow: 0 0 0 3px rgba(212, 118, 42, 0.15) !important;
  outline: none;
}

label {
  color: var(--text-secondary) !important;
}

/* ---- Tarjetas heredadas de style.css (destacados / pasos) ---- */
.destacado-card,
.paso-card {
  background: var(--glass-bg) !important;
  border: 1px solid var(--glass-border) !important;
  border-radius: 16px !important;
}

.destacado-card:hover,
.paso-card:hover {
  transform: scale(1.01);
  box-shadow: var(--glass-shadow);
}

.paso-icono {
  background: rgba(212, 118, 42, 0.15) !important;
  border: 1px solid rgba(212, 118, 42, 0.3) !important;
  color: var(--color-accent) !important;
}
```

### Notas de verificación (M1)

- `--color-dark` se voltea a `#f4f4f5`: único uso en CSS es `.luxury-heading { color: var(--color-dark) }`
  → títulos legibles sobre dark. NO se usa como fondo.
- Los `--*-dark` premium solo se usan como `color`; los `--*-paper` solo como `background` del
  trust-strip → el flip de vars es seguro (verificado por grep).
- El override NO referencia `.hero-premium`, `.hero-tracking-*`, `.kpi-*`, `.route-premium-card`,
  `.trust-logo-pill` → se preservan intactos.
- `--color-text` / `--color-muted` alias → `error.html` y `en/error.html` se re-tematizan gratis.

---

## Task M2 — Chrome: preconnect de fuentes + limpieza markup footer

### Cambios

1. `fragments/header.html` y `fragments/header-en.html` — tras el `<meta name="viewport">`, añadir:
   ```html
   <link rel="preconnect" href="https://fonts.googleapis.com">
   <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
   ```
   (`luxury-core.css` ya es la última hoja y trae el `@import` de Plus Jakarta Sans; NADA más cambia
   en el `<head>`.)
2. `fragments/footer.html` y `fragments/footer-en.html` — eliminar el `</div>` huérfano de la
   línea 49 (las columnas y `.footer-grid` cierran correctamente en líneas 47-48). **Sin ningún otro
   cambio de contenido/estructura.**

### Pasos

1. Aplicar los 4 cambios.
2. Regresión: `.\mvnw.cmd test "-Dtest=PublicControllerTest,TrackingWebControllerTest,ClientDashboardControllerTest"` → BUILD SUCCESS.
3. Commit: `style(ui): preconnect fuentes y limpieza de markup footer`.

---

## Task M3 — Gate Entrega B1

### Verificación

1. **Suite dirigida local:** `.\mvnw.cmd test "-Dtest=PublicControllerTest,TrackingWebControllerTest,ClientDashboardControllerTest"` → BUILD SUCCESS.
2. **Rebuild + redeploy:**
   ```powershell
   docker compose build app
   docker compose up -d app
   docker inspect --format "{{.State.Health.Status}}" monteastur-app   # → healthy
   ```
3. **Spot-check curl** (puerto por defecto 8080):
   - `/` → body `09090b`, header sin `linear-gradient(135deg, #3f6338`, `luxury-core.css` presente,
     `Plus+Jakarta+Sans` presente, `.trust-premium` con `rgba(18, 18, 21`, `.hero-premium` intacta
     (grep `.hero-premium-overlay` en el HTML no es aplicable — verificar en CSS servido por `/css/hero-premium.css`).
   - `/reservas`, `/contacto`, `/lacasa`, `/en`, `/en/reservas`, `/en/contacto` → body dark +
     formularios `rgba(255, 255, 255, 0.05)`.
   - `/tracking` (tracking-search Tailwind) → sigue con el navbar dark de Entrega A (sin cambios).
   - `/ruta-inexistente` → HTTP 404 y HTML de `error.html` (card glass dark + botón `btn-luxury`).
   - `/css/luxury-core.css` → contiene `--casa-dark: #f4f4f5` y `.tr-badge.estado-RECIBIDO`.
4. Actualizar ledger SDD: `.superpowers/sdd/progress.md`.
5. Si hubiera ajustes, commit `style(ui): ajustes Entrega B1`.

### Criterio de éxito

- `PublicControllerTest` (todas las rutas ES/EN) PASS.
- App healthy tras redeploy; spot-checks muestran dark en todas las páginas públicas y en la página
  de error; cero cambios en Thymeleaf/JS.
