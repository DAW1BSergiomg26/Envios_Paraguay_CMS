# Monteastur Envios — Configuración Dominio, DNS y SSL

> **Versión:** 1.0 | **Última actualización:** 2026-05-24
> **Objetivo:** Guía completa para configurar dominio, DNS y HTTPS en producción.

---

## Índice

- [A) Estructura de dominio recomendada](#a-estructura-de-dominio-recomendada)
- [B) Configuración DNS](#b-configuración-dns)
- [C) HTTPS / SSL con Let's Encrypt](#c-https--ssl-con-lets-encrypt)
- [D) Nginx reverse proxy producción](#d-nginx-reverse-proxy-producción)
- [E) Estrategia de subdominios](#e-estrategia-de-subdominios)
- [F) Troubleshooting](#f-troubleshooting)

---

## A) Estructura de dominio recomendada

### Dominio principal

```
monteastur.com          → Aplicación principal (www + root)
```

### Subdominios recomendados

| Subdominio | Servicio | Puerto interno | ¿Obligatorio? |
|------------|----------|----------------|---------------|
| `app.monteastur.com` | App principal (alias) | 443/80 | ❌ Opcional |
| `grafana.monteastur.com` | Dashboards Grafana | 3000 | ❌ Recomendado |
| `uptime.monteastur.com` | Uptime Kuma | 3001 | ❌ Recomendado |

### Subdominios adicionales (futuro)

| Subdominio | Servicio | Puerto | Cuándo añadir |
|------------|----------|--------|---------------|
| `api.monteastur.com` | API REST pública | 8080 | Si se expone API externa |
| `status.monteastur.com` | Página de estado | 3001 | Si Uptime Kuma es público |
| `dev.monteastur.com` | Entorno staging | 8081 | Si se añade staging |
| `cdn.monteastur.com` | Assets estáticos | — | Si se necesita CDN propio |

### Registro de dominios (recomendación)

| Servicio | Precio aprox | Notas |
|----------|-------------|-------|
| **Namecheap** | ~€10/año | Privacidad WHOIS gratuita |
| **Cloudflare Registrar** | ~€8/año | Precio coste + DNS gestionado |
| **GoDaddy** | ~€12-15/año | Caro, upsells constantes |

> **Recomendación:** Namecheap o Cloudflare Registrar. Evitar GoDaddy.

---

## B) Configuración DNS

### Registros mínimos (producción)

| Tipo | Nombre | Valor | TTL |
|------|--------|-------|-----|
| A | `@` | `<IP_DEL_VPS>` | 300 (setup) → 3600 (estable) |
| A | `www` | `<IP_DEL_VPS>` | 300 (setup) → 3600 (estable) |

### Registros con subdominios

| Tipo | Nombre | Valor | TTL | Servicio |
|------|--------|-------|-----|----------|
| A | `@` | `<IP_DEL_VPS>` | 3600 | App |
| A | `www` | `<IP_DEL_VPS>` | 3600 | App (redirect) |
| A | `grafana` | `<IP_DEL_VPS>` | 3600 | Grafana |
| A | `uptime` | `<IP_DEL_VPS>` | 3600 | Uptime Kuma |

### TTL recomendado

| Situación | TTL | Motivo |
|-----------|-----|--------|
| Setup inicial | 300 (5 min) | Permite cambios rápidos si hay errores |
| Producción estable | 3600 (1 hora) | Balance entre propagación rápida y carga DNS |
| Post-migración | 86400 (24h) | Si el dominio está muy estable |

### Propagación DNS

> La propagación DNS **no es instantánea**. Puede tardar desde 5 minutos hasta 48 horas.

```bash
# Verificar propagación
dig +short monteastur.com              # Debe mostrar IP del VPS
dig +short www.monteastur.com          # Debe mostrar IP del VPS
nslookup monteastur.com                 # Verificación alternativa

# Verificar TTL actual
dig monteastur.com | grep -E "^monteastur"
```

### DNS con Cloudflare (opcional pero recomendado)

**Ventajas del proxy Cloudflare:**
- CDN global (caché de assets estáticos)
- Protección DDoS básica
- Ocultación IP real del VPS
- SSL flexible
- Reglas de página

**Cuándo usar proxy ON / OFF:**

| Situación | Cloudflare Proxy | Motivo |
|-----------|------------------|--------|
| App principal | ✅ ON | SSL, caché, protección |
| Let's Encrypt SSL | ❌ OFF (DNS only) | Certbot necesita conexión directa |
| Grafana (restringido) | ❌ OFF o DNS only | No exponer panel sin auth |
| Uptime Kuma (público) | ✅ ON | Protegido por Cloudflare |

> Si usas Cloudflare Proxy para Let's Encrypt:
> 1. Poner el registro A en **DNS only** (naranja → gris)
> 2. Obtener certificado
> 3. Volver a **Proxied** (gris → naranja)
> 4. Usar "Full (strict)" SSL en Cloudflare

---

## C) HTTPS / SSL con Let's Encrypt

### Requisitos

Antes de obtener el certificado:

```bash
# 1. DNS debe estar propagado
dig +short monteastur.com
# → <IP_DEL_VPS>

# 2. HTTP debe responder en puerto 80
curl -I http://monteastur.com
# → HTTP/1.1 200 OK

# 3. UFW debe permitir puerto 80
sudo ufw status | grep 80
# → 80/tcp ALLOW

# 4. Nginx debe tener location para .well-known
# (ya configurado en monteastur.conf y production-example.conf)
```

### Obtener certificado

```bash
# Certificado único (solo dominio principal)
docker compose --profile certbot run --rm certbot certonly \
  --webroot -w /var/www/certbot \
  -d monteastur.com -d www.monteastur.com \
  --email admin@monteastur.com \
  --agree-tos --no-eff-email

# Certificado con subdominios
docker compose --profile certbot run --rm certbot certonly \
  --webroot -w /var/www/certbot \
  -d monteastur.com -d www.monteastur.com \
  -d grafana.monteastur.com -d uptime.monteastur.com \
  --email admin@monteastur.com \
  --agree-tos --no-eff-email
```

### Copiar certificados a nginx

```bash
# Verificar certificado descargado
ls -la /etc/letsencrypt/live/monteastur.com/
# → fullchain.pem  privkey.pem  cert.pem  chain.pem

# Copiar a nginx/ssl
cp /etc/letsencrypt/live/monteastur.com/fullchain.pem /opt/monteastur/nginx/ssl/
cp /etc/letsencrypt/live/monteastur.com/privkey.pem /opt/monteastur/nginx/ssl/
chmod 600 /opt/monteastur/nginx/ssl/privkey.pem
```

### Activar HTTPS en nginx

```bash
# Editar nginx/conf.d/monteastur.conf
# Cambiar server_name de _ a monteastur.com www.monteastur.com
# Descomentar el bloque server { listen 443 ssl http2; ... }

# Recargar nginx
docker compose restart nginx
```

### Verificar SSL

```bash
# HTTPS responde correctamente
curl -I https://monteastur.com
# → HTTP/2 200
# → strict-transport-security
# → content-security-policy

# Certificado válido
echo | openssl s_client -connect monteastur.com:443 \
  -servername monteastur.com 2>/dev/null | openssl x509 -noout -dates

# SSL Labs (prueba externa recomendada)
# https://www.ssllabs.com/ssltest/analyze.html?d=monteastur.com
```

### Renovación automática

```bash
# Añadir al crontab del usuario deploy
crontab -e
```

```cron
# Renovar certificados todos los días a las 3 AM (solo renueva si falta <30 días)
0 3 * * * cd /opt/monteastur && docker compose --profile certbot run --rm certbot renew && docker compose restart nginx
```

### Seguridad SSL adicional

```bash
# Cifrados recomendados (ya en config)
ssl_protocols TLSv1.2 TLSv1.3;
ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384;
ssl_prefer_server_ciphers on;
ssl_session_cache shared:SSL:10m;
ssl_session_timeout 10m;
```

---

## D) Nginx reverse proxy producción

### Arquitectura

```
Internet (HTTPS 443)
       │
       ▼
┌──────────────────────────────────────────────────┐
│              Nginx (reverse proxy)                │
│  ─ SSL termination                                │
│  ─ Security headers (HSTS, CSP, XFO)              │
│  ─ Gzip compression                               │
│  ─ Rate limiting                                  │
│  ─ Request size limit (10MB)                      │
└──────────┬───────────────────────────────────────┘
           │ proxy_pass http://app:8080
           ▼
┌──────────────────────────────────────────────────┐
│            Spring Boot (Tomcat)                   │
│  ─ Thymeleaf MVC + React SPA                      │
│  ─ REST API /api/v1/                              │
│  ─ Sesión HTTP (JSESSIONID)                       │
└──────────────────────────────────────────────────┘
```

### Configuración completa (ver `production-example.conf`)

| Elemento | Descripción |
|----------|-------------|
| `listen 80` | HTTP → redirect a HTTPS |
| `listen 443 ssl http2` | HTTPS con HTTP/2 |
| `server_name` | Dominio y subdominios |
| `ssl_certificate` | Ruta a fullchain.pem |
| `ssl_certificate_key` | Ruta a privkey.pem |
| `add_header` | HSTS, CSP, X-Frame-Options, etc. |
| `proxy_pass` | `http://app:8080` |
| `proxy_set_header` | Host, X-Real-IP, X-Forwarded-* |
| `client_max_body_size` | 10M |
| `location /.well-known/` | Let's Encrypt challenge |

### Headers de seguridad incluidos

| Header | Valor | Efecto |
|--------|-------|--------|
| `X-Frame-Options` | `DENY` | Evita clickjacking |
| `X-Content-Type-Options` | `nosniff` | Evita MIME sniffing |
| `Referrer-Policy` | `strict-origin-when-cross-origin` | Control de referer |
| `Permissions-Policy` | `geolocation=(), microphone=(), ...` | Restringe APIs del navegador |
| `Content-Security-Policy` | `default-src 'self'; ...` | Previene XSS |
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains` | HSTS (solo HTTPS) |

### Gzip

```nginx
gzip on;
gzip_types text/plain text/css application/json application/javascript text/xml;
gzip_min_length 1000;
gzip_vary on;
```

### WebSocket (para futuras funcionalidades)

```nginx
location /ws/ {
    proxy_pass http://app:8080;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host $host;
    proxy_read_timeout 86400;
}
```

---

## E) Estrategia de subdominios

### Escenario 1: Mínimo (recomendado para empezar)

```
monteastur.com        → App (puerto 80/443)
```

Un solo bloque server en nginx maneja todo. Sin subdominios.

**Ventajas:** Simple, un solo certificado, un solo bloque nginx.
**Desventajas:** Grafana y Uptime Kuma accesibles solo por IP:puerto.

### Escenario 2: Subdominios de monitoring

```
monteastur.com                  → App (puerto 80/443)
grafana.monteastur.com          → Grafana (puerto 3000)
uptime.monteastur.com           → Uptime Kuma (puerto 3001)
```

Requiere bloques server adicionales en nginx.

**Ventajas:** Monitoring accesible por nombre, más profesional.
**Desventajas:** Certificado con múltiples dominios (SAN), más complejidad nginx.

### Escenario 3: Completo (futuro)

```
monteastur.com                  → App
www.monteastur.com              → App
app.monteastur.com              → App (alias)
grafana.monteastur.com          → Grafana
uptime.monteastur.com           → Uptime Kuma
api.monteastur.com              → API (futuro)
status.monteastur.com           → Status page
dev.monteastur.com              → Staging (futuro)
```

---

## F) Troubleshooting

### DNS no propaga

```bash
# Verificar que el registro A existe
dig +short monteastur.com
# → Si no devuelve IP, el registro no está configurado o no se propagó

# Verificar TTL
dig monteastur.com | grep -E "^monteastur"

# Usar DNS público de Google
dig @8.8.8.8 monteastur.com

# Esperar (5 min a 48h)
# Si es urgente, reducir TTL a 60 antes del cambio
```

### Certbot: "DNS problem: NXDOMAIN"

```bash
# Causa: El dominio no resuelve a la IP del servidor
# Solución: Verificar registros DNS y esperar propagación

dig monteastur.com
# Si no devuelve IP → configurar registro A en el panel DNS

# Después de configurar, esperar TTL + 5 min
```

### Certbot: "Connection refused" en puerto 80

```bash
# Verificar que nginx está corriendo
docker ps | grep nginx

# Verificar UFW
sudo ufw status | grep 80

# Verificar que el puerto 80 está expuesto
curl -I http://localhost:80

# Si todo falla, detener otros servicios en puerto 80
sudo lsof -i :80
# sudo systemctl stop apache2  (si existe)
```

### Certificado expirado

```bash
# Renovar manualmente
cd /opt/monteastur
docker compose --profile certbot run --rm certbot renew
docker compose restart nginx

# Verificar fechas
echo | openssl s_client -connect monteastur.com:443 2>/dev/null | openssl x509 -noout -dates

# Si falla, revocar y obtener nuevo
# docker compose --profile certbot run --rm certbot revoke --cert-path /etc/letsencrypt/live/monteastur.com/cert.pem
```

### Redirect loop (HTTP → HTTPS)

```bash
# Causa: Cloudflare Proxy + Let's Encrypt sin modo "Full (strict)"
# Solución:
#   1. Ir a Cloudflare Dashboard → SSL/TLS
#   2. Cambiar a "Full (strict)"
#   3. O desactivar Proxy para Let's Encrypt
```

### HSTS impide acceso HTTP

```bash
# Si HSTS ya está activo y necesitas acceder por HTTP:
# 1. Abrir chrome://net-internals/#hsts
# 2. Delete domain security policies → monteastur.com
# 3. O usar navegador en modo incógnito
```

### Error mixto en navegador (Mixed Content)

```bash
# Causa: HTTPS carga recursos HTTP (imágenes, scripts, API)
# Solución:
#   1. Verificar que todos los assets se sirven por HTTPS
#   2. En React SPA, asegurar que fetch usa relative URLs
#   3. En API, el proxy nginx debe pasar X-Forwarded-Proto: $scheme
```

---

> **Documentos relacionados:**
> - [`nginx/conf.d/production-example.conf`](../nginx/conf.d/production-example.conf) — Ejemplo de configuración nginx
> - [`nginx/conf.d/monteastur.conf`](../nginx/conf.d/monteastur.conf) — Configuración activa
> - [`docs/HTTPS_SETUP.md`](HTTPS_SETUP.md) — Setup HTTPS existente
> - [`docs/LIVE_DEPLOY_PLAN.md`](LIVE_DEPLOY_PLAN.md) — Plan de deploy completo
>
> **Mantenido por:** Equipo Monteastur Envios
> **Próxima revisión:** 2026-06-24
