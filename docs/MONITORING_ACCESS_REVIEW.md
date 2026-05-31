# MONITORING_ACCESS_REVIEW

## Estado

```text
Proyecto: Envios_Paraguay_CMS
Rama: feature/revision-monitoring-access
Fase: 1 — Hardening secretos y perfil prod
Tipo: revision documental de acceso a monitoring
```

---

## Proposito

Este documento revisa como quedan expuestos Prometheus, Grafana y Uptime Kuma en el despliegue actual.

No modifica puertos ni configuracion. Su objetivo es decidir con criterio antes de cerrar o mover accesos.

---

## Servicios revisados

```text
Prometheus
Grafana
Uptime Kuma
Nginx
Docker Compose
```

---

## Estado actual detectado

En `docker-compose.yml` existen puertos publicados para monitoring:

```text
Prometheus  → ${PROMETHEUS_PORT:-9090}:9090
Grafana     → ${GRAFANA_PORT:-3000}:3000
Uptime Kuma → ${UPTIME_KUMA_PORT:-3001}:3001
```

Esto significa que, si el VPS/firewall permite esos puertos, los paneles pueden quedar accesibles directamente desde fuera.

---

## Nginx revisado

Archivo principal:

```text
nginx/nginx.conf
```

Hallazgo:

```text
Nginx carga configuraciones desde /etc/nginx/conf.d/*.conf.
```

Archivos reales detectados:

```text
nginx/conf.d/local.conf
nginx/conf.d/monteastur.conf
```

---

## local.conf

Estado:

```text
Proxy simple hacia app:8080 para localhost.
```

Ruta principal:

```text
location / → proxy_pass http://app:8080
```

No contiene reglas para:

```text
/grafana
/prometheus
/uptime
```

---

## monteastur.conf

Estado:

```text
Proxy principal hacia app:8080.
```

Tiene cabeceras de seguridad:

```text
X-Frame-Options
X-Content-Type-Options
Referrer-Policy
Permissions-Policy
Strict-Transport-Security
Content-Security-Policy
```

Ruta principal:

```text
location / → proxy_pass http://app:8080
```

No contiene reglas para:

```text
/grafana
/prometheus
/uptime
```

---

## Lectura tecnica

Actualmente Nginx protege y enruta la aplicacion principal, pero no parece gestionar el acceso a los paneles de monitoring.

Por tanto, el acceso a Prometheus, Grafana y Uptime Kuma depende de:

```text
puertos publicados en Docker Compose
firewall del VPS
reglas del proveedor cloud/hosting
credenciales internas de cada herramienta
```

---

## Riesgos detectados

### P1 — Prometheus expuesto

Prometheus no deberia quedar abierto a Internet sin proteccion.

Riesgo:

```text
Exposicion de metricas internas, nombres de servicios, rutas y estado del sistema.
```

---

### P1 — Grafana expuesto

Grafana tiene login, pero no conviene confiar solo en eso si esta abierto publicamente.

Riesgo:

```text
Ataques de fuerza bruta, enumeracion, intentos automatizados y ruido de bots.
```

---

### P1 — Uptime Kuma expuesto

Uptime Kuma puede contener informacion sobre endpoints, disponibilidad y configuracion de checks.

Riesgo:

```text
Exposicion de informacion operacional.
```

---

## Opciones de proteccion

### Opcion A — Mantener abierto temporalmente

Uso:

```text
Demo, preventa, pruebas controladas.
```

Condicion:

```text
Solo si el VPS/firewall esta controlado y las credenciales son fuertes.
```

Riesgo:

```text
Medio.
```

---

### Opcion B — Limitar a localhost

Ejemplo conceptual:

```text
127.0.0.1:9090:9090
127.0.0.1:3000:3000
127.0.0.1:3001:3001
```

Ventaja:

```text
No quedan expuestos publicamente.
```

Inconveniente:

```text
Para acceder desde fuera haria falta SSH tunnel o VPN.
```

Recomendacion:

```text
Muy buena para produccion pequena y segura.
```

---

### Opcion C — Proteger con firewall

Uso:

```text
Permitir acceso solo desde IPs concretas.
```

Ventaja:

```text
Simple y eficaz.
```

Inconveniente:

```text
Depende de IP fija o reglas bien mantenidas.
```

---

### Opcion D — Pasar por Nginx con autenticacion

Uso:

```text
/grafana
/prometheus
/uptime
```

Ventaja:

```text
Acceso unificado por dominio y HTTPS.
```

Inconveniente:

```text
Mas configuracion, riesgo de errores con subpaths, websockets y redirects.
```

---

### Opcion E — Red privada / VPN

Uso:

```text
Entornos mas profesionales o multiusuario.
```

Ventaja:

```text
Mayor control.
```

Inconveniente:

```text
Mas complejidad.
```

---

## Decision recomendada

Para este proyecto, la recomendacion por fases es:

```text
Preproduccion/demo:
Mantener monitoring accesible solo si firewall y credenciales estan controlados.

Produccion real:
No exponer Prometheus, Grafana ni Uptime Kuma publicamente sin proteccion.
```

Primera opcion tecnica recomendada para produccion:

```text
Limitar puertos de monitoring a localhost.
```

Ejemplo conceptual:

```text
127.0.0.1:${PROMETHEUS_PORT:-9090}:9090
127.0.0.1:${GRAFANA_PORT:-3000}:3000
127.0.0.1:${UPTIME_KUMA_PORT:-3001}:3001
```

---

## Cambio tecnico recomendado en una rama posterior

Crear rama:

```text
feature/limitar-monitoring-localhost
```

Objetivo:

```text
Modificar docker-compose.yml para publicar Prometheus, Grafana y Uptime Kuma solo en localhost.
```

No mezclar con:

```text
Nginx
SSL
Spring Security
React
Grafana dashboards
Deploy scripts
```

---

## Checklist antes de tocar puertos

```text
[ ] Confirmar si se accede a Grafana desde fuera del VPS.
[ ] Confirmar si Prometheus necesita acceso externo.
[ ] Confirmar si Uptime Kuma se usa desde navegador externo.
[ ] Confirmar si se acepta usar SSH tunnel.
[ ] Confirmar firewall actual del VPS.
[ ] Confirmar impacto en demo/preventa.
```

---

## Decision actual

```text
Estado: acceso a monitoring revisado
Riesgo general: medio-alto si los puertos estan abiertos publicamente
Siguiente paso: decidir si limitar monitoring a localhost
```

---

## Frase guia

Las metricas son los ojos del sistema.

Pero unos ojos abiertos al mundo tambien pueden revelar demasiado.
