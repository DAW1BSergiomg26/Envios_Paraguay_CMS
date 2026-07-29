# Bloque 7: Redis Caching & Distributed Sessions

**Date:** 2026-07-29
**Project:** Monteastur Envios CMS
**Branch:** main (new branch: feature/redis-caching-sessions)
**Status:** Design

## Objective

Integrate Redis as a caching layer for frequent database queries and as a distributed session store, improving response times and enabling horizontal scalability.

## Motivation

- Every service call hits MySQL directly — no caching at any level
- Dashboard and public tracking queries repeat identical reads under load
- HTTP sessions are in-memory — lost on restart, broken with multiple instances
- `InMemoryUserDetailsManager` prevents horizontal scaling (admin credentials lost per instance)
- Redis provides sub-millisecond reads for hot data patterns

## Current State

### Dependencies
No Redis or Spring Session dependencies in `pom.xml`.

### Database Query Patterns

| Query | Frequency | Read/Write | Cache Candidate |
|-------|-----------|------------|-----------------|
| Tracking by code (`EnvioTrackingRepository.findByCodigoUnico`) | High (public) | Read | ✅ `envios.tracking` |
| List all tracking (`findAllByOrderByUltimaActualizacionDesc`) | High (admin) | Read | ✅ `envios.dashboard` |
| Reservas list all (`findAllByOrderByCreatedAtDesc`) | Medium (admin) | Read | ✅ `envios.reservas` |
| Reserva by ID (`findById`) | Medium (admin) | Read | ✅ `envios.reservas` |
| Client by ID (`findById`) | Low-Medium | Read | ✅ `envios.clientes` |
| Client list all (`findAll`) | Low (admin) | Read | ✅ `envios.clientes` |
| Date overlap check (`existsOverlap`) | Medium (public) | Read | ✅ `envios.disponibilidad` |

### Session Management
- `InMemoryUserDetailsManager` — single admin user at startup
- HTTP sessions in-memory (default Spring Boot)
- No session persistence across restarts

### Infrastructure
No Redis service in `docker-compose.yml`.

## Requirements

### Dependencies
- `spring-boot-starter-data-redis` — Redis client (Lettuce)
- `spring-session-data-redis` — Spring Session backed by Redis

### Docker Compose
- Redis 7 Alpine service with healthcheck
- `SPRING_DATA_REDIS_HOST` env var passed to `app` service

### Cache Regions

| Cache Name | TTL | Data | Eviction Trigger |
|------------|-----|------|------------------|
| `envios.tracking` | 5 min | Envio by codigo + all list | State change, delete |
| `envios.dashboard` | 1 min | Admin dashboard listings | Time-based |
| `envios.reservas` | 10 min | Reserva byId + all list | Create, delete, update |
| `envios.clientes` | 10 min | Cliente byId + all list | Save/update |
| `envios.disponibilidad` | 2 min | Date overlap result | New reserva created |

### Session Management
- `@EnableRedisHttpSession` — HTTP sessions stored in Redis
- Session timeout configurable via `server.servlet.session.timeout`
- `InMemoryUserDetailsManager` remains (single admin user)

## Architecture Decisions

### Approach A: Spring Cache Abstraction (selected)
`@Cacheable` / `@CacheEvict` annotations on service methods. Simple, declarative, and tested pattern. No manual `RedisTemplate` usage needed for caching.

### Redis Serialization
- Cache keys: String-based (cache name + method arguments)
- Cache values: JSON via `GenericJackson2JsonRedisSerializer`
- Session data: JdkSerializationRedisFactory (Spring Session default)

### Cache-Null Strategy
`cache-null-values=false` — do not cache null results (prevents caching empty lookups as valid data)

## Implementation Details

### Configuration Classes

**RedisConfig.java:**
- `RedisCacheManagerBuilderCustomizer` — configure per-cache TTLs
- `RedisTemplate<String, Object>` — with `GenericJackson2JsonRedisSerializer`
- Connection factory pointing to `spring.data.redis.host`

**SessionConfig.java:**
- `@EnableRedisHttpSession` annotation
- `@Configuration` class
- `RedisIndexedSessionRepository` auto-configured by Spring Session

### Service Annotations

**EnvioTrackingService:**
```java
@Cacheable("envios.tracking")
public Optional<EnvioTracking> buscarPorCodigo(String codigo)

@Cacheable("envios.dashboard")
public List<EnvioTracking> listarTodos()

@CacheEvict(value = "envios.tracking", allEntries = true)
public EnvioTracking guardar(EnvioTracking envio)

@CacheEvict(value = "envios.tracking", allEntries = true)
public void eliminar(Long id)
```

**ReservaService:**
```java
@Cacheable("envios.reservas")
public Optional<Reserva> buscarPorId(Long id)

@Cacheable("envios.reservas")
public List<Reserva> listarTodas()

@CacheEvict(value = {"envios.reservas", "envios.disponibilidad"}, allEntries = true)
public Reserva crear(Reserva reserva)

@CacheEvict(value = {"envios.reservas", "envios.disponibilidad"}, allEntries = true)
public void eliminar(Long id)

@Cacheable("envios.disponibilidad")
public boolean verificarDisponibilidad(LocalDate entrada, LocalDate salida)
```

**ClienteService:**
```java
@Cacheable("envios.clientes")
public Optional<Cliente> buscarPorId(Long id)

@Cacheable("envios.clientes")
public List<Cliente> listarTodos()

@CacheEvict(value = "envios.clientes", allEntries = true)
public Cliente guardar(Cliente cliente)
```

### Docker Compose Addition
```yaml
redis:
  image: redis:7-alpine
  container_name: monteastur-redis
  ports:
    - "127.0.0.1:${REDIS_PORT:-6379}:6379"
  volumes:
    - redis_data:/data
  healthcheck:
    test: ["CMD", "redis-cli", "ping"]
    interval: 5s
    timeout: 3s
    retries: 5
  restart: unless-stopped
  networks:
    - backend
  mem_limit: 64m

volumes:
  redis_data:
```

### Properties

**application.properties:**
```properties
spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=${REDIS_PORT:6379}
spring.session.redis.namespace=monteastur:session
server.servlet.session.timeout=30m
```

## Out of Scope

- Redis Cluster / Sentinel — single Redis instance
- Redis as message broker (Pub/Sub, Streams) — future improvement
- Custom `RedisCacheManager` with advanced eviction policies — Spring defaults sufficient
- Database-backed UserDetails — `InMemoryUserDetailsManager` stays

## Testing Strategy

- Existing 47/47 tests must continue to pass
- Tests don't require Redis: if Redis is unavailable, `@Cacheable` methods degrade gracefully (hit database)
- For integration tests that verify caching behavior, use `@TestPropertySource` with
  embedded Redis or mock `RedisTemplate`
- `@DirtiesContext` to clear caches between tests that modify state

## Success Criteria

- [ ] All service methods annotated with `@Cacheable` / `@CacheEvict` as specified
- [ ] `RedisCacheManager` configured with per-cache TTLs
- [ ] `@EnableRedisHttpSession` activates session persistence
- [ ] Docker Compose starts Redis container with healthcheck
- [ ] App connects to Redis and caches queries successfully
- [ ] Sessions survive app restart (JSESSIONID persists)
- [ ] All 47/47 existing tests pass
- [ ] Build succeeds without Redis (graceful fallback to database)
