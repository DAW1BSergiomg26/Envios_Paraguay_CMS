# Despliegue Render.com: Conversor JDBC + Blueprint Docker — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Preparar `Envios_Paraguay_CMS` para despliegue automático en Render.com añadiendo un conversor JDBC automático (`SPRING_DATASOURCE_URL`/`DATABASE_URL` → prefijo `jdbc:`), ajustes JVM/Docker y un blueprint `render.yaml` oficial.

**Architecture:** Se extiende la infraestructura existente de `EnvironmentPostProcessor` (`BootstrapPropertyEnvironmentPostProcessor` + `BootstrapPropertyNormalizer`), se elimina el fallback TiDB del perfil prod (fail-fast), se ajusta el `Dockerfile` (JAVA_OPTS + HEALTHCHECK dinámico) y se reemplaza `render.yaml`.

**Tech Stack:** Spring Boot 3.5.16 (Java 25), Spring Environment/EnvironmentPostProcessor, MockEnvironment (spring-test), Maven, Docker, Render Blueprint YAML.

## Global Constraints

- Java puro, sin Lombok; inyección por constructor; cero `@Autowired` en campos.
- Migraciones solo Flyway; sin cambios de esquema en esta tarea.
- TDD obligatorio: test primero (RED), implementación mínima (GREEN).
- Suite completa en contenedor: `docker run ... maven:3.9-eclipse-temurin-25 mvn clean test` → BUILD SUCCESS.
- Push a `origin/main` solo con autorización explícita del usuario (otorgada en este sprint).
- No tocar `koyeb.yaml`, `start-app.ps1`, `frontend-react/` ni `docker-compose.yml`.
- Nomenclatura: clases/métodos en inglés, documentación en español, sin comentarios redundantes.

---

### Task 1: Normalizador JDBC con prefijo automático

**Files:**
- Modify: `src/main/java/com/monteastur/envios/config/BootstrapPropertyNormalizer.java`
- Test: `src/test/java/com/monteastur/envios/config/BootstrapPropertyNormalizerTest.java`

**Interfaces:**
- Consumes: nada (tarea 1, independiente).
- Produces: `public static String BootstrapPropertyNormalizer.normalizeJdbcUrl(String rawUrl)` con el nuevo comportamiento (prefijo `jdbc:` case-insensitive + `;`→`&`). Es usado por Task 2.

- [ ] **Step 1: Escribir los tests que fallan**

Añadir estos métodos a `BootstrapPropertyNormalizerTest.java` (conservar los 2 existentes):

```java
    @Test
    void normalizeJdbcUrlPrependsJdbcPrefixWhenMissing() {
        String normalized = BootstrapPropertyNormalizer.normalizeJdbcUrl(
            "mysql://host:3306/envios_paraguay_cms?useSSL=false"
        );

        assertThat(normalized)
            .isEqualTo("jdbc:mysql://host:3306/envios_paraguay_cms?useSSL=false");
    }

    @Test
    void normalizeJdbcUrlPrependsPrefixAndReplacesSemicolons() {
        String normalized = BootstrapPropertyNormalizer.normalizeJdbcUrl(
            "mysql://host:3306/envios_paraguay_cms?useSSL=false;serverTimezone=UTC"
        );

        assertThat(normalized)
            .isEqualTo("jdbc:mysql://host:3306/envios_paraguay_cms?useSSL=false&serverTimezone=UTC");
    }

    @Test
    void normalizeJdbcUrlDoesNotDoublePrefixOnUppercaseJdbc() {
        String original = "JDBC:mysql://host:3306/envios_paraguay_cms";

        assertThat(BootstrapPropertyNormalizer.normalizeJdbcUrl(original)).isEqualTo(original);
    }

    @Test
    void normalizeJdbcUrlLeavesBlankAndNullUntouched() {
        assertThat(BootstrapPropertyNormalizer.normalizeJdbcUrl(null)).isNull();
        assertThat(BootstrapPropertyNormalizer.normalizeJdbcUrl("")).isEmpty();
        assertThat(BootstrapPropertyNormalizer.normalizeJdbcUrl("   ")).isEqualTo("   ");
    }
```

- [ ] **Step 2: Ejecutar los tests y verificar que fallan (RED)**

```powershell
docker run --rm -v "${PWD}:/app" -w /app --network envios_paraguay_cms_backend -e SPRING_DATASOURCE_URL="jdbc:mysql://monteastur-mysql:3306/envios_paraguay_cms_test?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true" -e DB_USERNAME=root -e DB_PASSWORD=root -e SPRING_DATA_REDIS_HOST=monteastur-redis -v "${HOME}\.m2:/root/.m2" maven:3.9-eclipse-temurin-25 mvn test -Dtest=BootstrapPropertyNormalizerTest -DfailIfNoTests=false
```

Expected: `BootstrapPropertyNormalizerTest` con 2 fallos (los tests de prefijo `PrependsJdbcPrefixWhenMissing` y `PrependsPrefixAndReplacesSemicolons`). Los tests `LeavesBlankAndNullUntouched` y `DoesNotDoublePrefixOnUppercaseJdbc` ya pasan con la implementación actual (guards, no RED) — es correcto y no bloquea.

- [ ] **Step 3: Implementación mínima**

Reemplazar el cuerpo de `BootstrapPropertyNormalizer.java`:

```java
package com.monteastur.envios.config;

import java.util.Locale;

import org.springframework.util.StringUtils;

public final class BootstrapPropertyNormalizer {

    private static final String JDBC_PREFIX = "jdbc:";

    private BootstrapPropertyNormalizer() {
    }

    public static String normalizeJdbcUrl(String rawUrl) {
        if (!StringUtils.hasText(rawUrl)) {
            return rawUrl;
        }

        String normalized = rawUrl;
        if (!normalized.toLowerCase(Locale.ROOT).startsWith(JDBC_PREFIX)) {
            normalized = JDBC_PREFIX + normalized;
        }

        return normalized.replace(";", "&");
    }
}
```

- [ ] **Step 4: Ejecutar los tests y verificar que pasan (GREEN)**

Mismo comando del Step 2. Expected: `BUILD SUCCESS`, todos los métodos del test en verde.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/monteastur/envios/config/BootstrapPropertyNormalizer.java src/test/java/com/monteastur/envios/config/BootstrapPropertyNormalizerTest.java
git commit -m "feat(cloud): auto-prepend jdbc: prefix in BootstrapPropertyNormalizer"
```

---

### Task 2: EPP con precedencia SPRING_DATASOURCE_URL > DATABASE_URL

**Files:**
- Modify: `src/main/java/com/monteastur/envios/config/BootstrapPropertyEnvironmentPostProcessor.java`
- Test: `src/test/java/com/monteastur/envios/config/BootstrapPropertyEnvironmentPostProcessorTest.java` (nuevo)

**Interfaces:**
- Consumes: `BootstrapPropertyNormalizer.normalizeJdbcUrl(String)` (Task 1).
- Produces: `postProcessEnvironment(ConfigurableEnvironment, SpringApplication)` que publica `spring.datasource.url` normalizada (primer property source) y conserva los aliases `REDIS_HOST`/`REDIS_PORT`.

- [ ] **Step 1: Escribir el test que falla**

Crear `src/test/java/com/monteastur/envios/config/BootstrapPropertyEnvironmentPostProcessorTest.java`:

```java
package com.monteastur.envios.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class BootstrapPropertyEnvironmentPostProcessorTest {

    private final BootstrapPropertyEnvironmentPostProcessor processor =
        new BootstrapPropertyEnvironmentPostProcessor();

    private String normalizedDatasourceUrl(MockEnvironment environment) {
        processor.postProcessEnvironment(environment, new SpringApplication());
        return environment.getProperty("spring.datasource.url");
    }

    @Test
    void prependsJdbcPrefixToSpringDatasourceUrl() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("SPRING_DATASOURCE_URL", "mysql://host:3306/db?useSSL=false");

        assertThat(normalizedDatasourceUrl(environment))
            .isEqualTo("jdbc:mysql://host:3306/db?useSSL=false");
    }

    @Test
    void fallsBackToDatabaseUrlWhenSpringDatasourceUrlMissing() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("DATABASE_URL", "mysql://host:3306/db?useSSL=false");

        assertThat(normalizedDatasourceUrl(environment))
            .isEqualTo("jdbc:mysql://host:3306/db?useSSL=false");
    }

    @Test
    void springDatasourceUrlWinsOverDatabaseUrl() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("SPRING_DATASOURCE_URL", "jdbc:mysql://winner:3306/db");
        environment.setProperty("DATABASE_URL", "mysql://loser:3306/db");

        assertThat(normalizedDatasourceUrl(environment))
            .isEqualTo("jdbc:mysql://winner:3306/db");
    }

    @Test
    void doesNotOverrideUrlWhenNeitherVariableIsSet() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("spring.datasource.url", "jdbc:mysql://default:3306/db");

        assertThat(normalizedDatasourceUrl(environment))
            .isEqualTo("jdbc:mysql://default:3306/db");
    }

    @Test
    void keepsRedisAliasesBehavior() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("SPRING_DATA_REDIS_HOST", "upstash.example.com");
        environment.setProperty("REDIS_PORT", "6380");

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.data.redis.host"))
            .isEqualTo("upstash.example.com");
        assertThat(environment.getProperty("spring.data.redis.port"))
            .isEqualTo("6380");
    }
}
```

- [ ] **Step 2: Ejecutar el test y verificar que falla (RED)**

```powershell
docker run --rm -v "${PWD}:/app" -w /app --network envios_paraguay_cms_backend -e SPRING_DATASOURCE_URL="jdbc:mysql://monteastur-mysql:3306/envios_paraguay_cms_test?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true" -e DB_USERNAME=root -e DB_PASSWORD=root -e SPRING_DATA_REDIS_HOST=monteastur-redis -v "${HOME}\.m2:/root/.m2" maven:3.9-eclipse-temurin-25 mvn test -Dtest=BootstrapPropertyEnvironmentPostProcessorTest -DfailIfNoTests=false
```

Expected: fallos en `prependsJdbcPrefixToSpringDatasourceUrl`, `fallsBackToDatabaseUrlWhenSpringDatasourceUrlMissing`, `springDatasourceUrlWinsOverDatabaseUrl` (la lógica actual lee `spring.datasource.url` vía búsqueda relajada y no usa `DATABASE_URL`).

- [ ] **Step 3: Implementación mínima**

Reemplazar `postProcessEnvironment` de `BootstrapPropertyEnvironmentPostProcessor.java`:

```java
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> overrides = new LinkedHashMap<>();

        String springUrl = environment.getProperty("SPRING_DATASOURCE_URL");
        String databaseUrl = environment.getProperty("DATABASE_URL");
        String selectedUrl = StringUtils.hasText(springUrl) ? springUrl : databaseUrl;
        if (StringUtils.hasText(selectedUrl)) {
            overrides.put("spring.datasource.url", BootstrapPropertyNormalizer.normalizeJdbcUrl(selectedUrl));
        }

        String redisHost = environment.getProperty("spring.data.redis.host");
        if (!StringUtils.hasText(redisHost)) {
            String aliasHost = environment.getProperty("SPRING_DATA_REDIS_HOST");
            if (StringUtils.hasText(aliasHost)) {
                overrides.put("spring.data.redis.host", aliasHost);
            }
        }

        String redisPort = environment.getProperty("spring.data.redis.port");
        if (!StringUtils.hasText(redisPort)) {
            String aliasPort = environment.getProperty("REDIS_PORT");
            if (StringUtils.hasText(aliasPort)) {
                overrides.put("spring.data.redis.port", aliasPort);
            }
        }

        if (!overrides.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, overrides));
        }
    }
```

Los imports y la constante `PROPERTY_SOURCE_NAME` ya existen; el resto del archivo no cambia.

- [ ] **Step 4: Ejecutar el test y verificar que pasa (GREEN)**

Mismo comando del Step 2. Expected: `BUILD SUCCESS`, 5 métodos en verde.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/monteastur/envios/config/BootstrapPropertyEnvironmentPostProcessor.java src/test/java/com/monteastur/envios/config/BootstrapPropertyEnvironmentPostProcessorTest.java
git commit -m "feat(cloud): EPP prefers SPRING_DATASOURCE_URL over DATABASE_URL with jdbc normalization"
```

---

### Task 3: Perfil prod fail-fast (sin fallback TiDB)

**Files:**
- Modify: `src/main/resources/application-prod.properties:16`

**Interfaces:**
- Consumes: Task 2 (el EPP publica la URL antes de que se resuelva el placeholder).
- Produces: `spring.datasource.url=${SPRING_DATASOURCE_URL:}` (fallback vacío).

- [ ] **Step 1: Cambiar la propiedad**

En `src/main/resources/application-prod.properties`, sustituir la línea 16:

```properties
spring.datasource.url=${SPRING_DATASOURCE_URL:}
```

(La línea 15 tiene el comentario `# IMPORTANTE: los defaults apuntan a TiDB Cloud como fallback seguro` — reemplazarlo por `# URL de la BD (obligatoria en prod; sin ella la app falla al arrancar)`).

- [ ] **Step 2: Verificar con la suite completa**

```powershell
docker run --rm -v "${PWD}:/app" -w /app --network envios_paraguay_cms_backend -e SPRING_DATASOURCE_URL="jdbc:mysql://monteastur-mysql:3306/envios_paraguay_cms_test?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true" -e DB_USERNAME=root -e DB_PASSWORD=root -e SPRING_DATA_REDIS_HOST=monteastur-redis -v "${HOME}\.m2:/root/.m2" maven:3.9-eclipse-temurin-25 mvn clean test
```

Expected: `BUILD SUCCESS`, 264+ tests en verde. Los tests que cargan el contexto `prod` reciben URL vía env var (el comando la inyecta), por lo que no rompen.

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/application-prod.properties
git commit -m "feat(cloud): fail-fast datasource URL in prod profile without TiDB fallback"
```

---

### Task 4: Dockerfile — JAVA_OPTS y HEALTHCHECK dinámico

**Files:**
- Modify: `Dockerfile:52-54`

**Interfaces:**
- Consumes: nada.
- Produces: imagen que expande `JAVA_OPTS` en runtime y hace HEALTHCHECK en `${PORT:-8080}`.

- [ ] **Step 1: Editar ENTRYPOINT y HEALTHCHECK**

En `Dockerfile`, reemplazar las líneas 52-54:

```dockerfile
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD ["sh", "-c", "curl -fsS \"http://localhost:${PORT:-8080}/actuator/health\" || exit 1"]
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar --spring.profiles.active=prod"]
```

(La línea 55 `EXPOSE 8080` permanece igual.)

- [ ] **Step 2: Validar que la imagen compila**

```powershell
docker build -t monteastur-render-check .
```

Expected: build exitoso (las 3 etapas). Opcional: `docker run --rm -e PORT=10000 -e SPRING_DATASOURCE_URL="jdbc:mysql://x" monteastur-render-check sh -c 'echo $PORT'` no es necesario; basta el build.

- [ ] **Step 3: Commit**

```bash
git add Dockerfile
git commit -m "feat(cloud): JVM tuning via JAVA_OPTS and PORT-aware healthcheck in Dockerfile"
```

---

### Task 5: Blueprint render.yaml oficial

**Files:**
- Modify: `render.yaml` (reemplazo completo)

**Interfaces:**
- Consumes: Task 3 (perfil prod sin fallback) y Task 4 (JAVA_OPTS efectivo).
- Produces: blueprint desplegable en Render (branch `main`, `env: docker`, plan free).

- [ ] **Step 1: Sobreescribir render.yaml**

```yaml
# =============================================
# MONTEASTUR ENVIOS — Render Blueprint (Docker)
# =============================================
# Despliegue: Dashboard → New Web Service → Connect GitHub repo
# (o Render Blueprint: push a main con este render.yaml en la raíz).
# Runtime: Docker (Dockerfile multi-stage en la raíz).
# Plan: free — el servicio duerme tras inactividad y los uploads (/tmp)
# NO persisten entre redeploys.
# Credenciales sync:false — rellenar en el dashboard en el primer deploy.
# La URL MySQL puede pegarse como mysql://... (la app añade jdbc: sola).
# =============================================

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
      # ---- Spring Profile ----
      - key: SPRING_PROFILES_ACTIVE
        value: prod

      # ---- MySQL externo (Render no ofrece MySQL gestionado) ----
      - key: SPRING_DATASOURCE_URL
        sync: false
      - key: DB_USERNAME
        sync: false
      - key: DB_PASSWORD
        sync: false

      # ---- Redis externo free (ej. Upstash) ----
      - key: REDIS_HOST
        sync: false
      - key: REDIS_PORT
        value: "6379"

      # ---- Admin ----
      - key: ADMIN_USERNAME
        sync: false
      - key: ADMIN_PASSWORD
        sync: false

      # ---- URLs / notificaciones ----
      - key: APP_TRACKING_BASE_URL
        sync: false
      - key: APP_NOTIFICATION_MAIL_ENABLED
        value: "false"

      # ---- JVM Tuning (free tier: 512 MB) ----
      - key: JAVA_OPTS
        value: "-Xms256m -Xmx384m -XX:+UseG1GC -XX:+UseStringDeduplication"

      # ---- App / Thymeleaf ----
      - key: THYMELEAF_CACHE
        value: "true"
      - key: UPLOAD_DIR
        value: "/tmp/uploads"
      - key: JPA_SHOW_SQL
        value: "false"
```

- [ ] **Step 2: Validar YAML**

```powershell
python -c "import yaml,sys; yaml.safe_load(open('render.yaml', encoding='utf-8')); print('render.yaml OK')"
```

Expected: `render.yaml OK`.

- [ ] **Step 3: Commit**

```bash
git add render.yaml
git commit -m "feat(cloud): official Render free-tier docker blueprint"
```

---

### Task 6: Suite completa, push y handoff

**Files:**
- Modify: `docs/handoff.md`

**Interfaces:**
- Consumes: Tasks 1–5.
- Produces: `main` sincronizada en `origin/main` + handoff actualizado.

- [ ] **Step 1: Suite completa en contenedor**

```powershell
docker run --rm -v "${PWD}:/app" -w /app --network envios_paraguay_cms_backend -e SPRING_DATASOURCE_URL="jdbc:mysql://monteastur-mysql:3306/envios_paraguay_cms_test?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true" -e DB_USERNAME=root -e DB_PASSWORD=root -e SPRING_DATA_REDIS_HOST=monteastur-redis -v "${HOME}\.m2:/root/.m2" maven:3.9-eclipse-temurin-25 mvn clean test
```

Expected: `BUILD SUCCESS`, 264+ tests en verde (267 previos + 4 métodos normalizador + 5 métodos EPP).

- [ ] **Step 2: Push a origin/main**

Verificar que el working tree solo contiene archivos de este sprint (NUNCA usar `git add -A`: arrastraría `start-app.ps1`, que tiene cambios ajenos de una sesión previa):

```powershell
git status
```

Expected: working tree limpio (Tasks 1–5 ya commiteadas). Si queda algún archivo del sprint sin commitear, añadirlo explícitamente por ruta y commitear con el mensaje autorizado por el usuario:

```bash
git commit -am "feat(cloud): add automatic jdbc: prefix converter, JVM tuning and updated render.yaml" -- src/main/java/com/monteastur/envios/config/BootstrapPropertyNormalizer.java src/main/java/com/monteastur/envios/config/BootstrapPropertyEnvironmentPostProcessor.java src/main/resources/application-prod.properties Dockerfile render.yaml src/test/java/com/monteastur/envios/config/BootstrapPropertyNormalizerTest.java src/test/java/com/monteastur/envios/config/BootstrapPropertyEnvironmentPostProcessorTest.java
git push origin main
```

Expected: push de los commits de Tasks 1–5 a `origin/main`.

- [ ] **Step 3: Actualizar docs/handoff.md**

Añadir la entrada del sprint en `docs/handoff.md` (sección de progreso) y actualizar "Estado Git Actual" (HEAD nuevo, 264+ tests, conversor JDBC, blueprint Render):

- Conversor JDBC automático: `SPRING_DATASOURCE_URL`/`DATABASE_URL` → prefijo `jdbc:` + `;`→`&` (case-insensitive) en `BootstrapPropertyNormalizer`/`BootstrapPropertyEnvironmentPostProcessor`.
- Perfil prod fail-fast: `${SPRING_DATASOURCE_URL:}` sin fallback TiDB.
- Dockerfile: `JAVA_OPTS` inyectado y HEALTHCHECK `{PORT:-8080}`.
- `render.yaml`: Web Service docker free, branch main, credenciales sync:false, Redis externo (Upstash), mail desactivado.
- Suite: BUILD SUCCESS con 264+ tests.

- [ ] **Step 4: Commit del handoff y push**

```bash
git add docs/handoff.md
git commit -m "docs(handoff): sprint despliegue Render.com - conversor JDBC y blueprint docker"
git push origin main
```

Expected: `HEAD` == `origin/main`.
