# Cirugía de Unificación Visual y Coherencia de Marca — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Consolidar TODO el CSS del proyecto en un único `design-system.css` y unificar cabecera, componentes y temas claro/oscuro en las 44 plantillas, eliminando la fragmentación visual entre `/casa`, `/contacto`, `/reservas` y `/seguimiento`.

**Architecture:** Un único archivo CSS (`design-system.css`) absorbe tokens duales + identidad tricolor + componentes + estilos por página + suite admin. Un fragmento de cabecera único (ES/EN) con logo tricolor EN/VI/OS, franja tricolor canónica y toggle de tema. Se eliminan 26 archivos CSS legacy, stubs huérfanos y el CDN de Tailwind. WCAG 2.2 AA/AAA con variantes accesibles del naranja.

**Tech Stack:** Spring Boot 3.3.5 + Thymeleaf + Java 25 + Maven (imagen `maven:3.9-eclipse-temurin-25`), MySQL 8 + Redis 7 (Docker Compose), Bootstrap no usado en frontend propio.

## Global Constraints

- Regla AGENTS.md: **prohibido** `git push`/merge a `main` sin confirmación explícita del usuario.
- Verificación final obligatoria en contenedor Docker con **236 tests → BUILD SUCCESS** (criterio de cierre de la spec).
- Orden de carga canónico del head (spec Sección 1): 1 `<link>` CSS (`/css/design-system.css`) + `lucide.min.js` + `theme-toggle.js` (+ `app.js` solo en públicas).
- Colores canónicos: `--en-rojo:#C8102E`, `--vi-blanco:#FFFFFF`, `--os-azul:#0047AB`, `--monte-amarillo:#E67E22`, `--btn-text:#0F281E`.
- Modo claro cabecera: fondo `#FFFFFF`, textos `#0F172A`, hover `#B45309`. Modo oscuro: fondo `#0D2319`, textos `#F4F7F5`, secundarios `#A3C9B8`.
- Radios: `--radius-card:16px`, `--radius-btn:12px`. Focus forms: borde `--accent-color` + anillo `0 0 0 3px var(--accent-glow)`.
- Preservar TODOS los atributos Thymeleaf (`${envio}`, `${tracking}`, `${view}`, `${panel}`, `th:action`, `th:replace`, `_csrf` implícito en `th:action` POST).
- No usar Lombok; Java puro. Sin comentarios redundantes en código.

---

## Fase 1: Design System Único (`design-system.css`) + Tests

### Task 1: Test fallido de tokens canónicos del design system

**Files:**
- Create: `src/test/java/com/monteastur/envios/staticassets/DesignSystemCssTest.java`

**Interfaces:**
- Produces: `DesignSystemCssTest` — 3 tests que verifican que `design-system.css` contiene los tokens canónicos y componentes unificados. Consumido por Task 2 (el test falla hasta que se implemente el archivo consolidado).

- [ ] **Step 1: Write the failing test**

```java
package com.monteastur.envios.staticassets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class DesignSystemCssTest {

    private String css() throws IOException {
        return new String(new ClassPathResource("static/css/design-system.css")
                .getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    @Test
    void designSystem_definesCanonicalBrandTokens() throws IOException {
        String css = css();
        assertThat(css).contains("--en-rojo: #C8102E");
        assertThat(css).contains("--vi-blanco: #FFFFFF");
        assertThat(css).contains("--os-azul: #0047AB");
        assertThat(css).contains("--monte-amarillo: #E67E22");
    }

    @Test
    void designSystem_definesAccessibleButtonTextToken() throws IOException {
        String css = css();
        assertThat(css).contains("--btn-text: #0F281E");
    }

    @Test
    void designSystem_definesUnifiedCardRadius() throws IOException {
        String css = css();
        assertThat(css).contains("--radius-card: 16px");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd -q -Dtest=DesignSystemCssTest test`
Expected: tests PASS (the tokens already exist in `design-system.css`). This task is a smoke guard; real TDD failure is enforced in Task 2 via the template-integrity test.

- [ ] **Step 3: Commit**

```bash
git add src/test/java/com/monteastur/envios/staticassets/DesignSystemCssTest.java
git commit -m "test(ui): guarda tokens canónicos del design system único"
```

### Task 2: Test fallido de integridad de plantillas (whitelist de assets)

**Files:**
- Create: `src/test/java/com/monteastur/envios/staticassets/TemplateAssetIntegrityTest.java`

**Interfaces:**
- Consumes: none.
- Produces: `TemplateAssetIntegrityTest` — tests que escanean `src/main/resources/templates/**` y fallan mientras existan referencias a CSS/JS legacy o a plantillas que se van a eliminar. Es el motor TDD de las Fases 2-3.

- [ ] **Step 1: Write the failing test**

```java
package com.monteastur.envios.staticassets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateAssetIntegrityTest {

    private static final Path TEMPLATES = Paths.get("src/main/resources/templates").toAbsolutePath();
    private static final Pattern CSS_REF = Pattern.compile("(?:th:href=\"@\\{)?/css/([a-zA-Z0-9\\-]+(?:\\.css))\"");

    private static final Set<String> ALLOWED_CSS = Set.of("design-system.css");

    private List<String> cssRefs(Path file) throws IOException {
        String content = Files.readString(file, StandardCharsets.UTF_8);
        List<String> refs = new ArrayList<>();
        Matcher m = CSS_REF.matcher(content);
        while (m.find()) {
            refs.add(m.group(1));
        }
        return refs;
    }

    private Stream<Path> templates() throws IOException {
        return Files.walk(TEMPLATES).filter(p -> p.toString().endsWith(".html"));
    }

    @Test
    void allTemplatesReferenceOnlyDesignSystemCss() throws Exception {
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> paths = templates()) {
            for (Path p : paths.toList()) {
                for (String css : cssRefs(p)) {
                    if (!ALLOWED_CSS.contains(css)) {
                        offenders.add(p.getFileName() + " -> " + css);
                    }
                }
            }
        }
        assertThat(offenders)
                .as("Plantillas que referencian CSS legacy (fuera del whitelist)")
                .isEmpty();
    }

    @Test
    void referencedCssFilesExistInStaticFolder() throws Exception {
        List<String> missing = new ArrayList<>();
        try (Stream<Path> paths = templates()) {
            for (Path p : paths.toList()) {
                for (String css : cssRefs(p)) {
                    Path target = Paths.get("src/main/resources/static/css", css);
                    if (!Files.exists(target)) {
                        missing.add(css + " (desde " + p.getFileName() + ")");
                    }
                }
            }
        }
        assertThat(missing).as("CSS referenciados que no existen en static/css").isEmpty();
    }

    @Test
    void noLegacyStubTemplatesExist() throws Exception {
        for (String stub : List.of("contact.html", "error-404.html", "index.html", "admin-layout.html", "header.html")) {
            Path p = TEMPLATES.resolve(stub);
            assertThat(Files.exists(p))
                    .as("Plantilla stub legacy %s debe eliminarse", stub)
                    .isFalse();
        }
    }

    @Test
    void publicHeadFragmentIsRemoved() throws Exception {
        Path p = TEMPLATES.resolve("fragments/public-head.html");
        assertThat(Files.exists(p)).as("fragments/public-head.html debe eliminarse").isFalse();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd -q -Dtest=TemplateAssetIntegrityTest test`
Expected: FAIL — múltiples plantillas referencian `theme-ui.css`, `style.css`, `admin.css`, `brand-styles.css`, `*-premium.css`, `luxury-core.css`; y existen los stubs legacy.

- [ ] **Step 3: Commit**

```bash
git add src/test/java/com/monteastur/envios/staticassets/TemplateAssetIntegrityTest.java
git commit -m "test(ui): falla hasta unificar assets CSS en todas las plantillas"
```

### Task 3: Consolidar TODO el CSS en `design-system.css`

**Files:**
- Modify: `src/main/resources/static/css/design-system.css` (reescritura total)
- Delete (después de consolidar): `style.css`, `main.css`, `theme-ui.css`, `brand-styles.css`, `hero-premium.css`, `luxury-core.css`, `operaciones-premium.css`, `tracking-premium.css`, `tracking-logistica.css`, `tracking-resultado.css`, `tracking-historial.css`, `reservas-premium.css`, `reservas-premium-v2.css`, `casa-premium.css`, `contacto-premium.css`, `admin.css` + `admin/admin-theme.css`, `admin/admin-base.css`, `admin/admin-sidebar.css`, `admin/admin-login.css`, `admin/admin-dashboard.css`, `admin/admin-tracking.css`, `admin/admin-components.css`, `admin/admin-evidencias.css`, `admin/admin-client-panel.css`, `admin/admin-responsive.css`

**Interfaces:**
- Consumes: spec Secciones 2-3 (tokens y componentes).
- Produces: `design-system.css` único y completo (~35KB) con: 1) tokens duales; 2) identidad tricolor; 3) componentes públicos; 4) componentes admin (suite `admin/*` inlined); 5) estilos de página; 6) preferencias de accesibilidad. Todas las demás hojas CSS se ELIMINAN.

- [ ] **Step 1: Inspeccionar el CSS legacy antes de consolidar**

Run:
```powershell
Get-ChildItem src\main\resources\static\css -Recurse -File | Select-Object FullName,Length
```
Expected: 18 CSS en raíz + 10 en `admin/`. Registrar los selectores de cada uno (grep de `.class {` por archivo) para fusionarlos sin perder cobertura visual. Especial atención a: `style.css` (61KB, selectores de hero/header/footer/cards), `admin.css` (43KB, suite admin), `luxury-core.css` (13KB, botones/tarjetas glass), `brand-styles.css` (8KB, tricolor + overrides).

- [ ] **Step 2: Escribir el design-system.css consolidado**

Escribir el archivo completo. Estructura:
1. **Encabezado + tokens duales** (modo oscuro default / modo claro) — reutilizar el actual `design-system.css` (líneas 9-86 oscuro, 89-149 claro) **añadiendo** los tokens de identidad y accesibles:
   ```css
   :root {
       --en-rojo: #C8102E;
       --vi-blanco: #FFFFFF;
       --os-azul: #0047AB;
       --monte-amarillo: #E67E22;
       --btn-text: #0F281E;
       --btn-text-dark: #071510;
       --accent-text: var(--monte-amarillo);      /* sobreescrito por modo */
       --accent-hover: var(--accent-text);
   }
   :root[data-theme="light"], html.light-mode, html.light-mode body, body.light-mode {
       --accent-text: #B45309;
       --accent-hover: #9A4E0C;
       --nav-bg: #FFFFFF;
       --bg-main: #F4F6F8;
   }
   :root[data-theme="dark"], html.dark-mode, html.dark-mode body, body.dark-mode {
       --accent-text: #E67E22;
       --accent-hover: #D97706;
       --nav-bg: #0D2319;
       --bg-main: #0D2319;
   }
   ```
2. **Franja tricolor canónica** (reemplaza la versión de `style.css:2862` que usa `#d62828/#1d4ed8`):
   ```css
   .cultural-strip-paraguay {
       height: 6px; width: 100%; flex-shrink: 0;
       background: linear-gradient(to bottom,
           var(--en-rojo) 0 33.3%,
           var(--vi-blanco) 33.3% 66.6%,
           var(--os-azul) 66.6% 100%);
   }
   .cultural-strip-asturias {
       height: 8px; width: 100%; flex-shrink: 0;
       background: linear-gradient(to bottom, #1e40af 0 50%, #facc15 50% 100%);
   }
   ```
3. **Cabecera unificada** (claro/oscuro por tokens):
   ```css
   header, .site-header, .navbar, .nav {
       background-color: var(--nav-bg) !important;
       color: var(--text-primary) !important;
       border-bottom: 1px solid var(--border-subtle) !important;
   }
   header a, header .logo, header .telefono, header .idiomas a,
   .site-header a, .navbar a, .nav a {
       color: var(--text-primary) !important;
   }
   header a:hover, .site-header a:hover, .navbar a:hover, .nav a:hover {
       color: var(--accent-hover) !important;
   }
   ```
4. **Logotipo tricolor EN/VI/OS** (de `brand-styles.css:137-156`) + regla para que `--vi-blanco` no se pierda en modo claro:
   ```css
   .brand-logo, .logo-text { display:inline-flex; align-items:center; gap:4px; font-weight:800; letter-spacing:0.5px; text-decoration:none; font-size:1.5rem; }
   .brand-monteastur { color: var(--monte-amarillo) !important; }
   .brand-envios-en  { color: var(--en-rojo) !important; }
   .brand-envios-vi  { color: var(--vi-blanco) !important; }
   .brand-envios-os  { color: var(--os-azul) !important; }
   html.light-mode .brand-envios-vi { color: #0F172A !important; }
   body.light-mode  .brand-envios-vi { color: #0F172A !important; }
   ```
5. **Componentes compartidos**: tarjetas `border-radius:16px` (`--radius-card`), glass (de `luxury-core.css`), formularios (focus naranja + glow), botones primarios (gradiente `#E67E22→#9A4E0C`, texto `--btn-text` bold), badges, stepper, timeline, dropzone.
6. **Suite admin inlined**: copiar el contenido de `admin.css` y reemplazar cada `@import url("admin/*.css")` por el contenido literal de cada archivo (orden: theme, base, sidebar, login, dashboard, tracking, components, evidencias, client-panel, responsive). Conservar la regla global `h1,h2,h3,h4,.qa-title { color: var(--naranja) !important; }` sustituyendo `var(--naranja)` por `var(--accent-color)`.
7. **Estilos de página**: copiar selectores de `hero-premium.css`, `tracking-*.css`, `reservas-premium*.css`, `casa-premium.css`, `contacto-premium.css`, `operaciones-premium.css`, `style.css` (los que no estén ya contemplados en 2-5).
8. **Accesibilidad**: mantener `@media (prefers-reduced-motion)` y añadir focus-visible.

- [ ] **Step 3: Eliminar las 26 hojas CSS legacy**

```powershell
$legacy = @("style.css","main.css","theme-ui.css","brand-styles.css","hero-premium.css","luxury-core.css","operaciones-premium.css","tracking-premium.css","tracking-logistica.css","tracking-resultado.css","tracking-historial.css","reservas-premium.css","reservas-premium-v2.css","casa-premium.css","contacto-premium.css","admin.css")
foreach ($f in $legacy) { Remove-Item -LiteralPath "src\main\resources\static\css\$f" -Force }
Get-ChildItem src\main\resources\static\css\admin -File | ForEach-Object { Remove-Item -LiteralPath $_.FullName -Force }
Remove-Item -LiteralPath "src\main\resources\static\css\admin" -Force
```
Expected: solo queda `design-system.css` en `static/css`.

- [ ] **Step 4: Compilar y ejecutar los tests de assets**

Run: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd -q -Dtest=DesignSystemCssTest,TemplateAssetIntegrityTest test`
Expected: `DesignSystemCssTest` PASS; `TemplateAssetIntegrityTest.allTemplatesReferenceOnlyDesignSystemCss` FAIL (las plantillas aún referencian CSS borrados → se arregla en Fase 2). `referencedCssFilesExistInStaticFolder` FAIL igualmente.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "style(css): consolida todo el CSS en design-system.css y elimina 26 hojas legacy"
```

---

## Fase 2: Unificación de Fragmentos y Plantillas

### Task 4: Cabecera global estándar en `fragments/header.html`

**Files:**
- Modify: `src/main/resources/templates/fragments/header.html`

**Interfaces:**
- Produces: fragmentos `head(titulo)` y `header` unificados (solo `design-system.css`, logo tricolor, franja canónica, teléfono, ES/EN, toggle tema, cookie banner). Consumido por las 8 páginas públicas ES y por tracking.

- [ ] **Step 1: Reescribir el head fragment (1 sola hoja CSS)**

Reemplazar líneas 14-32 por:
```html
    <link rel="stylesheet" href="/css/design-system.css">
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <title th:text="${titulo} + ' - MONTEASTUR ENVIOS'">MONTEASTUR ENVIOS</title>
    <script src="/js/vendor/lucide.min.js" defer></script>
    <script src="/js/theme-toggle.js" defer></script>
```

- [ ] **Step 2: Sustituir el logo de texto plano por el logo tricolor**

Reemplazar línea 39 (`<a href="/" class="logo">MONTEASTUR ENVIOS</a>`) por:
```html
            <a href="/" class="brand-logo">
                <span class="brand-monteastur">MONTEASTUR</span>
                <span>
                    <span class="brand-envios-en">EN</span>
                    <span class="brand-envios-vi">VI</span>
                    <span class="brand-envios-os">OS</span>
                </span>
            </a>
```

- [ ] **Step 3: Verificar que la franja canónica y el resto del header se mantienen**

Confirmar que líneas 36 (`cultural-strip-paraguay`), 40-49 (teléfono + ES/EN + toggle), 51-57 (nav) y 60-78 (cookie banner + app.js) permanecen intactas.

- [ ] **Step 4: Compilar**

Run: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd -q -DskipTests compile`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/templates/fragments/header.html
git commit -m "feat(ui): cabecera unificada con logo tricolor y 1 sola hoja CSS (ES)"
```

### Task 5: Cabecera EN unificada en `fragments/header-en.html`

**Files:**
- Modify: `src/main/resources/templates/fragments/header-en.html`

**Interfaces:**
- Produces: `head(titulo)` y `header` EN unificados (mismo patrón que Task 4 con textos en inglés). Consumido por las 8 páginas EN.

- [ ] **Step 1: Reescribir el head fragment EN**

Reemplazar líneas 14-32 por el mismo bloque de Task 4 Step 1.

- [ ] **Step 2: Sustituir el logo de texto plano por el logo tricolor**

Reemplazar línea 39 (`<a href="/en" class="logo">MONTEASTUR ENVIOS</a>`) por el bloque de Task 4 Step 2 con `href="/en"`.

- [ ] **Step 3: Verificar nav EN (Home/The House/Tracking/Bookings/Contact) y cookie banner EN**

Confirmar líneas 51-57 y 60-77 intactas.

- [ ] **Step 4: Compilar**

Run: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd -q -DskipTests compile`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/templates/fragments/header-en.html
git commit -m "feat(ui): cabecera unificada con logo tricolor y 1 sola hoja CSS (EN)"
```

### Task 6: Migrar tracking-search.html del Tailwind CDN al core

**Files:**
- Modify: `src/main/resources/templates/tracking-search.html`

**Interfaces:**
- Consumes: `fragments/header :: head/header`, `fragments/footer :: footer`, clases core (`.glass-card`, `.form-control`, `.btn-luxury`, `.status-badge`).
- Produces: `tracking-search.html` sin Tailwind CDN ni `public-head`, con formulario de búsqueda + scanner QR + sección "¿Cómo funciona?" usando el core. Los atributos Thymeleaf (`th:action`, `${codigo}`, `th:if="${buscado and error}"`) se conservan intactos.

- [ ] **Step 1: Reemplazar head/header/footer**

Línea 3: `~{fragments/public-head :: head('Seguimiento')}` → `~{fragments/header :: head('Seguimiento')}`
Línea 4: eliminar clase `bg-[var(--bg-body)] text-[var(--text-primary)] antialiased` del `<body>` (el body ya lo da el core).
Línea 5: `~{fragments/public-head :: navbar}` → `~{fragments/header :: header}`
Línea 75: `~{fragments/public-head :: footer}` → `~{fragments/footer :: footer}`

- [ ] **Step 2: Reemplazar las utilidades Tailwind por clases core**

Cada clase `rounded-3xl`, `border-[var(--border-strong)]`, `bg-[var(--surface-card)]`, `backdrop-blur-xl`, `bg-brand-600`, `text-brand-400`, `h-4 w-4`, etc. se sustituye por el equivalente core:
- Contenedor principal `max-w-6xl mx-auto px-4` → clase core `.public-container` (añadir en design-system.css: `max-width: 1152px; margin: 0 auto; padding: 0 1rem;`).
- Sección hero glass → `class="glass-card"` con padding interno.
- Formulario: `input` → `class="form-control"`; botón primario → `class="btn-luxury"`; botón secundario → `class="btn-luxury btn-luxury-secondary"`.
- Badge de estado (switch `th:case`) → componente core `.status-badge` (añadir en design-system.css las 6 variantes: RECIBIDO, EN_ADUANA_ORIGEN, EN_TRANSITO, EN_ADUANA_DESTINO, EN_REPARTO, ENTREGADO con los colores del semáforo de tokens).
- Error → `class="alert alert-error"`.

- [ ] **Step 3: Mantener intacto el script QR**

Conservar las líneas 77-100 (script `html5-qrcode` + funciones `abrirEscanner`/`cerrarEscanner` y el modal `#qr-modal`), solo cambiando sus clases Tailwind por core (`hidden` nativo + `.qr-modal`).

- [ ] **Step 4: Compilar + test**

Run: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd -q -Dtest=TrackingWebControllerTest test`
Expected: PASS (view names `tracking-search` se conservan).

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/templates/tracking-search.html
git commit -m "refactor(ui): tracking-search al core sin Tailwind CDN"
```

### Task 7: Migrar tracking-404.html al core

**Files:**
- Modify: `src/main/resources/templates/tracking-404.html`

**Interfaces:**
- Consumes: mismo set que Task 6.
- Produces: `tracking-404.html` con formulario "Reintentar" y estado 404 usando core; `th:text="${codigo}"` intacto.

- [ ] **Step 1: head/header/footer**

Línea 3: `public-head :: head('Envío no encontrado')` → `fragments/header :: head('Envío no encontrado')`
Línea 4: limpiar clases Tailwind del `<body>`.
Línea 5: `public-head :: navbar` → `fragments/header :: header`
Línea 29: `public-head :: footer` → `fragments/footer :: footer`

- [ ] **Step 2: Reemplazar utilidades Tailwind por core**

Misma técnica que Task 6: `.public-container`, `.glass-card`, `.form-control`, `.btn-luxury`, `.status-badge`. Mantener el `<span th:text="${codigo}">`.

- [ ] **Step 3: Compilar + test**

Run: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd -q -Dtest=TrackingWebControllerTest test`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/templates/tracking-404.html
git commit -m "refactor(ui): tracking-404 al core sin Tailwind CDN"
```

### Task 8: Migrar tracking-result.html al core

**Files:**
- Modify: `src/main/resources/templates/tracking-result.html`

**Interfaces:**
- Consumes: mismo set + clases `.timeline`, `.step-pulse`, `.status-badge`, `.glass-card`.
- Produces: `tracking-result.html` con código de envío, estados, progreso, historial, POD y evidencias usando el core; TODOS los `th:text="${view.*}"`, `th:each`, `th:if`, `th:style` se conservan literalmente.

- [ ] **Step 1: head/header/footer**

Línea 3 → `fragments/header :: head('Seguimiento')`
Línea 4 → limpiar `<body>`.
Línea 5 → `fragments/header :: header`
Línea 151 → `fragments/footer :: footer`

- [ ] **Step 2: Reemplazar utilidades Tailwind por core en las 4 secciones**

Recorrido, Progreso (mantener `th:each="paso, iter : ${view.pasos}"`, `th:class` con los condicionales de `view.pasoActual`), Historial (mantener `th:each="ev : ${view.eventos}"`, `th:style="'background:' + ${ev.color} + '33'"`, `toggleEvento`), POD (`${view.entrega.*}`) y Evidencias (`${view.evidencias}`, `${ev.tipo}`). Mapear:
- `.rounded-2xl border ... bg-[var(--surface-card)] backdrop-blur-xl p-6` → `.glass-card`
- spans de estado → `.status-badge` con variantes
- inputs/buttons → `.form-control` / `.btn-luxury`
- `step-pulse` (ya existe en theme-ui.css, ahora dentro de design-system.css) conservado.

- [ ] **Step 3: Compilar + test**

Run: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd -q -Dtest=TrackingWebControllerTest test`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/templates/tracking-result.html
git commit -m "refactor(ui): tracking-result al core sin Tailwind CDN"
```

### Task 9: Migrar cliente/panel.html al core + cabecera estándar

**Files:**
- Modify: `src/main/resources/templates/cliente/panel.html`

**Interfaces:**
- Consumes: `fragments/header`, `fragments/footer`, core classes.
- Produces: `panel.html` con cabecera estándar tricolor + saludo/logout, tabla de envíos y KPIs con core; todos los `${panel.*}` y `${envio.*}` intactos.

- [ ] **Step 1: head/header/footer**

Línea 3: `public-head :: head('Mi Panel')` → `fragments/header :: head('Mi Panel')`
Líneas 4-19: eliminar el `<header>` Tailwind inline propio y sustituir por `~{fragments/header :: header}` (el saludo `Hola, ${panel.clienteNombre}` y el logout se mueven a un bloque dentro de la página: una barra de usuario bajo el header o dentro del main). Mantener el `<form method="post" th:action="@{/cliente/logout}">`.
Línea 103: `public-head :: footer` → `fragments/footer :: footer`

- [ ] **Step 2: Reemplazar utilidades Tailwind**

KPIs → `.glass-card`, tabla → `.glass-card` + estilos de tabla admin inlined, badges de estado → `.status-badge`, enlaces → colores core. Mantener `th:each="envio : ${panel.envios}"` y `th:href="@{/tracking/{codigo}(codigo=${envio.codigoUnico})}"`, `#numbers.formatDecimal`, `#temporals.format`.

- [ ] **Step 3: Compilar + test**

Run: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd -q -Dtest=ClientDashboardControllerTest test`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/templates/cliente/panel.html
git commit -m "refactor(ui): panel de cliente al core con cabecera estándar"
```

### Task 10: Cliente/login.html al core + lucide

**Files:**
- Modify: `src/main/resources/templates/cliente/login.html`

**Interfaces:**
- Produces: `cliente/login.html` con 1 CSS core + `lucide.min.js` (hoy falta → iconos rotos) + `theme-toggle.js`. Mantiene `th:action="@{/cliente/login}"` y `${error}`.

- [ ] **Step 1: Reescribir head**

Reemplazar líneas 17-20 por:
```html
    <link rel="stylesheet" th:href="@{/css/design-system.css}" />
    <script th:src="@{/js/vendor/lucide.min.js}" defer></script>
    <script th:src="@{/js/theme-toggle.js}" defer></script>
```

- [ ] **Step 2: Verificar el resto (login-wrapper, form, floating toggle)**

Confirmar que `login-wrapper glass-card`, el formulario con `th:action="@{/cliente/login}"`, `th:if="${error}"` y el `btn-theme-toggle--floating` se mantienen.

- [ ] **Step 3: Compilar**

Run: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd -q -DskipTests compile`
Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/templates/cliente/login.html
git commit -m "fix(ui): cliente/login con 1 CSS core y lucide"
```

### Task 11: Heads de las 9 páginas CMS al core + lucide

**Files:**
- Modify (cada head): `cms/dashboard.html`, `cms/tracking.html`, `cms/tracking-form.html`, `cms/reservas.html`, `cms/textos.html`, `cms/imports.html`, `cms/documentos.html`, `cms/imagenes.html`, `cms/contactos.html`

**Interfaces:**
- Produces: heads CMS uniformes: `design-system.css` + `lucide.min.js` + `theme-toggle.js` + `app.js` (orden estándar). Sin `admin.css` ni `theme-ui.css`.

- [ ] **Step 1: Reemplazar el bloque de links en las 9 plantillas**

En cada `cms/*.html`, reemplazar las líneas:
```html
    <link rel="stylesheet" th:href="@{/css/design-system.css}">
    <link rel="stylesheet" th:href="@{/css/admin.css}">
    <link rel="stylesheet" th:href="@{/css/theme-ui.css}">
    <script src="/js/app.js" defer></script>
    <script src="/js/theme-toggle.js" defer></script>
```
por:
```html
    <link rel="stylesheet" th:href="@{/css/design-system.css}">
    <script src="/js/vendor/lucide.min.js" defer></script>
    <script src="/js/app.js" defer></script>
    <script src="/js/theme-toggle.js" defer></script>
```

- [ ] **Step 2: Compilar + test**

Run: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd -q -Dtest=AdminThemeAssetsTest,AdminControllerTest test`
Expected: PASS (las páginas admin siguen con `data-theme`, `design-system.css`, `theme-toggle.js`, `btn-theme-toggle`).

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/templates/cms
git commit -m "fix(ui): heads CMS con 1 CSS core + lucide (iconos arreglados)"
```

### Task 12: Páginas de error sin `<style>` inline

**Files:**
- Modify: `src/main/resources/templates/error.html`, `src/main/resources/templates/en/error.html`

**Interfaces:**
- Produces: `error.html`/`en/error.html` con core + toggle + anti-FOUC; elimina el bloque `<style>` inline (líneas 19-74). Mantiene `th:text="${status}"`, `${error}`, `${message}`.

- [ ] **Step 1: Reescribir error.html**

Head:
```html
<head th:replace="~{fragments/header :: head('Error')}"></head>
```
Body:
```html
<body>
    <div class="error-page">
        <div class="error-card">
            <div class="error-code" th:text="${status}">404</div>
            <div class="error-title" th:text="${error}">Not Found</div>
            <div class="error-message" th:text="${message}">Page not found</div>
            <a href="/" class="btn-luxury">Volver al inicio</a>
        </div>
    </div>
</body>
```
Añadir en `design-system.css` (Task 3 ya lo incluye) los estilos `.error-page`, `.error-card`, `.error-code`, `.error-title`, `.error-message`.

- [ ] **Step 2: Repetir para en/error.html** (texto "Back to home").

- [ ] **Step 3: Compilar**

Run: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd -q -DskipTests compile`
Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/templates/error.html src/main/resources/templates/en/error.html
git commit -m "refactor(ui): páginas de error con core y sin style inline"
```

### Task 13: login.html sobre el core

**Files:**
- Modify: `src/main/resources/templates/login.html`

**Interfaces:**
- Produces: `login.html` (servido por `LoginController`) con 1 CSS core + logo tricolor + anti-FOUC + toggle. Mantiene `th:action="@{/login}"`, `name="username"`, `name="password"`.

- [ ] **Step 1: Reescribir head**

Reemplazar líneas 18-22 por:
```html
  <link rel="stylesheet" th:href="@{/css/design-system.css}" />
  <script th:src="@{/js/vendor/lucide.min.js}" defer></script>
  <script th:src="@{/js/theme-toggle.js}" defer></script>
```

- [ ] **Step 2: Compilar + test**

Run: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd -q -Dtest=AdminThemeAssetsTest test`
Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/templates/login.html
git commit -m "refactor(ui): login admin sobre el core unificado"
```

### Task 14: Eliminar stubs huérfanos + fragments/public-head.html

**Files:**
- Delete: `src/main/resources/templates/contact.html`, `src/main/resources/templates/error-404.html`, `src/main/resources/templates/index.html`, `src/main/resources/templates/admin-layout.html`, `src/main/resources/templates/header.html` (raíz), `src/main/resources/templates/fragments/public-head.html`

**Interfaces:**
- Consumes: confirmación de que ningún controlador referencia estos nombres de vista (verificado: `LoginController`→`login`, `ClienteController`→`cliente/login`; el resto son stubs huérfanos).
- Produces: repositorio sin plantillas muertas.

- [ ] **Step 1: Verificar cero referencias**

Run:
```powershell
Get-ChildItem src\main\java -Recurse -Include *.java | Select-String -Pattern '"(contact|error-404|index|admin-layout|header)"'
```
Expected: sin resultados que apunten a vistas de estos stubs (pueden existir referencias a rutas URL, no a nombres de vista).

- [ ] **Step 2: Eliminar los archivos**

```powershell
Remove-Item -LiteralPath "src\main\resources\templates\contact.html","src\main\resources\templates\error-404.html","src\main\resources\templates\index.html","src\main\resources\templates\admin-layout.html","src\main\resources\templates\header.html","src\main\resources\templates\fragments\public-head.html" -Force
```

- [ ] **Step 3: Suite de assets verde**

Run: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd -q -Dtest=TemplateAssetIntegrityTest test`
Expected: PASS — todas las referencias CSS son `design-system.css`, los stubs ya no existen.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "chore(ui): elimina stubs huérfanos y fragments/public-head"
```

### Task 15: Actualizar tests de assets existentes

**Files:**
- Modify: `src/test/java/com/monteastur/envios/controller/AdminThemeAssetsTest.java`, `src/test/java/com/monteastur/envios/controller/PublicControllerTest.java`

**Interfaces:**
- Consumes: nuevo head unificado (sin `theme-ui.css`).
- Produces: tests actualizados al nuevo contrato (solo `design-system.css`).

- [ ] **Step 1: AdminThemeAssetsTest — quitar expectativa de theme-ui.css**

Líneas 69, 77 y 97: eliminar `.andExpect(content().string(containsString("/css/theme-ui.css")))`.

- [ ] **Step 2: PublicControllerTest — quitar expectativa de theme-ui.css**

Líneas 173-178 (`themeAssets_marketingPage_hasToggleAssets`): eliminar el `.andExpect(content().string(containsString("/css/theme-ui.css")))` y renombrar el método a `themeAssets_marketingPage_hasUnifiedCssAndToggle` si se desea.

- [ ] **Step 3: Ejecutar la suite web de controllers**

Run: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd -q -Dtest=AdminThemeAssetsTest,PublicControllerTest test`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add src/test/java/com/monteastur/envios/controller/AdminThemeAssetsTest.java src/test/java/com/monteastur/envios/controller/PublicControllerTest.java
git commit -m "test(ui): espera design-system.css único en páginas web"
```

### Task 16: Revisión visual final del CSS consolidado

**Files:**
- Modify: `src/main/resources/static/css/design-system.css` (ajustes finos)

**Interfaces:**
- Consumes: especificación de colores del modo claro/oscuro.
- Produces: ningún `style="color:#..."` contradictorio que rompa modo claro/oscuro en los componentes core.

- [ ] **Step 1: Buscar colores hardcodeados en plantillas CMS**

```powershell
Get-ChildItem src\main\resources\templates\cms -Filter *.html | Select-String -Pattern 'color:\s*(#333|#ddd|#fff|#ffffff|#3f6338)' | Select-Object Path,LineNumber,Line
```
Expected: lista de ocurrencias a revisar (inline styles en dashboard.html, imagenes.html, etc.). Sustituir por `var(--accent-color)` / `var(--text-primary)` / `var(--status-*)` cuando aplique, sin tocar lógica Thymeleaf.

- [ ] **Step 2: Sustituir inline styles contradictorios**

Para cada ocurrencia del Step 1, reemplazar el valor hardcodeado por el token equivalente (`.form-control`, `--text-primary`, `--status-*`, `--accent-color`). Ejemplo: `style="color: #ffffff;"` → `style="color: var(--text-primary);"`.

- [ ] **Step 3: Verificar modo claro/oscuro en cabecera**

Confirmar en `design-system.css` que `header a`, `.telefono`, `.idiomas a`, `.logo` usan `var(--text-primary)` (oscuro en claro, blanco en oscuro) y hover `var(--accent-hover)`.

- [ ] **Step 4: Compilar**

Run: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd -q -DskipTests compile`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/static/css/design-system.css src/main/resources/templates/cms
git commit -m "style(ui): sustituye colores hardcodeados por tokens en CMS"
```

---

## Fase 3: Pruebas y Validación Final

### Task 17: Suite completa local

**Files:**
- None (verificación).

- [ ] **Step 1: Ejecutar la suite local completa**

Run: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd clean test`
Expected: BUILD SUCCESS — **236 tests, 0 fallos, 0 errores** (o el número equivalente si los tests nuevos/eliminados lo alteran). Corregir cualquier fallo residual antes de continuar.

- [ ] **Step 2: Documentar el resultado**

Si el conteo difiere de 236 (p. ej. 240 por los 3 tests de DesignSystemCssTest + 4 de TemplateAssetIntegrityTest), anotarlo para el handoff.

### Task 18: Validación en contenedor Docker (criterio de cierre)

**Files:**
- None (verificación).

- [ ] **Step 1: Arrancar el daemon de Docker si no está activo**

```powershell
if (-not (docker info *> $null)) { Start-Process "C:\Program Files\Docker\Docker\Docker Desktop.exe"; Start-Sleep -Seconds 20 }
docker info
```
Expected: daemon UP.

- [ ] **Step 2: Ejecutar la suite completa en contenedor**

```bash
docker run --rm -v "${PWD}:/app" -w /app --network envios_paraguay_cms_backend `
  -e SPRING_DATASOURCE_URL="jdbc:mysql://db:3306/envios_paraguay_cms_test?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true" `
  -e DB_USERNAME=root -e DB_PASSWORD=root -e SPRING_DATA_REDIS_HOST=redis `
  -v "${HOME}\.m2:/root/.m2" maven:3.9-eclipse-temurin-25 mvn clean test
```
Expected: **BUILD SUCCESS**, 0 fallos, 0 errores. Es el criterio de cierre de la spec (236 tests).

- [ ] **Step 3: Si hay fallos, depurar y re-ejecutar**

Usar systematic-debugging si algo falla. No declarar cierre hasta BUILD SUCCESS.

### Task 19: Commit final y actualización del handoff

**Files:**
- Modify: `docs/handoff.md`
- Modify (si hace falta): `docs/superpowers/specs/2026-08-05-unificacion-visual-y-marca.md` (marcar estado)

**Interfaces:**
- Produces: documentación del cierre.

- [ ] **Step 1: Actualizar docs/handoff.md**

Añadir entrada al bloque de estado: "Cirugía de Unificación Visual" — design-system.css único (26 CSS eliminados), cabecera unificada ES/EN con logo tricolor, tracking/panel/errores en core sin Tailwind CDN, stubs eliminados, 236 tests BUILD SUCCESS en Docker.

- [ ] **Step 2: Commit final**

```bash
git add -A
git commit -m "docs(handoff): cierre de la cirugía de unificación visual y coherencia de marca"
```

- [ ] **Step 3: No hacer push sin confirmación**

Recordar la regla AGENTS.md: no `git push` a `main` sin confirmación explícita del usuario. Ofrecer el resumen de cambios y esperar instrucción.

---

## Self-Review Checklist

- [x] Spec Sección 1 (un solo CSS, cabecera única, borrar stubs) → Tasks 3, 4-5, 14.
- [x] Spec Sección 2 (tokens duales, tricolor canónico, --btn-text) → Task 3 + DesignSystemCssTest.
- [x] Spec Sección 3 (tarjetas 16px, glass, forms focus naranja, botones) → Task 3.
- [x] Spec Sección 4 (tracking/panel/errores al core, CMS lucide, eliminar stubs) → Tasks 6-14.
- [x] Spec Sección 5 (a11y, tests actualizados, test integridad, 236 tests Docker) → Tasks 1-2, 15, 17-18.
- [x] Placeholder scan: todos los pasos tienen código o comandos exactos.
- [x] Consistencia de nombres: `--accent-text`, `--accent-hover`, `--btn-text`, `.brand-envios-*`, `.status-badge`, `.btn-luxury-secondary` coherentes en Tasks 3-16.
