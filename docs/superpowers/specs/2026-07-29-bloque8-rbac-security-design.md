# Bloque 8: Seguridad Avanzada y Control de Acceso (RBAC) — Diseño Técnico

> **Para agentes autónomos:** REQUERIDO SUB-HABILIDAD: Usa subagentes de desarrollo impulsado por habilidades para implementar este bloque tarea por tarea.

**Objetivo:** Integrar un sistema de control de acceso basado en roles (RBAC) usando Spring Security con roles jerárquicos (`ROLE_ADMIN`, `ROLE_OPERADOR`, `ROLE_CLIENTE`), usuarios basados en base de datos, autorización a nivel de método, y auditoría de seguridad completa.

**Arquitectura:**
- Usuarios gestionados en base de datos con tabla `users` + `roles` + `user_roles` + tabla de auditoría `auditoria_accesos`
- Spring Security con `JdbcUserDetailsManager` customizado para cargar usuarios y autoridades
- `@EnableMethodSecurity` para autorización a nivel de método
- Jerarquía de roles: `ROLE_ADMIN` > `ROLE_OPERADOR` > `ROLE_CLIENTE`
- `CustomAccessDeniedHandler` con `@PreAuthorize` para logging centralizado
- Resto de la configuración actual (SecurityConfig) mantenida como punto de entrada de filtro base

**Segmentación:**
1. **Esquema de BD (Migración Flyway V2)**: usuarios, roles, user_roles, auditoría_accesos
2. **Spring Security** (RBACSecurityConfig): manager de usuarios, cadena de filtros con reglas RBAC, controlador de denegación personalizado
3. **Auditoría** (RBACAuditorConfig): servicio registrable de auditoría
4. **Extensiones de controladores** (tareas separadas): agregar @PreAuthorize a endpoints específicos

**Modelos de roles:**
- `ROLE_ADMIN`: absoluto, todas las acciones, acceso a `/admin/**` y `/api/v1/admin/**`
- `ROLE_OPERADOR`: acceso completo a CRUD para reservas, envíos, eventos, presupuestos, informes, usuarios (solo lectura), dashboards
- `ROLE_CLIENTE`: solo sus propios datos, reservas, envíos, perfil, login (protegido)
- Público: sin autenticación, accesos solo de lectura (páginas de inicio, reservas, contacto, rastreo)

**Persistencia:**
- Tabla `users` vinculada con autenticación Cliente (correo electrónico password custodiado)
- Tabla `roles` con inserciones para ADMIN, OPERADOR, CLIENTE
- Tabla `user_roles` puente many-to-many
- Tabla `auditoria_accesos` con user_id, username, acción, recurso, IP, user_agent, éxito/fallo, descripción

**Reglas de autorización:**
1. **Autorización en cadena de filtros:** endpoints protegidos por rol base (`/admin/**`, `/api/v1/admin/**`, `/cliente/**`, `/operador/**`)
2. **Autorización a nivel de método:** `@PreAuthorize("hasRole('ROLE_ADMIN')")` para métodos de admin; `@PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_OPERADOR')")` para operador; `@PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_OPERADOR', 'ROLE_CLIENTE')")` para cliente
3. **Importante:** mantener `@EnableRedisHttpSession` activo, no interferir con CSRF para `/api/**`

**Componentes clave:**
- `RBACSecurityConfig`: <250 líneas, follows existing SecurityConfig patterns, reemplaza InMemoryUserDetailsManager
- `RBACAuditorConfig`: registra en DB cada intento de autorización (éxito/fallo)
- `CustomAccessDeniedHandler`: loggea denegaciones, lanza BadRequestException
- `CustomUserDetailsService` + `RBACUserDetails`: si se necesita entrega personalizada de UserDetails (opcional, usa JdbcUserDetailsManager nativo)

**Ambiente:**
- Spring Boot 3.3.5 (mismo)
- Flyway (mismo)
- MySQL 8 (mismo)
- Spring Security 7
- Spring Boot Starter Security (para auth)

**Dependencias:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-config</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-web</artifactId>
</dependency>
```

**Retrocompatibilidad:**
- Reemplaza admin in-memory con admin basado en DB (correo electrónico = app.admin.username, password del environment .env)
- Mantiene existing Cliente authentication for clientes huéspedes
- No rompe seguridad web-MVC híbrida (Thymeleaf + React SPA)
- Mantiene CSRF solo para API REST (/api/**)

**Validaciones:**
- Aplicación arranca → conección DB OK, Flyway corre V2 → datos insertedos
- `mvn test` pasa todos los 47/47 (tests existentes)
- Endpoints RBAC actuarían con roles cargados mediante `SecurityContextHolder`
- Auditoría registra en BD sin depender de Redis
- Performance: cargas masivas → registros de auditoría con inserción masiva diferenciada planificada

**Plan de implementación:**
- Tarea 1: Agregar migración Flyway V2 (`src/main/resources/db/migration/V2__add_rbac_tables.sql`)
- Tarea 2: Crear `RBACSecurityConfig.java` (Spring Security, UserDetailsService, cadena de filtros)
- Tarea 3: Crear `RBACAuditorConfig.java` (componente registrable, logger)
- Tarea 4: Agregar @PreAuthorize a controladores (público, cliente, operador, admin) y crear sub-agente para ejecución
- Tarea 5: Verificación final con test runner, pedidos que no existían deben ser creados

**Dependencias visuales:** no necesita companion visual.

**Habilidades requeridas:** <superpowers:subagent-driven-development> recomendado, <superpowers:executing-plans> también permitido.

**Referencias:**
- Spring Security 7 RBAC docs
- Flyway V2 migration best practices
- existing SecurityConfig.java:712 (SecurityFilterChain definition)
- existing Cliente.java: authentication model adapter