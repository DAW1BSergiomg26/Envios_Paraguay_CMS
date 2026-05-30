# Hardening de configuracion de produccion

Rama: `feature/hardening-config-produccion`

## Objetivo

Endurecer la configuracion de produccion de Monteastur Envios sin tocar la interfaz, la logica de negocio ni los controladores.

## Diagnostico

El proyecto ya tiene una base de produccion avanzada: Spring Boot, Thymeleaf, React, MySQL, Docker, Nginx, Actuator, Prometheus, CI/CD, scripts de backup y documentacion de despliegue.

Aun asi, hay puntos de configuracion que deben mantenerse bajo control para evitar errores al desplegar:

- El perfil local permite arranque rapido para desarrollo.
- El perfil `prod` debe ser obligatorio en servidores reales.
- Las credenciales reales deben vivir solo en variables de entorno o en un `.env` privado no versionado.
- Hibernate no debe modificar esquema automaticamente en produccion.
- Los logs SQL no deben estar activos en produccion.
- Actuator debe exponer solo endpoints necesarios.
- Las trazas internas no deben mostrarse al usuario final.

## Reglas de produccion recomendadas

### Spring profile

Usar siempre:

```env
SPRING_PROFILES_ACTIVE=prod
```

### JPA / Hibernate

Produccion normal:

```env
DB_DDL_AUTO=validate
JPA_SHOW_SQL=false
```

No usar `update` salvo en una fase controlada de migracion.

### Thymeleaf

Produccion:

```env
THYMELEAF_CACHE=true
```

### Datos demo

Produccion:

```env
APP_DEMO_DATA=false
```

### Actuator

Exponer solo lo necesario:

```env
ACTUATOR_ENDPOINTS=health,info,prometheus
```

Evitar exponer `env`, `beans`, `configprops` o endpoints sensibles.

### Errores HTTP

En produccion no se deben mostrar stacktraces ni excepciones internas.

Recomendacion:

```env
SERVER_ERROR_INCLUDE_MESSAGE=never
SERVER_ERROR_INCLUDE_STACKTRACE=never
SERVER_ERROR_INCLUDE_EXCEPTION=false
```

## Checklist antes de deploy

- [ ] `.env` real no esta versionado.
- [ ] `SPRING_PROFILES_ACTIVE=prod` activo.
- [ ] `DB_DDL_AUTO=validate`.
- [ ] `JPA_SHOW_SQL=false`.
- [ ] Credenciales admin cambiadas.
- [ ] Credenciales de base de datos cambiadas.
- [ ] Grafana con clave fuerte.
- [ ] Actuator limitado.
- [ ] Backups configurados.
- [ ] Uploads persistentes.
- [ ] Logs persistentes.
- [ ] Smoke tests ejecutados.

## Proxima fase recomendada

Crear una revision segura de:

- `application.properties`
- `application-prod.properties`
- `.env.example`
- `.env.production.example`

con cambios pequenos y probados localmente antes de mergear a `develop`.
