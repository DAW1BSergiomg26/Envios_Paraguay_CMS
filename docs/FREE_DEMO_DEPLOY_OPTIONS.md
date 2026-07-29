# Free Demo Deploy Options — MonteAstur Envios

Comparativa de opciones gratuitas para desplegar una demo/preventa del proyecto sin coste mensual.

---

## Resumen

| Opción | Spring Boot | MySQL | Coste | Límites | Riesgo suspensión | Ideal para |
|--------|-------------|-------|-------|---------|-------------------|------------|
| **Cloudflare Tunnel** (local) | ✅ Sí (Docker) | ✅ Sí (Docker) | €0 | Tu PC encendido, ancho de banda local | Ninguno | **Demostración en vivo** |
| **Render** | ✅ Sí | ✅ Sí (neon.tech) | €0 | 512MB RAM, 1 CPU, sleep tras inactividad | Medio (sleep tras 15 min) | Demo 24/7 básica |
| **Railway** | ✅ Sí | ✅ Sí | €0 (crédito inicial $5) | $5 gratis, luego consumo | Bajo si hay crédito | Demo corta |
| **Fly.io** | ✅ Sí | ✅ Sí | €0 (crédito inicial) | 3 VMs gratis, 3GB persistencia | Bajo si hay crédito | Demo corta |
| **Vercel** | ❌ No (solo frontend) | ❌ No | €0 | Frontend React estático | Ninguno | Solo SPA React |
| **Netlify** | ❌ No (solo frontend) | ❌ No | €0 | Frontend React estático | Ninguno | Solo SPA React |
| **GitHub Pages** | ❌ No (solo frontend) | ❌ No | €0 | Solo estático | Ninguno | Solo documentación |
| **Local + vídeo** | ✅ Sí | ✅ Sí | €0 | No interactivo | Ninguno | Demostración grabada |

---

## Opción 1: Cloudflare Tunnel (temporal desde local) ⭐ RECOMENDADA

**Coste**: €0
**Requiere**: Docker local funcionando, Cloudflare account gratis, `cloudflared` instalado

**Ventajas:**
- Muestra la app COMPLETA (backend + frontend + BD + monitoring)
- Usa el Docker local exactamente igual que producción
- Latencia real (no hay sleep por inactividad)
- URL pública temporal (dominio .trycloudflare.com)
- SSL incluido automáticamente
- Sin límite de peticiones
- Sin riesgo de suspensión
- Se apaga cuando cierras el túnel

**Desventajas:**
- Tu PC debe estar encendido y conectado a internet
- No apto para tráfico 24/7
- Ancho de banda limitado por tu conexión local
- URL aleatoria (no personalizada sin plan pago)

**Riesgos:**
- Cortes de luz/internet local → demo offline
- Si cierras terminal, se cae el túnel
- No escalable

---

## Opción 2: Render (free tier)

**Coste**: €0
**Requiere**: Cuenta Render, cuenta Neon.tech (PostgreSQL, alternativa a MySQL)

**Spring Boot**: ✅ Sí, con plan free (512MB RAM, 1 CPU)
**MySQL**: ⚠️ No nativo. Usar Neon.tech (PostgreSQL) o Aiven (MySQL free tier limitado)

**Ventajas:**
- 24/7 con sleep tras inactividad
- SSL automático
- Dominio `<nombre>.onrender.com`
- CI/CD integrado

**Desventajas:**
- Sleep tras 15 min sin tráfico (lento al despertar)
- 512MB RAM puede ser poco para Spring Boot + app
- No hay MySQL gratis nativo (usar externo)
- Límite de 750 horas/mes (un servicio 24/7 lo consume entero)

**Riesgo suspensión:**
- Si superas 750h/mes, el servicio se detiene hasta el mes siguiente
- Si la app usa >512MB RAM, se cae

---

## Opción 3: Railway (crédito inicial)

**Coste**: $5 gratis al registrarse
**Requiere**: Cuenta Railway

**Spring Boot**: ✅ Sí
**MySQL**: ✅ Sí (Railway MySQL plugin)

**Ventajas:**
- Fácil despliegue (conecta GitHub)
- MySQL nativo incluido
- Dominio `<nombre>.railway.app`

**Desventajas:**
- Solo $5 gratis (se consume según uso)
- Sin crédito recurrente (solo al registrarse)
- Una vez gastado, hay que pagar

**Riesgo suspensión:**
- Cuando se acaba el crédito, se detiene
- Sin tarjeta, no puedes añadir más crédito

---

## Opción 4: Fly.io (crédito inicial)

**Coste**: Crédito gratis al registrarse (~$5-10)
**Requiere**: Cuenta Fly.io

**Spring Boot**: ✅ Sí
**MySQL**: ✅ Sí (Fly.io Volumes + Docker MySQL)

**Ventajas:**
- 3 VMs siempre activas en free tier
- 3GB de persistencia
- Dominio `<nombre>.fly.dev`

**Desventajas:**
- Configuración más compleja (Dockerfile, fly.toml)
- Crédito limitado
- Puede requerir tarjeta de crédito para verificación

**Riesgo suspensión:**
- Similar a Railway: el crédito se agota

---

## Opción 5: Vercel / Netlify (frontend only)

**Coste**: €0
**Requiere**: Cuenta Vercel/Netlify

**Spring Boot**: ❌ No
**MySQL**: ❌ No

**Ventajas:**
- Excelente para frontend React
- SSL, CDN, dominio personalizado
- CI/CD automático

**Desventajas:**
- No puede ejecutar Spring Boot
- Para demo completa necesitas backend aparte

**Útil para:**
- Enseñar el SPA React por separado
- Prototipo de frontend si el backend aún no está listo

---

## Opción 6: GitHub Pages

**Coste**: €0
**Requiere**: Repositorio GitHub

**Spring Boot**: ❌ No
**MySQL**: ❌ No

**Ventajas:**
- Gratis, sin límite de repos públicos
- Fácil de configurar

**Desventajas:**
- Solo contenido estático
- No apto para app completa

---

## Opción 7: Local + vídeo grabado

**Coste**: €0
**Requiere**: Nada extra

**Ventajas:**
- Sin dependencias externas
- Puedes editar y retocar
- Muestras exactamente lo que quieres

**Desventajas:**
- No interactivo
- El cliente no puede "tocar"
- Menor impacto que demo en vivo

---

## Comparativa final

| Criterio | Cloudflare Tunnel | Render | Railway | Fly.io | Local+vídeo |
|----------|-------------------|--------|---------|--------|-------------|
| App completa | ✅ | ✅ | ✅ | ✅ | ✅ |
| Sin coste | ✅ | ✅ | Temporal | Temporal | ✅ |
| 24/7 | ❌ (local) | ⚠️ (sleep) | ✅ (hasta agotar) | ✅ (hasta agotar) | ❌ |
| SSL | ✅ | ✅ | ✅ | ✅ | ❌ |
| Fácil setup | ✅ | ✅ | ✅ | ⚠️ | ✅ |
| Interactivo | ✅ | ✅ | ✅ | ✅ | ❌ |
| Sin riesgo suspensión | ✅ (mientras local esté activo) | ⚠️ | ❌ | ❌ | ✅ |

---

## Decisión

Ver `docs/RECOMMENDED_FREE_DEMO_PLAN.md` para la recomendación final detallada.

TL;DR:
- **Para demostración en vivo (reunión con cliente)**: Cloudflare Tunnel — enseñas la app real desde tu PC
- **Para demo 24/7 sin mantener PC encendido**: Render + Neon.tech (con riesgo de sleep)
- **Para enseñar solo el frontend**: Vercel (despliegue separado del SPA)
- **Sin internet**: Vídeo grabado local