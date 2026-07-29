# Guía de Compra — Dominio para Producción

Cómo elegir, comprar y configurar el dominio para MonteAstur.

---

## A) Dónde comprar el dominio

| Proveedor | Precio .com/año | WHOIS Privado | DNS | Facilidad | Notas |
|-----------|----------------|---------------|-----|-----------|-------|
| **Cloudflare** | ~€9.15 | ✅ Incluido | ✅ Rápido + proxy DDoS | ⭐⭐⭐⭐⭐ | Precio coste (sin margen), DNS global |
| **Namecheap** | ~€10.69 | ✅ Incluido | ✅ Básico | ⭐⭐⭐⭐ | Buen soporte, panel claro |
| **Porkbun** | ~€9.52 | ✅ Incluido | ✅ Básico | ⭐⭐⭐⭐ | Barato, interfaz simple |
| GoDaddy | ~€15+ | ❌ Extra | ❌ Lento | ⭐⭐ | Caro, renovaciones altas |
| Hostinger | ~€10 | ✅ Incluido | ✅ Básico | ⭐⭐⭐ | Upsells constantes |

> Precios orientativos 2026. Pueden variar ligeramente.

---

## B) Recomendación final

### Primera opción: **Cloudflare**

| Ventaja | Detalle |
|---------|---------|
| Precio más bajo | Precio de coste (~€9.15/año) |
| WHOIS privado | Incluido gratis |
| DNS ultrarrápido | Red global anycast |
| Proxy DDoS | Puedes activar/desactivar por registro |
| SSL flexible | Full / Full (strict) / Flexible |
| API potente | Gestionable desde CLI |
| Un solo panel | Dominio + DNS + seguridad en un sitio |

**Única desventaja:** No puedes cambiar DNS fuera de Cloudflare (usan sus propios nameservers). Pero es justo lo que quieres.

### Segunda opción: **Namecheap**

| Ventaja | Detalle |
|---------|---------|
| Precio razonable | ~€10.69/año |
| Soporte 24/7 | Chat en vivo |
| WHOIS privado | Incluido |
| Panel claro | Fácil para principiantes |
| DNS básico | Funcional, sin extra |

Útil si prefieres un registro tradicional con soporte telefónico.

---

## C) Qué dominio elegir

### Reglas

| ✅ Hacer | ❌ Evitar |
|----------|-----------|
| Corto (6-12 caracteres) | Largo (>15 caracteres) |
| Fácil de recordar | Con guiones (monte-astur-envios) |
| Fácil de deletrear | Palabras difíciles |
| .com si es posible | .xyz, .top, .tk (spam) |
| Que incluya la marca | Números confusos (envios4u) |
| Sin caracteres especiales | ñ, ç, acentos |

### Sugerencias

| Dominio | Disponibilidad | Nota |
|---------|---------------|------|
| `monteastur.com` | Probablemente libre | ✅ Recomendado |
| `monteastur.es` | Posible | Alternativa si .com ocupado |
| `monteasturenvios.com` | Posible | Más largo pero descriptivo |
| `enviosmonteastur.com` | Posible | Alternativa |

### Recomendación final

> **monteastur.com** — corto (10 letras), fácil de recordar, .com profesional, sin guiones.

---

## D) Configuración DNS

Una vez comprado el dominio, configurar los registros DNS:

### Paso 1: Anotar IP del VPS

```bash
# Desde local, obtener IP
ssh deploy@<IP_DEL_VPS> curl -4 ifconfig.me
# → 203.0.113.10 (ejemplo)
```

### Paso 2: Crear registros A

| Tipo | Nombre | Valor | TTL | Nota |
|------|--------|-------|-----|------|
| A | `@` | `<IP_DEL_VPS>` | 300 | Dominio raíz |
| A | `www` | `<IP_DEL_VPS>` | 300 | Subdominio www |
| A | `api` | `<IP_DEL_VPS>` | 300 | API REST |
| A | `monitor` | `<IP_DEL_VPS>` | 300 | Monitoring (Grafana) |

> En Cloudflare, además puedes activar el **proxy** (naranja ☁️) para DDoS, SSL, caching.

### Paso 3: Verificar propagación

```bash
# Esperar 1-5 minutos (TTL 300 = 5 min)
ping monteastur.com
nslookup monteastur.com
dig monteastur.com +short
```

### Paso 4: Verificar desde fuera

```bash
curl -I http://monteastur.com
# → Debe responder con HTTP/1.1 o HTTP/2
```

---

## E) Cloudflare (opcional pero recomendado)

Si compras en Cloudflare, el DNS y proxy están integrados.

### Configuración recomendada

| Opción | Valor | Nota |
|--------|-------|------|
| SSL/TLS | **Full (strict)** | Requiere certificado válido en origen |
| Always Use HTTPS | **ON** | Redirige HTTP→HTTPS |
| Auto Minify | ON | HTML + CSS + JS |
| Brotli | ON | Compresión mejorada |
| Caching | **Standard** | Cachea estáticos |
| Security Level | **Medium** | Equilibrio seguridad/rendimiento |

### Proxy ON vs OFF

| Registro | Proxy (☁️) | DNS only (⛅) |
|----------|-----------|---------------|
| `@` | ON (recomendado) | OFF |
| `www` | ON (recomendado) | OFF |
| `api` | **OFF** (no cachear API) | ON |
| `monitor` | **OFF** (autenticación) | ON |

---

## F) Checklist dominio listo

- [ ] Dominio comprado (recomendado: monteastur.com en Cloudflare)
- [ ] WHOIS privado activado
- [ ] DNS apuntando al VPS
  - [ ] `monteastur.com → IP`
  - [ ] `www.monteastur.com → IP`
  - [ ] `api.monteastur.com → IP`
  - [ ] `monitor.monteastur.com → IP`
- [ ] TTL configurado a 300 (5 min)
- [ ] Propagación verificada (nslookup/ping)
- [ ] HTTP funciona (curl -I)
- [ ] Cloudflare (si aplica):
  - [ ] Proxy ON para @ y www
  - [ ] Proxy OFF para api y monitor
  - [ ] SSL/TLS: Full (strict)
  - [ ] Always Use HTTPS: ON
- [ ] Renovación automática activada

---

## Costes

| Concepto | Cloudflare | Namecheap |
|----------|-----------|-----------|
| Dominio .com (1 año) | ~€9.15 | ~€10.69 |
| WHOIS privado | €0 | €0 |
| DNS | €0 | €0 |
| Renovación/año | ~€9.15 | ~€10.69 |
| **Total 1er año** | **~€9.15** | **~€10.69** |
| **Coste/mes prorrateado** | **~€0.76** | **~€0.89** |
