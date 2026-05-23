# Uptime Monitoring — Monteastur Envios

## Stack

| Componente | Rol | Acceso |
|-----------|-----|--------|
| **Uptime Kuma** | Monitor de uptime interno | `http://localhost:3001` |
| **Healthchecks.io** | Heartbeat externo (documentado, no integrado) | `https://healthchecks.io` |

## Uptime Kuma

Uptime Kuma es un monitor de uptime auto-hospedado con UI web, notificaciones multi-canal y status page pública.

### Acceso

```
URL:    http://localhost:3001
```

Al primer acceso se crea un usuario administrador.

### Monitores recomendados

| Monitor | Tipo | Endpoint | Intervalo |
|---------|------|----------|-----------|
| App Health | HTTP | `http://nginx/actuator/health` | 60s |
| App Metrics | HTTP | `http://nginx/actuator/prometheus` | 60s |
| PWA Manifest | HTTP | `http://nginx/manifest.webmanifest` | 5m |
| SSL Certificate | SSL | `nginx:443` | 24h |
| DNS Resolution | DNS | `monteastur.com` | 24h |
| Docker Container | Docker | monteastur-app | 60s |
| MySQL | Docker | monteastur-mysql | 60s |
| Prometheus | HTTP | `http://prometheus:9090/-/healthy` | 60s |
| Grafana | HTTP | `http://grafana:3000/api/health` | 60s |

### Notificaciones

Uptime Kuma soporta múltiples canales de notificación:

| Canal | Configuración |
|-------|--------------|
| **Email (SMTP)** | Usar SMTP de `docker-compose.yml` si ya configurado |
| **Telegram** | Bot Token + Chat ID |
| **Discord** | Webhook URL |
| **Slack** | Webhook URL |
| **Pushover** | App Token + User Key |
| **Gotify** | Server URL + Token |

Para configurar: Settings → Notifications → Setup Notification.

### Status Page Pública

Uptime Kuma permite crear una status page pública opcional:

1. Ir a Settings → Status Page
2. Crear nueva página
3. Seleccionar monitores a mostrar
4. Publicar en ruta `/status`

### SSL Monitoring

El monitor de tipo **SSL** verifica:
- Fecha de expiración del certificado
- Validez del emisor
- Renovación automática (Let's Encrypt)

### Docker Monitoring

El monitor de tipo **Docker** requiere:
1. Montar el socket de Docker en uptime-kuma (añadir a docker-compose.yml):
   ```yaml
   volumes:
     - /var/run/docker.sock:/var/run/docker.sock:ro
   ```
2. Crear monitor tipo "Docker" apuntando a monteastur-app, monteastur-mysql, etc.

## Healthchecks.io

Healthchecks.io es un servicio externo de heartbeat monitoring. No está integrado directamente; esta sección documenta cómo añadirlo.

### Concepto

Un **heartbeat** es un ping periódico que un servicio envía a Healthchecks.io para indicar que sigue vivo. Si el ping no llega en el tiempo esperado, Healthchecks.io envía una notificación.

### Monitores recomendados

| Job | Endpoint | Periodo | Grace |
|-----|----------|---------|-------|
| App Health | `https://hc-ping.com/<UUID>` | 10m | 5m |
| Backup MySQL | `https://hc-ping.com/<UUID>` | 24h | 6h |
| Backup Uploads | `https://hc-ping.com/<UUID>` | 24h | 6h |
| Cert Renewal | `https://hc-ping.com/<UUID>` | 7d | 2d |

### Ejemplo de heartbeat desde script

```bash
HEARTBEAT_URL="https://hc-ping.com/tu-uuid-aqui"

# Reportar inicio
curl -fsS -m 10 --retry 5 "$HEARTBEAT_URL/start"

# Ejecutar backup
./scripts/backup-prod.sh

# Reportar éxito
curl -fsS -m 10 --retry 5 "$HEARTBEAT_URL"
```

### Ejemplo con PowerShell

```powershell
$heartbeat = "https://hc-ping.com/tu-uuid-aqui"
Invoke-RestMethod -Uri "$heartbeat/start" -TimeoutSec 10
# ejecutar backup...
Invoke-RestMethod -Uri $heartbeat -TimeoutSec 10
```

### Notificaciones Healthchecks.io

- Email (gratuito)
- Telegram (gratuito)
- Slack
- Discord
- Webhooks
- Pushover
- Gotify
- Matrix
- Apprise

### Límites (plan gratuito)

- Hasta 20 checks
- Historial 100 pings
- Periodo mínimo 1 minuto
- Grace mínimo 20 segundos
- Integración con GitHub, GitLab, Bitbucket

## Próximos pasos

1. Crear usuario admin en Uptime Kuma (primer acceso)
2. Añadir monitores HTTP para los endpoints críticos
3. Configurar notificaciones (Telegram o Discord)
4. _(Opcional)_ Montar Docker socket para monitores Docker
5. _(Futuro)_ Integrar Healthchecks.io en scripts de backup y cron
6. _(Futuro)_ Publicar status page si es necesario
