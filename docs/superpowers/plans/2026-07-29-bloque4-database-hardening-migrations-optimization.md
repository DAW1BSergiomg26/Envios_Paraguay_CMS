# Bloque 4: Implementation Plan

**Date:** 2026-07-29
**Spec:** `docs/superpowers/specs/2026-07-29-bloque4-database-hardening-migrations-optimization.md`

---

## Task 1: Flyway Migration Setup + V1 Initial Schema

**Goal:** Replace `ddl-auto=update`/`validate` with Flyway-managed migrations.

### 1.1 Add Flyway dependencies to `pom.xml`

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
</dependency>
```

### 1.2 Create `V1__initial_schema.sql`

Location: `src/main/resources/db/migration/V1__initial_schema.sql`

Contents: CREATE TABLE IF NOT EXISTS for all 8 tables, plus CREATE INDEX IF NOT EXISTS for strategic columns.

Tables in order (respecting FK dependencies):
1. `clientes` — no FKs
2. `envios_tracking` — FK → `clientes`
3. `eventos_tracking` — FK → `envios_tracking`
4. `evidencias_envio` — FK → `envios_tracking`
5. `reservas` — no FKs
6. `mensajes_contacto` — no FKs
7. `imagenes` — no FKs
8. `textos_legales` — no FKs

### 1.3 Update `application.properties`

```properties
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true
```

### 1.4 Update `application-prod.properties`

```properties
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
# No baseline-on-migrate in prod — fresh DB should have clean schema
```

### 1.5 Add `@Table(indexes = ...)` to all entity classes

| Entity | Indexes |
|--------|---------|
| `EnvioTracking` | `estado`, `ultimaActualizacion`, `cliente` |
| `EventoTracking` | `envioTracking` (FK) |
| `EvidenciaEnvio` | `envioTracking` (FK), composite `(envioTracking, visibleCliente)` |
| `Reserva` | `estado`, composite `(fechaEntrada, fechaSalida)` |
| `MensajeContacto` | `fechaEnvio` |
| `Imagen` | `orden` |
| `Cliente` | (already has unique email) |
| `TextoLegal` | (already has unique slug) |

---

## Task 2: N+1 Query Safeguards

**Goal:** Protect against N+1 queries without over-engineering.

### 2.1 Set default_batch_fetch_size

Add to `application.properties`:

```properties
spring.jpa.properties.hibernate.default_batch_fetch_size=20
```

Add to `application-prod.properties`:

```properties
spring.jpa.properties.hibernate.default_batch_fetch_size=20
```

### 2.2 Verify existing @EntityGraph

`EnvioTrackingRepository` already has:
- `findWithClienteById` with `@EntityGraph(attributePaths = "cliente")` ✓
- `findWithClienteByCodigoUnico` with `@EntityGraph(attributePaths = "cliente")` ✓

No additional changes needed.

---

## Task 3: Verification

**Goal:** Confirm Flyway works, tests pass, no regressions.

### 3.1 Build verification
```bash
mvn clean compile -DskipTests
```

### 3.2 Run full test suite
```bash
mvn test
```

### 3.3 Verify Flyway baseline (manual)
- Start application with dev profile
- Check logs for Flyway baseline + migration messages
- Verify database tables still work with existing data

---

## Execution Order

```
Task 1 (Flyway + V1 + indexes)
  ├── 1.1 pom.xml deps
  ├── 1.2 V1__initial_schema.sql
  ├── 1.3 application.properties
  ├── 1.4 application-prod.properties
  └── 1.5 Entity @Table(indexes=...) annotations

Task 2 (N+1 safeguards)
  ├── 2.1 default_batch_fetch_size config
  └── 2.2 Verify @EntityGraph

Task 3 (Verification)
  ├── 3.1 mvn compile
  ├── 3.2 mvn test
  └── 3.3 Manual verification
```

---

## Rollback Plan

If Flyway causes issues:
1. Revert `application.properties` to `ddl-auto=update`
2. Comment out Flyway properties
3. Remove Flyway deps from pom.xml
4. Revert entity annotations
