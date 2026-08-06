# Diseño: Despliegue Render.com — Conversor JDBC automático + Blueprint Docker

**Fecha:** 2026-08-06
**Estado:** Aprobado por el usuario (sin cambios)
**Rol:** Arquitecto Cloud / Especialista Docker-Render

## Contexto

El proyecto `Envios_Paraguay_CMS` (Spring Boot 3.5.16, Java 25, MySQL 8, Redis, Thymeleaf)
se prepara para desplegarse automáticamente en Render.com como Web Service Docker
sobre el plan gratuito.

Estado actual relevante:

- Existe un `BootstrapPropertyEnvironmentPostProcessor` (registrado en
  `src/main/resources/META-INF/spring.factories`) que normaliza `spring.datasource.url`
  y provee aliases `REDIS_HOST`/`REDIS_PORT`. Su normalizador
  `BootstrapPropertyNormalizer.normalizeJdbcUrl` solo sustituye `;` por `&`.
- `application-prod.properties` usa un fallback TiDB Cloud hardcodeado
  (`jdbc:mysql://gateway01...tidbcloud.com:4000/...`), impidiendo fail-fast claro y
  descartando por completo `DATABASE_URL`.
- Existe un `render.yaml` obsoleto (`runtime: image`, TiDB hardcodeado, sin Redis).
- El `Dockerfile` no consume `JAVA_OPTS` (ENTRYPOINT directo `java -jar`) y su
  HEALTHCHECK interno apunta fijo a `localhost:8080` (Render asigna `PORT` dinámico).
- La app **requiere Redis** (Spring Session Redis para autenticación admin + caché).

## Objetivos

1. **Conversor JDBC automático:** si `SPRING_DATASOURCE_URL` o `DATABASE_URL` no
   comienzan con `jdbc:`, anteponerlo (p. ej. `mysql://...` → `jdbc:mysql://...`),
   insensible a mayúsculas, manteniendo la sustitución `;` → `&`.
2. **Blueprint `render.yaml`:** Web Service Docker sobre la rama `main`, plan free,
   con credenciales en `sync: false` para rellenarlas en el dashboard de Render.

## Decisiones de diseño (aprobadas)

- **Estrategia Redis:** Redis externo gratuito (p. ej. Upstash) vía `REDIS_HOST`
  (`sync: false`). Cero cambios de código en la app.
- **Reemplazo del `render.yaml` anterior y del fallback TiDB:** el perfil `prod`
  pasa a fallback vacío `${SPRING_DATASOURCE_URL:}` (fail-fast). Precedencia
  `SPRING_DATASOURCE_URL` > `DATABASE_URL`.
- **Alcance de `DATABASE_URL`:** solo alimenta la URL (con prefijo `jdbc:` automático);
  las credenciales siguen viniendo de `DB_USERNAME`/`DB_PASSWORD`.
- **Dockerfile:** inyectar `$JAVA_OPTS` (tuning para free tier 512 MB) y HEALTHCHECK
  interno consciente de `${PORT:-8080}`.

## Enfoque elegido

Extender la infraestructura existente de EnvironmentPostProcessor en lugar de crear
beans `DataSource` o un EPP dedicado:

- **Ventajas:** cero interferencia con la auto-configuración de Spring Boot,
  precedencia máxima vía primer property source, testeable con `MockEnvironment`,
  sigue el patrón ya registrado en `spring.factories`.
- **Alternativas descartadas:** bean `DataSource`/`DataSourcePropertiesCustomizer`
  (el placeholder `${SPRING_DATASOURCE_URL:}` ya inyectó el valor; no cubre
  `DATABASE_URL` limpiamente); EPP separado (duplica registros, dos EPP tocando la
  misma propiedad).

## Arquitectura y componentes

### 1. `BootstrapPropertyNormalizer` (extensión)

Nueva lógica en `normalizeJdbcUrl(String rawUrl)`:

1. Si `rawUrl` es null/blank → devolver tal cual.
2. Si NO comienza con `jdbc:` (comparación case-insensitive) → anteponer `jdbc:`.
3. Sustituir `;` por `&` (comportamiento existente).
4. Devolver el resultado.

Sin cambios de firma → no rompe consumidores existentes.

### 2. `BootstrapPropertyEnvironmentPostProcessor` (reescritura)

Nuevo flujo en `postProcessEnvironment`:

1. `springUrl = environment.getProperty("SPRING_DATASOURCE_URL")` (clave exacta).
2. `databaseUrl = environment.getProperty("DATABASE_URL")` (clave exacta).
3. `selected = hasText(springUrl) ? springUrl : databaseUrl`.
4. Si `selected` tiene texto:
   - `normalized = BootstrapPropertyNormalizer.normalizeJdbcUrl(selected)`.
   - `overrides.put("spring.datasource.url", normalized)` (siempre, para que el
     override tenga precedencia máxima incluso cuando ya venía prefijado y el origen
     sea `DATABASE_URL`).
5. Conservar la lógica de aliases `REDIS_HOST`/`REDIS_PORT` (sin cambios).
6. Añadir el `MapPropertySource` primero si `overrides` no está vacío.

Motivo del cambio de `spring.datasource.url` (búsqueda relajada) a claves exactas:
robustez frente al orden de los `EnvironmentPostProcessors` (nuestro EPP corre con
`HIGHEST_PRECEDENCE`, antes de `ConfigDataEnvironmentPostProcessor`) y habilitación
de `DATABASE_URL` como origen alternativo.

### 3. `application-prod.properties`

Cambiar línea 16:

```properties
spring.datasource.url=${SPRING_DATASOURCE_URL:}
```

- Username/password siguen leyendo `DB_USERNAME`/`DB_PASSWORD` (obligatorios en prod).
- Sin URL → la app falla al arrancar con error claro (fail-fast).

### 4. `Dockerfile`

- **ENTRYPOINT** (forma shell para expandir `JAVA_OPTS`):

  ```dockerfile
  ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar --spring.profiles.active=prod"]
  ```

- **HEALTHCHECK** interno consciente del puerto de Render:

  ```dockerfile
  HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD ["sh", "-c", "curl -fsS \"http://localhost:${PORT:-8080}/actuator/health\" || exit 1"]
  ```

Render usa su propio `healthCheckPath`; este ajuste es saneamiento para no marcar
`unhealthy` cuando `PORT != 8080`.

### 5. `render.yaml` (nuevo blueprint)

```yaml
services:
  - type: web
    name: envios-paraguay-cms
    env: docker
    plan: free
    branch: main
    dockerfilePath: ./Dockerfile
    healthCheckPath: /actuator/health
    numInstances: 1
    envVars:
      - key: SPRING_PROFILES_ACTIVE
        value: prod
      - key: SPRING_DATASOURCE_URL
        sync: false
      - key: DB_USERNAME
        sync: false
      - key: DB_PASSWORD
        sync: false
      - key: REDIS_HOST
        sync: false
      - key: REDIS_PORT
        value: "6379"
      - key: ADMIN_USERNAME
        sync: false
      - key: ADMIN_PASSWORD
        sync: false
      - key: APP_TRACKING_BASE_URL
        sync: false
      - key: JAVA_OPTS
        value: "-Xms256m -Xmx384m -XX:+UseG1GC -XX:+UseStringDeduplication"
      - key: THYMELEAF_CACHE
        value: "true"
      - key: UPLOAD_DIR
        value: "/tmp/uploads"
      - key: APP_NOTIFICATION_MAIL_ENABLED
        value: "false"
      - key: JPA_SHOW_SQL
        value: "false"
```

Notas operativas (documentadas como comentarios en el YAML):

- `APP_NOTIFICATION_MAIL_ENABLED=false` por defecto: Render free no ofrece SMTP;
  puede activarse después añadiendo `SPRING_MAIL_*`.
- `UPLOAD_DIR=/tmp/uploads`: filesystem efímero en Render free; los uploads no
  persisten entre redeploys.
- Las claves `sync: false` se rellenan en el dashboard en el primer despliegue.
  La URL MySQL puede pegarse como `mysql://...` (el conversor añade `jdbc:`).

## Flujo de datos

1. Render inyecta env vars (definidas en `render.yaml`/dashboard).
2. Spring Boot arranca; `BootstrapPropertyEnvironmentPostProcessor` corre antes que
   la configuración de DataSource.
3. El EPP selecciona `SPRING_DATASOURCE_URL` o `DATABASE_URL`, normaliza
   (`jdbc:` + `;`→`&`) y publica `spring.datasource.url` con precedencia máxima.
4. Spring Boot auto-configura el pool Hikari con la URL normalizada.
5. Flyway valida/ejecuta migraciones; la app sirve en `PORT` con health check.

## Manejo de errores

- Sin `SPRING_DATASOURCE_URL` ni `DATABASE_URL` en `prod` → fallback vacío →
  `DataSourceAutoConfiguration` falla al arrancar con error de configuración claro.
- URLs malformadas pasan tal cual salvo el prefijo/sustitución (sin parsing adicional;
  el driver MySQL reporta el error real).

## Testing

- `BootstrapPropertyNormalizerTest` (extensión):
  - `mysql://host/db` → `jdbc:mysql://host/db`.
  - `mysql://host/db?useSSL=false;x=y` → prefijo + `;`→`&`.
  - `JDBC:mysql://...` (mayúsculas) → sin doble prefijo.
  - `jdbc:mysql://...` válido → intacto (caso existente).
  - null / blank → intactos.
- `BootstrapPropertyEnvironmentPostProcessorTest` (nuevo, con `MockEnvironment`):
  - `SPRING_DATASOURCE_URL=mysql://...` → `spring.datasource.url=jdbc:mysql://...`.
  - `DATABASE_URL=mysql://...` sin SPRING_* → se usa DATABASE_URL prefijado.
  - Ambos presentes → gana `SPRING_DATASOURCE_URL`.
  - Sin ninguna → sin override.
  - Aliases `REDIS_HOST`/`REDIS_PORT` intactos.

## Criterios de éxito

- Suite completa (container `maven:3.9-eclipse-temurin-25`) en **BUILD SUCCESS**,
  264+ tests en verde.
- `render.yaml` desplegable en Render (branch `main`, `env: docker`, plan free).
- La URL `mysql://...` se convierte en `jdbc:mysql://...` sin código de aplicación.
- `main` sincronizada en `origin/main` y `docs/handoff.md` actualizado.

## Alcance excluido (YAGNI)

- Parsing de credenciales embebidas en `DATABASE_URL` (no requerido).
- Recursos Redis gestionado de pago en el blueprint (se usa Redis externo free).
- Disco persistente de Render (no disponible en free; se documenta la limitación).
- Cambios en `koyeb.yaml`.
