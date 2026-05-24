# Monteastur Envios — Smoke Tests Producción

> **Versión:** 1.0 | **Fecha:** 2026-05-24
> **Propósito:** Verificación rápida post-deploy de que la aplicación funciona correctamente.
> **Duración estimada:** ~15 min
> **Prioridad:** 🔴 Alta = debe pasar sí o sí | 🟡 Media = debe pasar | 🟢 Baja = opcional

---

## Cómo ejecutar

1. Abrir terminal y navegador
2. Seguir cada test en orden
3. Marcar PASS/FAIL
4. Si algún 🔴 falla, ejecutar plan de contingencia

---

## TEST 1: Healthcheck endpoint

**Prioridad:** 🔴 Alta
**Tiempo:** 30s

```bash
curl -f https://monteastur.com/actuator/health
```

**Expected:**
```json
{"status":"UP"}
```

| Resultado | |
|-----------|-|
| PASS ☐ | FAIL ☐ |

---

## TEST 2: Home page carga

**Prioridad:** 🔴 Alta
**Tiempo:** 1 min

```bash
curl -s -o /dev/null -w "%{http_code}" https://monteastur.com
# → 200

curl -I https://monteastur.com
# → Debe incluir:
#   HTTP/2 200
#   strict-transport-security
#   content-security-policy
#   x-frame-options: DENY
```

Abrir `https://monteastur.com` en navegador.

**Comprobaciones visuales:**
- [ ] Logo visible
- [ ] Menú de navegación visible
- [ ] Hero/banner principal visible
- [ ] Sin errores en consola del navegador
- [ ] Diseño responsive correcto

| Resultado | |
|-----------|-|
| PASS ☐ | FAIL ☐ |

---

## TEST 3: Tracking público

**Prioridad:** 🔴 Alta
**Tiempo:** 1 min

```bash
# La página de tracking carga
curl -s -o /dev/null -w "%{http_code}" https://monteastur.com/seguimiento
# → 200

# API tracking pública (código existente en BD)
curl -s https://monteastur.com/api/v1/tracking/MT-2026-0001 | head -c 200
# → JSON con datos del envío

# Código inexistente debe devolver 404
curl -s -o /dev/null -w "%{http_code}" https://monteastur.com/api/v1/tracking/NO-EXISTE
# → 404
```

Abrir `https://monteastur.com/seguimiento` en navegador.

**Comprobaciones visuales:**
- [ ] Formulario de búsqueda visible
- [ ] Búsqueda con código válido muestra resultados
- [ ] Búsqueda con código inválido muestra error

| Resultado | |
|-----------|-|
| PASS ☐ | FAIL ☐ |

---

## TEST 4: Login admin

**Prioridad:** 🔴 Alta
**Tiempo:** 2 min

```bash
# La página de login carga
curl -s -o /dev/null -w "%{http_code}" https://monteastur.com/login
# → 200
```

Abrir `https://monteastur.com/login` en navegador.

**Comprobaciones:**
- [ ] Formulario de login visible (username + password)
- [ ] Login con credenciales de admin correctas → dashboard
- [ ] Login con credenciales incorrectas → mensaje de error
- [ ] Dashboard admin carga con datos reales (stats, tabla envíos)
- [ ] Logout funciona

| Resultado | |
|-----------|-|
| PASS ☐ | FAIL ☐ |

---

## TEST 5: Login cliente

**Prioridad:** 🔴 Alta
**Tiempo:** 2 min

Abrir `https://monteastur.com/cliente/login` en navegador.

**Comprobaciones:**
- [ ] Formulario de login cliente visible
- [ ] Login con credenciales de cliente → panel cliente
- [ ] Panel cliente muestra envíos del cliente
- [ ] Detalle de envío funciona
- [ ] Logout funciona

| Resultado | |
|-----------|-|
| PASS ☐ | FAIL ☐ |

---

## TEST 6: Dashboard React SPA

**Prioridad:** 🟡 Media
**Tiempo:** 2 min

Abrir `https://monteastur.com/react-dashboard` en navegador.

**Comprobaciones:**
- [ ] Login page del SPA carga
- [ ] Login exitoso redirige a dashboard
- [ ] Dashboard muestra stats cards
- [ ] Tabla de envíos se carga con datos
- [ ] Navbar visible con opciones
- [ ] Gráficos se renderizan
- [ ] Sin errores en consola del navegador (0 page errors)
- [ ] PWA: manifest se carga (Chrome DevTools → Application → Manifest)

| Resultado | |
|-----------|-|
| PASS ☐ | FAIL ☐ |

---

## TEST 7: Upload / subida de imágenes

**Prioridad:** 🟡 Media
**Tiempo:** 1 min

```bash
# Verificar que el directorio uploads existe y es accesible
curl -s -o /dev/null -w "%{http_code}" https://monteastur.com/admin/tracking
# → 200 o 302 (si requiere login)
```

Probar subida de imagen desde el panel admin.

**Comprobaciones:**
- [ ] Formulario de subida visible en admin
- [ ] Imagen se sube correctamente
- [ ] Imagen se muestra después de subir
- [ ] Sin errores 413 (request too large)

| Resultado | |
|-----------|-|
| PASS ☐ | FAIL ☐ |

---

## TEST 8: Monitoring

**Prioridad:** 🟡 Media
**Tiempo:** 3 min

```bash
# Prometheus targets
curl -s http://localhost:9090/targets | grep -o '"health":"[^"]*"' | head -5
# → Debe mostrar "health":"up" para app

# Healthcheck desde script
./scripts/server-healthcheck.sh
# → All checks passed

# Docker
docker ps
# → 6/6 containers UP
```

**Comprobaciones visuales:**
- [ ] Grafana accesible en `http://<IP>:3000`
- [ ] Dashboard "Monteastur Envios" cargado
- [ ] Paneles con datos (no "No data")
- [ ] Uptime Kuma accesible en `http://<IP>:3001`

| Resultado | |
|-----------|-|
| PASS ☐ | FAIL ☐ |

---

## TEST 9: PWA

**Prioridad:** 🟢 Baja
**Tiempo:** 2 min

Abrir `https://monteastur.com/react-dashboard` en Chrome/Edge.

**Comprobaciones:**
- [ ] Icono de instalación en la barra de direcciones
- [ ] Manifest válido: DevTools → Application → Manifest
- [ ] Service Worker registrado: DevTools → Application → Service Workers
- [ ] Offline: desconectar red → ver mensaje "Estás sin conexión"
- [ ] Volver a conectar → datos se sincronizan

| Resultado | |
|-----------|-|
| PASS ☐ | FAIL ☐ |

---

## TEST 10: SSL Labs

**Prioridad:** 🟢 Baja
**Tiempo:** 2 min

Ir a: https://www.ssllabs.com/ssltest/analyze.html?d=monteastur.com

**Expected:** Grade A o superior.

| Resultado | |
|-----------|-|
| PASS ☐ | FAIL ☐ |

---

## TEST 11: Mobile responsive

**Prioridad:** 🟢 Baja
**Tiempo:** 2 min

Abrir `https://monteastur.com` en Chrome DevTools modo responsive (320px-768px).

**Comprobaciones:**
- [ ] Menú hamburguesa visible
- [ ] Texto legible sin zoom
- [ ] Botones tappables
- [ ] Formularios usables
- [ ] Sin desbordamiento horizontal

| Resultado | |
|-----------|-|
| PASS ☐ | FAIL ☐ |

---

## Resumen de resultados

| # | Test | Prioridad | Resultado |
|---|------|-----------|-----------|
| 1 | Healthcheck endpoint | 🔴 Alta | ☐ |
| 2 | Home page carga | 🔴 Alta | ☐ |
| 3 | Tracking público | 🔴 Alta | ☐ |
| 4 | Login admin | 🔴 Alta | ☐ |
| 5 | Login cliente | 🔴 Alta | ☐ |
| 6 | Dashboard React SPA | 🟡 Media | ☐ |
| 7 | Upload / subida | 🟡 Media | ☐ |
| 8 | Monitoring | 🟡 Media | ☐ |
| 9 | PWA | 🟢 Baja | ☐ |
| 10 | SSL Labs | 🟢 Baja | ☐ |
| 11 | Mobile responsive | 🟢 Baja | ☐ |

**Tests 🔴:** ___ / 5 | **Tests 🟡:** ___ / 3 | **Tests 🟢:** ___ / 3 | **Total:** ___ / 11

> **Criterio de aceptación:** Todos los 🔴 deben pasar. Si algún 🔴 falla, no considerar el deploy como exitoso.

---

> **Documentos relacionados:**
> - [`FINAL_PRODUCTION_DEPLOY_CHECKLIST.md`](FINAL_PRODUCTION_DEPLOY_CHECKLIST.md) — Checklist completa
> - [`LIVE_DEPLOY_PLAN.md`](LIVE_DEPLOY_PLAN.md) — Plan de deploy
> - [`PRODUCTION_VPS_RUNBOOK.md`](PRODUCTION_VPS_RUNBOOK.md) — Runbook de operaciones
>
> **Mantenido por:** Equipo Monteastur Envios
> **Próxima revisión:** 2026-06-24
