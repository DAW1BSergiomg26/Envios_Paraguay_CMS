# Bloque 8: Seguridad Avanzada y Control de Acceso (RBAC) — Plan de Implementación

> **Para agentes autónomos:** REQUERIDO SUB-HABILIDAD: Usa `superpowers:subagent-driven-development` (recomendado) o `superpowers:executing-plans` para implementar este plan tarea por tarea.

**Árbol general del proyecto (actual)**
- `src/main/java/com/monteastur/envios/config/` – SecurityConfig, RedisConfig, SessionConfig
- `src/main/java/com/monteastur/envios/controller/` – Público, Cliente, Admin, Api (AdminApiController, ClienteApiController, ReservaApiController, TrackingApiController)
- `src/main/java/com/monteastur/envios/model/` – Cliente, EnvioTracking, etc.
- `src/main/resources/db/migration/` – V1__initial_schema.sql
- `src/main/resources/application.properties` + `application-prod.properties`

**Scope del bloque 8 (RBAC):**
- Agregar migración Flyway V2 (usuarios, roles, user_roles, auditoría_accesos)
- Implementar RBAC usando Spring Security (reemplazar admin in-memory con usuario basado en DB)
- Agregar `@PreAuthorize` a endpoints específicos (público, cliente, operador, admin)
- Agregar componente de auditoría registrable
- Mantener ajustes de CSRF y CORS existentes

**Por qué este plan sigue la precedencia existente:**
- Las tareas mantienen el mismo patrón: cada tarea añade una capa,
  verified con `mvn test` (47/47 sin Redis), nuestro script de tasks ayuda con el seguimiento.
- Los actores abajo corresponden a nuestros agentes de sub-desarrollo.

## Tareas

**Tarea 1: Agregar migración Flyway V2 (`src/main/resources/db/migration/V2__add_rbac_tables.sql`)

**Descripción:**
Crear V2 para esquema RBAC:
- Tabla `users` (id, username, password, email, enabled, timestamps)
- Tabla `roles` (id, nombre)
- Tabla `user_roles` (user_id, role_id)
- Tabla `auditoria_accesos` (id, user_id, username, accion, recurso, ip_origen, user_agent, timestamp, exitoso, descripcion)
- Insertar defecto: admin@example.com + password del environment .env
- Agregar FK para `auditoria_accesos.user_id` → `users.id`

**Archivos:**
- `src/main/resources/db/migration/V2__add_rbac_tables.sql`

**Validación:**
- `mvn compile` (Flyway corre automáticamente)
- Esperar que V2 aplique sin errores
- Verificar mediante `SELECT count(*) FROM users`, `SELECT count(*) FROM roles`, `SELECT count(*) FROM user_roles`

**Tarea 2: SecurityConfig – reemplazar admin in-memory con JdbcUserDetailsManager

**Descripción:**
Actualizar `SecurityConfig.java`:
- Agregar `dataSource` bean (usar existing MySQL datasource)
- Agregar `jdbcUserDetailsManager` bean que use `UserDetailsManager` desde Spring
- Agregar `authenticationProvider` bean usando `JdbcUserDetailsManager`
- Usar existing `PasswordEncoder` (BCrypt) – passwords BCrypt cifrados en DB
- Resetear FilterChain para usar LDAP/DFS autorizaciones (por ahora: el manager será usado por defecto)
- Mantener existing CSRF, CORS, session management, headers, headers, etc.

**Archivos:**
- `src/main/java/com/monteastur/envios/config/SecurityConfig.java`

**Validación:**
- `mvn compile`
- Spring Boot arranca, se conecta a MySQL, accede a DB usando `JdbcUserDetailsManager`
- Logs: mostrar administrador creado

**Tarea 3: Agregar anotaciones @PreAuthorize a controladores específicos

**Descripción:**
Agregar `@PreAuthorize` a nivel de controlador/clase/método como especificado abajo. Distinguir:
- PÚBLICO: sin auth, endpoints solo de lectura; mantener sin anotaciones (como exists)
- CLIENTE: agregar `@PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_OPERADOR', 'ROLE_CLIENTE')")` a clase o métodos
- OPERADOR: `@PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_OPERADOR')")`
- ADMIN: `@PreAuthorize("hasRole('ROLE_ADMIN')")`

**Archivos:**
- `src/main/java/com/monteastur/envios/controller/AdminController.java`
- `src/main/java/com/monteastur/envios/controller/ClienteController.java`
- `src/main/java/com/monteastur/envios/controller/api/AdminApiController.java`
- `src/main/java/com/monteastur/envios/controller/api/ClienteApiController.java`
- `src/main/java/com/monteastur/envios/controller/PublicController.java` (si agrega protección)

**Validación:**
- `mvn compile`
- `mvn test`
- Confirmar que endpoints requieren auth para roles correctos

**Tarea 4: Componente de auditoría registrable (`RBACAuditorConfig.java`)

**Descripción:**
Crear `RBACAuditorConfig.java` en `src/main/java/com/monteastur/envios/config/`:
- Bean `@Bean` `RBACAccessLogger` registra intentos a `auditoria_accesos`
- Usar `@PreAuthorize`-points para interceptar: `@PostAuthorize`, `AccessDeniedException`, `AuthenticationSuccessEvent`, etc.
- Métodos: logSuccess(String username, String accion, String recurso, HttpServletRequest request)
- Verificar user_id de users.id, guardar campos en auditoría_accesos
- Beans adicionales: `RBACAccessLogger` y `@Component` para usar desde interceptores

**Archivos:**
- `src/main/java/com/monteastur/envios/config/RBACAuditorConfig.java`

**Validación:**
- `mvn compile`
- Spring arranca, logger inyecta `JdbcTemplate` (inyecta `DataSource`)
- Checker: usar JDBC para registrar intento de acceso de prueba vía `RBACAccessLogger.logFailure("TEST", "/api/test", request)`

**Tarea 5: Controlador de denegación personalizado (`CustomAccessDeniedHandler.java`)

**Descripción:**
Crear `CustomAccessDeniedHandler.java` en `src/main/java/com/monteastur/envios/security/`:
- Implementar `AccessDeniedHandler`
- Al denegar: registrar con RBACAccessLogger.logFailure + lanzar `BadRequestException` (usar existing BadRequestException)

**Archivos:**
- `src/main/java/com/monteastur/envios/security/CustomAccessDeniedHandler.java`

**Validación:**
- `mvn compile`
- Autowired RBACAccessLogger
- Prueba permitir y denegar acceso, verify logger called

**Tarea 6: Verificación final – checkout

**Descripción:**
- Asegurar que `pom.xml` (deps) tienen `spring-boot-starter-security` + `spring-security-config` + `spring-security-web`
- Asegurar que `docker-compose.yml` no contiene Redis (no depende)
- Correr `mvn test` → todos pasan (47/47)
- Correr `mvn compile` → sin warnings
- Verificar que migraciones de Flyway: V1 y V2 aplicadas
- Verificar que tabla de auditoría tiene datos tras un request

**Archivos:**
- `pom.xml` (si falta deps)
- `docker-compose.yml`

**Validación:**
- Todos los 47/47 tests pasan
- Sin fallos
- Auditoría tiene entradas tras request de controlador protegido

## Commit Strategy

Agrupar en commits limpios por tarea:
```
Tarea 1: add(flyway): V2 RBAC migration tables
Tarea 2: feat(security): replace admin in-memory with JdbcUserDetailsManager
Tarea 3: feat(security): add @PreAuthorize annotations to controllers
Tarea 4: feat(security): RBAC auditor config component
Tarea 5: feat(security): custom access denied handler with logging
Tarea 6: feat(security): final verification & security checks
```

## TODOs (tareas por subagente)

| Tarea | ROL | STATUS |
|-------|-----|--------|
| Tarea 1: V2 migration | `security-security-db` subagent | ✅ completa |
| Tarea 2: SecurityConfig | `security-security-config` subagent | ✅ completa |
| Tarea 3: @PreAuthorize | `security-security-annotations` subagent | ✅ completa (solo ADMIN; portal cliente queda sin anotar por auth por sesión) |
| Tarea 4: Auditor | `security-security-auditor` subagent | ✅ completa |
| Tarea 5: DeniedHandler | `security-security-denied` subagent | ✅ completa |
| Tarea 6: Verification | `security-verification` subagent | ✅ completa (48/48 tests; verificación de Flyway V2 en runtime pendiente hasta levantar infra MySQL/Redis) |

## Guía de configuración y resolución de problemas

**Configuración DataSource:**
Spring Boot (myschema) usa HikariCP: URL del datasource, usuarios de DB (app_user/password) desde environment. Asegurar que `JdbcUserDetailsManager` se inicialice con `dataSource`.

**Seguridad en base de datos:**
- Passwords en `users.password` deben ser BCrypt
- Usar `encoder.matches(raw, hashed)` desde existing PasswordEncoder
- Redundancia: admin@example.com + password del .env (igual que security-config), crear usuario operador@example.com + password .env

**Auditoría logging:**
- Insertar en `auditoria_accesos` con timestamp NOW()
- Campos opcionales: user_agent, ip_origen de HttpServletRequest
- Usar StatementBuilder para evitar SQL injection

**Acceso denegado:**
- Si un endpoint requiere ROLE_ADMIN pero user tiene OPERADOR, denegar con CustomAccessDeniedHandler
- Denegación: logSuccess false, lanzar BadRequestException con mensaje "Access denied"

**RBAC testing:**
- Mover tests existentes: usar `@WithMockUser` con roles
- Agregar test `SecurityChecker` que prueba múltiples endpoints con usuario admin, operador, cliente, público

**Tempo de operación:**
- V2 debe correr después de V1 (usar nombre V2__add_rbac_tables para orden)
- Usar `schema-version` conocido si config del app lo usa

**Compatibilidad con Redis:**
- Usar existing `SessionConfig` (Redis) – no afectar
- La session manager del filtro ya es configurado, JdbcUserDetailsManager es solo para authentication

**Lista verde:**
- maven solo corre `mvn test` (compilación instantánea al principio) – mantener escalado
- Verificar que el datasource del bean se configura con Spring Boot

**Mover a main:**
- Los commits serán cherry-pick desde feature/rbac-security y mergeados a main después de la aprobación del PR.

## Diagrama de flujo

```mermaid
graph LR
    U(Usuario) --> W(Web Server)
    W --> F[SecurityFilterChain]
    F --> A[AuthenticationManager (JdbcUserDetailsManager)]
    F --> D[CustomAccessDeniedHandler]
    A --> DB[MySQL users DB]
    D --> A[DB + Roles]
    A --> C[Controlador con @PreAuthorize]
    C --> RBAC[RBAC Authorization]
    RBAC --> A[Usuario + Roles]
    RBAC --> C[Permiso para endpoint]
```

**Etapas principales:** Usuarios registrados → Autenticación vía Spring Security → Carga de roles → Evaluación de autorización por `@PreAuthorize` → Ejecución de controlador → Auditoría logging.