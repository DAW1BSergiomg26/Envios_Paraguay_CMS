# Recommended Free Demo Plan — MonteAstur Envios

## Recomendación final

**Opción A: Cloudflare Tunnel (temporal desde PC local) ⭐**

Para demostraciones en vivo con clientes, la mejor opción es exponer tu Docker local temporalmente mediante Cloudflare Tunnel.

**Opción B: Render + Neon.tech (24/7 básico)**

Si necesitas que la demo esté disponible sin mantener tu PC encendido.

---

## Opción A: Cloudflare Tunnel (recomendada para demos en vivo)

### Por qué es la mejor opción

1. **App completa**: backend, frontend, BD, monitoring — todo exactamente igual que producción
2. **Sin coste**: Cloudflare Tunnel es gratuito
3. **Sin límites**: no hay restricciones de RAM, CPU, requests
4. **SSL automático**: HTTPS incluido
5. **Setup rápido**: en <5 minutos tienes una URL pública
6. **Seguro**: el túnel solo dura mientras tú lo mantengas abierto
7. **Sin registro**: puedes probar sin cuenta (trycloudflare.com)

### Cuándo usarla

| Situación | Recomendado |
|-----------|-------------|
| Reunión con cliente potencial | ✅ Ideal |
| Demo en vivo para inversores | ✅ Ideal |
| Prueba desde móvil/tablet | ✅ Ideal |
| Feedback rápido del cliente | ✅ Ideal |
| Demo 24/7 sin PC encendido | ❌ No (elegir Opción B) |

### Pasos rápidos

1. Tener Docker compose funcionando localmente
2. Instalar `cloudflared`
3. Ejecutar: `cloudflared tunnel --url http://localhost:8090`
4. Compartir URL: `https://<aleatorio>.trycloudflare.com`
5. Al terminar: `Ctrl+C` en el túnel

Ver `docs/CLOUDFLARE_TUNNEL_DEMO_GUIDE.md` para guía detallada.

---

## Opción B: Render + Neon.tech (alternativa 24/7)

### Cuándo usarla

| Situación | Recomendado |
|-----------|-------------|
| Demo permanente sin mantener PC | ✅ Ideal |
| Cliente quiere probar durante días | ✅ Ideal |
| No tienes PC para mantener encendido | ✅ Ideal |

### Estructura

```
Render (Spring Boot) ──► Neon.tech (PostgreSQL)
       │
       └──► Render Static Site (React SPA, opcional)
```

### Limitaciones

- **Sleep**: Render duerme el servicio tras 15 min de inactividad. El primer request puede tardar ~30s en responder.
- **RAM**: 512MB — suficiente para Spring Boot con datos demo, pero justo.
- **MySQL → PostgreSQL**: Render no ofrece MySQL gratis. Neon.tech es PostgreSQL. Habría que migrar el esquema o adaptar las queries. No es trivial.
- **Horas**: 750h/mes — un servicio 24/7 consume exactamente eso. Sin margen.

### Por qué no es la primera opción

La necesidad de migrar de MySQL a PostgreSQL (o pagar por MySQL externo) añade complejidad que no merece la pena para una demo. Si el cliente ya está convencido tras la demo con Cloudflare Tunnel, se pasa directamente a VPS real con MySQL.

---

## Plan de acción

### Fase 1: Preparación (30 min)

```bash
# 1. Verificar Docker local funciona
docker compose ps
curl http://localhost:8080/actuator/health

# 2. Verificar app demo-data cargada
curl http://localhost:8090/login

# 3. Instalar cloudflared
# Windows: winget install cloudflare.cloudflared
# Linux:   sudo apt install cloudflared
# macOS:   brew install cloudflared
```

### Fase 2: Demo (duración de la reunión)

```bash
# 1. Iniciar túnel
cloudflared tunnel --url http://localhost:8090

# 2. Copiar URL (ej: https://abc123.trycloudflare.com)
# 3. Compartir con el cliente
# 4. Navegar por la app juntos
```

### Fase 3: Post-demo

```bash
# 1. Cerrar túnel (Ctrl+C)
# 2. Recoger feedback
# 3. Decidir si pasar a VPS real
```

---

## Comparativa económica

| Escenario | Coste/mes | Qué incluye |
|-----------|-----------|-------------|
| Cloudflare Tunnel (demo local) | €0 | Demo mientras tu PC esté encendido |
| Render + Neon.tech | €0 | Demo 24/7 con sleep y limitaciones |
| VPS Hetzner CX22 (producción) | ~€4.50 | App completa 24/7 sin limitaciones |
| VPS + dominio | ~€5.33/mes | Producción real con SSL |

---

## Riesgos de cada opción

### Cloudflare Tunnel

| Riesgo | Probabilidad | Impacto | Mitigación |
|--------|-------------|---------|------------|
| Corte de luz/internet local | Baja | Demo offline | Tener batería y datos móviles |
| PC se suspende | Media | Túnel caído | Configurar "nunca suspender" durante demo |
| Cliente quiere probar más tarde | Media | Sin URL permanente | Grabar vídeo o programar otra sesión |
| Límite de ancho de banda subida | Baja | App lenta | Conexión simétrica recomendada |

### Render + Neon.tech

| Riesgo | Probabilidad | Impacto | Mitigación |
|--------|-------------|---------|------------|
| Sleep (carga lenta) | Alta | Mala experiencia | Avisar al cliente antes |
| 512MB RAM insuficiente | Media | App crash | Monitorizar, escalar si necesario |
| MySQL no soportado | Alta | Requiere migración | Usar PostgreSQL o pagar MySQL externo |

---

## Decisión final

```
╔══════════════════════════════════════════════════════╗
║  ✅ USAR CLOUDFLARE TUNNEL PARA DEMO EN VIVO       ║
║  🎯 Objetivo: enseñar, probar, convencer            ║
║  💰 Coste: €0                                       ║
║  ⏱️ Tiempo setup: <5 min                            ║
║  📅 Pasos: docs/CLOUDFLARE_TUNNEL_DEMO_GUIDE.md     ║
╚══════════════════════════════════════════════════════╝
```

Si el cliente necesita una demo permanente de varios días:
- Evaluar **VPS real** (~€5/mes) si hay presupuesto
- O **Render + Neon.tech** como alternativa gratuita (con limitaciones)

Ver `docs/CLOUDFLARE_TUNNEL_DEMO_GUIDE.md` para instrucciones detalladas.