# Theme Switcher Dark/Light + Pulido Visual + Páginas Admin — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Añadir un selector de tema Oscuro/Claro dinámico (anti-FOUC, localStorage, tokens canónicos) en todo el ecosistema (marketing, tracking/cliente Tailwind y admin), pulir vistas clave con motion a 60fps, y crear dos páginas admin nuevas funcionales (`/admin/imports`, `/admin/documentos`), con la suite en verde y push a GitHub.

**Architecture:** Enfoque A "Token Bridge": un `design-system.css` canónico con tokens dark/light; las variables existentes (`luxury-core.css`, `admin-theme.css`) se remapean a los canónicos para que el flip de `data-theme` en `<html>` cambie toda la cascada automáticamente; capas `:root[data-theme="light"]` al final de `admin.css`/`luxury-core.css` para vencer hardcodes; overlay en plantillas Tailwind sustituyendo clases dark por arbitrarias `var()`. Las páginas admin nuevas se renderizan server-side por `AdminController` y consumen los APIs existentes (`/api/v1/admin/imports`, `/api/v1/admin/documentos`).

**Tech Stack:** Spring Boot 3.3.5 · Thymeleaf · Java 17 · CSS puro (tokens + override) · Tailwind Play CDN · Lucide Icons · MockMvc + AssertJ (tests) · MySQL 8 + Redis (suite Docker).

## Global Constraints

- **CERO Lombok** — Java puro; constructor con `private final`, getters/setters manuales.
- **Inyección por constructor** en servicios/repos/controllers; nunca `@Autowired` en campos.
- **Thymeleaf intacto** — prohibido alterar/eliminar atributos `th:*`, `${...}`, `@{...}` ni rutas de controladores existentes; solo se editan atributos `class`.
- **Manejo de errores real** — `ErrorDto(Instant.now().toString(), status, message)`; vistas de error vía `mvcError(request, model, status, error, message)`; no tocar `GlobalExceptionHandler`.
- **Cascada CSS** — overrides al FINAL del fichero; capa light admin al final de `admin.css` (tras la "DARK FORM FOCUS OVERRIDE").
- **Tokens canónicos** — exactos: dark `--bg-body:#09090b`, `--bg-surface:#121215`, `--bg-card:#18181b`, `--text-primary:#f4f4f5`, `--text-secondary:#a1a1aa`, `--border-subtle:rgba(255,255,255,0.08)`, `--accent-color:#d4762a`, `--accent-glow:rgba(212,118,42,0.25)`, `--status-success:#4ade80`; light `--bg-body:#f8fafc`, `--bg-surface:#ffffff`, `--bg-card:#ffffff`, `--text-primary:#0f172a`, `--text-secondary:#64748b`, `--border-subtle:rgba(0,0,0,0.08)`, `--accent-color:#d4762a`, `--accent-glow:rgba(212,118,42,0.15)`, `--status-success:#16a34a`.
- **Tema por defecto** — siempre Oscuro (`theme = localStorage.getItem('theme') || 'dark'`; NO `prefers-color-scheme`).
- **Sin atajo de teclado** para el toggle.
- **60 fps** — animaciones solo `transform`/`opacity`; `@media (prefers-reduced-motion: reduce)` global.
- **Verificación** — NO existe `mvnw`: usar `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd` (local) y la suite Docker de AGENTS.md (MySQL+Redis).
- **Commit final** — `feat(ui): add dynamic dark/light theme switcher with anti-FOUC and motion polish`; push a `origin/main`.
- **CSRF** — exento para `/api/**` (SecurityConfig). Los fetch de imports NO llevan token CSRF; solo cookie de sesión.

---

## File Structure

**Nuevos (código completo):**
- `src/main/resources/static/css/design-system.css` — tokens canónicos dark/light + reduced-motion.
- `src/main/resources/static/css/theme-ui.css` — `.btn-theme-toggle`, pulso stepper, motion polish.
- `src/main/resources/static/js/theme-toggle.js` — toggle + persistencia + swap iconos Lucide.
- `src/main/resources/templates/cms/imports.html` — dropzone CSV + polling + modal errores.
- `src/main/resources/templates/cms/documentos.html` — etiquetas, manifiestos, auditoría.
- `src/test/java/com/monteastur/envios/controller/AdminControllerTest.java` — tests páginas admin.

**Modificados:**
- `fragments/public-head.html`, `fragments/header.html`, `fragments/header-en.html` — link tokens + theme-ui + js + anti-FOUC + `.btn-theme-toggle` en navbar.
- 9 heads admin (`login.html`, `cliente/login.html`, `cms/dashboard.html`, `cms/contactos.html`, `cms/textos.html`, `cms/reservas.html`, `cms/tracking.html`, `cms/tracking-form.html`, `cms/imagenes.html`) — mismos assets + anti-FOUC.
- `css/luxury-core.css` — remap `:root` + capa `:root[data-theme="light"]`.
- `css/admin/admin-theme.css` — remap `:root`.
- `css/admin.css` — capa light al final + CSS dropzone/modal/historial.
- `templates/fragments/admin-sidebar.html` — toggle + enlaces imports/documentos.
- `templates/login.html`, `templates/cliente/login.html` — toggle flotante.
- `templates/tracking-search.html`, `templates/tracking-result.html`, `templates/tracking-404.html`, `templates/cliente/panel.html` — sustitución clases Tailwind por vars.
- `controller/AdminController.java` — rutas `/admin/imports`, `/admin/documentos`.
- `service/batch/BatchImportPersistenceService.java` + `repository/BatchImportRepository.java` — `listarLotes()`.
- `cms/tracking.html` — menú contextual de acciones por fila.
- `test/.../PublicControllerTest.java`, `TrackingWebControllerTest.java`, `BatchImportPersistenceServiceTest.java` — tests nuevos.
- `docs/handoff.md` — registro final.

---

## Phase 1 — Sistema de tema

### Task 1: design-system.css + link en heads públicos

**Files:**
- Create: `src/main/resources/static/css/design-system.css`
- Modify: `src/main/resources/templates/fragments/public-head.html`, `fragments/header.html`, `fragments/header-en.html`
- Test: `src/test/java/com/monteastur/envios/controller/PublicControllerTest.java`

**Interfaces:**
- Consumes: nada (primer task).
- Produces: fichero `/css/design-system.css` (cargado primero en todos los heads); tokens usados por Tasks 2, 5, 6.

- [ ] **Step 1: Write the failing test**

Añade al final de `PublicControllerTest` (antes del cierre de clase):

```java
    @Test
    void themeAssets_marketingPage_hasDesignSystemCss() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/css/design-system.css")));
    }
```

Añade el import estático `org.hamcrest.Matchers.containsString` (junto a los imports de `MockMvcResultMatchers`).

- [ ] **Step 2: Run test to verify it fails**

Run: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd -q -Dtest=PublicControllerTest#themeAssets_marketingPage_hasDesignSystemCss test`
Expected: FAIL (content no contiene `/css/design-system.css`).

- [ ] **Step 3: Create `design-system.css`**

```css
/* =============================================
   DESIGN SYSTEM TOKENS — Envios Paraguay CMS
   Dark (Obsidian, default) / Light (Pristine Quartz)
   Fuente unica de verdad para el theme switcher.
   Cargar SIEMPRE primero en <head>.
   ============================================= */

/* Modo Oscuro (Predeterminado — Obsidian SaaS) */
:root,
:root[data-theme="dark"] {
    --bg-body: #09090b;
    --bg-surface: #121215;
    --bg-card: #18181b;
    --bg-card-glass: rgba(24, 24, 27, 0.75);
    --text-primary: #f4f4f5;
    --text-secondary: #a1a1aa;
    --text-muted: #71717a;
    --border-subtle: rgba(255, 255, 255, 0.08);
    --border-strong: rgba(255, 255, 255, 0.16);
    --accent-color: #d4762a;
    --accent-color-hover: #e08c3f;
    --accent-glow: rgba(212, 118, 42, 0.25);
    --text-on-accent: #ffffff;
    --status-success: #4ade80;
    --status-warning: #f0a830;
    --status-danger: #f87171;
    --status-info: #a5b4fc;
    --glass-bg: rgba(255, 255, 255, 0.03);
    --glass-bg-hover: rgba(255, 255, 255, 0.06);
    --glass-border: rgba(255, 255, 255, 0.1);
    --glass-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
    --glass-blur: 12px;
    --surface-header: rgba(9, 9, 11, 0.8);
    --surface-card: rgba(255, 255, 255, 0.03);
    --surface-card-strong: rgba(255, 255, 255, 0.06);
    --surface-card-hover: rgba(255, 255, 255, 0.08);
    --badge-info: #a5b4fc;
    --badge-violet: #c4b5fd;
    --badge-warning: #fcd34d;
    --badge-sky: #7dd3fc;
    --badge-success: #6ee7b7;
    --badge-neutral: #d4d4d8;
    --transition-default: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    --transition-slow: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);
}

/* Modo Claro (Pristine Quartz — Ultra Clean Enterprise) */
:root[data-theme="light"] {
    --bg-body: #f8fafc;
    --bg-surface: #ffffff;
    --bg-card: #ffffff;
    --bg-card-glass: rgba(255, 255, 255, 0.85);
    --text-primary: #0f172a;
    --text-secondary: #64748b;
    --text-muted: #94a3b8;
    --border-subtle: rgba(0, 0, 0, 0.08);
    --border-strong: rgba(0, 0, 0, 0.16);
    --accent-color: #d4762a;
    --accent-color-hover: #b8661f;
    --accent-glow: rgba(212, 118, 42, 0.15);
    --text-on-accent: #ffffff;
    --status-success: #16a34a;
    --status-warning: #d97706;
    --status-danger: #dc2626;
    --status-info: #6366f1;
    --glass-bg: rgba(255, 255, 255, 0.85);
    --glass-bg-hover: #ffffff;
    --glass-border: rgba(0, 0, 0, 0.08);
    --glass-shadow: 0 8px 32px rgba(15, 23, 42, 0.08);
    --glass-blur: 12px;
    --surface-header: rgba(248, 250, 252, 0.85);
    --surface-card: #ffffff;
    --surface-card-strong: #f1f5f9;
    --surface-card-hover: #e2e8f0;
    --badge-info: #4f46e5;
    --badge-violet: #7c3aed;
    --badge-warning: #b45309;
    --badge-sky: #0284c7;
    --badge-success: #059669;
    --badge-neutral: #64748b;
    --transition-default: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    --transition-slow: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);
}

/* Accesibilidad: respetar prefers-reduced-motion */
@media (prefers-reduced-motion: reduce) {
    *, *::before, *::after {
        animation-duration: 0.01ms !important;
        animation-iteration-count: 1 !important;
        transition-duration: 0.01ms !important;
        scroll-behavior: auto !important;
    }
}
```

- [ ] **Step 4: Añadir `<link>` de design-system.css PRIMERO en los 3 heads públicos**

En `public-head.html`, `header.html` y `header-en.html`, insertar justo después de `<meta name="viewport" ...>` y ANTES del primer `<link ...>` (en public-head, antes del preconnect a Google Fonts):

```html
    <link rel="stylesheet" href="/css/design-system.css">
```

- [ ] **Step 5: Run test to verify it passes**

Run: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd -q -Dtest=PublicControllerTest#themeAssets_marketingPage_hasDesignSystemCss test`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/static/css/design-system.css src/main/resources/templates/fragments/public-head.html src/main/resources/templates/fragments/header.html src/main/resources/templates/fragments/header-en.html src/test/java/com/monteastur/envios/controller/PublicControllerTest.java
git commit -m "feat(theme): design-system tokens dark/light + reduced motion, linked in public heads"
```

---

### Task 2: theme-ui.css + theme-toggle.js + links en heads públicos

**Files:**
- Create: `src/main/resources/static/css/theme-ui.css`
- Create: `src/main/resources/static/js/theme-toggle.js`
- Modify: `src/main/resources/templates/fragments/public-head.html`, `fragments/header.html`, `fragments/header-en.html` (mismos heads)
- Test: `src/test/java/com/monteastur/envios/controller/PublicControllerTest.java`

**Interfaces:**
- Consumes: tokens de design-system.css (Task 1).
- Produces: `/css/theme-ui.css`, `/js/theme-toggle.js`; `.btn-theme-toggle` (Task 3) usa theme-ui.css.

- [ ] **Step 1: Write the failing test**

Añade a `PublicControllerTest`:

```java
    @Test
    void themeAssets_marketingPage_hasToggleAssets() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/css/theme-ui.css")))
                .andExpect(content().string(containsString("/js/theme-toggle.js")));
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd -q -Dtest=PublicControllerTest#themeAssets_marketingPage_hasToggleAssets test`
Expected: FAIL.

- [ ] **Step 3: Create `theme-ui.css`**

```css
/* =============================================
   THEME UI — Boton toggle, pulso stepper, motion
   Envios Paraguay CMS (theme switcher)
   ============================================= */

/* ---- Theme toggle (estilo Vercel/Linear) ---- */
.btn-theme-toggle {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 38px;
    height: 38px;
    border-radius: 999px;
    background: var(--glass-bg);
    border: 1px solid var(--glass-border);
    color: var(--text-primary);
    cursor: pointer;
    flex-shrink: 0;
    transition: background-color 0.3s ease, border-color 0.3s ease, color 0.3s ease, transform 0.2s ease;
}

.btn-theme-toggle:hover {
    background: var(--glass-bg-hover);
    transform: translateY(-1px);
}

.btn-theme-toggle svg {
    transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.btn-theme-toggle.theme-rotating svg {
    transform: rotate(180deg);
}

.btn-theme-toggle:focus-visible {
    outline: none;
    box-shadow: 0 0 0 3px var(--accent-glow);
}

/* ---- Stepper pulse (tracking-result) ---- */
.step-pulse {
    position: relative;
}

.step-pulse::after {
    content: '';
    position: absolute;
    inset: -5px;
    border-radius: 999px;
    border: 2px solid var(--accent-color);
    opacity: 0.7;
    animation: step-pulse-ring 2s cubic-bezier(0.4, 0, 0.2, 1) infinite;
    pointer-events: none;
}

@keyframes step-pulse-ring {
    0%   { transform: scale(0.85); opacity: 0.7; }
    70%  { transform: scale(1.2);  opacity: 0; }
    100% { transform: scale(1.2);  opacity: 0; }
}

/* ---- Motion polish (60fps: solo transform/opacity) ---- */
.card,
.quick-action-card,
.dropzone,
.status-card,
.timeline-item {
    transition: transform 0.2s ease, box-shadow 0.3s ease,
                background-color 0.3s ease, border-color 0.3s ease, color 0.3s ease;
}
```

- [ ] **Step 4: Create `theme-toggle.js`**

```js
/* Theme Switcher — Envios Paraguay CMS */
(function () {
    'use strict';

    function currentTheme() {
        return document.documentElement.getAttribute('data-theme') || 'dark';
    }

    function syncIcon(theme) {
        var iconName = theme === 'dark' ? 'sun' : 'moon';
        var label = theme === 'dark' ? 'Activar modo claro' : 'Activar modo oscuro';
        document.querySelectorAll('.btn-theme-toggle').forEach(function (btn) {
            btn.setAttribute('aria-label', label);
            btn.setAttribute('title', label);
            var icon = btn.querySelector('[data-lucide]');
            if (icon) {
                icon.setAttribute('data-lucide', iconName);
            }
        });
        if (window.lucide) {
            lucide.createIcons();
        }
    }

    function toggleTheme() {
        var next = currentTheme() === 'dark' ? 'light' : 'dark';
        document.documentElement.setAttribute('data-theme', next);
        try {
            localStorage.setItem('theme', next);
        } catch (e) {
            /* almacenamiento no disponible */
        }
        syncIcon(next);
    }

    function bindButtons() {
        document.querySelectorAll('.btn-theme-toggle').forEach(function (btn) {
            btn.addEventListener('click', function () {
                btn.classList.add('theme-rotating');
                toggleTheme();
                setTimeout(function () {
                    btn.classList.remove('theme-rotating');
                }, 450);
            });
        });
    }

    document.addEventListener('DOMContentLoaded', function () {
        syncIcon(currentTheme());
        bindButtons();
    });
})();
```

- [ ] **Step 5: Añadir `<link theme-ui.css>` y `<script theme-toggle.js>` a los 3 heads públicos**

En `public-head.html`, `header.html`, `header-en.html`, añadir tras el link de design-system.css (y antes del resto de hojas):

```html
    <link rel="stylesheet" href="/css/theme-ui.css">
```

Y antes del cierre de `<head>` (tras el último `<script ...>`):

```html
    <script src="/js/theme-toggle.js" defer></script>
```

- [ ] **Step 6: Run test to verify it passes**

Run: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd -q -Dtest=PublicControllerTest#themeAssets_marketingPage_hasToggleAssets test`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add src/main/resources/static/css/theme-ui.css src/main/resources/static/js/theme-toggle.js src/main/resources/templates/fragments/public-head.html src/main/resources/templates/fragments/header.html src/main/resources/templates/fragments/header-en.html src/test/java/com/monteastur/envios/controller/PublicControllerTest.java
git commit -m "feat(theme): theme-ui css y theme-toggle.js vinculados en heads publicos"
```

---

### Task 3: Anti-FOUC inline en heads públicos + botón toggle en navbars

**Files:**
- Modify: `src/main/resources/templates/fragments/public-head.html`, `fragments/header.html`, `fragments/header-en.html`
- Test: `src/test/java/com/monteastur/envios/controller/PublicControllerTest.java`

**Interfaces:**
- Consumes: `theme-ui.css` (Task 2) para estilos de `.btn-theme-toggle`.
- Produces: markup `<button class="btn-theme-toggle">` en navbar público (Task 4 lo replica en sidebar admin).

- [ ] **Step 1: Write the failing test**

Añade a `PublicControllerTest`:

```java
    @Test
    void themeAssets_marketingPage_hasAntiFoucAndToggle() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("setAttribute('data-theme'")))
                .andExpect(content().string(containsString("btn-theme-toggle")));
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd -q -Dtest=PublicControllerTest#themeAssets_marketingPage_hasAntiFoucAndToggle test`
Expected: FAIL.

- [ ] **Step 3: Insertar anti-FOUC inline al PRINCIPIO del `<head>`** en `public-head.html`, `header.html`, `header-en.html`

Justo después de `<meta name="viewport" ...>` (y ANTES del link de design-system.css):

```html
    <script>
        (function () {
            try {
                var theme = localStorage.getItem('theme') || 'dark';
                document.documentElement.setAttribute('data-theme', theme);
            } catch (e) {
                document.documentElement.setAttribute('data-theme', 'dark');
            }
        })();
    </script>
```

- [ ] **Step 4: Añadir el botón toggle en las navbars públicas**

En `public-head.html` dentro del `<nav ...>` (antes del cierre `</nav>`), insertar:

```html
        <div class="flex items-center gap-3">
            <button type="button" class="btn-theme-toggle" aria-label="Activar modo claro" title="Activar modo claro">
                <i data-lucide="sun" class="h-5 w-5"></i>
            </button>
        </div>
```

En `header.html` dentro de `.header-derecha` (junto al selector de idiomas `ES`/`EN`), insertar antes del cierre de la div:

```html
                <button type="button" class="btn-theme-toggle" aria-label="Activar modo claro" title="Activar modo claro">
                    <i data-lucide="sun" class="luxury-icon luxury-icon-sm"></i>
                </button>
```

Repetir el mismo bloque en `header-en.html` (navbar EN, mismo `.header-derecha`).

- [ ] **Step 5: Run test to verify it passes**

Run: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd -q -Dtest=PublicControllerTest#themeAssets_marketingPage_hasAntiFoucAndToggle test`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/templates/fragments/public-head.html src/main/resources/templates/fragments/header.html src/main/resources/templates/fragments/header-en.html src/test/java/com/monteastur/envios/controller/PublicControllerTest.java
git commit -m "feat(theme): anti-FOUC inline y boton toggle en navbars publicas"
```

### Task 4: Assets de tema + anti-FOUC en los 9 heads admin + toggle en sidebar y logins

**Files:**
- Modify (heads): `templates/login.html`, `templates/cliente/login.html`, `templates/cms/dashboard.html`, `templates/cms/contactos.html`, `templates/cms/textos.html`, `templates/cms/reservas.html`, `templates/cms/tracking.html`, `templates/cms/tracking-form.html`, `templates/cms/imagenes.html`
- Modify: `templates/fragments/admin-sidebar.html`, `static/css/theme-ui.css`
- Create: `src/test/java/com/monteastur/envios/controller/AdminThemeAssetsTest.java`

**Interfaces:**
- Consumes: `theme-ui.css`, `theme-toggle.js` (Task 2).
- Produces: `.btn-theme-toggle--floating` añadido a `theme-ui.css`; todos los heads admin listos para Tasks 5-10.

**Rutas de render (verificadas):** `/login` → `login` (LoginController), `/cliente/login` → `cliente/login` (ClienteController), `/admin/*` → `cms/*` (AdminController, `@RequestMapping("/admin")` + `@PreAuthorize("hasRole('ROLE_ADMIN')")`).

- [ ] **Step 1: Write the failing test** — crear `AdminThemeAssetsTest.java`:

```java
package com.monteastur.envios.controller;

import com.monteastur.envios.config.RBACAccessLogger;
import com.monteastur.envios.config.SecurityConfig;
import com.monteastur.envios.repository.ClienteRepository;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.repository.ImagenRepository;
import com.monteastur.envios.repository.MensajeContactoRepository;
import com.monteastur.envios.repository.ReservaRepository;
import com.monteastur.envios.repository.TextoLegalRepository;
import com.monteastur.envios.security.CustomAccessDeniedHandler;
import com.monteastur.envios.service.ClienteService;
import com.monteastur.envios.service.EmailService;
import com.monteastur.envios.service.EnvioTrackingService;
import com.monteastur.envios.service.EvidenciaEnvioService;
import com.monteastur.envios.service.EventoTrackingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({AdminController.class, LoginController.class, ClienteController.class})
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
    "app.admin.username=admin",
    "app.admin.password=test",
    "app.upload.dir=./uploads"
})
class AdminThemeAssetsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private ReservaRepository reservaRepo;
    @MockBean private ImagenRepository imagenRepo;
    @MockBean private MensajeContactoRepository mensajeRepo;
    @MockBean private TextoLegalRepository textoRepo;
    @MockBean private EnvioTrackingRepository trackingRepo;
    @MockBean private EmailService emailService;
    @MockBean private ClienteRepository clienteRepo;
    @MockBean private EvidenciaEnvioService evidenciaService;
    @MockBean private EventoTrackingService eventoTrackingService;
    @MockBean private EnvioTrackingService envioTrackingService;
    @MockBean private ClienteService clienteService;
    @MockBean private RBACAccessLogger rbacAccessLogger;
    @MockBean private CustomAccessDeniedHandler customAccessDeniedHandler;
    @MockBean private DataSource dataSource;

    @Test
    void loginPages_haveThemeAssetsAndAntiFouc() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-theme")))
                .andExpect(content().string(containsString("/css/design-system.css")))
                .andExpect(content().string(containsString("/css/theme-ui.css")))
                .andExpect(content().string(containsString("/js/theme-toggle.js")))
                .andExpect(content().string(containsString("btn-theme-toggle")));

        mockMvc.perform(get("/cliente/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-theme")))
                .andExpect(content().string(containsString("/css/design-system.css")))
                .andExpect(content().string(containsString("/css/theme-ui.css")))
                .andExpect(content().string(containsString("/js/theme-toggle.js")))
                .andExpect(content().string(containsString("btn-theme-toggle")));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/admin/dashboard",
        "/admin/mensajesrecibidos",
        "/admin/reservas",
        "/admin/imagenes",
        "/admin/textos",
        "/admin/tracking",
        "/admin/tracking/nuevo"
    })
    void adminPages_haveThemeAssetsAndAntiFouc(String url) throws Exception {
        mockMvc.perform(get(url).with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-theme")))
                .andExpect(content().string(containsString("/css/design-system.css")))
                .andExpect(content().string(containsString("/css/theme-ui.css")))
                .andExpect(content().string(containsString("/js/theme-toggle.js")))
                .andExpect(content().string(containsString("btn-theme-toggle")));
    }
}
```

> Nota: los mocks por defecto de Mockito devuelven `0`/listas vacías, suficiente para renderizar todas las rutas `cms/*`.

- [ ] **Step 2: Run test to verify it fails**

Run: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd -q -Dtest=AdminThemeAssetsTest test`
Expected: FAIL (el HTML no contiene aún los assets).

- [ ] **Step 3: Añadir assets + anti-FOUC a los 9 heads admin**

Para cada uno de los 9 ficheros, después de `<meta name="viewport" ...>` y ANTES del primer `<link ...>`:

```html
    <script>
        (function () {
            try {
                var theme = localStorage.getItem('theme') || 'dark';
                document.documentElement.setAttribute('data-theme', theme);
            } catch (e) {
                document.documentElement.setAttribute('data-theme', 'dark');
            }
        })();
    </script>
    <link rel="stylesheet" th:href="@{/css/design-system.css}">
```

Después del `<link ...>` de `admin.css` (o de la última hoja existente):

```html
    <link rel="stylesheet" th:href="@{/css/theme-ui.css}">
```

Y antes de `</head>` (tras los `<script ...>` existentes):

```html
    <script src="/js/theme-toggle.js" defer></script>
```

> Usa `th:href` si el head ya usa `th:href` (login.html usa `th:href="@{/css/admin.css}"`); usa `href` plano si el fichero usa `href` plano. Mantén siempre la convención del fichero.

- [ ] **Step 4: Toggle en el sidebar admin**

En `fragments/admin-sidebar.html`, insertar justo antes del `<form class="logout-form">`:

```html
    <button type="button" class="btn-theme-toggle" aria-label="Activar modo claro" title="Activar modo claro">
        <i data-lucide="sun" class="luxury-icon luxury-icon-sm"></i>
    </button>
```

- [ ] **Step 5: Toggle flotante en las páginas de login** — añadir a `theme-ui.css`:

```css
/* ---- Toggle flotante (login) ---- */
.btn-theme-toggle--floating {
    position: fixed;
    top: 1rem;
    right: 1rem;
    z-index: 60;
}
```

En `login.html` insertar antes de `</body>`:

```html
    <button type="button" class="btn-theme-toggle btn-theme-toggle--floating" aria-label="Activar modo claro" title="Activar modo claro">
        <i data-lucide="sun" class="luxury-icon luxury-icon-sm"></i>
    </button>
```

Lo mismo en `cliente/login.html`, usando las clases de tamaño de icono que ya usa esa página.

- [ ] **Step 6: Run test to verify it passes**

Run: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd -q -Dtest=AdminThemeAssetsTest test`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add src/main/resources/templates src/main/resources/static/css/theme-ui.css src/test/java/com/monteastur/envios/controller/AdminThemeAssetsTest.java
git commit -m "feat(theme): assets y anti-FOUC en heads admin, toggle en sidebar y logins"
```

---

## Phase 2 — Adaptación de temas + pulido + páginas admin nuevas

### Task 5: Remap de luxury-core.css y admin-theme.css + capa light en admin.css

**Files:**
- Modify: `static/css/luxury-core.css`, `static/css/admin/admin-theme.css`, `static/css/admin.css`

**Interfaces:**
- Consumes: tokens canónicos de `design-system.css` (cargado primero).
- Produces: el flip de `data-theme` cambia marketing (luxury) y admin vía `var()`.

- [ ] **Step 1: `admin-theme.css` — reescribir todo el fichero** mapeando a tokens canónicos (los que colisionan con design-system se ELIMINAN para no pisarlo; `--glass-*` ya los da design-system):

```css
:root {
    /* Identidad */
    --verde: #3f6338;
    --verde-oscuro: #2d4a28;
    --verde-gradient: linear-gradient(135deg, #3f6338, #557c45);
    --naranja: var(--accent-color);
    --naranja-hover: var(--accent-color-hover);

    /* Textos */
    --blanco-principal: var(--text-primary);
    --blanco-secundario: var(--text-secondary);
    --gris-texto-legible: var(--text-secondary);

    /* Semáforo */
    --verde-exito: var(--status-success);
    --verde-aprobada: var(--status-success);
    --verde-confirmada: var(--status-success);
    --naranja-advertencia: var(--status-warning);
    --rojo-error: var(--status-danger);
    --rojo: var(--status-danger);

    /* Superficies */
    --fondo: var(--bg-body);
    --texto: var(--text-primary);
    --texto-sec: var(--text-secondary);
    --borde: var(--border-subtle);
    --sombra: var(--glass-shadow);

    /* Texto en inputs / labels */
    --oscuro-texto-fondo-claro: var(--text-primary);
    --oscuro-texto-fondo-claro-sec: var(--text-secondary);

    --texto-secundario: var(--text-secondary);
    --texto-descriptivo: var(--text-primary);

    /* Borde del punto de timeline (fondo dark) */
    --blanco: #09090b;
}

/* Theme switcher: modo claro */
:root[data-theme="light"] {
    --blanco: #0f172a;
    --sombra: 0 8px 25px rgba(15, 23, 42, 0.12);
}
```

- [ ] **Step 2: `luxury-core.css` — sustituir hardcodes por `var()`**

1. En `body`, cambiar `background-color: #09090b !important;` por `background-color: var(--bg-body) !important;`.
2. En el `:root`, sustituir los valores literales dark por referencias canónicas:
   - `--color-dark: var(--text-primary);`, `--color-white: var(--text-primary);`, `--color-charcoal: var(--bg-card);`, `--color-stone: var(--bg-card);`, `--color-parchment: var(--bg-surface);`, `--color-text: var(--text-primary);`, `--color-muted: var(--text-muted);`, `--casa-dark: var(--text-primary);`, `--contacto-green-dark: var(--text-primary);`, `--reservas-green-dark: var(--text-primary);`, `--tracking-logistic-dark: var(--text-primary);`, `--casa-paper: var(--surface-card);`, `--contacto-paper: var(--surface-card);`, `--reservas-paper: var(--surface-card);`, `--tracking-logistic-paper: var(--surface-card);`.
   - **ELIMINAR** del `:root` las re-declaraciones que colisionan con design-system y se cargan después: `--text-primary`, `--text-secondary`, `--text-muted`, `--text-on-accent`, `--glass-bg`, `--glass-bg-hover`, `--glass-border`, `--glass-shadow`, `--transition-default`, `--transition-slow` (las provee design-system.css, que además hace el flip claro/oscuro). Mantener `--glass-blur: 12px;` y `--brand-glow`.
3. Grep del fichero en busca de `#ffffff`, `#fff`, `#09090b`, `#000000` usados como colores de tema y sustituirlos por `var(--text-primary)` / `var(--bg-body)` / `var(--bg-card)` según corresponda (dejar los `#d4762a` como marca o usar `var(--color-accent)`).
4. Añadir al FINAL del fichero:

```css
/* =============================================
   Theme switcher: MODO CLARO (override final)
   ============================================= */
:root[data-theme="light"] {
    --color-dark: var(--text-primary);
    --color-charcoal: var(--bg-card);
    --color-stone: var(--bg-card);
    --color-parchment: var(--bg-surface);
    --color-white: var(--text-primary);
    --casa-dark: var(--text-primary);
    --contacto-green-dark: var(--text-primary);
    --reservas-green-dark: var(--text-primary);
    --tracking-logistic-dark: var(--text-primary);
    --casa-paper: var(--surface-card);
    --contacto-paper: var(--surface-card);
    --reservas-paper: var(--surface-card);
    --tracking-logistic-paper: var(--surface-card);
    --brand-glow: 0 0 40px rgba(212, 118, 42, 0.12);
}
```

- [ ] **Step 3: `admin.css` — hacer theme-aware la "DARK FORM FOCUS OVERRIDE" + capa light final**

1. En el bloque final "DARK FORM FOCUS OVERRIDE" (≈ líneas 1389+), sustituir los rgba literal (p. ej. `rgba(212, 118, 42, 0.4)` y `rgba(255,255,255,...)`) por `var(--accent-glow)` y `var(--text-primary)`.
2. Grep de `admin.css` en busca de `#ffffff` y `#09090b` como colores de texto/fondo dependientes de tema (p. ej. inputs en líneas ~108/245/509/528) y sustituirlos por `var(--text-primary)` / `var(--bg-body)`.
3. Añadir al FINAL de `admin.css` (después de todo):

```css
/* =============================================
   Theme switcher: MODO CLARO (override final)
   ============================================= */
:root[data-theme="light"] {
    --sombra: 0 8px 25px rgba(15, 23, 42, 0.12);
}

:root[data-theme="light"] .login-wrapper {
    background: var(--bg-surface) !important;
    border-color: var(--border-subtle) !important;
}

:root[data-theme="light"] .sidebar {
    background: var(--bg-surface) !important;
    border-color: var(--border-subtle) !important;
}

:root[data-theme="light"] .card {
    background: var(--bg-card) !important;
    border-color: var(--border-subtle) !important;
    box-shadow: var(--glass-shadow);
}
```

> Si algún selector concreto de admin.css sigue usando colores oscuros en claro (cómo se comprueba con el smoke test de la Fase 3), añade su override en este bloque final.

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/static/css/luxury-core.css src/main/resources/static/css/admin/admin-theme.css src/main/resources/static/css/admin.css
git commit -m "feat(theme): remap tokens luxury y admin a design-system + capa modo claro"
```

### Task 6: Overlay Tailwind — sustituir clases dark por vars canónicas

**Files:**
- Modify: `templates/tracking-search.html`, `templates/tracking-result.html`, `templates/tracking-404.html`, `templates/cliente/panel.html`

**Interfaces:**
- Consumes: tokens canónicos (Task 1). Produce: estas 4 vistas flipean con `data-theme` sin tocar su layout ni `th:*`.

- [ ] **Step 1: Sustituir clases hardcodeadas** usando esta tabla de reemplazo (aplicar en las 4 plantillas; grep por cada clase):

| Clase actual (dark) | Reemplazo |
|---|---|
| `bg-[#09090b]/80` (headers sticky) | `bg-[var(--surface-header)]` |
| `bg-[#09090b]` | `bg-[var(--bg-body)]` |
| `bg-[#121215]` | `bg-[var(--bg-surface)]` |
| `bg-[#18181b]` / `bg-[#1a1a1f]` | `bg-[var(--bg-card)]` |
| `bg-white/[0.03]` | `bg-[var(--surface-card)]` |
| `bg-white/[0.06]` | `bg-[var(--surface-card-strong)]` |
| `bg-white/10` | `bg-[var(--glass-bg)]` |
| `border-white/10` | `border-[var(--border-subtle)]` |
| `border-white/15` | `border-[var(--border-strong)]` |
| `border-white/20` | `border-[var(--border-strong)]` |
| `text-white` / `text-zinc-100` / `text-zinc-300` | `text-[var(--text-primary)]` |
| `text-zinc-400` / `text-gray-400` | `text-[var(--text-secondary)]` |
| `text-zinc-500` | `text-[var(--text-muted)]` |
| `bg-[#d4762a]` | `bg-[var(--accent-color)]` |
| `hover:bg-[#e08c3f]` | `hover:bg-[var(--accent-color-hover)]` |
| `text-[#d4762a]` | `text-[var(--accent-color)]` |
| `bg-green-500` / badges de estado | `bg-[var(--status-success)]` (o `--status-warning`/`--status-danger`) |
| `ring-white/10` | `ring-[var(--border-subtle)]` |
| `shadow-[...rgba(0,0,0,...)]` (glass) | `shadow-[var(--glass-shadow)]` |

1. El `<body>` de estas plantillas debe usar `class="... bg-[var(--bg-body)] text-[var(--text-primary)] ..."` (reemplazar el `bg-[#09090b] text-white` actual).
2. Los `backdrop-blur-*` se mantienen tal cual.
3. Las clases de color de los badges de estado (p. ej. `bg-emerald-500`, `bg-amber-500`, `bg-rose-500`) pueden mantenerse si son aceptables en ambos temas; si se prefiere coherencia, sustituirlas por `bg-[var(--status-success)]` / `bg-[var(--status-warning)]` / `bg-[var(--status-danger)]`.
4. En `tracking-result.html`, los colores de la timeline/stepper (`text-emerald-400`, `border-emerald-500`, etc.) pueden mantenerse (verde válido en ambos temas) o mapearse a `var(--status-success)`.

> Tailwind Play CDN compila las clases arbitrarias `var()` en el navegador: el flip es inmediato al cambiar `data-theme`.

- [ ] **Step 2: Smoke manual** — arrancar la app y verificar `/tracking/{codigo}`, `/tracking`, `/cliente/panel` en ambos temas (sin test automatizado; el layout y los `th:*` no cambian).

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/templates/tracking-search.html src/main/resources/templates/tracking-result.html src/main/resources/templates/tracking-404.html src/main/resources/templates/cliente/panel.html
git commit -m "feat(theme): overlay Tailwind con vars canonicas en tracking y panel cliente"
```

---

### Task 7: Pulido de vistas — stepper pulse, KPI, menú contextual, logins

**Files:**
- Modify: `templates/tracking-result.html`, `templates/cms/dashboard.html`, `templates/cms/tracking.html`, `templates/login.html`, `static/css/admin.css`

**Interfaces:**
- Consumes: `.step-pulse` y `.card/.quick-action-card` de `theme-ui.css`.

- [ ] **Step 1: Stepper pulse en `tracking-result.html`**

Añadir la clase `step-pulse` al indicador (círculo) del paso correspondiente al estado ACTUAL del envío (el que el Thymeleaf marca como activo en la timeline), además de sus clases existentes. El anillo animado lo aporta `theme-ui.css` (`.step-pulse::after` con `@keyframes step-pulse-ring`). No alterar la estructura `th:*` de la timeline.

- [ ] **Step 2: KPIs y badges en `cms/dashboard.html`**

1. Añadir `class="card quick-action-card"` (o similar ya existente) a las tarjetas KPI para obtener el hover de `theme-ui.css` (transform/opacity 60fps).
2. Grep de colores hardcodeados en badges/estados (p. ej. `background-color: #f0a830` o clases `text-green-500` en línea) y sustituirlos por `var(--status-warning)` / `var(--status-success)` / `var(--status-danger)`.

- [ ] **Step 3: Menú contextual por fila en `cms/tracking.html`**

1. En cada fila de la tabla de envíos, añadir una columna/celda de acciones con un botón 3 puntos (Lucide `more-horizontal`) y un `<div class="row-menu" hidden>` con los enlaces:

```html
<td class="row-actions">
    <button type="button" class="row-menu-btn" aria-label="Acciones">
        <i data-lucide="more-horizontal" class="luxury-icon luxury-icon-sm"></i>
    </button>
    <div class="row-menu" hidden>
        <a th:href="@{/admin/tracking/editar/{id}(id=${envio.id})}">Editar</a>
        <a th:href="@{/tracking/{codigo}(codigo=${envio.codigo})}" target="_blank">Ver público</a>
        <a th:href="@{/api/v1/admin/documentos/envios/{codigo}/etiqueta(codigo=${envio.codigo})}" target="_blank">Etiqueta PDF</a>
    </div>
</td>
```

2. Añadir al FINAL de `admin.css`:

```css
/* ---- Menú contextual por fila (tracking) ---- */
.row-actions {
    position: relative;
    text-align: right;
}

.row-menu-btn {
    background: transparent;
    border: 1px solid var(--border-subtle);
    border-radius: 8px;
    color: var(--text-secondary);
    cursor: pointer;
    padding: 4px 6px;
    transition: background-color 0.2s ease, color 0.2s ease;
}

.row-menu-btn:hover {
    background: var(--surface-card-strong);
    color: var(--text-primary);
}

.row-menu {
    position: absolute;
    right: 0;
    top: 100%;
    z-index: 30;
    min-width: 160px;
    background: var(--bg-card);
    border: 1px solid var(--border-subtle);
    border-radius: 10px;
    box-shadow: var(--glass-shadow);
    padding: 4px;
    display: flex;
    flex-direction: column;
}

.row-menu a {
    padding: 8px 12px;
    border-radius: 6px;
    color: var(--text-primary);
    font-size: 0.85rem;
    text-decoration: none;
}

.row-menu a:hover {
    background: var(--surface-card-strong);
    color: var(--accent-color);
}
```

3. Añadir al final de `cms/tracking.html` un `<script>` inline (no modificar el resto del JS):

```html
<script>
    document.addEventListener('click', function (e) {
        var btn = e.target.closest('.row-menu-btn');
        document.querySelectorAll('.row-menu').forEach(function (m) {
            m.hidden = m.hidden && m.parentElement !== (btn ? btn.parentElement : null);
        });
        if (btn) {
            var menu = btn.parentElement.querySelector('.row-menu');
            menu.hidden = !menu.hidden;
            e.stopPropagation();
        }
    });
</script>
```

- [ ] **Step 4: Logins — accent glow theme-aware**

Verificar que la clase `focus-glow` de `login.html`/`cliente/login.html` usa `box-shadow: 0 0 0 3px var(--accent-glow)` (o similar con var) para que brille en ambos temas; si usa rgba literal, sustituirlo por `var(--accent-glow)`.

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/templates/tracking-result.html src/main/resources/templates/cms/dashboard.html src/main/resources/templates/cms/tracking.html src/main/resources/templates/login.html src/main/resources/static/css/admin.css
git commit -m "feat(ui): pulso stepper, hover KPI, menu contextual por fila y glow en logins"
```

### Task 8: Página admin `/admin/imports` (carga CSV)

**Files:**
- Modify: `service/batch/BatchImportPersistenceService.java`, `repository/BatchImportRepository.java`, `controller/AdminController.java`, `fragments/admin-sidebar.html`, `static/css/admin.css`
- Create: `templates/cms/imports.html`, `src/test/java/com/monteastur/envios/controller/AdminControllerTest.java`
- Modify (test): `test/.../service/batch/BatchImportPersistenceServiceTest.java`, `test/.../controller/AdminThemeAssetsTest.java`

**Interfaces:**
- Consumes: `BatchImportController` (`POST /api/v1/admin/imports/csv` → 202 con `BatchImportResponseDto`, `GET /{id}`, `GET /{id}/errors`). CSRF exento en `/api/**`; autenticación por sesión.
- Produces: `AdminController.listarLotes()` / `BatchImportRepository.findAllByOrderByIdDesc()`; ruta `GET /admin/imports`.
- DTOs JSON (verificados): `BatchImportResponseDto` → `id, clienteId, nombreArchivo, totalRegistros, procesados, exitosos, fallidos, estado, errorResumen, fechaCreacion, fechaFin`; `BatchImportErrorDto` → `lineaNumero, codigoRastreo, errorMensaje`.

- [ ] **Step 1: Write the failing tests**

1. Añadir a `BatchImportPersistenceServiceTest` (adaptar al estilo/`@BeforeEach` existente):

```java
    @Test
    void listarLotes_delegaEnRepositorioOrdenado() {
        BatchImport lote = new BatchImport(1L, "envios.csv", BatchImportEstado.COMPLETADO);
        when(batchImportRepository.findAllByOrderByIdDesc()).thenReturn(List.of(lote));

        assertThat(persistence.listarLotes()).containsExactly(lote);
        verify(batchImportRepository).findAllByOrderByIdDesc();
    }
```

> Imports: `com.monteastur.envios.model.BatchImport`, `com.monteastur.envios.model.BatchImportEstado`. Nombre de los mocks/atributos según el fixture ya existente en el fichero.

2. Crear `AdminControllerTest.java`:

```java
package com.monteastur.envios.controller;

import com.monteastur.envios.config.RBACAccessLogger;
import com.monteastur.envios.config.SecurityConfig;
import com.monteastur.envios.model.BatchImport;
import com.monteastur.envios.model.BatchImportEstado;
import com.monteastur.envios.model.Cliente;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.repository.ClienteRepository;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.repository.ImagenRepository;
import com.monteastur.envios.repository.MensajeContactoRepository;
import com.monteastur.envios.repository.ReservaRepository;
import com.monteastur.envios.repository.TextoLegalRepository;
import com.monteastur.envios.security.CustomAccessDeniedHandler;
import com.monteastur.envios.service.DocumentoPdfService;
import com.monteastur.envios.service.EmailService;
import com.monteastur.envios.service.EnvioTrackingService;
import com.monteastur.envios.service.EvidenciaEnvioService;
import com.monteastur.envios.service.EventoTrackingService;
import com.monteastur.envios.service.batch.BatchImportPersistenceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
    "app.admin.username=admin",
    "app.admin.password=test",
    "app.upload.dir=./uploads"
})
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private ReservaRepository reservaRepo;
    @MockBean private ImagenRepository imagenRepo;
    @MockBean private MensajeContactoRepository mensajeRepo;
    @MockBean private TextoLegalRepository textoRepo;
    @MockBean private EnvioTrackingRepository trackingRepo;
    @MockBean private EmailService emailService;
    @MockBean private ClienteRepository clienteRepo;
    @MockBean private EvidenciaEnvioService evidenciaService;
    @MockBean private EventoTrackingService eventoTrackingService;
    @MockBean private EnvioTrackingService envioTrackingService;
    @MockBean private BatchImportPersistenceService batchImportPersistenceService;
    @MockBean private DocumentoPdfService documentoPdfService;
    @MockBean private RBACAccessLogger rbacAccessLogger;
    @MockBean private CustomAccessDeniedHandler customAccessDeniedHandler;
    @MockBean private DataSource dataSource;

    @Test
    void imports_returnsViewWithClientesAndLotes() throws Exception {
        when(clienteRepo.findAll()).thenReturn(List.of(new Cliente()));
        when(batchImportPersistenceService.listarLotes())
                .thenReturn(List.of(new BatchImport(1L, "envios.csv", BatchImportEstado.COMPLETADO)));

        mockMvc.perform(get("/admin/imports").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("cms/imports"))
                .andExpect(model().attributeExists("clientes", "lotes"));
    }
}
```

3. Añadir a `AdminThemeAssetsTest` los nuevos `@MockBean` (el constructor de AdminController cambia):

```java
    @MockBean private com.monteastur.envios.service.batch.BatchImportPersistenceService batchImportPersistenceService;
    @MockBean private com.monteastur.envios.service.DocumentoPdfService documentoPdfService;
```

- [ ] **Step 2: Run tests to verify they fail (RED)**

Run: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd -q -Dtest=AdminControllerTest,BatchImportPersistenceServiceTest test`
Expected: FAIL/error de compilación (`listarLotes` no existe aún).

- [ ] **Step 3: Implementar backend (GREEN)**

1. `BatchImportRepository` — añadir:

```java
    List<BatchImport> findAllByOrderByIdDesc();
```

2. `BatchImportPersistenceService` — añadir:

```java
    public List<BatchImport> listarLotes() {
        return batchImportRepository.findAllByOrderByIdDesc();
    }
```

3. `AdminController` — añadir campos `private final BatchImportPersistenceService batchImportPersistenceService;` y `private final DocumentoPdfService documentoPdfService;`, actualizar el constructor (mismos 2 parámetros nuevos al final) y añadir:

```java
    @GetMapping("/imports")
    public String imports(Model model) {
        model.addAttribute("clientes", clienteRepo.findAll());
        model.addAttribute("lotes", batchImportPersistenceService.listarLotes());
        return "cms/imports";
    }
```

Imports nuevos en `AdminController`: `com.monteastur.envios.service.DocumentoPdfService`, `com.monteastur.envios.service.batch.BatchImportPersistenceService`.

- [ ] **Step 4: CSS de la página** — añadir al FINAL de `admin.css`:

```css
/* ---- Página imports: dropzone, progreso, modal, badges ---- */
.dropzone {
    border: 2px dashed var(--border-strong);
    border-radius: 12px;
    padding: 2rem;
    text-align: center;
    color: var(--text-secondary);
    cursor: pointer;
    transition: border-color 0.2s ease, background-color 0.2s ease;
}

.dropzone.dragover {
    border-color: var(--accent-color);
    background: var(--accent-glow);
}

.progress-track {
    height: 8px;
    border-radius: 999px;
    background: var(--surface-card-strong);
    overflow: hidden;
}

.progress-bar {
    height: 100%;
    width: 0%;
    border-radius: 999px;
    background: var(--accent-color);
    transition: width 0.4s ease;
}

.estado-badge {
    display: inline-block;
    padding: 2px 10px;
    border-radius: 999px;
    font-size: 0.75rem;
    font-weight: 600;
}

.status-success { background: var(--status-success); color: var(--bg-body); }
.status-warning { background: var(--status-warning); color: var(--bg-body); }
.status-danger  { background: var(--status-danger);  color: var(--bg-body); }
.status-info    { background: var(--status-info);    color: var(--bg-body); }

.modal {
    position: fixed;
    inset: 0;
    z-index: 100;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(0, 0, 0, 0.55);
}

.modal-content {
    width: min(720px, 92vw);
    max-height: 80vh;
    overflow: auto;
    background: var(--bg-card);
    border: 1px solid var(--border-subtle);
    border-radius: 14px;
    box-shadow: var(--glass-shadow);
    padding: 1.25rem;
}

.modal-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 1rem;
}
```

- [ ] **Step 5: Crear `templates/cms/imports.html`** (código completo):

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Importar Envíos | MONTEASTUR ENVIOS</title>
    <script>
        (function () {
            try {
                var theme = localStorage.getItem('theme') || 'dark';
                document.documentElement.setAttribute('data-theme', theme);
            } catch (e) {
                document.documentElement.setAttribute('data-theme', 'dark');
            }
        })();
    </script>
    <link rel="stylesheet" th:href="@{/css/design-system.css}">
    <link rel="stylesheet" th:href="@{/css/admin.css}">
    <link rel="stylesheet" th:href="@{/css/theme-ui.css}">
    <script src="/js/theme-toggle.js" defer></script>
    <script src="/js/app.js" defer></script>
</head>
<body class="cms-body">
    <div th:replace="~{fragments/admin-sidebar :: admin-sidebar('imports')}"></div>
    <main class="main-content">
        <h1 class="luxury-heading-lg">Carga masiva de envíos (CSV)</h1>
        <p class="subtitle">Sube un CSV para registrar envíos en lote. El procesamiento es asíncrono.</p>

        <div class="card">
            <div id="dropzone" class="dropzone">
                <i data-lucide="upload" class="luxury-icon luxury-icon-lg"></i>
                <p>Arrastra tu CSV aquí o haz clic para seleccionar</p>
                <small>Máx. 5 MB</small>
                <input type="file" id="fileInput" accept=".csv" hidden>
            </div>
            <div class="form-group" style="margin-top: 1rem;">
                <label for="clienteId">Cliente (opcional)</label>
                <select id="clienteId" name="clienteId">
                    <option value="">Sin asignar</option>
                    <option th:each="c : ${clientes}" th:value="${c.id}" th:text="${c.nombre}"></option>
                </select>
            </div>
            <button type="button" id="btnImportar" class="btn-luxury" disabled>
                <i data-lucide="play" class="luxury-icon luxury-icon-sm"></i> Importar CSV
            </button>
        </div>

        <div class="card" id="progresoCard" hidden>
            <h2 class="luxury-heading-md">Lote <span id="loteId"></span></h2>
            <div class="progress-track"><div class="progress-bar" id="progressBar"></div></div>
            <p id="progresoTexto" class="subtitle"></p>
        </div>

        <div class="card">
            <h2 class="luxury-heading-md">Lotes recientes</h2>
            <table class="admin-table">
                <thead>
                    <tr>
                        <th>ID</th><th>Archivo</th><th>Cliente</th><th>Estado</th>
                        <th>Procesados</th><th>Exitosos</th><th>Fallidos</th><th>Fecha</th><th></th>
                    </tr>
                </thead>
                <tbody>
                    <tr th:each="l : ${lotes}"
                        th:with="cls=${l.estado.name() == 'COMPLETADO' ? 'status-success' : (l.estado.name() == 'COMPLETADO_CON_ERRORES' ? 'status-warning' : (l.estado.name() == 'FALLIDO' ? 'status-danger' : 'status-info'))}">
                        <td th:text="${l.id}"></td>
                        <td th:text="${l.nombreArchivo}"></td>
                        <td th:text="${l.clienteId}"></td>
                        <td><span class="estado-badge" th:classappend="${cls}" th:text="${l.estado.name()}"></span></td>
                        <td th:text="${l.procesados}"></td>
                        <td th:text="${l.exitosos}"></td>
                        <td th:text="${l.fallidos}"></td>
                        <td th:text="${l.fechaCreacion != null ? #temporals.format(l.fechaCreacion, 'dd/MM/yyyy HH:mm') : '-'}"></td>
                        <td>
                            <button type="button" class="row-menu-btn" th:onclick="'verErrores(' + ${l.id} + ')'">Errores</button>
                        </td>
                    </tr>
                    <tr th:if="${#lists.isEmpty(lotes)}">
                        <td colspan="9" class="empty-row">No hay lotes todavía.</td>
                    </tr>
                </tbody>
            </table>
        </div>
    </main>

    <div class="modal" id="erroresModal" hidden>
        <div class="modal-content">
            <div class="modal-header">
                <h2 class="luxury-heading-md">Errores del lote</h2>
                <button type="button" id="cerrarErrores" class="row-menu-btn">&times;</button>
            </div>
            <table class="admin-table">
                <thead><tr><th>Línea</th><th>Código</th><th>Error</th></tr></thead>
                <tbody id="erroresBody"></tbody>
            </table>
        </div>
    </div>

    <script>
        var dropzone = document.getElementById('dropzone');
        var fileInput = document.getElementById('fileInput');
        var btnImportar = document.getElementById('btnImportar');

        dropzone.addEventListener('click', function () { fileInput.click(); });
        dropzone.addEventListener('dragover', function (e) { e.preventDefault(); dropzone.classList.add('dragover'); });
        dropzone.addEventListener('dragleave', function () { dropzone.classList.remove('dragover'); });
        dropzone.addEventListener('drop', function (e) {
            e.preventDefault();
            dropzone.classList.remove('dragover');
            if (e.dataTransfer.files.length) { fileInput.files = e.dataTransfer.files; btnImportar.disabled = false; }
        });
        fileInput.addEventListener('change', function () { btnImportar.disabled = !fileInput.files.length; });

        btnImportar.addEventListener('click', function () {
            var fd = new FormData();
            fd.append('file', fileInput.files[0]);
            var clienteId = document.getElementById('clienteId').value;
            if (clienteId) fd.append('clienteId', clienteId);
            fetch('/api/v1/admin/imports/csv', { method: 'POST', body: fd })
                .then(function (r) {
                    if (!r.ok) { throw new Error('Error al subir: HTTP ' + r.status); }
                    return r.json();
                })
                .then(function (data) {
                    document.getElementById('progresoCard').hidden = false;
                    document.getElementById('loteId').textContent = '#' + data.id;
                    poll(data.id);
                })
                .catch(function (err) { alert(err.message); });
        });

        function poll(id) {
            fetch('/api/v1/admin/imports/' + id)
                .then(function (r) { return r.json(); })
                .then(function (d) {
                    var terminal = ['COMPLETADO', 'COMPLETADO_CON_ERRORES', 'FALLIDO'].indexOf(d.estado) >= 0;
                    var total = d.totalRegistros > 0 ? d.totalRegistros : d.procesados;
                    var pct = total > 0 ? Math.round((d.procesados / total) * 100) : 0;
                    document.getElementById('progressBar').style.width = pct + '%';
                    document.getElementById('progresoTexto').textContent =
                        d.estado + ' — procesados ' + d.procesados + ' / exitosos ' + d.exitosos + ' / fallidos ' + d.fallidos;
                    if (!terminal) { setTimeout(function () { poll(id); }, 2000); }
                });
        }

        function verErrores(id) {
            fetch('/api/v1/admin/imports/' + id + '/errors')
                .then(function (r) { return r.json(); })
                .then(function (errores) {
                    var body = document.getElementById('erroresBody');
                    body.innerHTML = '';
                    errores.forEach(function (e) {
                        var tr = document.createElement('tr');
                        tr.innerHTML = '<td>' + e.lineaNumero + '</td><td>' + (e.codigoRastreo || '-') + '</td><td>' + e.errorMensaje + '</td>';
                        body.appendChild(tr);
                    });
                    document.getElementById('erroresModal').hidden = false;
                });
        }

        document.getElementById('cerrarErrores').addEventListener('click', function () {
            document.getElementById('erroresModal').hidden = true;
        });
    </script>
</body>
</html>
```

- [ ] **Step 6: Enlace en el sidebar** — en `fragments/admin-sidebar.html`, añadir tras el enlace de Tracking:

```html
        <a href="/admin/imports" th:class="${seccionActiva == 'imports'} ? 'active' : ''"><i data-lucide="upload" class="luxury-icon luxury-icon-sm"></i> Importar Envíos</a>
```

- [ ] **Step 7: Run tests to verify they pass (GREEN)**

Run: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd -q -Dtest=AdminControllerTest,BatchImportPersistenceServiceTest,AdminThemeAssetsTest test`
Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add src/main/java src/main/resources/templates/cms/imports.html src/main/resources/templates/fragments/admin-sidebar.html src/main/resources/static/css/admin.css src/test/java
git commit -m "feat(admin): pagina imports con carga CSV asincrona, polling y modal de errores"
```

---

### Task 9: Página admin `/admin/documentos` (etiquetas, manifiestos, auditoría)

**Files:**
- Modify: `controller/AdminController.java`, `fragments/admin-sidebar.html`
- Create: `templates/cms/documentos.html`
- Modify (test): `test/.../controller/AdminControllerTest.java`

**Interfaces:**
- Consumes: `DocumentosController` (`GET /api/v1/admin/documentos/envios/{codigo}/etiqueta` inline, `GET /lotes/{batchId}/etiquetas`, `GET /lotes/{batchId}/manifiesto` attachment, `GET /` auditoría); `DocumentoPdfService.listarEmisiones(TipoDocumento)`.
- Produces: ruta `GET /admin/documentos` con `model`: `envios`, `lotes`, `emisiones`.
- Modelo: `EnvioTracking.codigo`, `BatchImport.id`, `DocumentoGenerado` (tipo enum → `.name()`, referenciaId, nombreArchivo, pesoBytes, usuarioGeneracion, fechaCreacion LocalDateTime).

- [ ] **Step 1: Write the failing test** — añadir a `AdminControllerTest`:

```java
    @Test
    void documentos_returnsViewWithModel() throws Exception {
        when(trackingRepo.findAllByOrderByUltimaActualizacionDesc()).thenReturn(List.of(new EnvioTracking()));
        when(batchImportPersistenceService.listarLotes()).thenReturn(List.of());
        when(documentoPdfService.listarEmisiones(null)).thenReturn(List.of());

        mockMvc.perform(get("/admin/documentos").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("cms/documentos"))
                .andExpect(model().attributeExists("envios", "lotes", "emisiones"));
    }
```

- [ ] **Step 2: Run test to verify it fails (RED)**

Run: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd -q -Dtest=AdminControllerTest#documentos_returnsViewWithModel test`
Expected: FAIL (no existe la ruta).

- [ ] **Step 3: Implementar en `AdminController` (GREEN)** — añadir:

```java
    @GetMapping("/documentos")
    public String documentos(Model model) {
        model.addAttribute("envios", trackingRepo.findAllByOrderByUltimaActualizacionDesc());
        model.addAttribute("lotes", batchImportPersistenceService.listarLotes());
        model.addAttribute("emisiones", documentoPdfService.listarEmisiones(null));
        return "cms/documentos";
    }
```

- [ ] **Step 4: Crear `templates/cms/documentos.html`** (código completo):

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Documentos | MONTEASTUR ENVIOS</title>
    <script>
        (function () {
            try {
                var theme = localStorage.getItem('theme') || 'dark';
                document.documentElement.setAttribute('data-theme', theme);
            } catch (e) {
                document.documentElement.setAttribute('data-theme', 'dark');
            }
        })();
    </script>
    <link rel="stylesheet" th:href="@{/css/design-system.css}">
    <link rel="stylesheet" th:href="@{/css/admin.css}">
    <link rel="stylesheet" th:href="@{/css/theme-ui.css}">
    <script src="/js/theme-toggle.js" defer></script>
    <script src="/js/app.js" defer></script>
</head>
<body class="cms-body">
    <div th:replace="~{fragments/admin-sidebar :: admin-sidebar('documentos')}"></div>
    <main class="main-content">
        <h1 class="luxury-heading-lg">Documentos y Etiquetas</h1>
        <p class="subtitle">Genera etiquetas térmicas, etiquetas por lote y manifiestos de carga.</p>

        <div class="card">
            <h2 class="luxury-heading-md">Etiqueta térmica por envío</h2>
            <p class="subtitle">PDF 100x150 mm con código de barras y QR. Selecciona un envío:</p>
            <table class="admin-table">
                <thead><tr><th>Código</th><th>Cliente</th><th>Estado</th><th></th></tr></thead>
                <tbody>
                    <tr th:each="e : ${envios}">
                        <td th:text="${e.codigo}"></td>
                        <td th:text="${e.cliente != null ? e.cliente.nombre : '-'}"></td>
                        <td th:text="${e.estado}"></td>
                        <td>
                            <a class="btn-luxury btn-sm" target="_blank"
                               th:href="@{/api/v1/admin/documentos/envios/{codigo}/etiqueta(codigo=${e.codigo})}">
                                <i data-lucide="printer" class="luxury-icon luxury-icon-sm"></i> Etiqueta
                            </a>
                        </td>
                    </tr>
                    <tr th:if="${#lists.isEmpty(envios)}">
                        <td colspan="4" class="empty-row">No hay envíos registrados.</td>
                    </tr>
                </tbody>
            </table>
        </div>

        <div class="card">
            <h2 class="luxury-heading-md">Documentos por lote</h2>
            <table class="admin-table">
                <thead><tr><th>Lote</th><th>Archivo</th><th>Fecha</th><th></th></tr></thead>
                <tbody>
                    <tr th:each="l : ${lotes}">
                        <td th:text="${'Lote #' + l.id}"></td>
                        <td th:text="${l.nombreArchivo}"></td>
                        <td th:text="${l.fechaCreacion != null ? #temporals.format(l.fechaCreacion, 'dd/MM/yyyy HH:mm') : '-'}"></td>
                        <td>
                            <a class="btn-luxury btn-sm" target="_blank"
                               th:href="@{/api/v1/admin/documentos/lotes/{id}/etiquetas(id=${l.id})}">Etiquetas</a>
                            <a class="btn-luxury btn-sm" target="_blank"
                               th:href="@{/api/v1/admin/documentos/lotes/{id}/manifiesto(id=${l.id})}">Manifiesto</a>
                        </td>
                    </tr>
                    <tr th:if="${#lists.isEmpty(lotes)}">
                        <td colspan="4" class="empty-row">No hay lotes todavía.</td>
                    </tr>
                </tbody>
            </table>
        </div>

        <div class="card">
            <h2 class="luxury-heading-md">Auditoría de emisiones</h2>
            <table class="admin-table">
                <thead><tr><th>Tipo</th><th>Referencia</th><th>Archivo</th><th>Tamaño</th><th>Generado por</th><th>Fecha</th></tr></thead>
                <tbody>
                    <tr th:each="d : ${emisiones}">
                        <td th:text="${d.tipo.name()}"></td>
                        <td th:text="${d.referenciaId}"></td>
                        <td th:text="${d.nombreArchivo}"></td>
                        <td th:text="${d.pesoBytes + ' B'}"></td>
                        <td th:text="${d.usuarioGeneracion}"></td>
                        <td th:text="${d.fechaCreacion != null ? #temporals.format(d.fechaCreacion, 'dd/MM/yyyy HH:mm') : '-'}"></td>
                    </tr>
                    <tr th:if="${#lists.isEmpty(emisiones)}">
                        <td colspan="6" class="empty-row">Aún no se ha generado ningún documento.</td>
                    </tr>
                </tbody>
            </table>
        </div>
    </main>
</body>
</html>
```

> Si `admin.css` no define `.btn-sm`, añadirlo en el bloque CSS de admin (`padding: 6px 12px; font-size: 0.8rem;`).

- [ ] **Step 5: Enlace en el sidebar** — añadir tras "Importar Envíos":

```html
        <a href="/admin/documentos" th:class="${seccionActiva == 'documentos'} ? 'active' : ''"><i data-lucide="file-text" class="luxury-icon luxury-icon-sm"></i> Documentos</a>
```

- [ ] **Step 6: Run test to verify it passes (GREEN)**

Run: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd -q -Dtest=AdminControllerTest test`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/monteastur/envios/controller/AdminController.java src/main/resources/templates/cms/documentos.html src/main/resources/templates/fragments/admin-sidebar.html src/test/java/com/monteastur/envios/controller/AdminControllerTest.java
git commit -m "feat(admin): pagina documentos con etiquetas, manifiestos y auditoria"
```

---

## Phase 3 — Verificación global y entrega

### Task 10: Verificación completa, smoke, commit final y push

**Files:**
- Modify: `docs/handoff.md`
- No code nuevo (solo verificación y release).

**Interfaces:**
- Consumes: todo lo anterior. Produce: `BUILD SUCCESS`, push a `origin/main`, GH Actions verde, handoff actualizado.

- [ ] **Step 1: Suite completa local**

Run: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd clean test`
Expected: `BUILD SUCCESS` con la base (219) + tests nuevos (3 en `PublicControllerTest`, `AdminThemeAssetsTest`, `AdminControllerTest`, `listarLotes`).

- [ ] **Step 2: Suite completa en contenedor (MySQL 8 + Redis 7)**

Ejecutar el comando Docker de `AGENTS.md` (red `envios_paraguay_cms_backend`, datasource de test, `DB_USERNAME=root`, `DB_PASSWORD=root`, `SPRING_DATA_REDIS_HOST=redis`, imagen `maven:3.9-eclipse-temurin-17`).
Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Smoke manual del theme switcher**

Levantar la app con `db` + `redis` y verificar:
1. `/` carga siempre Oscuro la primera vez (sin FOUC), el toggle cambia a Claro y persiste tras recargar.
2. `/tracking/{codigo}`, `/tracking`, `/cliente/panel`, `/cliente/login`, `/login` y todas las rutas `/admin/*` flipean correctamente en ambos temas.
3. `/admin/imports`: subir un CSV real → 202, polling a estado terminal, modal de errores si los hay.
4. `/admin/documentos`: etiqueta de un envío (100x150), etiquetas/manifiesto de un lote, y la tabla de auditoría con datos.
5. En modo claro, formularios, sidebar, login-wrapper y cards se ven correctos (añadir overrides de luz si algo quedara oscuro).
6. `prefers-reduced-motion` activado no muestra animaciones.

- [ ] **Step 4: Commit final (mensaje EXACTO) y push**

```bash
git add -A
git commit -m "feat(ui): add dynamic dark/light theme switcher with anti-FOUC and motion polish"
git push origin main
```

- [ ] **Step 5: Verificar GitHub Actions**

Confirmar que el run de CI (Test suite MySQL 8 + Redis 7 y Docker build + smoke test) termina en **PASS** para el commit final.

- [ ] **Step 6: Actualizar `docs/handoff.md`**

Añadir la entrada del sprint: theme switcher (design-system/theme-ui/theme-toggle.js/anti-FOUC), remap luxury+admin, overlay Tailwind, pulso stepper, menú contextual, páginas `/admin/imports` y `/admin/documentos`, tests añadidos, commits y estado de la rama `main`.

---

## Acceptance Criteria (todo debe cumplirse)

1. El tema por defecto es Oscuro; persiste en `localStorage` (`theme`); el toggle cambia y persiste; no hay FOUC (anti-FOUC inline en los 12 heads).
2. `data-theme="light"` se ve coherente en marketing, tracking/cliente y admin (sin fondos/textos hardcodeados oscuros rotos).
3. `AdminThemeAssetsTest`, `AdminControllerTest`, los 3 tests nuevos de `PublicControllerTest` y `listarLotes` pasan.
4. `mvn.cmd clean test` y la suite Docker terminan en `BUILD SUCCESS`.
5. `/admin/imports` sube CSV (fetch + FormData), hace polling y muestra errores; `/admin/documentos` lista envíos/lotes/emisiones y genera PDFs.
6. Motion solo `transform`/`opacity`; `prefers-reduced-motion` respetado.
7. Commit final con el mensaje exacto, push a `origin/main`, GH Actions en PASS.
8. `docs/handoff.md` actualizado.

## Risks / Notes

- **`admin.css` / `luxury-core.css`** tienen hardcodes `#09090b`/`#ffffff`: la capa light final y el grep de la Task 5 deben cubrirlos; el smoke manual es la verificación definitiva.
- **`@WebMvcTest` de AdminController**: cualquier nuevo parámetro de constructor exigirá añadir su `@MockBean` en `AdminThemeAssetsTest` y `AdminControllerTest`.
- **`estado` de `BatchImport`** es enum → usar `.name()` en Thymeleaf; el DTO ya entrega `estado` como String para el JS.
- **Tailwind Play CDN** compila clases arbitrarias `var()` en cliente: el overlay de la Task 6 no cambia el HTML ni los `th:*`.
- **CSRF**: los fetch a `/api/v1/admin/imports/*` no llevan token (exento en `/api/**`); solo cookie de sesión.
- **Redis**: los tests de integración requieren Redis levantado; usar siempre la suite Docker de AGENTS.md.

## Open Questions
- Ninguna pendiente: las 4 decisiones de diseño fueron aprobadas por el usuario (default Oscuro, Enfoque A Token Bridge, sin atajo de teclado, páginas funcionales nuevas).



