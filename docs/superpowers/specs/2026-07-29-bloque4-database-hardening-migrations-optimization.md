# Bloque 4: Database Hardening, Migrations & Performance Optimization

**Date:** 2026-07-29
**Project:** Monteastur Envios CMS
**Branch:** feature/seguimiento-premium
**Status:** Draft

## Objective

Replace Hibernate `ddl-auto` schema management with Flyway migrations, add strategic database indexes, and safeguard against N+1 query patterns.

## Motivation

- **`ddl-auto=update`** is unsafe for production — schema changes become implicit, unreviewable, and cannot be rolled back.
- **No explicit indexes** exist beyond Hibernate-generated ones (PKs, unique constraints). Frequently queried columns (`estado`, `fecha_entrada`, `codigo_unico`, `envio_id`, etc.) lack strategic indexes.
- **No N+1 protection** — while no classic N+1 loop exists today, the codebase has multiple places where lazy associations (`cliente`, `eventos`, `evidencias`) are accessed after the initial query. A `default_batch_fetch_size` provides a safety net.

## Current State

### Entities (8 tables)

| Entity | Table | FK | Current Indexes |
|--------|-------|----|----------------|
| `EnvioTracking` | `envios_tracking` | `cliente_id → clientes` | PK, unique(codigo_unico) |
| `EventoTracking` | `eventos_tracking` | `envio_id → envios_tracking` | PK only |
| `EvidenciaEnvio` | `evidencias_envio` | `envio_id → envios_tracking` | PK only |
| `Reserva` | `reservas` | — | PK only |
| `Cliente` | `clientes` | — | PK, unique(email) |
| `MensajeContacto` | `mensajes_contacto` | — | PK only |
| `Imagen` | `imagenes` | — | PK only |
| `TextoLegal` | `textos_legales` | — | PK, unique(slug) |

### N+1 Analysis

| Location | Pattern | Risk |
|----------|---------|------|
| `AdminApiController.toTrackingDto()` | 2 service calls (eventos + evidencias) per envio | Low — only called for single-envio detail views |
| `ClienteApiController.detalleEnvio()` | 2 service calls (eventos + evidencias) per envio | Low — single-envio endpoint |
| `ClienteApiController.descargarEvidencia()` | `evidencia → envioTracking → cliente` lazy chain | Low — single-item endpoint |
| `AdminApiController.listarEnvios()` page.map() | No lazy access in current DTO | None today, but fragile |
| `ClienteApiController.listarEnvios()` stream.map() | No lazy access in current DTO | None today, but fragile |

No classic N+1 loop exists today, but the codebase is one edit away from introducing one.

### Database Config

- `application.properties`: `spring.jpa.hibernate.ddl-auto=${DB_DDL_AUTO:update}`
- `application-prod.properties`: `spring.jpa.hibernate.ddl-auto=${DB_DDL_AUTO:validate}`
- Dialect: `MySQLDialect`
- Currently MySQL 8 (dev) / TiDB Cloud (prod)

## Proposed Changes

### 1. Flyway Migration Setup

**Dependencies:**
- `flyway-core` — core engine (included in Spring Boot BOM)
- `flyway-mysql` — MySQL dialect support

**Migration file:** `V1__initial_schema.sql`
- `CREATE TABLE IF NOT EXISTS` for all 8 entities
- `CREATE INDEX` for strategic columns
- MySQL-specific DDL (backtick quoting, ENGINE=InnoDB, CHARSET=utf8mb4)

**Configuration changes:**
- `spring.jpa.hibernate.ddl-auto=validate` in all profiles (Flyway takes over schema management)
- `spring.flyway.enabled=true`
- `spring.flyway.locations=classpath:db/migration`
- `spring.flyway.baseline-on-migrate=true` (for existing dev databases)

### 2. Strategic Database Indexes

| Table | Index Columns | Rationale |
|-------|--------------|-----------|
| `envios_tracking` | `(estado)` | Filter by status in admin list |
| `envios_tracking` | `(ultima_actualizacion)` | Sort by last update |
| `envios_tracking` | `(cliente_id)` | FK + client lookup |
| `envios_tracking` | `(codigo_unico)` | Already unique, but explicit index |
| `eventos_tracking` | `(envio_id)` | FK + list by envio |
| `evidencias_envio` | `(envio_id)` | FK + list by envio |
| `evidencias_envio` | `(envio_id, visible_cliente)` | Composite: client-visible evidencias |
| `reservas` | `(estado)` | Filter by status |
| `reservas` | `(fecha_entrada, fecha_salida)` | Range overlap queries |
| `mensajes_contacto` | `(fecha_envio)` | Sort by date |

These will be defined both in:
- Flyway `V1__initial_schema.sql` (CREATE INDEX)
- JPA `@Table(indexes = @Index(...))` annotations (documentation + test schema generation)

### 3. N+1 Query Safeguards

- Set `spring.jpa.properties.hibernate.default_batch_fetch_size=20` globally
- This makes lazy-loading of `cliente`, `eventos`, `evidencias`, etc. batch-fetch 20 at a time instead of N+1 individual queries
- Add `@EntityGraph` to any existing repo methods that should eagerly load associations (already done for `findWithCliente*`)

### 4. Entity Index Annotations

Add `@Table(indexes = ...)` to all entity classes to:
- Document intended indexes alongside the entity definition
- Enable Hibernate's schema validation (`ddl-auto=validate`) to verify indexes exist
- Serve as source of truth for what indexes should exist

## Non-Goals

- No schema redesign or table alterations
- No query optimization beyond N+1 safeguards (e.g., no custom JPQL rewrites)
- No migration of existing data (Flyway baseline preserves current data)
- No read-replica or connection pooling changes (already configured via HikariCP)

## Out of Scope (Future Bloques)

- Caching layer (Redis)
- Query result streaming for large exports
- Database sharding or read replicas
- Connection pool tuning beyond current HikariCP config
