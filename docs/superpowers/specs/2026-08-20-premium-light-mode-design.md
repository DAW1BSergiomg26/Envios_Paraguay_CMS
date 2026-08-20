# Design: Premium Light Mode + Toggle Fix + Logo Ghost

**Date:** 2026-08-20  
**Scope:** CSS (`design-system.css`), JS (`theme-toggle.js`, `menu-cookie.js`), Thymeleaf fragments (`header.html`, `header-en.html`)  
**Status:** Approved — ready for implementation

---

## Problem Statement

Three issues degrade the light-mode experience:

1. **Toggle non-functional in Spanish routes** — The theme toggle button works on `/en/*` pages but fails on `/es` (root) pages.
2. **Logo "VI" ghost** — The white "VI" syllable in "MONTEASTUR ENVIOS" disappears entirely against the white light-mode background.
3. **Flat, lifeless light mode** — The current light mode is plain white with no depth, no brand accents, and no interactive delight.

Additionally, a latent CSS bug was discovered: `--color-accent` is referenced 15 times but never defined (the actual variable is `--accent-color`).

---

## Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Accent color for light mode | `#d4762a` (corporate brand) | Official brand color, warm and earthy, already used in legacy banner CSS |
| Logo fix scope | `.tri-blanco` only | Orange "MONTEASTUR" text is already visible on white; only the white "VI" needs help |
| Toggle fix approach | Add `id="theme-toggle"` to both headers + keep event delegation | Belt-and-suspenders; JS already targets `.btn-theme-toggle` via delegation |
| `--color-accent` bug | Alias to `--accent-color` | Fixes 15 broken accent references without changing usage patterns |

---

## TAREA 1: Theme Toggle Fix

### Root Cause
Both `fragments/header.html` (ES, line 36) and `fragments/header-en.html` (EN, line 25) have identical button markup with `class="btn-theme-toggle"` and identical JS includes. The previous `theme-toggle.js` (before rewrite) may have had a narrower selector. The current rewrite already uses event delegation on `.btn-theme-toggle`.

### Changes

**`fragments/header.html` line 36:**
```html
<!-- BEFORE -->
<button type="button" class="btn-theme-toggle" ...>

<!-- AFTER -->
<button type="button" id="theme-toggle" class="btn-theme-toggle" ...>
```

**`fragments/header-en.html` line 25:**
```html
<!-- BEFORE -->
<button type="button" class="btn-theme-toggle" ...>

<!-- AFTER -->
<button type="button" id="theme-toggle" class="btn-theme-toggle" ...>
```

**`theme-toggle.js`** — No changes needed. Already targets `#theme-toggle`, `.theme-toggle-btn`, AND `.btn-theme-toggle` via `getAllButtons()`. Event delegation on `document` ensures robustness even if elements load late.

---

## TAREA 2: Logo "VI" Ghost Fix

### Root Cause
The "VI" text uses `<span class="tri-blanco">VI</span>`. CSS defines `.tri-blanco { color: var(--tri-blanco) !important; }` which resolves to `#FFFFFF`. In light mode, white text on a white/light background is invisible. The `.logo-vi` class exists in CSS (line 1360) but is dead code — never used in any template.

### Changes

**`design-system.css`** — Add to the light-mode overrides section:
```css
html.light-mode .tri-blanco {
    color: #ffffff !important;
    text-shadow: 
        0px 1px 2px rgba(0, 0, 0, 0.5),
        0px 0px 4px rgba(0, 0, 0, 0.25),
        0px 2px 8px rgba(0, 0, 0, 0.15);
    -webkit-text-stroke: 0.3px rgba(0, 0, 0, 0.15);
}
```

Effect: The "VI" stays white but gains a soft dark halo + stroke that makes it pop against any light background. Elegant, not heavy-handed.

---

## TAREA 3: Premium Light Mode Design

### 3a. Orange Accent System (`#d4762a`)

Update the light-mode CSS variable block to use the corporate brand color:
```css
:root[data-theme="light"], html.light-mode, ... {
    --accent-color: #d4762a;           /* was #c2410c */
    --accent-color-hover: #b8652a;     /* was #9a3412 */
    --accent-color-active: #9c5520;    /* was #7c2d12 */
    --accent-glow: rgba(212, 118, 42, 0.2);
    --accent-paraguay-glow: rgba(212, 118, 42, 0.2);
    --accent-paraguay-glow-strong: rgba(212, 118, 42, 0.35);
}
```

Fix the `--color-accent` bug:
```css
:root {
    --color-accent: var(--accent-color);  /* NEW — aliases 15 broken references */
}
```

### 3b. Nav Active Border

```css
html.light-mode #nav-principal a.active,
html.light-mode #nav-principal a:hover {
    border-bottom-color: var(--accent-color) !important;
    color: var(--accent-color) !important;
}
```

### 3c. Card Glassmorphism + Depth

Replace hard borders with layered shadows on cards/containers in light mode:
```css
html.light-mode .glass-card,
html.light-mode .card,
html.light-mode .container-box,
html.light-mode .panel,
html.light-mode .box {
    border: 1px solid rgba(0, 0, 0, 0.04) !important;
    box-shadow: 
        0 1px 2px rgba(0, 0, 0, 0.03),
        0 4px 12px rgba(0, 0, 0, 0.04),
        0 12px 28px rgba(0, 0, 0, 0.03) !important;
}
```

### 3d. Hover Interactions (Cards + Buttons)

```css
html.light-mode .glass-card,
html.light-mode .card,
html.light-mode .container-box,
html.light-mode .btn-luxury,
html.light-mode .btn-primary {
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

html.light-mode .glass-card:hover,
html.light-mode .card:hover,
html.light-mode .container-box:hover {
    transform: translateY(-3px);
    box-shadow: 
        0 4px 8px rgba(0, 0, 0, 0.06),
        0 12px 24px rgba(212, 118, 42, 0.08),
        0 20px 40px rgba(0, 0, 0, 0.04) !important;
    border-color: rgba(212, 118, 42, 0.15) !important;
}

html.light-mode .btn-luxury:hover,
html.light-mode .btn-primary:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 20px rgba(212, 118, 42, 0.25) !important;
}
```

### 3e. Warm Background

```css
:root[data-theme="light"], html.light-mode, ... {
    --bg-body: #f9fafb;          /* was #f8fafc — warmer */
    --bg-body-gradient: linear-gradient(180deg, #ffffff 0%, #f9fafb 50%, #f5f6f8 100%);
}
```

This creates subtle depth: pure white cards float against a slightly warm-gray body.

---

## Files Modified

| File | Change |
|------|--------|
| `templates/fragments/header.html` | Add `id="theme-toggle"` to button (line 36) |
| `templates/fragments/header-en.html` | Add `id="theme-toggle"` to button (line 25) |
| `static/js/theme-toggle.js` | No changes (already correct) |
| `static/js/menu-cookie.js` | No changes (already correct) |
| `static/css/design-system.css` | Fix `--color-accent` alias, update light-mode accent to `#d4762a`, add `.tri-blanco` light-mode rule, add glassmorphism shadows, add hover transforms, warm background |

---

## Verification

1. `mvn clean compile` — must pass
2. Visual test: toggle theme on `/` (ES) and `/en` (EN) — both must work
3. Visual test: logo "VI" must be white with visible shadow in light mode
4. Visual test: cards float with layered shadows, hover lifts with orange glow
5. Visual test: nav active border is orange, buttons have orange hover glow
