# Luxury Visual Transformation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace all emoji icons with Lucide SVG icons (CDN), apply luxury-grade visual styling (glassmorphism, amber accents, micro-interactions) across all 32 Thymeleaf templates and 22 CSS files.

**Architecture:** Single centralized `luxury-core.css` file with CSS custom properties + utility classes. Lucide CDN script in header fragments. Each page replaces emoji with `<i data-lucide="icon-name" class="luxury-icon"></i>` and calls `lucide.createIcons()` before `</body>`. All CSS transitions unified to `cubic-bezier(0.4, 0, 0.2, 1)`.

**Tech Stack:** Spring Boot 3.3.5 / Thymeleaf / Lucide CDN (unpkg.com/lucide@latest) / Vanilla CSS

## Global Constraints

- Zero emoji or childlike icons in rendered output
- All icons: SVG via CDN, `data-lucide` attribute, class `luxury-icon`
- Stroke-width: 1.5px, stroke-linecap: round, stroke-linejoin: round
- Accent color: `#c27803` (amber/terracotta)
- Transitions: `all 0.3s cubic-bezier(0.4, 0, 0.2, 1)` on buttons, cards, interactive elements
- ES and EN views must be identical in quality
- `mvn clean compile` must pass without errors

---

### Task 1: Create luxury-core.css

**Files:**
- Create: `src/main/resources/static/css/luxury-core.css`

**Interfaces:**
- Consumes: nothing
- Produces: CSS design tokens + utility classes used by all templates

- [ ] **Step 1: Write luxury-core.css with full design system**

```css
/* =============================================
   LUXURY CORE — Monteastur Envios Design System
   Stroke: 1.5px | Caps: round | Joins: round
   Accent: #c27803 | Transitions: cubic-bezier
   ============================================= */

:root {
  --color-accent: #c27803;
  --color-accent-dark: #9a5f02;
  --color-accent-light: #e8a33d;
  --color-dark: #1a1a2e;
  --color-charcoal: #2d2d44;
  --color-stone: #f8f6f3;
  --color-parchment: #f0ece6;
  --color-white: #ffffff;
  --text-primary: #1a1a2e;
  --text-secondary: #5a5a72;
  --text-muted: #8a8a9e;
  --text-on-accent: #ffffff;
  --icon-stroke-width: 1.5px;
  --icon-size: 24px;
  --icon-color: var(--color-accent);
  --transition-default: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  --transition-slow: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);
  --glass-bg: rgba(255, 255, 255, 0.6);
  --glass-border: rgba(255, 255, 255, 0.3);
  --glass-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
  --glass-blur: 12px;
}

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
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.btn-luxury {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 28px;
  background: var(--color-accent);
  color: var(--text-on-accent);
  border: none;
  border-radius: 8px;
  font-size: 0.95rem;
  font-weight: 600;
  letter-spacing: 0.04em;
  cursor: pointer;
  transition: var(--transition-default);
}

.btn-luxury:hover {
  background: var(--color-accent-dark);
  box-shadow: 0 4px 20px rgba(194, 120, 3, 0.35);
  transform: translateY(-1px);
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

.luxury-heading {
  letter-spacing: 0.06em;
  font-weight: 700;
  color: var(--color-dark);
}

.luxury-heading-lg {
  letter-spacing: 0.08em;
  font-weight: 800;
}

.luxury-heading-xl {
  letter-spacing: 0.12em;
  font-weight: 800;
  text-transform: uppercase;
}

.focus-glow:focus {
  outline: none;
  border-color: var(--color-accent);
  box-shadow: 0 0 0 3px rgba(194, 120, 3, 0.2);
}

.card-hover {
  transition: var(--transition-default);
}

.card-hover:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.1);
}

.border-accent-left {
  border-left: 3px solid var(--color-accent);
}

/* Smooth scroll for whole page */
html { scroll-behavior: smooth; }
```

- [ ] **Step 2: Verify file created and well-formed**

Run: `Test-Path "src/main/resources/static/css/luxury-core.css"`
Expected: True

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/static/css/luxury-core.css
git commit -m "feat: add luxury-core.css design system with tokens and utility classes"
```

---

### Task 2: Update Header + Footer Fragments with CDN Lucide

**Files:**
- Modify: `src/main/resources/templates/fragments/header.html`
- Modify: `src/main/resources/templates/fragments/header-en.html`
- Modify: `src/main/resources/templates/fragments/footer.html`
- Modify: `src/main/resources/templates/fragments/footer-en.html`

**Interfaces:**
- Consumes: `luxury-core.css` (Task 1)
- Produces: CDN Lucide script available in all pages, `lucide.createIcons()` called

- [ ] **Step 1: Read all 4 fragment files**

Run reads on header.html, header-en.html, footer.html, footer-en.html

- [ ] **Step 2: Add Lucide CDN script + luxury-core.css link to `<head>` of header.html**

Add inside `<head>`:
```html
<link rel="stylesheet" th:href="@{/css/luxury-core.css}">
<script src="https://unpkg.com/lucide@latest" defer></script>
```

- [ ] **Step 3: Add `lucide.createIcons()` before `</body>` in footer.html**

Add before `</body>`:
```html
<script defer>
  document.addEventListener('DOMContentLoaded', function() {
    if (typeof lucide !== 'undefined') lucide.createIcons();
  });
</script>
```

- [ ] **Step 4: Replace any emoji icons in header.html**

Replace nav/language emoji indicators with:
```html
<i data-lucide="globe" class="luxury-icon" style="width:16px;height:16px;"></i>
```

- [ ] **Step 5: Repeat steps 2-4 for header-en.html and footer-en.html**

Identical changes, English fragment files.

- [ ] **Step 6: Verify no syntax errors**

Run: `mvn clean compile -q`
Expected: BUILD SUCCESS (no errors)

- [ ] **Step 7: Commit**

```bash
git add src/main/resources/templates/fragments/
git commit -m "feat: add Lucide CDN and luxury-core.css to header/footer fragments"
```

---

### Task 3: Refactor Home Page (ES + EN)

**Files:**
- Modify: `src/main/resources/templates/home.html`
- Modify: `src/main/resources/templates/en/home.html`
- Modify: `src/main/resources/static/css/style.css` (minor cleanup)

**Icon map for home:**
| Location | Emoji | Lucide |
|---|---|---|
| Hero bullets | any emoji | `check` |
| 3-step cards icon 1 | 📦 | `package` |
| 3-step cards icon 2 | 🚚 | `truck` |
| 3-step cards icon 3 | 🛡️ / any | `shield-check` |
| Services grid items | any emoji | Per context: `package`, `truck`, `clock`, `users`, `shield-check` |
| Stats counters | any emoji | Remove icon, keep number + label |

- [ ] **Step 1: Read home.html and en/home.html**

- [ ] **Step 2: Replace hero section emojis with Lucide icons**

Replace any emoji/th elements with:
```html
<i data-lucide="check" class="luxury-icon luxury-icon-sm"></i>
```

- [ ] **Step 3: Replace 3-step process icons**

Find the 3 step cards. Replace content with:
```html
<div class="step-icon"><i data-lucide="package" class="luxury-icon luxury-icon-lg"></i></div>
<div class="step-icon"><i data-lucide="truck" class="luxury-icon luxury-icon-lg"></i></div>
<div class="step-icon"><i data-lucide="shield-check" class="luxury-icon luxury-icon-lg"></i></div>
```

- [ ] **Step 4: Replace services grid emojis**

Each service card icon replaced with appropriate Lucide.

- [ ] **Step 5: Add luxury classes to hero heading**

Wrap heading in `<h1 class="luxury-heading-xl">` and tagline in appropriate class.

- [ ] **Step 6: Apply glass-card to stat cards**

Add `class="glass-card"` to stat counter containers.

- [ ] **Step 7: Repeat steps 2-6 for en/home.html**

- [ ] **Step 8: Compile and verify**

Run: `mvn clean compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 9: Commit**

```bash
git add src/main/resources/templates/home.html src/main/resources/templates/en/home.html
git commit -m "feat: refactor home page icons to Lucide SVGs + luxury styling"
```

---

### Task 4: Refactor La Casa / Casa (ES + EN)

**Files:**
- Modify: `src/main/resources/templates/lacasa.html`
- Modify: `src/main/resources/templates/en/casa.html`

**Icon map for casa:**
| Location | Lucide |
|---|---|
| Carga cuidada card | `package` |
| Ruta coordinada card | `truck` |
| Atención cercana card | `shield-check` |
| Route: Asturias node | `map-pin` |
| Route: Ruta atlántica | `ship` |
| Route: Paraguay node | `flag` |

- [ ] **Step 1: Read lacasa.html and en/casa.html**

- [ ] **Step 2: Replace 3 top service cards emojis with Lucide**

```html
<i data-lucide="package" class="luxury-icon luxury-icon-lg"></i>
<i data-lucide="truck" class="luxury-icon luxury-icon-lg"></i>
<i data-lucide="shield-check" class="luxury-icon luxury-icon-lg"></i>
```

- [ ] **Step 3: Replace route timeline emojis**

```html
<i data-lucide="map-pin" class="luxury-icon"></i>
<i data-lucide="ship" class="luxury-icon"></i>
<i data-lucide="flag" class="luxury-icon"></i>
```

- [ ] **Step 4: Add glass-card + card-hover to service cards**

- [ ] **Step 5: Refine typography (luxury-heading classes)**

- [ ] **Step 6: Repeat for en/casa.html**

- [ ] **Step 7: Compile and commit**

---

### Task 5: Refactor Tracking (ES + EN)

**Files:**
- Modify: `src/main/resources/templates/tracking.html`
- Modify: `src/main/resources/templates/en/tracking.html`

**Icon map for tracking:**
| Location | Lucide |
|---|---|
| Quick guide step 1 (Código) | `barcode` |
| Quick guide step 2 (Estado) | `compass` |
| Quick guide step 3 (Ayuda) | `headphones` |
| Search button | `search` |
| Route timeline: Asturias | `map-pin` |
| Route timeline: Ruta atlántica | `ship` |
| Route timeline: Vuelo | `plane` |
| Route timeline: Destino | `flag` |
| Shipment status badges | CSS colored dots (no icon) |
| Previous shipments status | CSS colored dots (no icon) |

- [ ] **Step 1: Read tracking.html and en/tracking.html**

- [ ] **Step 2: Replace quick guide 3-step emojis**

```html
<i data-lucide="barcode" class="luxury-icon luxury-icon-lg"></i>
<i data-lucide="compass" class="luxury-icon luxury-icon-lg"></i>
<i data-lucide="headphones" class="luxury-icon luxury-icon-lg"></i>
```

- [ ] **Step 3: Replace route timeline emojis**

Replace ⛴️ 📍 ✈️ 🏁 with corresponding Lucide icons.

- [ ] **Step 4: Style search bar with focus-glow class**

Add `class="focus-glow"` to search input.

- [ ] **Step 5: Replace emoji status badges with CSS-only dots**

Replace text emoji with:
```html
<span class="status-dot status-${status}"></span>
```

Add CSS to tracking-premium.css or tracking-resultado.css:
```css
.status-dot {
  display: inline-block;
  width: 10px; height: 10px;
  border-radius: 50%;
  margin-right: 6px;
}
.status-dot.status-en-transito { background: #c27803; }
.status-dot.status-entregado { background: #22c55e; }
.status-dot.status-pendiente { background: #94a3b8; }
.status-dot.status-cancelado { background: #ef4444; }
```

- [ ] **Step 6: Apply glass-card to result container**

- [ ] **Step 7: Repeat for en/tracking.html**

- [ ] **Step 8: Compile and commit**

---

### Task 6: Refactor Reservas (ES + EN)

**Files:**
- Modify: `src/main/resources/templates/reservas.html`
- Modify: `src/main/resources/templates/en/reservas.html`

**Icon map for reservas:**
| Location | Lucide |
|---|---|
| Step indicator 1 | `calendar-days` |
| Step indicator 2 | `file-text` |
| Step indicator 3 | `check` |
| Route: Origen | `map-pin` |
| Route: Destino | `map-pin` |
| Metrics cards | `package`, `truck`, `clock`, `users` |
| Submit button | `send` |

- [ ] **Step 1: Read reservas.html and en/reservas.html**

- [ ] **Step 2: Replace multi-step indicator emojis**

```html
<i data-lucide="calendar-days" class="luxury-icon"></i>
<i data-lucide="file-text" class="luxury-icon"></i>
<i data-lucide="check" class="luxury-icon"></i>
```

- [ ] **Step 3: Replace route visual emojis**

```html
<i data-lucide="map-pin" class="luxury-icon"></i> Origen
<i data-lucide="arrow-right" class="luxury-icon"></i>
<i data-lucide="map-pin" class="luxury-icon"></i> Destino
```

- [ ] **Step 4: Replace metrics card emojis with Lucide**

- [ ] **Step 5: Style form inputs with focus-glow**

- [ ] **Step 6: Apply glass-card to form container and metrics cards**

- [ ] **Step 7: Refine button with btn-luxury**

- [ ] **Step 8: Repeat for en/reservas.html**

- [ ] **Step 9: Compile and commit**

---

### Task 7: Refactor Contacto (ES + EN)

**Files:**
- Modify: `src/main/resources/templates/contacto.html`
- Modify: `src/main/resources/templates/en/contacto.html`

**Icon map:**
| Location | Lucide |
|---|---|
| Dirección card | `building-2` |
| Teléfono card | `phone-call` |
| Email card | `mail` |
| Horario card | `clock` |
| Submit button | `send` |

- [ ] **Step 1: Read contacto.html and en/contacto.html**

- [ ] **Step 2: Replace info card emojis**

```html
<i data-lucide="building-2" class="luxury-icon luxury-icon-lg"></i>
<i data-lucide="phone-call" class="luxury-icon luxury-icon-lg"></i>
<i data-lucide="mail" class="luxury-icon luxury-icon-lg"></i>
<i data-lucide="clock" class="luxury-icon luxury-icon-lg"></i>
```

- [ ] **Step 3: Apply glass-card to info cards**

- [ ] **Step 4: Style form with focus-glow inputs + btn-luxury submit**

- [ ] **Step 5: Add border-accent-left to office hours table**

- [ ] **Step 6: Repeat for en/contacto.html**

- [ ] **Step 7: Compile and commit**

---

### Task 8: Refactor Operaciones (ES + EN)

**Files:**
- Modify: `src/main/resources/templates/operaciones.html`
- Modify: `src/main/resources/templates/en/operaciones.html`

**Icon map:**
| Location | Lucide |
|---|---|
| Carga consolidada | `package` |
| Recepción | `clipboard-check` |
| Entrega final | `truck` |
| Aduana | `file-check` |
| Logística inversa | `refresh-cw` |
| Stats section | Remove emoji, keep number+label |

- [ ] **Step 1: Read operaciones.html and en/operaciones.html**

- [ ] **Step 2: Replace service card emojis with Lucide**

- [ ] **Step 3: Apply glass-card to service cards + card-hover**

- [ ] **Step 4: Repeat for en/operaciones.html**

- [ ] **Step 5: Compile and commit**

---

### Task 9: Refactor Entorno + Login + Legal Pages (ES)

**Files:**
- Modify: `src/main/resources/templates/entorno.html`
- Modify: `src/main/resources/templates/login.html`
- Modify: `src/main/resources/templates/cliente/login.html`
- Modify: `src/main/resources/templates/cliente/panel.html`
- Modify: `src/main/resources/templates/aviso-legal.html`
- Modify: `src/main/resources/templates/politica-cookies.html`
- Modify: `src/main/resources/templates/en/aviso-legal.html`
- Modify: `src/main/resources/templates/en/politica-cookies.html`

**Icon maps:**
- entorno: `leaf`, `users`, `award`, `heart`
- login: `lock`, `user`, `log-in`
- cliente/panel: `package`, `truck`, `clock`, `user`

- [ ] **Step 1: Read all 8 files**

- [ ] **Step 2: Replace entorno emojis with Lucide**

```html
<i data-lucide="leaf" class="luxury-icon luxury-icon-lg"></i>
<i data-lucide="users" class="luxury-icon luxury-icon-lg"></i>
<i data-lucide="award" class="luxury-icon luxury-icon-lg"></i>
<i data-lucide="heart" class="luxury-icon luxury-icon-lg"></i>
```

- [ ] **Step 3: Replace login emojis with Lucide**

```html
<i data-lucide="lock" class="luxury-icon"></i>
<i data-lucide="user" class="luxury-icon"></i>
<i data-lucide="log-in" class="luxury-icon"></i>
```

- [ ] **Step 4: Apply glass-card to login form container**

- [ ] **Step 5: Style buttons with btn-luxury**

- [ ] **Step 6: Replace cliente/panel emojis**

- [ ] **Step 7: Legal pages: remove any decorative emoji**

- [ ] **Step 8: Compile and commit**

---

### Task 10: Refactor Admin / CMS Views

**Files:**
- Modify: `src/main/resources/templates/cms/dashboard.html`
- Modify: `src/main/resources/templates/cms/tracking.html`
- Modify: `src/main/resources/templates/cms/tracking-form.html`
- Modify: `src/main/resources/templates/cms/reservas.html`
- Modify: `src/main/resources/templates/cms/contactos.html`
- Modify: `src/main/resources/templates/cms/imagenes.html`
- Modify: `src/main/resources/templates/cms/textos.html`
- Modify: `src/main/resources/templates/fragments/admin-sidebar.html`

- [ ] **Step 1: Read all 8 admin files**

- [ ] **Step 2: Replace admin sidebar emoji nav icons**

```html
<i data-lucide="layout-dashboard" class="luxury-icon" style="width:18px;height:18px;color:var(--color-accent);"></i>
<i data-lucide="package-search" class="luxury-icon" style="width:18px;height:18px;"></i>
<i data-lucide="calendar-check" class="luxury-icon" style="width:18px;height:18px;"></i>
<i data-lucide="message-square" class="luxury-icon" style="width:18px;height:18px;"></i>
<i data-lucide="image" class="luxury-icon" style="width:18px;height:18px;"></i>
<i data-lucide="file-text" class="luxury-icon" style="width:18px;height:18px;"></i>
```

- [ ] **Step 3: Replace dashboard stat card emojis**

```html
<i data-lucide="package" class="luxury-icon luxury-icon-lg"></i>
<i data-lucide="message-circle" class="luxury-icon luxury-icon-lg"></i>
<i data-lucide="calendar-check" class="luxury-icon luxury-icon-lg"></i>
```

- [ ] **Step 4: Replace CMS tracking page emojis**

- [ ] **Step 5: Replace CMS reservas page emojis**

- [ ] **Step 6: Replace CMS contactos status emojis with CSS dots**

- [ ] **Step 7: Replace CMS imagenes page emojis**

- [ ] **Step 8: Replace CMS textos page emojis**

- [ ] **Step 9: Compile and commit**

---

### Task 11: Final Cleanup, Compile & Verify

- [ ] **Step 1: Full compile check**

Run: `mvn clean compile 2>&1`
Expected: BUILD SUCCESS, zero errors

- [ ] **Step 2: Verify no emoji remnants in any template**

```bash
rg '[📦🚚📍✈️⛴️🏁🛡️🔍✓📅📄📞📧🏢🧑‍💻🔖🧭🗺️✚🕐👥🌱⚠️✅❌❗❓⭐🔥💯♻️⚡️➕➖❎💼🔗🎯💡🔑💪🎉👏🙌🤝✋👉👈👆👇👀🧐💬🗨️📝✏️🔧⚙️🔄📋📌🎨🖼️]' src/main/resources/templates/
```
Expected: no matches (or only matches in CDN/defer scripts)

- [ ] **Step 3: Verify all data-lucide attributes match Lucide icon names**

Check that no `data-lucide="nonExistentIcon"` exists (spot-check known names).

- [ ] **Step 4: Check all templates include luxury-core.css**

```bash
rg 'luxury-core.css' src/main/resources/templates/
```
Expected: at least one match per header fragment (header.html, header-en.html)

- [ ] **Step 5: Final commit**

```bash
git add -A
git commit -m "feat: complete luxury visual transformation — Lucide icons, glassmorphism, premium styling"
git push origin feature/seguimiento-premium
```
