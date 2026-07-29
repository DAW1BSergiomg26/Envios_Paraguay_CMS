# First Deploy Risk Register — MonteAstur Envios

Registro de riesgos para el primer despliegue real en VPS.

| # | Riesgo | Probabilidad | Impacto | Mitigación | Rollback | Prioridad |
|---|--------|-------------|---------|------------|----------|-----------|
| 1 | **DNS tarda en propagarse** | Alta (~30%) | Medio — SSL no funciona, web no accesible | Configurar DNS antes del deploy. TTL=300 durante la migración. Usar `dig @8.8.8.8` para verificar. | No aplica (esperar propagación) | 🟡 Media |
| 2 | **SSL falla (certbot)** | Media (~20%) | Alto — HTTPS no disponible | Verificar DNS propagado y puerto 80 abierto. Usar `--dry-run` primero. | HTTP sin SSL mientras se resuelve | 🔴 Alta |
| 3 | **Docker build falla** | Baja (~10%) | Alto — App no arranca | Build local verificado previamente. Usar `--no-cache` si hay error. Usar `docker compose logs` para debug. | `git checkout v20.0-pre-deploy` y rebuild | 🔴 Alta |
| 4 | **MySQL schema falla (DDL_AUTO=validate)** | Media (~20%) | Alto — App no conecta a BD | Usar `DB_DDL_AUTO=update` en primer arranque. Luego cambiar a `validate`. Verificar logs de app. | Cambiar a `update` temporalmente, luego volver a `validate` | 🔴 Alta |
| 5 | **Credenciales incorrectas en .env** | Media (~15%) | Alto — App no arranca o no conecta | Verificar `.env` antes de `docker compose up`. Usar variables consistentes (MYSQL_USER = DB_USERNAME). | Corregir `.env` y `docker compose up -d` | 🔴 Alta |
| 6 | **GitHub Actions SSH falla** | Media (~20%) | Medio — CD automático no funciona | Verificar SSH manualmente antes. `ssh deploy@<IP>` debe funcionar. Verificar GitHub Secrets. | Deploy manual por SSH como fallback | 🟡 Media |
| 7 | **Nginx no arranca** | Baja (~10%) | Alto — Web no accesible | Verificar sintaxis con `docker compose exec nginx nginx -t`. Verificar puertos. | `docker compose logs nginx`, corregir config, restart | 🔴 Alta |
| 8 | **App no conecta a BD** | Baja (~10%) | Alto — App no funciona | Verificar `SPRING_DATASOURCE_URL`. Verificar que MySQL está healthy. | `docker compose logs app`, corregir .env, restart | 🔴 Alta |
| 9 | **Puerto ocupado (80, 443)** | Media (~15%) | Medio — Nginx no puede bind | Verificar con `sudo netstat -tlnp \| grep ':80'` antes de arrancar. Cambiar `NGINX_PORT` si es necesario. | Usar puerto alternativo, investigar qué ocupa el puerto | 🟡 Media |
| 10 | **VPS sin recursos (RAM/CPU/Disco)** | Baja (~5%) | Medio — App lenta o OutOfMemory | Verificar especificaciones CX22 (4GB RAM, 40GB SSD). `docker stats` para monitorizar. | Escalar a CX32 si es necesario | 🟡 Media |
| 11 | **Backup falla por falta de espacio** | Baja (~5%) | Medio — Sin copias de seguridad | Verificar `df -h` antes del deploy. Configurar retención de 30 días. | Liberar espacio, ejecutar backup manual | 🟡 Media |
| 12 | **Firewall bloquea puertos** | Media (~15%) | Alto — Servicios no accesibles | Verificar UFW: `sudo ufw status verbose`. Puertos 22, 80, 443 deben estar abiertos. | `sudo ufw allow 80/tcp && sudo ufw allow 443/tcp` | 🔴 Alta |
| 13 | **fail2ban bloquea IP propia** | Baja (~5%) | Medio — Pérdida de acceso SSH | Verificar whitelist de IPs. Tener acceso de consola desde panel Hetzner. | `sudo fail2ban-client set sshd unbanip <IP>` | 🟡 Media |
| 14 | **Certificado SSL expira sin renovación** | Baja (~5%) | Alto — HTTPS deja de funcionar | Configurar crontab para renovación automática. Verificar con `certbot renew --dry-run`. | Renovar manualmente o reinstalar certificado | 🔴 Alta |
| 15 | **Error humano (borrar .env, archivos)** | Baja (~10%) | Alto — App caída | Tener backup de `.env` en gestor de contraseñas. Usar `git checkout` para restaurar archivos. | Rollback a tag anterior + restaurar .env | 🔴 Alta |
| 16 | **Ataque de fuerza bruta a /login** | Media (~20%) | Medio — Cuenta admin comprometida | fail2ban configurado. Rate limiting pendiente (no implementado). Contraseñas seguras. | Bloquear IP con fail2ban, cambiar contraseña | 🟡 Media |
| 17 | **Ataque DDoS** | Baja (~5%) | Alto — App no disponible | Cloudflare proxy (protección DDoS). Uptime Kuma para detectar. | Activar Cloudflare Under Attack mode | 🟡 Media |
| 18 | **MySQL corrupción de datos** | Baja (~5%) | Alto — Pérdida de datos | Backups diarios configurados. Volumen persistente `mysql_data`. | Restore desde backup más reciente | 🔴 Alta |

---

## Matriz de riesgos

```
Impacto
  Alto  │  4, 5, 7, 8, 12, 14, 15, 18    │  2, 3, 6
        │                                 │
  Medio │  1, 9, 10, 11, 13, 16           │  17
        │                                 │
  Bajo  │                                 │
        └─────────────────────────────────┤
              Baja          Media       Alta
                          Probabilidad
```

### Riesgos críticos (Alta probabilidad + Alto impacto)

| # | Riesgo | Acción preventiva |
|---|--------|-------------------|
| 2 | SSL falla | `--dry-run` primero, DNS propagado, puerto 80 abierto |
| 3 | Docker build falla | Verificar build local antes |
| 4 | MySQL schema falla | Usar `update` en primer arranque |

### Decisiones rápidas durante el deploy

| Si ocurre... | Decisión |
|-------------|----------|
| DNS no propaga tras 30 min | Continuar con IP directa para pruebas, SSL más tarde |
| SSL falla | Continuar en HTTP, resolver SSL después |
| Docker build tarda >10 min | Verificar disco, usar `--no-cache` |
| Healthcheck no UP tras 3 min | Verificar logs (`docker compose logs app --tail 50`) |
| Smoke tests fallan | NO declarar deploy exitoso. Rollback. |

---

## Plan de contingencia

### Si TODO falla

```bash
# 1. Rollback inmediato
cd /opt/monteastur && ./scripts/rollback-prod.sh v20.0-pre-deploy

# 2. Verificar estado básico
curl -f http://localhost/actuator/health
docker compose ps

# 3. Si rollback no funciona
git checkout v20.0-pre-deploy
docker compose down -v   # CUIDADO: borra volúmenes MySQL
docker compose up -d --build

# 4. Documentar en GitHub Issue
#   - Qué paso
#   - Cuándo paso
#   - Cómo se resolvió
#   - Cómo prevenirlo
```

### Quién toma decisiones

| Decisión | Responsable |
|----------|-------------|
| Continuar tras error menor | Admin del proyecto |
| Rollback | Admin del proyecto |
| Escalar VPS | Admin del proyecto |
| Contactar soporte VPS | Admin del proyecto |
| Cancelar deploy | Admin del proyecto |

---

## Documentos relacionados

| Documento | Enlace |
|-----------|--------|
| Execution Plan | `docs/VPS_DEPLOY_EXECUTION_PLAN.md` |
| Day Runbook | `docs/VPS_DEPLOY_DAY_RUNBOOK.md` |
| Secrets Template | `docs/PRODUCTION_SECRETS_TEMPLATE.md` |
| VPS Next Actions | `docs/VPS_REAL_NEXT_ACTIONS.md` |