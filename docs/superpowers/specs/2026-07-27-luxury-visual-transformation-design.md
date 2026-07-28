# Luxury Visual Transformation — Monteastur Envios

## Overview

Refactor all 32 Thymeleaf templates and 22 CSS files to replace emoji icons with Lucide SVG icons (CDN-delivered), apply luxury-grade visual styling (glassmorphism, amber/terracotta accents, smooth micro-interactions), and ensure consistent corporate tone in ES/EN views.

## Architecture

### Icon Delivery: CDN Lucide
- Add `<script src="https://unpkg.com/lucide@latest" defer></script>` in `header.html` and `header-en.html`
- Add `lucide.createIcons()` call at end of `<body>` in each fragment
- Use `<i data-lucide="icon-name" class="luxury-icon"></i>` for all icons
- No local SVG files, no sprite — single CDN script covers all pages

### CSS: Centralized Design System

**New file:** `src/main/resources/static/css/luxury-core.css`

Contains:
1. **CSS Custom Properties** (design tokens)
2. **`.luxury-icon`** base class (stroke, color, sizing)
3. **Transition defaults** (buttons, cards, borders)
4. **Glassmorphism utilities** (`.glass-card`, `.glass-hover`)
5. **Typography refinements** (letter-spacing for headings, executive spacing)
6. **Focus/active states** (glow rings on inputs, buttons)

### Design Tokens

```css
:root {
  /* Brand — Amber/Terracotta */
  --color-accent:       #c27803;
  --color-accent-dark:  #9a5f02;
  --color-accent-light: #e8a33d;

  /* Neutral — Corporate */
  --color-dark:         #1a1a2e;
  --color-charcoal:     #2d2d44;
  --color-stone:        #f8f6f3;
  --color-parchment:    #f0ece6;
  --color-white:        #ffffff;

  /* Text */
  --text-primary:   #1a1a2e;
  --text-secondary: #5a5a72;
  --text-muted:     #8a8a9e;
  --text-on-accent: #ffffff;

  /* Icon */
  --icon-stroke-width: 1.5px;
  --icon-size: 24px;
  --icon-color: var(--color-accent);

  /* Transitions */
  --transition-default: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  --transition-slow:    all 0.5s cubic-bezier(0.4, 0, 0.2, 1);

  /* Glassmorphism */
  --glass-bg:     rgba(255, 255, 255, 0.6);
  --glass-border: rgba(255, 255, 255, 0.3);
  --glass-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
  --glass-blur:   12px;

  /* Shadows */
  --shadow-sm: 0 2px 8px rgba(0, 0, 0, 0.06);
  --shadow-md: 0 4px 16px rgba(0, 0, 0, 0.08);
  --shadow-lg: 0 8px 32px rgba(0, 0, 0, 0.10);
  --shadow-accent: 0 4px 20px rgba(194, 120, 3, 0.25);
}
```

### Icon Replacement Map

| Emoji / Old | Lucide Icon | Views |
|---|---|---|
| 📦 | `package` | home, casa |
| 🚚 | `truck` | home, casa, tracking, operaciones |
| 📍 | `map-pin` | tracking, casa (ruta) |
| ✈️ | `plane` | tracking (ruta aérea) |
| ⛴️ | `ship` | tracking (ruta atlántica) |
| 🏁 | `flag` | tracking (destino) |
| 🛡️ | `shield-check` | casa (atención cercana) |
| 🔍 | `search` | tracking (buscador) |
| ✓ | `check` | reservas |
| 📅 | `calendar-days` | reservas, tracking |
| 📄 | `file-text` | reservas, contacto |
| 📞 | `phone-call` | contacto |
| 📧 | `mail` | contacto |
| 🏢 | `building-2` | contacto |
| 🧑‍💻 | `headphones` | tracking (ayuda) |
| 🔖 | `barcode` | tracking (código) |
| 🧭 | `compass` | tracking (estado) |
| 🗺️ | `navigation` | tracking (estado alt) |
| ✚ | `plus` | operaciones |
| 🕐 | `clock` | tracking, reservas |
| 👥 | `users` | entorno, home |
| 🌱 | `leaf` | entorno |
| Other emojis | Corresponding Lucide | All views |

## Per-View Changes

### 1. Header (`fragments/header.html` + `header-en.html`)
- Add CDN Lucide script
- Add `luxury-core.css` stylesheet link
- Replace any emoji nav icons with `data-lucide`
- Add `lucide.createIcons()` before `</body>`
- Refine nav link letter-spacing: `0.05em`
- Active nav indicator: amber underline with transition

### 2. Footer (`fragments/footer.html` + `footer-en.html`)
- Replace emoji contact icons (📞 📧) with `phone-call` / `mail`
- Copyright line: remove any decorative emoji
- Add `lucide.createIcons()` before `</body>`

### 3. Home (`home.html` + `en/home.html`)
- **Hero:** Replace emoji bullet points with `<i data-lucide="check" class="luxury-icon"></i>`
- **3-step process:** Replace numbered circles with clean icon cards using `package`, `truck`, `shield-check`
- **Services grid:** Replace any emoji indicators with Lucide equivalents
- Add glassmorphism effect to stat cards
- Typography: apply `letter-spacing: 0.08em` to hero heading, `0.12em` to tagline
- Button hover: subtle amber glow shadow

### 4. La Casa (`lacasa.html` + `en/casa.html`)
- **Top cards:**
  - `Carga cuidada` → `package`
  - `Ruta coordinada` → `truck`
  - `Atención cercana` → `shield-check`
- **Route timeline (Asturias → Ruta atlántica → Paraguay):**
  - Asturias → `map-pin`
  - Ruta atlántica → `ship`
  - Paraguay → `flag`
- Card hover: scale(1.02) + elevated shadow
- Refine section headings with executive letter-spacing

### 5. Tracking (`tracking.html` + `en/tracking.html`)
- **Quick guide (3 pasos):**
  - 1. Código → `barcode`
  - 2. Estado → `compass` or `navigation`
  - 3. Ayuda → `headphones`
- **Search bar:** floating command-bar design with focus glow (amber ring)
- **Timeline:** replace ⛴️ 📍 ✈️ 🏁 with `ship`, `map-pin`, `plane`, `flag`
- **Shipment status badges:** subtle gradient backgrounds, no emoji
- **Previous shipments table:** replace status emoji with colored dot indicators (CSS-only)

### 6. Reservas (`reservas.html` + `en/reservas.html`)
- **Multi-step indicators:** replace emoji steps with `calendar-days`, `file-text`, `check`
- **Form fields:** minimal borders, focus glow in amber
- **Route visual (Origen → Destino):** replace emoji with `map-pin` → `arrow-right` → `map-pin`
- **Service tier cards:** replace checkmark emoji with `check` icon, amber on hover
- **Metrics cards (envíos gestionados, etc.):** clean stat layout with icon above number

### 7. Contacto (`contacto.html` + `en/contacto.html`)
- **Info cards:**
  - Dirección → `building-2`
  - Teléfono → `phone-call`
  - Email → `mail`
  - Horario → `clock`
- **Form:** replace any decorative emoji/icon on submit button with `send` icon
- Card layout: symmetric two-column with glassmorphism
- Office hours table: clean lines, amber accent border-left

### 8. Operaciones (`operaciones.html` + `en/operaciones.html`)
- **Service cards:** replace emoji with Lucide:
  - Carga consolidada → `package`
  - Recepción → `clipboard-check`
  - Entrega final → `truck`
  - Aduana → `file-check`
  - Logística inversa → `refresh-cw`
- **Stats section:** refine counters with icon above number

### 9. Entorno (`entorno.html`)
- Replace emoji values with `leaf`, `users`, `award`, `heart`
- Card refinements consistent with rest of system

### 10. Login (`login.html`, `cliente/login.html`)
- Replace any emoji with Lucide (`lock`, `user`, `log-in`)
- Glassmorphism card centered

### 11. Admin / CMS views
- Admin sidebar: replace any emoji nav icons with Lucide
- CMS tracking form: replace emoji status indicators
- CMS dashboard: replace stat card emoji
- Keep dark theme, just swap emoji → data-lucide

### 12. English pages
- Apply identical changes to all `en/` counterparts
- Same Lucide icons, same CSS, just translated text

## Implementation Order

1. Create `luxury-core.css` with all design tokens and utility classes
2. Update `header.html` + `header-en.html` (CDN script, link CSS, lucide.createIcons)
3. Update `footer.html` + `footer-en.html` (icon replacements, lucide.createIcons)
4. Refactor public views in dependency order:
   a. home (ES/EN)
   b. lacasa / casa (ES/EN)
   c. tracking (ES/EN)
   d. reservas (ES/EN)
   e. contacto (ES/EN)
   f. operaciones (ES/EN)
   g. entorno
   h. login, aviso-legal, politica-cookies
5. Refactor admin/CMS views
6. Clean up obsolete CSS (remove emoji-specific styles from page CSS files)
7. Compile and verify (`mvn clean compile`)

## Files Modified

### New
- `src/main/resources/static/css/luxury-core.css`

### Modified
- `src/main/resources/templates/fragments/header.html`
- `src/main/resources/templates/fragments/header-en.html`
- `src/main/resources/templates/fragments/footer.html`
- `src/main/resources/templates/fragments/footer-en.html`
- `src/main/resources/templates/home.html`
- `src/main/resources/templates/en/home.html`
- `src/main/resources/templates/lacasa.html`
- `src/main/resources/templates/en/casa.html`
- `src/main/resources/templates/tracking.html`
- `src/main/resources/templates/en/tracking.html`
- `src/main/resources/templates/reservas.html`
- `src/main/resources/templates/en/reservas.html`
- `src/main/resources/templates/contacto.html`
- `src/main/resources/templates/en/contacto.html`
- `src/main/resources/templates/operaciones.html`
- `src/main/resources/templates/en/operaciones.html`
- `src/main/resources/templates/entorno.html`
- `src/main/resources/templates/login.html`
- `src/main/resources/templates/cliente/login.html`
- `src/main/resources/templates/cliente/panel.html`
- `src/main/resources/templates/cms/dashboard.html`
- `src/main/resources/templates/cms/tracking.html`
- `src/main/resources/templates/cms/tracking-form.html`
- `src/main/resources/templates/cms/reservas.html`
- `src/main/resources/templates/cms/contactos.html`
- `src/main/resources/templates/cms/imagenes.html`
- `src/main/resources/templates/cms/textos.html`
- page-specific CSS files for icon/emoji cleanup

## Success Criteria

1. Zero emoji or childlike icons in any rendered view
2. All icons are SVG, stroke-width 1.5-1.75px, round caps/joins, amber `#c27803`
3. Every button, card, and interactive element has `transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1)`
4. Glassmorphism effects applied to hero, stat cards, and info panels
5. Typography uses executive letter-spacing for headings (0.06em–0.12em)
6. ES and EN views are identical in quality and styling
7. `mvn clean compile` passes without errors
