# Premium Light Mode + Toggle Fix + Logo Ghost — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix the theme toggle for Spanish routes, make the logo "VI" visible in light mode, and upgrade the light-mode design to a premium, interactive, vibrant experience with corporate orange accents.

**Architecture:** Changes span 3 layers: Thymeleaf templates (button IDs), CSS design system (light-mode overrides, accent system, glassmorphism), and JS (no changes needed — already correct). All CSS changes go into a single file. No new files created.

**Tech Stack:** Thymeleaf fragments, CSS custom properties, vanilla JS event delegation, Lucide icons.

## Global Constraints

- Java 25, Spring Boot 3.5.16, Maven at `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd`
- `JAVA_HOME` = `C:\Users\astur\.jdks\openjdk-25.0.2` (must set per PowerShell session)
- Corporate brand color: `#d4762a`
- No Lombok. Inyección por constructor. Cero `@Autowired` en campos.
- CSS file: `src/main/resources/static/css/design-system.css` (single stylesheet, ~6462 lines)
- JS files: `src/main/resources/static/js/theme-toggle.js`, `menu-cookie.js`
- Thymeleaf fragments: `templates/fragments/header.html` (ES), `templates/fragments/header-en.html` (EN)
- Design spec: `docs/superpowers/specs/2026-08-20-premium-light-mode-design.md`

---

### Task 1: Fix Theme Toggle Button IDs in Thymeleaf Headers

**Files:**
- Modify: `src/main/resources/templates/fragments/header.html:36`
- Modify: `src/main/resources/templates/fragments/header-en.html:25`

**Interfaces:**
- Consumes: Nothing (first task)
- Produces: Both header fragments expose `id="theme-toggle"` on the theme button, enabling belt-and-suspenders selector coverage alongside the existing `.btn-theme-toggle` class

- [ ] **Step 1: Add `id="theme-toggle"` to Spanish header button**

In `src/main/resources/templates/fragments/header.html`, line 36, change:

```html
<button type="button" class="btn-theme-toggle" aria-label="Activar modo claro" title="Activar modo claro">
```

to:

```html
<button type="button" id="theme-toggle" class="btn-theme-toggle" aria-label="Activar modo claro" title="Activar modo claro">
```

- [ ] **Step 2: Add `id="theme-toggle"` to English header button**

In `src/main/resources/templates/fragments/header-en.html`, line 25, change:

```html
<button type="button" class="btn-theme-toggle" aria-label="Activar modo claro" title="Activar modo claro">
```

to:

```html
<button type="button" id="theme-toggle" class="btn-theme-toggle" aria-label="Activar modo claro" title="Activar modo claro">
```

- [ ] **Step 3: Verify JS already targets all selectors**

Open `src/main/resources/static/js/theme-toggle.js`. Confirm the `getAllButtons()` function (around line 38) includes:
```js
var selectors = ['#theme-toggle', '.theme-toggle-btn', '.btn-theme-toggle'];
```
No JS changes needed — this was already correct from the prior rewrite.

- [ ] **Step 4: Build to verify no breakage**

Run: `$env:JAVA_HOME = "C:\Users\astur\.jdks\openjdk-25.0.2"; & "C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd" -q clean compile -f "C:\Users\astur\Desktop\Envios_Paraguay_CMS\pom.xml"`
Expected: BUILD SUCCESS (only JDK 25 warnings)

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/templates/fragments/header.html src/main/resources/templates/fragments/header-en.html
git commit -m "fix: add id='theme-toggle' to both ES/EN header buttons for belt-and-suspenders selector coverage"
```

---

### Task 2: Fix Logo "VI" Ghost in Light Mode

**Files:**
- Modify: `src/main/resources/static/css/design-system.css` (append to light-mode overrides section at end of file)

**Interfaces:**
- Consumes: Existing `.tri-blanco` class (used in both `header.html:28` and `header-en.html:17`)
- Produces: `.tri-blanco` text remains white but gains visible shadow + stroke in light mode

- [ ] **Step 1: Add `.tri-blanco` light-mode override**

At the end of `src/main/resources/static/css/design-system.css`, inside the light-mode overrides section (after the existing `html.light-mode .trust-logos-premium` rule), add:

```css
/* --- Logo VI ghost fix: white text with elegant halo in light mode --- */
html.light-mode .tri-blanco {
    color: #ffffff !important;
    text-shadow:
        0px 1px 2px rgba(0, 0, 0, 0.5),
        0px 0px 4px rgba(0, 0, 0, 0.25),
        0px 2px 8px rgba(0, 0, 0, 0.15);
    -webkit-text-stroke: 0.3px rgba(0, 0, 0, 0.15);
}
```

- [ ] **Step 2: Build to verify**

Run: `$env:JAVA_HOME = "C:\Users\astur\.jdks\openjdk-25.0.2"; & "C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd" -q clean compile -f "C:\Users\astur\Desktop\Envios_Paraguay_CMS\pom.xml"`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/static/css/design-system.css
git commit -m "fix: add text-shadow to .tri-blanco in light mode so logo VI stays visible on white backgrounds"
```

---

### Task 3: Fix `--color-accent` Undefined Variable Bug

**Files:**
- Modify: `src/main/resources/static/css/design-system.css` (line ~217, the legacy `:root` block)

**Interfaces:**
- Consumes: Existing `--accent-color` variable (defined in both dark and light theme blocks)
- Produces: `--color-accent` aliased to `--accent-color`, fixing 15 broken accent references (`.luxury-icon-accent`, `.btn-luxury`, `.btn-luxury-outline`, `.focus-glow`, etc.)

- [ ] **Step 1: Add `--color-accent` alias to legacy `:root` block**

In `src/main/resources/static/css/design-system.css`, find the legacy `:root` block (around line 217-238). Add this line inside it:

```css
:root {
  --primario-monte: #0d2319;
  --primario-ict1: #e67e22;
  --primario-ict2: #c8102e;
  --primario-ict3: #ffffff;
  --primario-ict4: #0047ab;
  --monte-amarillo: #E67E22;
  --tri-rojo: #C8102E;
  --tri-blanco: #FFFFFF;
  --tri-azul: #0047AB;
  --btn-text: #0F281E;
  --btn-text-dark: #071510;
  --fuente-sistema: "Roboto", sans-serif;
  --color-accent: var(--accent-color);  /* NEW: aliases 15 broken references */
}
```

- [ ] **Step 2: Build to verify**

Run: `$env:JAVA_HOME = "C:\Users\astur\.jdks\openjdk-25.0.2"; & "C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd" -q clean compile -f "C:\Users\astur\Desktop\Envios_Paraguay_CMS\pom.xml"`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/static/css/design-system.css
git commit -m "fix: alias --color-accent to --accent-color to fix 15 broken accent references"
```

---

### Task 4: Upgrade Light-Mode Accent System to Corporate `#d4762a`

**Files:**
- Modify: `src/main/resources/static/css/design-system.css` (light-mode variable block, lines ~100-171)

**Interfaces:**
- Consumes: Existing light-mode `:root[data-theme="light"]` variable block
- Produces: Updated accent variables using corporate brand color `#d4762a`

- [ ] **Step 1: Update light-mode accent variables**

In `src/main/resources/static/css/design-system.css`, find the light-mode variable block (starts around line 100 with `:root[data-theme="light"], html.light-mode, ...`). Replace the accent-related lines:

```css
/* BEFORE */
--accent-color: #c2410c;
--accent-paraguay: #c2410c;
--accent-color-hover: #9a3412;
--accent-paraguay-hover: #9a3412;
--accent-color-active: #7c2d12;
--accent-paraguay-active: #7c2d12;
--accent-glow: rgba(230, 126, 34, 0.18);
--accent-paraguay-glow: rgba(230, 126, 34, 0.18);
--accent-paraguay-glow-strong: rgba(230, 126, 34, 0.3);

/* AFTER */
--accent-color: #d4762a;
--accent-paraguay: #d4762a;
--accent-color-hover: #b8652a;
--accent-paraguay-hover: #b8652a;
--accent-color-active: #9c5520;
--accent-paraguay-active: #9c5520;
--accent-glow: rgba(212, 118, 42, 0.2);
--accent-paraguay-glow: rgba(212, 118, 42, 0.2);
--accent-paraguay-glow-strong: rgba(212, 118, 42, 0.35);
```

- [ ] **Step 2: Build to verify**

Run: `$env:JAVA_HOME = "C:\Users\astur\.jdks\openjdk-25.0.2"; & "C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd" -q clean compile -f "C:\Users\astur\Desktop\Envios_Paraguay_CMS\pom.xml"`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/static/css/design-system.css
git commit -m "feat: upgrade light-mode accent to corporate brand #d4762a"
```

---

### Task 5: Premium Light-Mode Glassmorphism, Hover Interactions, and Warm Background

**Files:**
- Modify: `src/main/resources/static/css/design-system.css` (light-mode overrides section at end of file)

**Interfaces:**
- Consumes: Existing `--accent-color: #d4762a` from Task 4, existing `.tri-blanco` override from Task 2
- Produces: Complete premium light-mode experience with layered shadows, hover transforms, warm background, and orange accent borders

- [ ] **Step 1: Update light-mode body background to warmer tone**

In the light-mode variable block, change:

```css
/* BEFORE */
--bg-body: #f8fafc;
--bg-body-asturias: #f8fafc;
--bg-body-gradient: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);

/* AFTER */
--bg-body: #f9fafb;
--bg-body-asturias: #f9fafb;
--bg-body-gradient: linear-gradient(180deg, #ffffff 0%, #f9fafb 50%, #f5f6f8 100%);
```

- [ ] **Step 2: Add nav active border accent**

At the end of `design-system.css`, in the light-mode overrides section, add:

```css
/* --- Nav active border: corporate orange accent --- */
html.light-mode #nav-principal a.active,
html.light-mode #nav-principal a:hover {
    border-bottom-color: var(--accent-color) !important;
    color: var(--accent-color) !important;
}
```

- [ ] **Step 3: Add glassmorphism card shadows**

In the light-mode overrides section, REPLACE the existing card/container overrides with:

```css
/* --- Glass cards: layered shadows for floating depth --- */
html.light-mode .glass-card,
html.light-mode .card,
html.light-mode .container-box,
html.light-mode .panel,
html.light-mode .box {
    background-color: var(--card-bg) !important;
    color: var(--text-main) !important;
    border: 1px solid rgba(0, 0, 0, 0.04) !important;
    box-shadow:
        0 1px 2px rgba(0, 0, 0, 0.03),
        0 4px 12px rgba(0, 0, 0, 0.04),
        0 12px 28px rgba(0, 0, 0, 0.03) !important;
}
```

- [ ] **Step 4: Add hover transform + orange glow**

In the light-mode overrides section, add after the glass-card block:

```css
/* --- Hover: lift + intensify shadow with orange glow --- */
html.light-mode .glass-card,
html.light-mode .card,
html.light-mode .container-box {
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

/* --- Button hover: lift + orange glow --- */
html.light-mode .btn-luxury,
html.light-mode .btn-primary {
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

html.light-mode .btn-luxury:hover,
html.light-mode .btn-primary:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 20px rgba(212, 118, 42, 0.25) !important;
}
```

- [ ] **Step 5: Build to verify**

Run: `$env:JAVA_HOME = "C:\Users\astur\.jdks\openjdk-25.0.2"; & "C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd" -q clean compile -f "C:\Users\astur\Desktop\Envios_Paraguay_CMS\pom.xml"`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/static/css/design-system.css
git commit -m "feat: premium light-mode — glassmorphism shadows, hover transforms, warm background, orange accents"
```

---

### Task 6: Final Verification and Cleanup

**Files:**
- Verify: All files modified in Tasks 1-5

**Interfaces:**
- Consumes: All prior tasks
- Produces: Confirmed working state across both languages

- [ ] **Step 1: Full compile**

Run: `$env:JAVA_HOME = "C:\Users\astur\.jdks\openjdk-25.0.2"; & "C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd" -q clean compile -f "C:\Users\astur\Desktop\Envios_Paraguay_CMS\pom.xml"`
Expected: BUILD SUCCESS

- [ ] **Step 2: Verify CSS changes are syntactically valid**

Run: `$env:JAVA_HOME = "C:\Users\astur\.jdks\openjdk-25.0.2"; & "C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd" -q clean package -f "C:\Users\astur\Desktop\Envios_Paraguay_CMS\pom.xml" -DskipTests`
Expected: BUILD SUCCESS (packages static resources)

- [ ] **Step 3: Review git log**

Run: `git log --oneline -8`
Expected: 6 new commits (Tasks 1-5 + design doc) on top of prior work

- [ ] **Step 4: No uncommitted changes**

Run: `git status`
Expected: clean working tree
