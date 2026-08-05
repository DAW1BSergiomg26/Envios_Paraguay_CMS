# Spec — Cirugía Mayor de Unificación Visual y Coherencia de Marca

Fecha: 2026-08-05
Estado: Aprobado (Secciones 1–5)
Criterio de cierre: 236 tests, BUILD SUCCESS en contenedor Docker (Temurin-25, MySQL 8 + Redis 7).

## Problema

La web "Envios_Paraguay_CMS" tiene fragmentación visual: al navegar entre `/casa`,
`/contacto`, `/reservas` y `/seguimiento` cada vista carga CSS distintos de forma
inconsistente. Existen 5 "familias" de cabecera en 44 plantillas:

- **A — Público clásico** (16 páginas ES+EN): `fragments/header.html`, carga 13 CSS por página.
- **B — Público Tailwind** (tracking-search/result/404 + cliente/panel): Tailwind CDN, navbar propio sin teléfono ni ES/EN.
- **C — CMS admin** (9 páginas `cms/*` + `cliente/login`): suite `admin.css` (10 archivos via `@import`).
- **D — Stubs legacy huérfanos**: `index.html`, `login.html`, `contact.html`, `error-404.html`, `admin-layout.html`, `header.html` raíz.
- **E — Páginas de error**: `error.html` + `en/error.html` con `<style>` inline.

La franja tricolor real usa `#d62828`/`#1d4ed8` (no los colores canónicos de marca),
el logo del header clásico es texto plano (el logo tricolor `EN/VI/OS` solo vive en
plantillas legacy), y `lucide.min.js` falta en las 9 páginas CMS (iconos rotos).

## Decisiones de alcance (aprobadas)

1. **Superficie:** público completo (A, B, E) + CMS admin (C); stubs legacy (D) eliminados o migrados.
2. **Fuente única de verdad:** un solo `design-system.css` que absorbe todo el CSS del proyecto.
3. **Tracking:** se migra de Tailwind CDN al core (se elimina el CDN).
4. **Stubs:** eliminar `contact.html`, `error-404.html`, `index.html`, `admin-layout.html`, `header.html` raíz; reescribir `login.html` sobre el core.
5. **Bundles:** un solo archivo CSS para todo el proyecto.
6. **Admin:** la suite `admin.css` + `admin/*.css` se integra en el archivo único.
7. **Accesibilidad:** variantes accesibles del naranja (ver Sección 3).

## Sección 1 — Arquitectura de archivos y carga

- **`src/main/resources/static/css/design-system.css`** se convierte en EL design system:
  tokens duales, identidad tricolor, componentes compartidos, estilos por página
  (público, tracking, admin, error). Todo el CSS del proyecto se fusiona aquí.
- **Archivos CSS a eliminar** (absorbidos):
  `style.css`, `brand-styles.css`, `theme-ui.css`, `main.css`,
  `hero-premium.css`, `luxury-core.css`, `tracking-resultado.css`,
  `reservas-premium.css`, `reservas-premium-v2.css`, `casa-premium.css`,
  `contacto-premium.css`, `tracking-premium.css`, `tracking-logistica.css`,
  `tracking-historial.css`, `operaciones-premium.css`,
  `admin.css` + `admin/*.css` (10 archivos, 2 vacíos).
- **Carga por vista:** 1 `<link>` CSS + `lucide.min.js` + `theme-toggle.js`
  (+ `app.js` solo en públicas). Sin Tailwind CDN.
- **Fragmentos:** unificar cabecera en `fragments/header.html` (ES) y
  `fragments/header-en.html` (EN) — mismo markup, mismo head. Eliminar
  `fragments/public-head.html` (su navbar/footer pasan al core).
- **Stubs legacy:** eliminar `contact.html`, `error-404.html`, `index.html`,
  `admin-layout.html` y `header.html` raíz. Reescribir `login.html` sobre el core.
- **Páginas de error:** sin `<style>` inline; core + toggle.

## Sección 2 — Tokens de diseño (en `design-system.css`)

### Identidad (constantes en ambos modos)

```css
--en-rojo: #C8102E;
--vi-blanco: #FFFFFF;
--os-azul: #0047AB;
--monte-amarillo: #E67E22;
--btn-text: #0F281E;          /* texto de botón naranja, bold, AAA */
--btn-text-dark: #071510;     /* variante 7:1 estricto si se requiere */
```

### Modo oscuro (predeterminado) — Verde Bosque Esmeralda

| Token | Valor |
|---|---|
| `--bg-main` / `--bg-body` | `#0D2319` |
| `--bg-surface` / `--nav-bg` | `#153C2D` |
| `--bg-card` | `#1B4D3B` |
| `--text-primary` | `#F4F7F5` |
| `--text-secondary` | `#A3C9B8` |
| `--text-muted` | `#7BA897` |
| `--accent-color` | `#E67E22` (libre, AAA ~8:1 sobre #0D2319) |
| `--accent-hover` | `#D97706` |
| `--border-subtle` | `rgba(163,201,184,0.2)` |
| `--border-strong` | `rgba(163,201,184,0.35)` |
| `--surface-header` | `rgba(11,30,22,0.85)` |
| `--glass-bg` | `rgba(27,77,59,0.6)` |

### Modo claro — Pristine Quartz

| Token | Valor |
|---|---|
| `--bg-main` / `--bg-body` | `#F4F6F8` |
| `--nav-bg` | `#FFFFFF` |
| `--bg-card` | `#FFFFFF` |
| `--text-primary` | `#0F172A` |
| `--text-secondary` | `#64748B` |
| `--text-muted` | `#94A3B8` |
| `--accent-text` | `#B45309` (links/hover, 5:1 sobre blanco) |
| `--accent-color` | `#E67E22` (solo decorativo grande) |
| `--accent-hover` | `#9A4E0C` |
| `--border-subtle` | `rgba(0,0,0,0.08)` |
| `--border-strong` | `rgba(0,0,0,0.16)` |
| `--surface-header` | `rgba(248,250,252,0.9)` |

### Otros tokens

- `--radius-card: 16px`; `--radius-btn: 12px`; `--radius-badge: 9999px`.
- `--shadow-card` y `--glass-shadow` por modo.
- `--font-family-base: 'Plus Jakarta Sans', ...`.
- `prefers-reduced-motion` respetado.

## Sección 3 — Componentes unificados

### Cabecera estándar (idéntica en A, B y panel de cliente)

- Franja tricolor canónica 6px arriba: `.cultural-strip-paraguay` con
  `--en-rojo` / `--vi-blanco` / `--os-azul`.
- Logo tricolor `MONTEASTUR` + `EN/VI/OS` (`.brand-envios-*`).
- Nav: Inicio, La Casa, Seguimiento, Reservas, Contacto.
- Teléfono `+34 642 687 292`, toggle ES/EN, botón tema.
- Claro: fondo blanco, textos `#0F172A`, hover `#B45309`.
- Oscuro: fondo `#0D2319`, textos `#F4F7F5`, secundarios menta `#A3C9B8`.
- Prohibidos en oscuro: textos borgoña, marrones oscuros, grises ilegibles.

### Tarjetas y glass

- `border-radius: 16px`, `background: var(--bg-card)`, padding uniforme `1.5rem`.
- `.glass-card`: `backdrop-filter: blur(12px)`, `--glass-border`, `--glass-shadow`.
- Sombras `--shadow-card`.

### Formularios

- `input`, `textarea`, `select`: mismo borde (`--border-subtle`), fondo `var(--bg-card)`,
  radio `--radius-btn`, texto `var(--text-primary)`.
- Focus: borde `--accent-color` + `box-shadow: 0 0 0 3px var(--accent-glow)` (≥3:1).
- Placeholder `--text-muted` (AAA).

### Botones

- **Primario**: gradiente `#E67E22 → #9A4E0C`, texto `--btn-text` (`#0F281E`) bold
  (~5.5:1 → AA normal, AAA grande). Variante 7:1 con `--btn-text-dark` disponible.
- **Secundario**: glass con borde `--border-strong`.
- Semáforo (`--status-*`) ajustado por modo.

### Badges, sidebar admin, tablas, stepper, timeline, dropzone

- Heredan tokens; sin colores hardcodeados.

## Sección 4 — Migración de plantillas

- **Público ES (8)** → `fragments/header`. **EN (8)** → `header-en`.
- **Tracking (3)** → abandonan `public-head`/Tailwind CDN; usan `fragments/header`.
  Se conserva `html5-qrcode` (funcionalidad).
- **cliente/panel** → cabecera estándar tricolor + saludo/logout en el header.
- **CMS (9)** → head 1 CSS + `theme-toggle.js` + `lucide.min.js` + `app.js`;
  sidebar admin estilado con tokens.
- **cliente/login** y **login.html** → reescritos sobre el core.
- **Errores (2)** → sin `<style>` inline, con core + toggle.
- **Eliminar**: `contact.html`, `error-404.html`, `index.html`, `admin-layout.html`,
  `header.html` raíz.

## Sección 5 — Accesibilidad y pruebas

- Focus-visible 3:1, `prefers-reduced-motion`, `aria-label` en toggle, skip-link.
- `AdminThemeAssetsTest` se actualiza: espera `design-system.css` + `theme-toggle.js` +
  `btn-theme-toggle` (ya NO `theme-ui.css`).
- Nuevo test de integridad de recursos: ninguna plantilla referencia un CSS/JS eliminado
  (whitelist de archivos vivos). Evita regresiones estructurales de fragmentación.
- Verificación: `mvn clean test` en Docker (`maven:3.9-eclipse-temurin-25`,
  red `envios_paraguay_cms_backend`, MySQL/Redis) → **236 tests, BUILD SUCCESS**.
