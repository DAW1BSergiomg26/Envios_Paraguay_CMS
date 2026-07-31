# Spec: Sprint de Optimización y Resiliencia

- Fecha: 2026-07-31
- Estado: Aprobado (Enfoque A consensuado en brainstorming)
- Proyecto: Envios_Paraguay_CMS (Spring Boot 3.3.5, Java 17, MySQL 8, Redis 7)

## Contexto

Las pruebas de carga k6 del Bloque 9 (2026-07-31) revelaron dos hallazgos operativos reales:

1. **`Session was invalidated` + `RedisSystemException: Connection reset`** en `/login` bajo picos de concurrencia (200 VUs). La causa raíz apunta a la gestión de conexiones Lettuce: `spring-boot-starter-data-redis` sin `commons-pool2` usa una **única conexión compartida** para todas las operaciones síncronas (incluidas las de Spring Session), y las propiedades de pool que se intenten configurar se **ignoran silenciosamente**.
2. El `REPORT.md` de k6 afirmaba que `GET /api/v1/tracking/{codigo}` consultaba `EnvioTrackingRepository` directamente. **Esa afirmación es incorrecta**: el commit `4407c07` (2026-07-31 13:56, anterior a los runs k6 de las 20:30) ya enrutó el endpoint a `EnvioTrackingService.buscarPorCodigo` con `@Cacheable("envios.tracking")`. Los tests de carga midieron por tanto cache-hits. El reporte está desactualizado y debe corregirse.

## Objetivos

1. **Tracking público en caché** — verificación y blindaje: el endpoint ya usa el servicio cacheado; se añade un test de integración que valida el ciclo de vida real de la caché (`@Cacheable`, `@CacheEvict`, TTL) contra Redis real, y se corrige el `REPORT.md`.
2. **Mitigar invalidación de sesiones bajo pico** — añadir `org.apache.commons:commons-pool2` al pom (imprescindible para que los pool properties de Lettuce surtan efecto), configurar el pool Lettuce (max-active=30, max-idle=15, min-idle=5, max-wait=2000ms) y `spring.data.redis.timeout=3000ms`. Mantener `save-mode=on-set-attribute` y `flush-mode=on-save` explícitos (son los defaults de Spring Session; documentan intención) y el namespace `monteastur:session` intacto (cambiarlo invalidaría sesiones existentes sin beneficio).
3. **Ajuste fino de pools** — HikariCP base (maximum-pool-size=25, minimum-idle=5, connection-timeout=20000, idle-timeout=300000, max-lifetime=1200000) y reconciliar `application-prod.properties` (pool-size=25, connection-timeout=20000) conservando sus directivas de hardening (`validation-timeout`, `leak-detection`, `connection-test-query`, `auto-commit`, `pool-name=EnviosProdPool`).

## No-goals

- No se modifica `TrackingApiController` ni `EnvioTrackingService` (el objetivo 1 ya está implementado).
- No se cambia el namespace de sesión.
- No se añaden flags JVM al Dockerfile.
- No se extiende la caché a otros endpoints (formulario legacy `POST /tracking`, detalle admin, `envios.dashboard`).
- No se re-ejecuta la suite de carga k6 completa (solo `mvn clean test` obligatorio; smoke k6 opcional).

## Decisiones de diseño

| Decisión | Valor | Justificación |
|---|---|---|
| Dependencia pooling | `org.apache.commons:commons-pool2` (versión gestionada por parent) | Sin ella, `LettuceConnectionConfiguration` ignora los pool properties y usa conexión única compartida |
| Pool Lettuce | max-active=30, max-idle=15, min-idle=5, max-wait=2000ms | Conexiones paralelas para ráfagas de creación de sesiones; fail-fast en borrow |
| Redis timeout | 3000ms (default Spring Data: 60s) | Fallar rápido ante `Connection reset` en vez de colgar requests |
| Session | save-mode=on-set-attribute, flush-mode=on-save, namespace `monteastur:session` | Explícito/no-op salvo namespace, que se mantiene |
| Hikari base | max=25, min=5, ct=20000, idle=300000, max-lifetime=1200000 | Headroom para 200 VUs; fail-fast ante saturación de pool |
| Hikari prod | max=25, min=5, ct=20000; conserva validation-timeout=5000, leak-detection=30000, connection-test-query=SELECT 1, auto-commit, pool-name=EnviosProdPool | El perfil que validan las pruebas k6 (docker-compose activa prod) |
| Test de caché | `@SpringBootTest @ActiveProfiles("test")` contra MySQL+Redis reales | Mismo patrón que `EnvioNotificacionIntegrationTest`; CI provee los servicios |

## Verificación del pool Lettuce activo

El riesgo de "propiedades silenciosamente ignoradas" se blinda con un assert en el test de integración: el bean `GenericObjectPoolConfig` debe existir en el contexto de Spring con `getMaxTotal() == 30`. Sin `commons-pool2` este bean no se registra y el test falla.

## Criterios de aceptación

1. `mvn clean test` en verde con la suite completa (incluido el nuevo test de integración).
2. El test de integración valida: (a) populate de caché (2ª consulta servida desde Redis tras borrar la fila de DB), (b) evict en `guardar()` de `envios.tracking`, (c) TTL ≤ 300s de la key `envios.tracking::<codigo>`, (d) bean `GenericObjectPoolConfig` con `maxTotal=30`.
3. `application.properties` y `application-prod.properties` reflejan los valores de la tabla de decisiones sin contradecirse.
4. `REPORT.md` corrige la afirmación obsoleta sobre tracking y ajusta las recomendaciones.
5. `pom.xml` contiene `commons-pool2`.
