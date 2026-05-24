# Timeline — Primer Deploy Real a Producción

Planificación temporal para el primer despliegue completo de MonteAstur en producción.

---

## Resumen

| Fase | Duración | Coste |
|------|----------|-------|
| Setup inicial | ~3 días | ~€5.40 (VPS) + ~€9.15 (dominio) = ~€14.55 |
| Primer deploy | ~3 horas | €0 |
| Post-deploy | ~2 horas | €0 |
| **Total** | **~3 días** | **~€14.55** (primer mes) |

---

## Día 1 — Compra VPS + Dominio

| Tarea | Duración | Depende de | Riesgo |
|-------|----------|------------|--------|
| Crear cuenta Hetzner | 15 min | — | Bajo |
| Verificar identidad Hetzner | 2-24h* | — | ⚠️ Medio (puede tardar) |
| Comprar dominio | 10 min | — | Bajo |
| Configurar DNS (apuntar al VPS) | 15 min | IP del VPS | Bajo |
| **Total día 1** | **~40 min + espera verificación** | | |

> \* La verificación de identidad en Hetzner es el cuello de botella. Si se alarga,
> se puede avanzar con la documentación y scripts en local.

### Punto crítico ⚠️

- **Verificación Hetzner:** si no llega en 24h, contactar soporte con ticket.
- Mientras esperas, puedes tener todo el código, scripts y .env preparados.

---

## Día 2 — DNS + HTTPS

| Tarea | Duración | Depende de | Riesgo |
|-------|----------|------------|--------|
| Crear servidor VPS (CX22) | 10 min | Cuenta verificada | Bajo |
| Bootstrap VPS | 15 min | VPS activo | Bajo |
| Docker compose up | 10 min | Bootstrap + .env | Bajo |
| Esperar propagación DNS | 5-30 min* | DNS configurado | ⚠️ Medio |
| Obtener HTTPS (Let's Encrypt) | 10 min | DNS propagado | Bajo |
| Verificar web funcionando | 10 min | HTTPS listo | Bajo |
| **Total día 2** | **~1h + espera DNS** | | |

> \* Con TTL 300, la propagación DNS suele ser de 1-5 min.
> En casos raros (cambios de nameserver) puede tardar 24-48h.

### Punto crítico ⚠️

- **Propagación DNS:** A veces un ISP concreto cachea DNS antiguo.
  - Usar `dig @1.1.1.1 monteastur.com` para ver el registro real.
  - Si no funciona tras 30 min, probar desde otro dispositivo/red.

---

## Día 3 — Monitoring + Backups + Deploy Final

| Tarea | Duración | Depende de | Riesgo |
|-------|----------|------------|--------|
| Verificar 6/6 containers UP | 5 min | Docker up | Bajo |
| Configurar GitHub Secrets | 10 min | Repo + VPS SSH key | Bajo |
| Primer deploy Actions manual | 5 min | Secrets listos | Bajo |
| Verificar smoke tests | 15 min | Deploy exitoso | ⚠️ Medio |
| Configurar monitoreo (Grafana) | 10 min | Containers UP | Bajo |
| Configurar backups automáticos | 15 min | Docker up | Bajo |
| Probar rollback | 5 min | Tag git existente | Bajo |
| **Total día 3** | **~1h** | | |

### Punto crítico ⚠️

- **Smoke tests:** Si algún test 🔴 falla, no considerar deploy exitoso.
  - Healthcheck → `{"status":"UP"}`
  - Web → HTTP 200
  - Login funcional
  - API tracking responde

---

## Timeline resumen

```
DÍA 1 (~40 min + espera)
├── [15 min] Crear cuenta Hetzner
├── [~24h]   Verificar identidad (cuello de botella)
├── [10 min] Comprar dominio
├── [15 min] Configurar DNS
└── [--]     Preparar scripts y .env mientras esperas

DÍA 2 (~1h + espera DNS)
├── [10 min] Crear servidor CX22
├── [15 min] Bootstrap VPS
├── [10 min] docker compose up
├── [5-30min] Esperar propagación DNS
├── [10 min] Let's Encrypt HTTPS
├── [10 min] Verificar

DÍA 3 (~1h)
├── [5 min]  Verificar containers
├── [10 min] GitHub Secrets
├── [5 min]  Deploy Actions
├── [15 min] Smoke tests
├── [10 min] Monitoring
├── [15 min] Backups
├── [5 min]  Rollback test
└── ✅ PRODUCCIÓN ACTIVA
```

---

## Tiempos estimados totales

| Concepto | Tiempo real |
|----------|-------------|
| Tiempo activo (manos a la obra) | ~3 horas totales |
| Tiempo de espera (verificación + DNS) | ~24-30h |
| **Total calendario** | **~3 días** |
| **Mínimo posible** | **~2-3 horas** (si verificación y DNS son rápidos) |

---

## Presupuesto total (primer mes)

| Concepto | Coste |
|----------|-------|
| VPS Hetzner CX22 + backup | ~€5.40 |
| Dominio .com (prorrateado) | ~€0.76 |
| **Total primer mes** | **~€6.16** |
| **Meses siguientes** | **~€5.33/mes** (dominio ya pagado) |
| **Coste anual** | **~€69** (VPS 12 meses + dominio 1 año) |

---

## Riesgos y mitigación

| Riesgo | Probabilidad | Impacto | Mitigación |
|--------|-------------|---------|------------|
| Verificación Hetzner >24h | 🟡 Media | 🟡 Media | Contactar soporte, tener todo listo |
| Propagación DNS lenta | 🟢 Baja | 🟡 Media | Usar `dig @1.1.1.1`, esperar |
| SSL falla en Let's Encrypt | 🟢 Baja | 🔴 Alto | Verificar DNS, puerto 80 abierto |
| Docker build falla en VPS | 🟢 Baja | 🔴 Alto | Testear build local antes |
| MySQL no arranca | 🟢 Baja | 🔴 Alto | Verificar .env, volúmenes |
| Actions SSH no conecta | 🟢 Baja | 🟡 Medio | Verificar secrets, SSH key |
| Smoke tests fallan | 🟡 Media | 🔴 Alto | Rollback inmediato, debuggear |

---

## Cuándo abortar el deploy

Si en cualquier punto ocurre algo de esto, parar y no forzar:

1. ❌ No puedes hacer SSH al VPS tras 3 intentos
2. ❌ Docker compose up no levanta después de 3 intentos
3. ❌ Healthcheck endpoint no responde tras 5 minutos
4. ❌ HTTPS no funciona tras 30 minutos de tener DNS propagado
5. ❌ Login no funciona en smoke tests
6. ❌ No tienes un rollback plan claro

**En caso de abortar:**
- Documentar qué falló
- Revisar logs (`docker compose logs`)
- Preguntar o investigar
- Reintentar al día siguiente

---

## Documentación relacionada

- [`HETZNER_VPS_PURCHASE_GUIDE.md`](HETZNER_VPS_PURCHASE_GUIDE.md) — Compra VPS paso a paso
- [`DOMAIN_PURCHASE_GUIDE.md`](DOMAIN_PURCHASE_GUIDE.md) — Compra dominio + DNS
- [`VPS_REAL_EXECUTION_GUIDE.md`](VPS_REAL_EXECUTION_GUIDE.md) — Ejecución completa deploy
- [`FINAL_PRODUCTION_DEPLOY_CHECKLIST.md`](FINAL_PRODUCTION_DEPLOY_CHECKLIST.md) — Checklist final 7 fases
- [`SMOKE_TESTS_PRODUCTION.md`](SMOKE_TESTS_PRODUCTION.md) — Tests post-deploy
