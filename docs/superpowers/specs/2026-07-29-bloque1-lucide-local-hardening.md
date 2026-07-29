# Bloque 1: Lucide Local + Caché Nginx + Hardening de Credenciales

**Fecha:** 2026-07-29
**Proyecto:** Monteastur Envios (Envios_Paraguay_CMS)
**Rama:** `feature/seguimiento-premium`

---

## Resumen

Aplicar mejoras de alta rentabilidad técnica en dos áreas críticas: independencia offline de assets (Lucide icons) y blindaje de credenciales en producción. Son cambios localizados, reversibles y de alto impacto.

---

## Área 1: Independencia Offline de Lucide + Caché Nginx

### Problema

- Lucide icons se cargan desde `https://unpkg.com/lucide@latest` (CDN externo)
- Si unpkg.com está caído o lento, los iconos no renderizan
- Sin `Cache-Control` en Nginx → el navegador no cachea assets estáticos
- 34+ templates dependen de `data-lucide` + `lucide.createIcons()`

### Solución

1. **Descargar Lucide (UMD) localmente** a `static/js/vendor/lucide.min.js`
2. **Referenciar localmente** desde `header.html` / `header-en.html`:
   - Cambiar `<script src="https://unpkg.com/lucide@latest" defer>`
   - Por `<script src="/js/vendor/lucide.min.js" defer>`
3. **Cache headers en Nginx** para assets con hash / vendor:
   - `location ~* \.(jpg|jpeg|png|gif|ico|css|js|svg)$ { expires 30d; add_header Cache-Control "public, immutable"; }`
   - Versión para vendor JS: mismo tratamiento (inmutables)
4. **CSP simplificado** en `monteastur.conf`:
   - Ya no necesitamos `https://unpkg.com` en `script-src` al servir localmente
   - Mantener `'self'` + `'unsafe-inline'` para estilos

### Archivos a modificar

| Archivo | Cambio |
|---------|--------|
| `src/main/resources/templates/fragments/header.html` | CDN → `/js/vendor/lucide.min.js` |
| `src/main/resources/templates/fragments/header-en.html` | CDN → `/js/vendor/lucide.min.js` |
| `nginx/conf.d/monteastur.conf` | `script-src` quitar `unpkg.com`, añadir `location` cache |
| `nginx/conf.d/local.conf` | Añadir `location` cache para assets |
| `nginx/nginx.conf` | Sin cambios (gzip ya configurado) |

### Archivos a crear

| Archivo | Contenido |
|---------|-----------|
| `src/main/resources/static/js/vendor/lucide.min.js` | UMD bundle de Lucide v1.27.0 |

### No incluido (YAGNI)

- No se añade hash en filenames (requiere build tooling)
- No se implementa Service Worker para offline total
- No se configura `proxy_cache` en Nginx (complejidad para el beneficio actual)

---

## Área 4: Hardening de Credenciales en Producción

### Problema

- `application.properties` y `application-prod.properties` contienen credenciales por defecto:
  - `spring.datasource.username=root` / password vacía
  - `admin.username=admin` / `admin.password=admin123`
- `application-prod.properties` tiene fallback TiDB con `root`/`` (inseguro)
- `DB_DDL_AUTO=update` en prod debería ser `validate` o `none`
- Si alguien despliega sin `.env`, las credenciales por defecto son inseguras

### Solución

1. **Eliminar valores por defecto inseguros** en `application-prod.properties`:
   - `spring.datasource.username=${DB_USERNAME}` (sin fallback)
   - `spring.datasource.password=${DB_PASSWORD}` (sin fallback)
   - `admin.username=${ADMIN_USERNAME}` (sin fallback)
   - `admin.password=${ADMIN_PASSWORD}` (sin fallback)
2. **`application.properties` (dev):** mantener defaults razonables para desarrollo local
3. **`application-prod.properties`:** forzar `ddl-auto=validate`
4. **Agregar validación de entorno** al arranque para detectar variables obligatorias faltantes

### Archivos a modificar

| Archivo | Cambio |
|---------|--------|
| `src/main/resources/application-prod.properties` | Eliminar fallbacks inseguros, `ddl-auto=validate` |
| Posible: clase `@Configuration` | Validación de env vars al startup |

---

## Criterios de Aceptación

1. `curl http://localhost:8090/js/vendor/lucide.min.js` → HTTP 200, JS válido (~414 KB)
2. `curl -I http://localhost:8090/css/luxury-core.css` → `Cache-Control: public, immutable`
3. Página sin CDN externo → Lucide icons renderizan correctamente
4. Despliegue sin `.env` → Spring Boot falla al arrancar (no usa defaults inseguros)
5. `mvn clean compile -q` → sin errores
6. `docker compose build` → imagen construye correctamente

---

## Riesgos y Mitigaciones

| Riesgo | Mitigación |
|--------|------------|
| Versión de Lucide desactualizada | Proceso manual: descargar nueva versión cuando se necesite |
| JS bundle muy grande (414 KB) | Es UMD completo; podría reducirse con tree-shaking, pero YAGNI ahora |
| Prod falla si faltan env vars | Preferible a desplegar con credenciales inseguras; el error es explícito |
