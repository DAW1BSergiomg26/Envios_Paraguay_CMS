# Entrega A — Rediseño UI Dark (Sistema de diseño + Tracking + Panel Cliente) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Aplicar el sistema de diseño dark Linear/Stripe/Vercel (tokens oficiales del spec) al fragmento `public-head.html` y a las 4 vistas de tracking/panel, sin romper los 217 tests.

**Architecture:** Centralizar todos los tokens/estilos en `fragments/public-head.html` v2 (Google Fonts Plus Jakarta Sans + Tailwind CDN config extendida + Lucide vendored + fragmentos UI `navbar`/`footer`/`ui-badge`/`glass-card`). Las plantillas heredan vía `th:replace` y solo cambian clases CSS + contenedores.

**Tech Stack:** Thymeleaf, Tailwind CDN (v3), Plus Jakarta Sans (Google Fonts), Lucide vendored (`/js/vendor/lucide.min.js`), Spring Boot 3.3.5.

## Global Constraints

- **Integridad Thymeleaf byte a byte:** conservar intactas todas las expresiones (`th:each`, `th:switch`, `th:case`, `${view.*}`, `${panel.*}`, `${codigo}`, `${_csrf}`, `th:action`, `th:href`, `th:value`), los atributos `name`/`id` y las funciones JS (`toggleEvento`, `abrirEscanner`, `cerrarEscanner`). Solo cambian clases, contenedores y tema.
- **View names inmutables:** los tests asertan `view().name(...)`. No renombrar plantillas ni fragmentos.
- **Brand `#d4762a`** siempre presente; hover `#e08c3f`.
- **Lucide:** usar la copia vendored, nunca el CDN de unpkg.
- **Fondo base `#09090b`**, superficie `#121215`, tarjetas `bg-white/[0.03] backdrop-blur-xl border border-white/15 rounded-2xl`.
- **Verificación de cada entrega:** `./mvnw clean test` → BUILD SUCCESS; `docker compose build app` + `docker compose up -d app` → healthy.

---

## Recipes de clases (reutilizar en todas las tareas)

**body:** `class="bg-[#09090b] text-zinc-100 antialiased font-sans"`

**glass-card:** `class="rounded-2xl border border-white/15 bg-white/[0.03] backdrop-blur-xl transition-all duration-300 ease-in-out hover:border-white/30 hover:scale-[1.01]"`

**input dark:** `class="w-full rounded-xl border border-white/15 bg-white/[0.04] px-4 py-3 text-zinc-100 placeholder-zinc-500 focus:outline-none focus:ring-2 focus:ring-brand-600/40 focus:border-brand-600/40"`

**botón primario:** `class="inline-flex items-center justify-center gap-2 rounded-xl bg-brand-600 hover:bg-brand-700 text-white font-semibold px-6 py-3 transition-all duration-300 ease-in-out hover:scale-[1.01] shadow-glow"`

**badge semántico (wrapper th:switch):** `class="inline-flex items-center rounded-full border px-3 py-1 text-xs font-bold uppercase tracking-wider backdrop-blur-xl"`

| Estado | Clase de la rama `th:case` |
|---|---|
| `RECIBIDO` | `border-indigo-400/30 bg-indigo-400/10 text-indigo-300` |
| `EN_ADUANA_ORIGEN` | `border-violet-400/30 bg-violet-400/10 text-violet-300` |
| `EN_TRANSITO` | `border-amber-400/40 bg-amber-400/10 text-amber-300 shadow-[0_0_16px_rgba(251,191,36,0.2)]` |
| `EN_ADUANA_DESTINO` | `border-violet-400/30 bg-violet-400/10 text-violet-300` |
| `EN_REPARTO` | `border-sky-400/30 bg-sky-400/10 text-sky-300` |
| `ENTREGADO` | `border-emerald-400/40 bg-emerald-400/10 text-emerald-300 shadow-[0_0_18px_rgba(52,211,153,0.35)]` |
| `*` | `border-white/15 bg-white/[0.06] text-zinc-300` |

**título de tarjeta:** `class="text-lg font-bold tracking-tight text-zinc-100"` con eyebrow `class="text-xs font-semibold uppercase tracking-widest text-brand-500"`

---

### Task 1: `public-head.html` v2 — tokens + fonts + lucide + fragmentos UI

**Files:**
- Modify: `src/main/resources/templates/fragments/public-head.html` (full rewrite)
- Test: ejecutar la suite completa (los WebMvcTest renderizan estas plantillas vía los controllers)

**Interfaces:**
- Produces: fragmentos `head(titulo)`, `navbar`, `footer`, `ui-badge(estado)`, `glass-card` — consumidos por las Tasks 2–5.

- [ ] **Step 1: Reescribir el fragmento completo**

Contenido exacto del archivo `src/main/resources/templates/fragments/public-head.html`:

```html
<head th:fragment="head(titulo)">
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title th:text="${titulo} + ' - MONTEASTUR ENVIOS'">MONTEASTUR ENVIOS</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <script src="https://cdn.tailwindcss.com"></script>
    <script>
        tailwind.config = {
            theme: {
                extend: {
                    colors: {
                        brand: {
                            DEFAULT: '#d4762a',
                            50: '#fdf3ea', 100: '#fbe5d1', 400: '#e6a26a',
                            500: '#e08c3f', 600: '#b9631f', 700: '#9a4f1a'
                        }
                    },
                    fontFamily: {
                        sans: ['Plus Jakarta Sans', 'ui-sans-serif', 'system-ui', 'sans-serif']
                    },
                    boxShadow: {
                        glow: '0 0 40px rgba(212,118,42,0.15)',
                        'glow-amber': '0 0 16px rgba(251,191,36,0.2)',
                        'glow-emerald': '0 0 18px rgba(52,211,153,0.35)'
                    }
                }
            }
        }
    </script>
    <script src="/js/vendor/lucide.min.js" defer></script>
    <script>
        document.addEventListener('DOMContentLoaded', function () {
            if (window.lucide) { lucide.createIcons(); }
        });
    </script>
    <style>
        body { font-family: 'Plus Jakarta Sans', ui-sans-serif, system-ui, sans-serif; }
    </style>
</head>

<header th:fragment="navbar" class="sticky top-0 z-40 bg-[#09090b]/80 backdrop-blur-xl border-b border-white/10">
    <nav class="max-w-6xl mx-auto px-4 h-16 flex items-center justify-between">
        <a href="/" class="text-xl font-extrabold tracking-tight">
            <span class="text-brand-500">MONTEASTUR</span> <span class="text-zinc-100">ENVIOS</span>
        </a>
        <div class="hidden md:flex items-center gap-6 text-sm font-medium">
            <a href="/" class="text-zinc-400 hover:text-brand-400 transition-colors">Inicio</a>
            <a href="/casa" class="text-zinc-400 hover:text-brand-400 transition-colors">La Casa</a>
            <a href="/tracking" class="text-brand-400 font-semibold">Seguimiento</a>
            <a href="/reservas" class="text-zinc-400 hover:text-brand-400 transition-colors">Reservas</a>
            <a href="/contacto" class="text-zinc-400 hover:text-brand-400 transition-colors">Contacto</a>
        </div>
    </nav>
</header>

<footer th:fragment="footer" class="bg-[#121215] text-zinc-400 mt-16 border-t border-white/10">
    <div class="max-w-6xl mx-auto px-4 py-10 grid gap-6 md:grid-cols-3">
        <div>
            <p class="font-bold text-zinc-100">MONTEASTUR ENVIOS</p>
            <p class="text-sm mt-2">Envíos entre Asturias y Paraguay con seguimiento en tiempo real.</p>
        </div>
        <div class="text-sm">
            <p class="font-semibold text-zinc-100 mb-2">Enlaces</p>
            <a href="/tracking" class="block hover:text-brand-400">Seguimiento</a>
            <a href="/contacto" class="block hover:text-brand-400">Contacto</a>
        </div>
        <div class="text-sm">
            <p class="font-semibold text-zinc-100 mb-2">Contacto</p>
            <p>+34 642 687 292</p>
            <p>Asturias &harr; Paraguay</p>
        </div>
    </div>
    <div class="border-t border-white/10 text-center text-xs py-4">© 2026 MONTEASTUR ENVIOS</div>
</footer>

<span th:fragment="ui-badge(estado)"
      th:switch="${estado}"
      class="inline-flex items-center rounded-full border px-3 py-1 text-xs font-bold uppercase tracking-wider backdrop-blur-xl">
    <span th:case="'RECIBIDO'" class="border-indigo-400/30 bg-indigo-400/10 text-indigo-300 rounded-full px-3 py-1">Recibido</span>
    <span th:case="'EN_ADUANA_ORIGEN'" class="border-violet-400/30 bg-violet-400/10 text-violet-300 rounded-full px-3 py-1">Aduana de origen</span>
    <span th:case="'EN_TRANSITO'" class="border-amber-400/40 bg-amber-400/10 text-amber-300 shadow-glow-amber rounded-full px-3 py-1">En tránsito</span>
    <span th:case="'EN_ADUANA_DESTINO'" class="border-violet-400/30 bg-violet-400/10 text-violet-300 rounded-full px-3 py-1">Aduana de destino</span>
    <span th:case="'EN_REPARTO'" class="border-sky-400/30 bg-sky-400/10 text-sky-300 rounded-full px-3 py-1">En reparto</span>
    <span th:case="'ENTREGADO'" class="border-emerald-400/40 bg-emerald-400/10 text-emerald-300 shadow-glow-emerald rounded-full px-3 py-1">Entregado</span>
    <span th:case="*" class="border-white/15 bg-white/[0.06] text-zinc-300 rounded-full px-3 py-1" th:text="${estado}">Estado</span>
</span>

<div th:fragment="glass-card" class="rounded-2xl border border-white/15 bg-white/[0.03] backdrop-blur-xl transition-all duration-300 ease-in-out hover:border-white/30"></div>
```

- [ ] **Step 2: Validar que la suite sigue verde**

Run: `./mvnw clean test`
Expected: `BUILD SUCCESS` — 217 tests, 0 fallos (los WebMvcTest de `PublicController`, `TrackingWebController`, `ClientDashboardController` renderizan estas plantillas).

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/templates/fragments/public-head.html
git commit -m "feat(ui): public-head v2 - dark design tokens, Plus Jakarta Sans, Lucide, ui-badge fragment"
```

---

### Task 2: `tracking-search.html` — hero glass dark

**Files:**
- Modify: `src/main/resources/templates/tracking-search.html`

**Interfaces:**
- Consumes: fragmentos `head`, `navbar`, `footer` de `public-head` v2 (Task 1).
- Produces: vista `tracking-search` (view name inmutable).

- [ ] **Step 1: Reescribir la plantilla**

Contenido exacto de `src/main/resources/templates/tracking-search.html` (conserva `name="codigo"`, `th:value="${codigo}"`, `th:if="${buscado and error}"`, funciones `abrirEscanner`/`cerrarEscanner` y el modal QR):

```html
<!DOCTYPE html>
<html lang="es" xmlns:th="http://www.thymeleaf.org">
<head th:replace="~{fragments/public-head :: head('Seguimiento')}"></head>
<body class="bg-[#09090b] text-zinc-100 antialiased">
<header th:replace="~{fragments/public-head :: navbar}"></header>

<main class="max-w-6xl mx-auto px-4">
    <section class="relative mt-12 overflow-hidden rounded-3xl border border-white/15 bg-white/[0.03] backdrop-blur-xl p-8 md:p-12">
        <div class="pointer-events-none absolute -top-24 -right-24 h-72 w-72 rounded-full bg-brand-600/20 blur-3xl"></div>
        <div class="pointer-events-none absolute -bottom-24 -left-24 h-72 w-72 rounded-full bg-indigo-500/10 blur-3xl"></div>
        <span class="inline-flex items-center gap-1.5 rounded-full border border-white/15 bg-white/[0.04] px-3 py-1 text-xs font-semibold uppercase tracking-widest text-brand-400">
            <i data-lucide="map-pin" class="h-3.5 w-3.5"></i> Asturias &harr; Paraguay
        </span>
        <h1 class="mt-4 text-3xl md:text-4xl font-extrabold tracking-tight">Seguimiento de tu envío</h1>
        <p class="mt-2 text-zinc-400 max-w-xl">Introduce tu código MONTEASTUR o escanea el QR de la etiqueta para ver el estado actualizado de tu paquete.</p>

        <form method="post" th:action="@{/tracking}" class="mt-8 flex flex-col sm:flex-row gap-3 max-w-2xl">
            <input type="text" name="codigo" th:value="${codigo}" required autocomplete="off"
                   placeholder="Introduce tu código MONTEASTUR"
                   class="flex-1 rounded-xl border border-white/15 bg-white/[0.04] px-4 py-3 text-zinc-100 placeholder-zinc-500 focus:outline-none focus:ring-2 focus:ring-brand-600/40 focus:border-brand-600/40">
            <button type="submit"
                    class="inline-flex items-center justify-center gap-2 rounded-xl bg-brand-600 hover:bg-brand-700 text-white font-semibold px-6 py-3 transition-all duration-300 ease-in-out hover:scale-[1.01] shadow-glow">
                <i data-lucide="search" class="h-4 w-4"></i> Buscar envío
            </button>
            <button type="button" onclick="abrirEscanner()"
                    class="inline-flex items-center justify-center gap-2 rounded-xl border border-white/15 bg-white/[0.04] hover:bg-white/[0.08] hover:border-white/30 text-zinc-100 font-semibold px-6 py-3 transition-all duration-300 ease-in-out">
                <i data-lucide="qrcode" class="h-4 w-4"></i> Escanear QR
            </button>
        </form>

        <p th:if="${buscado and error}"
           class="mt-4 inline-flex items-center gap-2 rounded-lg border border-red-400/30 bg-red-400/10 text-red-300 text-sm font-medium px-4 py-2">
            <i data-lucide="alert-circle" class="h-4 w-4"></i>
            No encontramos ningún envío con el código
            <span class="font-bold" th:text="${codigo}">MT-...</span>. Verifícalo e inténtalo de nuevo.
        </p>
    </section>

    <section class="mt-12">
        <h2 class="text-xl font-bold tracking-tight">¿Cómo funciona?</h2>
        <div class="mt-4 grid gap-4 md:grid-cols-3">
            <div class="rounded-2xl border border-white/15 bg-white/[0.03] backdrop-blur-xl p-6 transition-all duration-300 ease-in-out hover:border-white/30 hover:scale-[1.01]">
                <span class="inline-flex h-10 w-10 items-center justify-center rounded-xl border border-brand-600/30 bg-brand-600/10 text-brand-400"><i data-lucide="search" class="h-5 w-5"></i></span>
                <p class="mt-3 text-xs font-bold uppercase tracking-wide text-brand-400">1. Busca</p>
                <h3 class="mt-1 font-semibold">Introduce el código</h3>
                <p class="text-sm text-zinc-400 mt-1">El código MONTEASTUR viene en la etiqueta de tu envío (formato MT-AAAA-0001).</p>
            </div>
            <div class="rounded-2xl border border-white/15 bg-white/[0.03] backdrop-blur-xl p-6 transition-all duration-300 ease-in-out hover:border-white/30 hover:scale-[1.01]">
                <span class="inline-flex h-10 w-10 items-center justify-center rounded-xl border border-brand-600/30 bg-brand-600/10 text-brand-400"><i data-lucide="qrcode" class="h-5 w-5"></i></span>
                <p class="mt-3 text-xs font-bold uppercase tracking-wide text-brand-400">2. Escanea</p>
                <h3 class="mt-1 font-semibold">Usa la cámara</h3>
                <p class="text-sm text-zinc-400 mt-1">O escanea el código QR de la etiqueta térmica para abrir el seguimiento al instante.</p>
            </div>
            <div class="rounded-2xl border border-white/15 bg-white/[0.03] backdrop-blur-xl p-6 transition-all duration-300 ease-in-out hover:border-white/30 hover:scale-[1.01]">
                <span class="inline-flex h-10 w-10 items-center justify-center rounded-xl border border-brand-600/30 bg-brand-600/10 text-brand-400"><i data-lucide="package-check" class="h-5 w-5"></i></span>
                <p class="mt-3 text-xs font-bold uppercase tracking-wide text-brand-400">3. Sigue</p>
                <h3 class="mt-1 font-semibold">Consulta el estado</h3>
                <p class="text-sm text-zinc-400 mt-1">Revisa la línea de tiempo, los eventos y el comprobante de entrega (POD) si ya fue entregado.</p>
            </div>
        </div>
    </section>
</main>

<div id="qr-modal" class="hidden fixed inset-0 z-50 bg-[#09090b]/80 backdrop-blur-sm flex items-center justify-center p-4">
    <div class="rounded-2xl border border-white/15 bg-[#121215] max-w-md w-full p-6 shadow-2xl">
        <div class="flex items-center justify-between">
            <h3 class="font-bold tracking-tight">Escanear código QR</h3>
            <button type="button" onclick="cerrarEscanner()" class="text-zinc-400 hover:text-zinc-100 font-bold transition-colors">Cerrar</button>
        </div>
        <div id="qr-reader" class="mt-4 rounded-xl overflow-hidden"></div>
        <p id="qr-error" class="mt-3 text-sm text-red-400 hidden">No se pudo acceder a la cámara. Inténtalo de nuevo o introduce el código manualmente.</p>
    </div>
</div>

<footer th:replace="~{fragments/public-head :: footer}"></footer>

<script src="https://unpkg.com/html5-qrcode@2.3.8/html5-qrcode.min.js"></script>
<script>
    let qrScanner = null;
    function abrirEscanner() {
        document.getElementById('qr-modal').classList.remove('hidden');
        document.getElementById('qr-error').classList.add('hidden');
        if (typeof Html5Qrcode === 'undefined') {
            document.getElementById('qr-error').classList.remove('hidden');
            return;
        }
        qrScanner = new Html5Qrcode('qr-reader');
        qrScanner.start({ facingMode: 'environment' }, { fps: 10, qrbox: 220 },
            function (texto) {
                const m = texto.match(/\/tracking\/([A-Za-z0-9-]+)/);
                const codigo = m ? m[1] : texto.trim();
                window.location.href = '/tracking/' + encodeURIComponent(codigo);
            },
            function () {});
    }
    function cerrarEscanner() {
        document.getElementById('qr-modal').classList.add('hidden');
        if (qrScanner) { qrScanner.stop().then(() => qrScanner.clear()); qrScanner = null; }
    }
</script>
</body>
</html>
```

- [ ] **Step 2: Validar vista**

Run: `./mvnw test -Dtest=TrackingWebControllerTest,PublicControllerTest`
Expected: PASS (view `tracking-search`; los formularios con `name="codigo"` y `th:action` intactos).

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/templates/tracking-search.html
git commit -m "feat(ui): tracking-search dark glass hero with Lucide and glow"
```

---

### Task 3: `tracking-result.html` — tarjetas glass + badges semánticos

**Files:**
- Modify: `src/main/resources/templates/tracking-result.html`

**Interfaces:**
- Consumes: fragmentos `head`, `navbar`, `footer` (Task 1).
- Produces: vista `tracking-result`; mantiene las expresiones `${view.*}` y la función `toggleEvento`.

- [ ] **Step 1: Reescribir la plantilla**

Contenido exacto de `src/main/resources/templates/tracking-result.html` (cada `th:case` usa las clases del recipe de badges; el patrón del progreso conserva la expresión ternaria `th:class` de la línea 64 original; `toggleEvento` intacta):

```html
<!DOCTYPE html>
<html lang="es" xmlns:th="http://www.thymeleaf.org">
<head th:replace="~{fragments/public-head :: head('Seguimiento')}"></head>
<body class="bg-[#09090b] text-zinc-100 antialiased">
<header th:replace="~{fragments/public-head :: navbar}"></header>

<main class="max-w-6xl mx-auto px-4 py-10">
    <a href="/tracking" class="inline-flex items-center gap-1.5 text-sm font-medium text-brand-400 hover:text-brand-500 transition-colors">
        <i data-lucide="arrow-left" class="h-4 w-4"></i> Buscar otro envío
    </a>

    <section class="relative mt-4 overflow-hidden rounded-2xl border border-white/15 bg-white/[0.03] backdrop-blur-xl p-6">
        <div class="pointer-events-none absolute -top-20 -right-20 h-56 w-56 rounded-full bg-brand-600/15 blur-3xl"></div>
        <div class="flex flex-wrap items-center justify-between gap-4">
            <div>
                <span class="text-xs font-semibold uppercase tracking-widest text-zinc-500">Código de envío</span>
                <h1 class="mt-1 text-2xl font-extrabold tracking-tight text-brand-400" th:text="${view.codigoUnico}">MT-2026-0001</h1>
            </div>
            <span th:switch="${view.estado}" class="inline-flex items-center rounded-full border px-3 py-1 text-xs font-bold uppercase tracking-wider backdrop-blur-xl">
                <span th:case="'RECIBIDO'" class="border-indigo-400/30 bg-indigo-400/10 text-indigo-300 rounded-full px-3 py-1">Recibido</span>
                <span th:case="'EN_ADUANA_ORIGEN'" class="border-violet-400/30 bg-violet-400/10 text-violet-300 rounded-full px-3 py-1">Aduana de origen</span>
                <span th:case="'EN_TRANSITO'" class="border-amber-400/40 bg-amber-400/10 text-amber-300 shadow-glow-amber rounded-full px-3 py-1">En tránsito</span>
                <span th:case="'EN_ADUANA_DESTINO'" class="border-violet-400/30 bg-violet-400/10 text-violet-300 rounded-full px-3 py-1">Aduana de destino</span>
                <span th:case="'EN_REPARTO'" class="border-sky-400/30 bg-sky-400/10 text-sky-300 rounded-full px-3 py-1">En reparto</span>
                <span th:case="'ENTREGADO'" class="border-emerald-400/40 bg-emerald-400/10 text-emerald-300 shadow-glow-emerald rounded-full px-3 py-1">Entregado</span>
                <span th:case="*" class="border-white/15 bg-white/[0.06] text-zinc-300 rounded-full px-3 py-1" th:text="${view.estado}">Estado</span>
            </span>
        </div>
        <p class="mt-4 text-sm text-zinc-500">
            Última actualización:
            <span class="font-medium text-zinc-300" th:text="${#temporals.format(view.ultimaActualizacion, 'dd/MM/yyyy HH:mm')}">15/05/2026 14:30</span>
        </p>
    </section>

    <section class="mt-6 grid gap-6 lg:grid-cols-2">
        <div class="rounded-2xl border border-white/15 bg-white/[0.03] backdrop-blur-xl p-6 transition-all duration-300 ease-in-out hover:border-white/30">
            <h2 class="text-lg font-bold tracking-tight">Recorrido</h2>
            <div class="mt-4 flex items-center gap-3">
                <span class="flex-1 rounded-xl border border-white/15 bg-white/[0.04] px-4 py-3 text-sm">
                    <span class="block text-xs uppercase text-zinc-500">Origen</span>
                    <span class="font-semibold text-zinc-100" th:text="${view.origen}">Asturias, España</span>
                </span>
                <span class="text-2xl text-brand-500"><i data-lucide="arrow-right" class="h-6 w-6"></i></span>
                <span class="flex-1 rounded-xl border border-white/15 bg-white/[0.04] px-4 py-3 text-sm">
                    <span class="block text-xs uppercase text-zinc-500">Destino</span>
                    <span class="font-semibold text-zinc-100" th:text="${view.destino}">Asunción, Paraguay</span>
                </span>
            </div>
            <div class="mt-4 grid grid-cols-2 gap-3 text-sm">
                <div>
                    <span class="block text-xs uppercase text-zinc-500">Peso</span>
                    <span class="font-semibold text-zinc-100" th:text="${view.peso}">10 kg</span>
                </div>
                <div>
                    <span class="block text-xs uppercase text-zinc-500">Contenido</span>
                    <span class="font-semibold text-zinc-100" th:text="${view.contenido}">Documentos</span>
                </div>
            </div>
            <p th:if="${view.observaciones != null and !view.observaciones.isEmpty()}"
               class="mt-4 rounded-xl border border-brand-600/30 bg-brand-600/10 px-4 py-3 text-sm text-brand-100" th:text="${view.observaciones}">Nota del operador</p>
        </div>

        <div class="rounded-2xl border border-white/15 bg-white/[0.03] backdrop-blur-xl p-6 transition-all duration-300 ease-in-out hover:border-white/30">
            <h2 class="text-lg font-bold tracking-tight">Progreso del envío</h2>
            <ol class="mt-6">
                <li th:each="paso, iter : ${view.pasos}" class="relative flex gap-4 pb-8 last:pb-0">
                    <span th:class="'mt-1 flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-sm font-bold ' + (${view.pasoActual >= 0 and iter.index < view.pasoActual} ? 'bg-brand-600 text-white' : (${view.pasoActual == iter.index} ? 'ring-2 ring-brand-500 ring-offset-2 ring-offset-[#09090b] bg-brand-600/20 text-brand-300' : 'bg-white/[0.06] text-zinc-500'))"
                          th:text="${iter.index + 1}">1</span>
                    <div class="flex flex-col">
                        <span th:switch="${paso}"
                              th:class="'font-semibold ' + (${view.pasoActual == iter.index} ? 'text-brand-400' : 'text-zinc-300')">
                            <span th:case="'RECIBIDO'">Recibido en origen</span>
                            <span th:case="'EN_ADUANA_ORIGEN'">Aduana de origen</span>
                            <span th:case="'EN_TRANSITO'">Tránsito internacional</span>
                            <span th:case="'EN_ADUANA_DESTINO'">Aduana de destino</span>
                            <span th:case="'EN_REPARTO'">En reparto</span>
                            <span th:case="'ENTREGADO'">Entregado</span>
                            <span th:case="*" th:text="${paso}"></span>
                        </span>
                        <span th:if="${view.pasoActual == iter.index}" class="text-sm font-medium text-brand-500">Estado actual</span>
                    </div>
                </li>
            </ol>
        </div>
    </section>

    <section class="mt-6 rounded-2xl border border-white/15 bg-white/[0.03] backdrop-blur-xl p-6 transition-all duration-300 ease-in-out hover:border-white/30">
        <h2 class="text-lg font-bold tracking-tight">Historial del envío</h2>
        <ul th:if="${not #lists.isEmpty(view.eventos)}" class="mt-4 space-y-3">
            <li th:each="ev : ${view.eventos}" class="rounded-xl border border-white/15 bg-white/[0.02] p-4 transition-colors hover:border-white/25">
                <button type="button" onclick="toggleEvento(this)"
                        class="w-full flex items-center justify-between gap-3 text-left">
                    <span class="flex items-center gap-3">
                        <span class="flex h-8 w-8 items-center justify-center rounded-full text-lg"
                              th:style="'background:' + ${ev.color} + '33'" th:text="${ev.icono}">📦</span>
                        <span>
                            <span class="font-semibold block text-zinc-100" th:text="${ev.titulo}">Título del evento</span>
                            <span class="text-sm text-zinc-500" th:text="${#temporals.format(ev.fechaEvento, 'dd/MM/yyyy HH:mm')}">Fecha</span>
                        </span>
                    </span>
                    <span class="text-zinc-500"><i data-lucide="chevron-down" class="h-4 w-4"></i></span>
                </button>
                <div class="hidden mt-3 text-sm text-zinc-400 space-y-1">
                    <p th:if="${ev.ubicacion != null and !ev.ubicacion.isEmpty()}" class="font-medium text-zinc-300" th:text="'Ubicación: ' + ${ev.ubicacion}">Ubicación</p>
                    <p th:if="${ev.descripcion != null and !ev.descripcion.isEmpty()}" th:text="${ev.descripcion}">Descripción</p>
                </div>
            </li>
        </ul>
        <p th:if="${#lists.isEmpty(view.eventos)}" class="mt-4 text-sm text-zinc-500">Aún no hay eventos registrados para este envío.</p>
    </section>

    <section th:if="${view.entrega != null}" class="mt-6 rounded-2xl border border-emerald-400/30 bg-emerald-400/[0.04] backdrop-blur-xl p-6">
        <h2 class="text-lg font-bold tracking-tight text-emerald-300">Comprobante de entrega (POD)</h2>
        <div class="mt-4 grid gap-6 md:grid-cols-2">
            <div class="space-y-2 text-sm">
                <p class="text-zinc-300"><span class="font-semibold text-zinc-100">Receptor:</span> <span th:text="${view.entrega.receptorNombre}">Nombre</span></p>
                <p class="text-zinc-300"><span class="font-semibold text-zinc-100">Documento:</span> <span th:text="${view.entrega.receptorDocumento}">Doc</span></p>
                <p class="text-zinc-300"><span class="font-semibold text-zinc-100">Fecha de entrega:</span> <span th:text="${#temporals.format(view.entrega.fechaEntrega, 'dd/MM/yyyy HH:mm')}">Fecha</span></p>
                <p th:if="${view.entrega.latitud != null}" class="text-zinc-300">
                    <span class="font-semibold text-zinc-100">GPS:</span>
                    <span th:text="${view.entrega.latitud} + ', ' + ${view.entrega.longitud}">lat, lon</span>
                </p>
                <p th:if="${view.entrega.notas != null and !view.entrega.notas.isEmpty()}" class="text-zinc-300" th:text="${view.entrega.notas}">Notas</p>
            </div>
            <div class="flex flex-col items-center justify-center">
                <span class="text-sm font-semibold text-zinc-500">Firma del receptor</span>
                <img th:src="'data:image/png;base64,' + ${view.entrega.firmaBase64}"
                     alt="Firma del receptor"
                     class="mt-2 max-h-32 rounded-lg border border-white/15 bg-white/[0.04]">
            </div>
        </div>
    </section>

    <section th:if="${not #lists.isEmpty(view.evidencias)}" class="mt-6 rounded-2xl border border-white/15 bg-white/[0.03] backdrop-blur-xl p-6">
        <h2 class="text-lg font-bold tracking-tight">Evidencias del envío</h2>
        <div class="mt-4 grid gap-3 sm:grid-cols-2">
            <div th:each="ev : ${view.evidencias}" class="flex items-center gap-3 rounded-xl border border-white/15 bg-white/[0.02] p-3 transition-colors hover:border-white/25">
                <img th:if="${ev.tipo == 'FOTO'}" th:src="${ev.urlArchivo}" th:alt="${ev.titulo}"
                     class="h-14 w-14 rounded-lg object-cover">
                <span th:if="${ev.tipo != 'FOTO'}" class="flex h-14 w-14 items-center justify-center rounded-lg bg-white/[0.04] text-2xl">📄</span>
                <span>
                    <span class="font-semibold block text-zinc-100" th:text="${ev.titulo}">Título</span>
                    <span class="text-sm text-zinc-500 block" th:text="${#temporals.format(ev.fechaSubida, 'dd/MM/yyyy')}">Fecha</span>
                    <a th:href="${ev.urlArchivo}" target="_blank" class="text-sm font-medium text-brand-400 hover:text-brand-500 hover:underline">Ver</a>
                </span>
            </div>
        </div>
    </section>
</main>

<footer th:replace="~{fragments/public-head :: footer}"></footer>

<script>
    function toggleEvento(boton) {
        const detalle = boton.nextElementSibling;
        detalle.classList.toggle('hidden');
    }
</script>
</body>
</html>
```

- [ ] **Step 2: Validar vista**

Run: `./mvnw test -Dtest=TrackingWebControllerTest,PortalTrackingDashboardIntegrationTest`
Expected: PASS (view `tracking-result`; `toggleEvento` intacta).

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/templates/tracking-result.html
git commit -m "feat(ui): tracking-result dark glass cards with semantic status badges"
```

---

### Task 4: `tracking-404.html` — estado vacío dark

**Files:**
- Modify: `src/main/resources/templates/tracking-404.html`

**Interfaces:**
- Consumes: fragmentos `head`, `navbar`, `footer` (Task 1).
- Produces: vista `tracking-404`.

- [ ] **Step 1: Reescribir la plantilla**

Contenido exacto de `src/main/resources/templates/tracking-404.html`:

```html
<!DOCTYPE html>
<html lang="es" xmlns:th="http://www.thymeleaf.org">
<head th:replace="~{fragments/public-head :: head('Envío no encontrado')}"></head>
<body class="bg-[#09090b] text-zinc-100 antialiased">
<header th:replace="~{fragments/public-head :: navbar}"></header>

<main class="max-w-2xl mx-auto px-4 py-20 text-center">
    <div class="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl border border-white/15 bg-white/[0.04]">
        <i data-lucide="package-search" class="h-8 w-8 text-brand-400"></i>
    </div>
    <p class="mt-6 text-6xl font-extrabold tracking-tight text-brand-500">404</p>
    <h1 class="mt-4 text-2xl font-bold tracking-tight">Envío no encontrado</h1>
    <p class="mt-2 text-zinc-400">
        No encontramos ningún envío con el código
        <span class="font-semibold text-zinc-100" th:text="${codigo}">MT-...</span>. Verifícalo e inténtalo de nuevo.
    </p>
    <form method="post" th:action="@{/tracking}" class="mt-8 flex flex-col sm:flex-row gap-3">
        <input type="text" name="codigo" th:value="${codigo}" required
               class="flex-1 rounded-xl border border-white/15 bg-white/[0.04] px-4 py-3 text-zinc-100 placeholder-zinc-500 focus:outline-none focus:ring-2 focus:ring-brand-600/40 focus:border-brand-600/40">
        <button type="submit" class="inline-flex items-center justify-center gap-2 rounded-xl bg-brand-600 hover:bg-brand-700 text-white font-semibold px-6 py-3 transition-all duration-300 ease-in-out hover:scale-[1.01] shadow-glow">
            <i data-lucide="rotate-ccw" class="h-4 w-4"></i> Reintentar
        </button>
    </form>
    <a href="/tracking" class="mt-6 inline-flex items-center gap-1.5 text-sm font-medium text-brand-400 hover:text-brand-500 hover:underline">
        <i data-lucide="qrcode" class="h-4 w-4"></i> Escanear otro QR
    </a>
</main>

<footer th:replace="~{fragments/public-head :: footer}"></footer>
</body>
</html>
```

- [ ] **Step 2: Validar vista**

Run: `./mvnw test -Dtest=TrackingWebControllerTest,PortalTrackingDashboardIntegrationTest`
Expected: PASS (view `tracking-404`).

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/templates/tracking-404.html
git commit -m "feat(ui): tracking-404 dark empty state with Lucide and glow"
```

---

### Task 5: `cliente/panel.html` — panel cliente dark

**Files:**
- Modify: `src/main/resources/templates/cliente/panel.html`

**Interfaces:**
- Consumes: fragmentos `head`, `footer` (Task 1).
- Produces: vista `cliente/panel`; conserva `${panel.*}`, `${envio.*}` y los enlaces `th:href`.

- [ ] **Step 1: Reescribir la plantilla**

Contenido exacto de `src/main/resources/templates/cliente/panel.html` (header propio pasada a dark glass; conserva el form de logout con `th:action="@{/cliente/logout}"`):

```html
<!DOCTYPE html>
<html lang="es" xmlns:th="http://www.thymeleaf.org">
<head th:replace="~{fragments/public-head :: head('Mi Panel')}"></head>
<body class="bg-[#09090b] text-zinc-100 antialiased">
<header class="sticky top-0 z-40 bg-[#09090b]/80 backdrop-blur-xl border-b border-white/10">
    <div class="max-w-6xl mx-auto px-4 h-16 flex items-center justify-between">
        <span class="text-xl font-extrabold tracking-tight">
            <span class="text-brand-500">MONTEASTUR</span> <span class="text-zinc-100">ENVIOS</span>
        </span>
        <div class="flex items-center gap-4">
            <span class="text-sm font-medium text-zinc-300" th:text="'Hola, ' + ${panel.clienteNombre}">Hola, Nombre</span>
            <form method="post" th:action="@{/cliente/logout}">
                <button type="submit" class="inline-flex items-center gap-1.5 text-sm font-medium text-zinc-400 hover:text-red-400 transition-colors">
                    <i data-lucide="log-out" class="h-4 w-4"></i> Cerrar sesión
                </button>
            </form>
        </div>
    </div>
</header>

<main class="max-w-6xl mx-auto px-4 py-10">
    <h1 class="text-2xl font-bold tracking-tight">Mis envíos</h1>

    <div class="mt-6 grid gap-4 grid-cols-2 md:grid-cols-3 lg:grid-cols-5">
        <div class="rounded-2xl border border-white/15 bg-white/[0.03] backdrop-blur-xl p-4 transition-all duration-300 ease-in-out hover:border-white/30">
            <p class="text-xs font-semibold uppercase tracking-widest text-zinc-500">Total</p>
            <p class="mt-1 text-2xl font-extrabold text-brand-400" th:text="${panel.totalEnvios}">0</p>
        </div>
        <div class="rounded-2xl border border-white/15 bg-white/[0.03] backdrop-blur-xl p-4 transition-all duration-300 ease-in-out hover:border-white/30">
            <p class="text-xs font-semibold uppercase tracking-widest text-zinc-500">Envíos activos</p>
            <p class="mt-1 text-2xl font-extrabold text-zinc-100" th:text="${panel.enviosActivos}">0</p>
        </div>
        <div class="rounded-2xl border border-white/15 bg-white/[0.03] backdrop-blur-xl p-4 transition-all duration-300 ease-in-out hover:border-white/30">
            <p class="text-xs font-semibold uppercase tracking-widest text-zinc-500">Entregados</p>
            <p class="mt-1 text-2xl font-extrabold text-emerald-300" th:text="${panel.enviosEntregados}">0</p>
        </div>
        <div class="rounded-2xl border border-white/15 bg-white/[0.03] backdrop-blur-xl p-4 transition-all duration-300 ease-in-out hover:border-white/30">
            <p class="text-xs font-semibold uppercase tracking-widest text-zinc-500">Peso total (kg)</p>
            <p class="mt-1 text-2xl font-extrabold text-zinc-100" th:text="${#numbers.formatDecimal(panel.pesoTotalKg, 1, 1)}">0.0</p>
        </div>
        <div class="rounded-2xl border border-white/15 bg-white/[0.03] backdrop-blur-xl p-4 transition-all duration-300 ease-in-out hover:border-white/30">
            <p class="text-xs font-semibold uppercase tracking-widest text-zinc-500">Peso activo (kg)</p>
            <p class="mt-1 text-2xl font-extrabold text-zinc-100" th:text="${#numbers.formatDecimal(panel.pesoActivoKg, 1, 1)}">0.0</p>
        </div>
    </div>

    <div class="mt-8 overflow-hidden rounded-2xl border border-white/15 bg-white/[0.02] backdrop-blur-xl">
        <div class="overflow-x-auto">
            <table class="w-full text-sm">
                <thead class="bg-white/[0.04] text-zinc-500 text-left">
                    <tr>
                        <th class="px-4 py-3 font-semibold">Código</th>
                        <th class="px-4 py-3 font-semibold">Destino</th>
                        <th class="px-4 py-3 font-semibold">Contenido</th>
                        <th class="px-4 py-3 font-semibold">Peso</th>
                        <th class="px-4 py-3 font-semibold">Estado</th>
                        <th class="px-4 py-3 font-semibold">Última actualización</th>
                        <th class="px-4 py-3 font-semibold">Acciones</th>
                    </tr>
                </thead>
                <tbody>
                    <tr th:each="envio : ${panel.envios}" class="border-t border-white/10 hover:bg-white/[0.03] transition-colors">
                        <td class="px-4 py-3 font-semibold text-brand-400">
                            <a th:href="@{/tracking/{codigo}(codigo=${envio.codigoUnico})}" th:text="${envio.codigoUnico}">MT-2026-0001</a>
                        </td>
                        <td class="px-4 py-3" th:text="${envio.destino}">Asunción, Paraguay</td>
                        <td class="px-4 py-3" th:text="${envio.contenido}">Documentos</td>
                        <td class="px-4 py-3" th:text="${envio.peso}">10 kg</td>
                        <td class="px-4 py-3">
                            <span th:switch="${envio.estado}" class="inline-flex items-center rounded-full border px-3 py-1 text-xs font-bold uppercase tracking-wider backdrop-blur-xl">
                                <span th:case="'RECIBIDO'" class="border-indigo-400/30 bg-indigo-400/10 text-indigo-300 rounded-full px-2.5 py-0.5">Recibido</span>
                                <span th:case="'EN_ADUANA_ORIGEN'" class="border-violet-400/30 bg-violet-400/10 text-violet-300 rounded-full px-2.5 py-0.5">Aduana origen</span>
                                <span th:case="'EN_TRANSITO'" class="border-amber-400/40 bg-amber-400/10 text-amber-300 shadow-glow-amber rounded-full px-2.5 py-0.5">Tránsito</span>
                                <span th:case="'EN_ADUANA_DESTINO'" class="border-violet-400/30 bg-violet-400/10 text-violet-300 rounded-full px-2.5 py-0.5">Aduana destino</span>
                                <span th:case="'EN_REPARTO'" class="border-sky-400/30 bg-sky-400/10 text-sky-300 rounded-full px-2.5 py-0.5">Reparto</span>
                                <span th:case="'ENTREGADO'" class="border-emerald-400/40 bg-emerald-400/10 text-emerald-300 shadow-glow-emerald rounded-full px-2.5 py-0.5">Entregado</span>
                                <span th:case="*" class="border-white/15 bg-white/[0.06] text-zinc-300 rounded-full px-2.5 py-0.5" th:text="${envio.estado}">Estado</span>
                            </span>
                        </td>
                        <td class="px-4 py-3" th:text="${#temporals.format(envio.ultimaActualizacion, 'dd/MM/yyyy HH:mm')}">15/05/2026 14:30</td>
                        <td class="px-4 py-3">
                            <div class="flex items-center gap-3">
                                <a th:href="@{/tracking/{codigo}(codigo=${envio.codigoUnico})}"
                                   class="inline-flex items-center gap-1 font-medium text-brand-400 hover:text-brand-500 hover:underline">
                                    <i data-lucide="external-link" class="h-3.5 w-3.5"></i> Ver tracking
                                </a>
                                <a th:href="@{/cliente/panel/envio/{codigo}/etiqueta(codigo=${envio.codigoUnico})}"
                                   class="inline-flex items-center gap-1 font-medium text-zinc-400 hover:text-brand-400 hover:underline">
                                    <i data-lucide="download" class="h-3.5 w-3.5"></i> Descargar etiqueta
                                </a>
                            </div>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>
        <div th:if="${#lists.isEmpty(panel.envios)}" class="p-10 text-center text-zinc-500">
            No tienes envíos registrados todavía.
        </div>
    </div>
</main>

<footer th:replace="~{fragments/public-head :: footer}"></footer>
</body>
</html>
```

- [ ] **Step 2: Validar vista**

Run: `./mvnw test -Dtest=ClientDashboardControllerTest,PortalTrackingDashboardIntegrationTest`
Expected: PASS (view `cliente/panel`; el PDF de etiqueta sigue intacto).

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/templates/cliente/panel.html
git commit -m "feat(ui): cliente panel dark glass with semantic badges"
```

---

### Task 6: Gate de Entrega A — suite completa + redeploy + spot-check

**Files:**
- None (validación).

- [ ] **Step 1: Suite completa**

Run: `./mvnw clean test`
Expected: `BUILD SUCCESS` — 217 tests, 0 fallos.

- [ ] **Step 2: Redeploy de la imagen**

Run:
```bash
docker compose build app
docker compose up -d app
```
Expected: contenedor `monteastur-app` healthy (perfil prod).

- [ ] **Step 3: Spot-check visual**

Con curl verificar 200 y que el HTML contiene clases dark:
```bash
curl -s http://localhost:8080/tracking | Select-String -Pattern "bg-\[#09090b\]"
curl -s http://localhost:8080/tracking/PY-WEB-001 | Select-String -Pattern "border-amber-400|border-emerald-400"
curl -s http://localhost:8080/cliente/panel | Select-String -Pattern "bg-\[#09090b\]"
```
Expected: todas las URLs 200 y con las clases dark presentes (las de caché evictar `envios.tracking.pagina::*` con redis-cli si hace falta para ver HTML fresco).

- [ ] **Step 4: Commit de cierre (si hubiera ajustes)**

```bash
git add -A
git commit -m "style(ui): Entrega A - sistema de diseño dark aplicado a tracking y panel cliente"
```

---

## Self-Review del Plan A

- **Cobertura del spec:** tokens (Task 1), `ui-badge` semántico (Task 1), tracking-search/result/404 (Tasks 2–4), panel cliente (Task 5), gate de verificación (Task 6). Cubre la Entrega A completa.
- **Placeholders:** ningún paso usa TBD/TODO; todos los archivos tienen contenido completo.
- **Consistencia de tipos:** el fragmento `ui-badge(estado)` acepta una variable `estado`; las Tasks 2–5 no dependen de él (lo usan vía switch inline, por simplicidad de conservar las etiquetas exactas). Los nombres de vista `tracking-search`/`tracking-result`/`tracking-404`/`cliente/panel` son inmutables y coinciden con los tests.
