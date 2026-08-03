# Spec — Rediseño UI/UX Dark Mode estilo Linear/Stripe/Vercel (Entrega A y B)

**Fecha:** 2026-08-03
**Autor:** OpenCode (Director UI/UX + Arquitecto Frontend)
**Estado:** Aprobado por el usuario (design Q&A + prompt de ejecución faseado)

---

## 1. Contexto y objetivos

Rediseñar la capa visual del CMS de envíos **Envios_Paraguay_CMS** (Spring Boot 3.3.5 + Thymeleaf + Tailwind CDN + Lucide vendored) a una estética dark premium tipo Linear/Stripe/Vercel, **sin romper** las 217 pruebas (verificación `./mvnw clean test`) ni el funcionamiento del backend.

Requisito de oro: **integridad Thymeleaf byte a byte**. Se conservan intactas todas las expresiones (`th:each`, `th:switch`, `th:case`, `${view.*}`, `${codigo}`, `${_csrf}`, `th:object`, `th:field`, etc.), atributos `name`/`id` de formularios y funciones JS existentes (`toggleEvento`, `abrirEscanner`, `cerrarEscanner`, `app.js`). Solo cambian clases CSS, contenedores estructurales y el sistema de temas.

Los tests de controladores (`WebMvcTest`, integración) solo asertan `view().name(...)` y PDFs, nunca contenido HTML → el cambio de capa visual es seguro para la suite.

## 2. Decisiones aprobadas (Q&A)

1. **Sprint faseado en 2 entregas:** A = sistema de diseño + portal tracking + panel cliente; B = marketing público (ES/EN) + admin CMS + logins.
2. **Dark completo en todas partes** (`bg-base #09090b`, superficie `#121215`).
3. **Admin: re-skin de `admin.css`** (conservar clases `glass-card`, `sidebar`, etc. y re-tematizar los 8 archivos CSS de `admin/`). No convertir a Tailwind.
4. **Verificación por entrega:** `./mvnw clean test` (217) + rebuild de imagen docker + spot-check visual en localhost.
5. **Arquitectura UI:** tokens centralizados en `fragments/public-head.html` (Tailwind CDN config extendida) + fragmentos UI reutilizables (badge de estado, glass card).

## 3. Sistema de tokens visuales (oficial)

| Token | Valor |
|---|---|
| `bg-base` | `#09090b` (zinc-950) |
| Superficie | `#121215` |
| Tarjeta glass | `bg-white/[0.03] backdrop-blur-xl border border-white/15 rounded-2xl` |
| Texto | `zinc-100` principal / `zinc-400` secundario / `zinc-500` muted |
| Marca | `#d4762a` (hover `#e08c3f`) |
| Glow marca | `shadow-[0_0_40px_rgba(212,118,42,0.15)]` |
| Bordes | `white/10` base → `white/30` hover |
| Tipografía | Plus Jakarta Sans (400–800) vía Google Fonts; `tracking-tight` en headings |
| Radius | `rounded-2xl` / `rounded-3xl` |
| Micro-interacción | `transition-all duration-300 ease-in-out hover:scale-[1.01] hover:border-white/30` |

### Badges semánticos de estado (glass + borde coloreado, no fills planos)

| Estado | Color |
|---|---|
| `RECIBIDO` | Índigo |
| `EN_ADUANA_ORIGEN` / `EN_ADUANA_DESTINO` | Índigo / Violeta |
| `EN_TRANSITO` | Ámbar brillante |
| `EN_REPARTO` | Sky |
| `ENTREGADO` | Esmeralda brillante + glow |

El patrón `th:switch`/`th:case` de `tracking-result.html` se conserva; cambian las clases de cada rama. El fragmento `ui-badge(estado)` encapsula este patrón.

## 4. Arquitectura de fragmentos

### Entrega A — `fragments/public-head.html` v2
- Google Fonts Plus Jakarta Sans (pesos 400–800) en `<head>`.
- Tailwind CDN con `tailwind.config` extendido:
  - `colors.brand` (DEFAULT `#d4762a`, hover `#e08c3f`, escala 50–700).
  - `fontFamily.sans = ['Plus Jakarta Sans', ...]`.
  - `boxShadow.glow = '0 0 40px rgba(212,118,42,0.15)'`.
  - Fondos radiales ambientales (`bg-radial`).
- Lucide vendored (`/js/vendor/lucide.min.js`) + `lucide.createIcons()` al cargar.
- Fragmentos expuestos:
  - `head(titulo)` — head global.
  - `navbar` — sticky glass dark (reemplaza el navbar claro actual).
  - `footer` — dark.
  - `ui-badge(estado)` — badge semántico encapsulado.
  - `glass-card` — tarjeta glass estándar.
- `cliente/panel.html` hereda estos fragmentos (ya usa `public-head`).

### Entrega B — re-skin CSS (conservar estructura Thymeleaf)
- **Marketing:** re-tematizar a dark los CSS existentes (`luxury-core.css`, `hero-premium.css`, `reservas-premium*.css`, `contacto-premium.css`, `casa-premium.css`, `operaciones-premium.css`, `style.css`) e inyectar Plus Jakarta Sans + glow. Los fragmentos `header.html`/`header-en.html`/`footer.html`/`footer-en.html` se pasan a dark glass.
- **Admin:** re-tematizar `admin.css` + `css/admin/*.css` (8 archivos) a dark; inyectar Jakarta Sans con `@import url('https://fonts.googleapis.com/...')` al inicio de `admin.css` y mantener Lucide vendored (ya cargado por las plantillas admin). Conservar clases y layout actuales (`glass-card`, `sidebar`, `stats-grid`, `quick-action-card`, etc.).
- **Logins** (`login.html`, `cliente/login.html`): tarjeta glass dark centrada sobre fondo `#09090b`.
- **Error** (`error.html`, `en/error.html`): dark.
- **Duplicados EN** (`en/*`): mismos cambios que sus ES.

## 5. Alcance detallado

### Entrega A (sistema + tracking + panel)
- `fragments/public-head.html` (v2: tokens + fonts + lucide + navbar/footer dark + `ui-badge` + `glass-card`).
- `tracking-search.html` — hero glass dark con glow, inputs dark, "Cómo funciona" en glass cards.
- `tracking-result.html` — tarjetas glass (código/estado, recorrido, progreso, historial, POD, evidencias); badges semánticos; JS `toggleEvento` intacto.
- `tracking-404.html` — estado vacío dark glass con icono.
- `cliente/panel.html` — re-tematizar a dark (hereda `public-head`).

### Entrega B (marketing + admin)
- Marketing ES/EN: `home`, `entorno`, `operaciones`, `lacasa`, `reservas`, `contacto`, `politica-cookies`, `aviso-legal`, `error` (+ `en/*`) — vía re-tema de CSS + fragments `header*`/`footer*` dark.
- Admin: `cms/dashboard`, `cms/textos`, `cms/reservas`, `cms/imagenes`, `cms/contactos`, `cms/tracking`, `cms/tracking-form`, `fragments/admin-sidebar`, `login.html`, `cliente/login.html` — re-skin `admin.css`.

## 6. Verificación y definición de terminado

Por cada entrega:
1. `./mvnw clean test` → **BUILD SUCCESS** (217 tests, 0 fallos).
2. `docker compose build app` + `docker compose up -d app` → healthy.
3. Spot-check visual en `localhost:8080` (tracking, panel, home, admin, login) — dark glass, fonts, iconos, sin errores JS.
4. Confirmar que no hay cambios en Java/controllers/backend (solo `src/main/resources/templates`, `src/main/resources/static/css` y fragmentos).

## 7. Riesgos y mitigaciones

| Riesgo | Mitigación |
|---|---|
| Ruptura de binding Thymeleaf | Regla byte a byte: solo clases/contenedores; `mvnw clean test` por entrega |
| Colisión de estilos legacy | Cambios centralizados en fragmentos `public-head`/`header*`; Tailwind CDN sin conflicto con CSS propios |
| Lucide no renderiza | Usar la copia vendored `js/vendor/lucide.min.js` + `createIcons()` (ya operativa) |
| Drift de imagen docker | Rebuild de la imagen desde el working tree tras cada entrega |
| Formularios/JS (app.js, reservas, QR) | No tocar `name`/`id`/funciones; spot-check funcional en el redeploy |
