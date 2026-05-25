# Monteastur Envios — Decision Log: Primer Deploy Real

> **Versión:** 1.0 | **Fecha:** 2026-05-25
> **Propósito:** Registrar decisiones técnicas, justificaciones, costes y riesgos del primer deploy real.

---

## 1. Decisiones tomadas

### 1.1 Proveedor VPS

| Decisión | Opción elegida | Alternativas descartadas |
|----------|---------------|--------------------------|
| **Proveedor** | **Hetzner Cloud** | DigitalOcean (~€6/mes más caro), Linode (~similar pero sin datacenter en Europa central), OVH (~misma gama pero panel menos amigable), AWS Lightsail (~sobrecoste por marca) |
| **Plan** | **CX22** (2 vCPU, 4 GB RAM, 40 GB SSD) | CX11 (2 GB RAM insuficiente para MySQL + app + monitoring), CX32 (más RAM, pero no necesario para carga inicial) |

**Justificación:** Hetzner ofrece la mejor relación calidad/precio en Europa. CX22 es el punto óptimo: suficiente para 3 contenedores principales + 3 de monitoring, con margen para picos de tráfico.

### 1.2 Proveedor Dominio

| Decisión | Opción elegida | Alternativas descartadas |
|----------|---------------|--------------------------|
| **Registrador** | **Cloudflare Registrar** | Namecheap (~€1.54/año más caro), GoDaddy (~€6+/año más caro + WHOIS privado de pago), Porkbun (~similar pero sin proxy DNS) |

**Justificación:** Cloudflare vende a precio de coste, incluye WHOIS privado, DNS anycast ultrarrápido, proxy DDoS gratuito y SSL flexible. Un solo panel para dominio + DNS + seguridad.

### 1.3 Nombre Dominio

| Decisión | Opción elegida |
|----------|---------------|
| **Dominio** | **monteastur.com** |

**Justificación:** 10 caracteres, fácil de recordar, .com profesional, sin guiones ni caracteres especiales. La marca "MonteAstur" está presente en todo el códigobase.

### 1.4 Sistema Operativo VPS

| Decisión | Opción elegida |
|----------|---------------|
| **SO** | **Ubuntu 24.04 LTS** |

**Justificación:** LTS con soporte hasta 2029. Docker, Docker Compose, fail2ban y UFW funcionan sin problemas. Ubuntu es el SO más documentado para Spring Boot + Docker.

### 1.5 Stack de monitoring

| Componente | Decisión |
|------------|----------|
| Métricas | **Prometheus** (open source, estándar de facto, integración nativa con Spring Boot Actuator) |
| Dashboards | **Grafana** (visualización, alertas, datasource Prometheus auto-configurado) |
| Uptime | **Uptime Kuma** (auto-hospedado, notificaciones multicanal, monitor SSL incluido) |
| Heartbeat externo | **Healthchecks.io** (gratuito, documentado, pendiente de configurar) |

### 1.6 SSL/TLS

| Decisión | Opción elegida |
|----------|---------------|
| **Certificados** | **Let's Encrypt** (Certbot standalone o Docker) |
| **Renovación** | **Automática vía crontab** |

**Justificación:** Gratuito, ampliamente soportado, renovación automática, grade A+ en SSL Labs.

### 1.7 Estrategia de backup

| Tipo | Frecuencia | Retención |
|------|-----------|-----------|
| MySQL dump | Diaria (3:00 AM) | 30 días |
| Uploads (tarball) | Diaria (4:00 AM) | 30 días |
| .env | Manual (tras cada cambio) | Indefinido (gestor contraseñas) |

### 1.8 CI/CD

| Decisión | Opción elegida |
|----------|---------------|
| **CD automático** | **GitHub Actions** con SSH deploy key |
| **Protección** | Solo desde branch `develop`, confirmación manual "deploy" |
| **Pre-deploy** | `mvn test` + `npm test` + `npm run build` + `docker compose config` |

**Justificación:** GitHub Actions ya está configurado. No se necesita Jenkins, GitLab CI ni herramientas externas. La protección con confirmación manual evita deploys accidentales.

---

## 2. Coste estimado mensual

| Concepto | Coste/mes | Coste/año | Notas |
|----------|-----------|-----------|-------|
| VPS Hetzner CX22 | ~€4.50 | ~€54.00 | Sin backup: -€0.90 |
| Backup VPS (20%) | ~€0.90 | ~€10.80 | Opcional pero recomendado |
| Dominio monteastur.com | ~€0.76 | ~€9.15 | Precio Cloudflare, renovación anual |
| Let's Encrypt SSL | €0 | €0 | Renovación automática |
| Prometheus + Grafana | €0 | €0 | Auto-hospedado en Docker |
| Uptime Kuma | €0 | €0 | Auto-hospedado en Docker |
| Healthchecks.io | €0 | €0 | Plan gratuito (hasta 20 checks) |
| GitHub Actions | €0 | €0 | Plan gratuito (2000 min/mes) |
| **Total** | **~€5.33** | **~€73.95** | Con backup: ~€6.16/mes |

### Coste inicial (primer mes)

| Concepto | Coste único |
|----------|-------------|
| Setup VPS (prorrateo) | ~€4.50 |
| Dominio .com (1 año) | ~€9.15 |
| **Total primer mes** | **~€13.65** |

---

## 3. Riesgos asumidos

| Riesgo | Probabilidad | Impacto | Mitigación |
|--------|-------------|---------|------------|
| **VPS caído por falta de pago** | Baja | Alto (app offline) | Facturación automática, alertas de email |
| **SSL no renovado** | Baja | Alto (HTTPS caído) | Crontab + `--dry-run` semanal |
| **Backups no ejecutados** | Baja | Alto (pérdida datos) | Verificación manual post-deploy, script healthcheck |
| **MySQL corrupto** | Baja | Crítico (pérdida datos) | Backups diarios + restore probado |
| **Seguridad VPS comprometida** | Baja | Crítico | Hardening SSH, UFW, fail2ban, unattended-upgrades |
| **Dominio caducado** | Muy baja | Alto (app inaccesible) | Renovación automática activada |
| **Tráfico inesperado** | Media | Medio (lentitud, caída) | 4 GB RAM con margen; si es recurrente, escalar a CX32 |
| **Disco lleno (logs/backups)** | Media | Medio (app falla) | Limpieza automática 30 días, alertas de disco |
| **Error humano en deploy** | Media | Medio (rollback necesario) | Workflow con pre-checks, rollback probado |
| **Fallo de GitHub Actions** | Baja | Bajo (deploy manual posible) | `ssh deploy@VPS && docker compose up -d --build` |

---

## 4. Qué se deja para fase posterior

| Funcionalidad | Motivo | Cuándo |
|--------------|--------|--------|
| **CD automático (push a develop)** | Riesgo de deploy accidental sin supervisión | Fase 19 tras validar 2 deploys manuales |
| **Notificaciones email/SMS** | Complejidad de configuración SMTP | Fase 20 |
| **Rate limiting API** | No crítico para MVP | Fase 21 |
| **Audit logs** | Mejora de seguridad, no bloqueante | Fase 22 |
| **CDN para assets estáticos** | No necesario con 40 GB SSD + Nginx | Fase 23 |
| **WAF (Web Application Firewall)** | Cloudflare Free ya ofrece protección básica | Fase 24 |
| **Backup externo (S3/Backblaze)** | Coste adicional, no crítico inicialmente | Fase 25 |
| **Staging environment** | Complejidad, un solo VPS por ahora | Fase 26 |
| **Healthchecks.io heartbeat** | No bloqueante, solo notificación extra | Fase 19 |
| **Monitorización de backups** | Script ya notifica en crontab local | Fase 20 |

---

## 5. Proveedor recomendado (resumen)

| Recurso | Proveedor | Plan/Producto | Coste |
|---------|-----------|--------------|-------|
| VPS | **Hetzner Cloud** | CX22 (Ubuntu 24.04, 2 vCPU, 4GB, 40GB SSD) | ~€4.50/mes |
| Dominio | **Cloudflare Registrar** | monteastur.com (WHOIS privado incluido) | ~€9.15/año |
| DNS | Cloudflare DNS | Anycast + proxy DDoS gratuito | €0 |
| SSL | **Let's Encrypt** | Certbot, renovación automática | €0 |
| CI/CD | **GitHub Actions** | Workflow deploy-prod.yml | €0 |
| Monitoring | **Prometheus + Grafana + Uptime Kuma** | Docker Compose | €0 |

---

> **Documentos relacionados:**
> - [`FIRST_REAL_DEPLOY_MASTER_CHECKLIST.md`](FIRST_REAL_DEPLOY_MASTER_CHECKLIST.md) — Checklist maestra A-P
> - [`scripts/production-smoke-test.sh`](../scripts/production-smoke-test.sh) — Smoke tests automatizados
> - [`scripts/production-post-deploy-check.sh`](../scripts/production-post-deploy-check.sh) — Verificación post-deploy
> - [`FINAL_PRODUCTION_DEPLOY_CHECKLIST.md`](FINAL_PRODUCTION_DEPLOY_CHECKLIST.md) — Checklist final detallada
> - [`PRODUCTION_VPS_RUNBOOK.md`](PRODUCTION_VPS_RUNBOOK.md) — Runbook de operaciones
>
> **Mantenido por:** Equipo Monteastur Envios
> **Próxima revisión:** 2026-06-25
