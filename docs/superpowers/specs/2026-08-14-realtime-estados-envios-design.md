# Especificación de diseño — Estados de envío en tiempo real (WebSocket)

- **Fecha:** 2026-08-14
- **Estado:** Aprobado para implementación
- **Alcance:** Backend (listener de broadcast) + Frontend React (hook `useRealTimeEnvios` + integración en `AdminDashboard`)

## 1. Contexto y problema

El módulo WebSocket del backend ya está desplegado:

- `WebSocketConfig`: endpoint `/ws` con SockJS, `.setAllowedOriginPatterns("*")`, broker simple `/topic` + `/queue`, prefijo de aplicación `/app`.
- `EnvioWebSocketController`: `@MessageMapping("/actualizar-estado")` + `@SendTo("/topic/envios")` — es un *echo controller* que retransmite lo que un cliente publica.

**Hallazgo crítico:** nada en el backend hace broadcast cuando un envío cambia de estado a través de la API REST (`putAdminEnvioEstado`). Existe el evento de dominio `EstadoEnvioActualizadoEvent` (con `envioId`, `codigoRastreo`, `estadoAnterior`, `estadoNuevo`, `timestamp`) y listeners `@TransactionalEventListener(AFTER_COMMIT)` para webhooks/notificaciones, pero **no hay un listener que publique al topic WebSocket**.

Sin ello, el frontend se conectaría a un topic donde nunca llega nada.

### Decisión de arquitectura (aprobada por el usuario)

Añadir un listener en el backend que, sobre `EstadoEnvioActualizadoEvent`, publique `EnvioEstadoWsMessage` a `/topic/envios` con `SimpMessagingTemplate`. El frontend solo escucha. Esto cubre cualquier origen de cambio: REST, importación masiva, offline queue, etc.

## 2. Contrato de datos

`EnvioEstadoWsMessage` (JSON serializado por Jackson):

```json
{
  "envioId": 42,
  "tracking": "ABC-2026-00123",
  "estado": "EN_REPARTO",
  "timestamp": "2026-08-14T10:30:00Z"
}
```

- `tracking` (WS) == `codigoUnico` (fila del dashboard).
- `estado` usa los valores canónicos del dominio: `RECIBIDO`, `EN_ADUANA_ORIGEN`, `EN_TRANSITO`, `EN_ADUANA_DESTINO`, `EN_REPARTO`, `ENTREGADO`.

## 3. Backend — `WebSocketEventListener` (nuevo)

**Ubicación:** `src/main/java/com/monteastur/envios/listener/WebSocketEventListener.java`

Patrón idéntico a `WebhookEventListener`:

```java
@Component
public class WebSocketEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketEventListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void manejar(EstadoEnvioActualizadoEvent event) {
        try {
            EnvioEstadoWsMessage mensaje = new EnvioEstadoWsMessage(
                event.envioId(),
                event.codigoRastreo(),
                event.estadoNuevo(),
                event.timestamp().toInstant(ZoneOffset.UTC));
            messagingTemplate.convertAndSend("/topic/envios", mensaje);
        } catch (Exception e) {
            log.error("Fallo al difundir actualización de estado del envío {}", event.codigoRastreo(), e);
        }
    }
}
```

- **Inyección por constructor** (`private final`), cero `@Autowired` en campos (regla global).
- **`event.timestamp()` es `LocalDateTime`** (sin zona): se convierte a `Instant` con `toInstant(ZoneOffset.UTC)` (la JDBC URL del proyecto usa `serverTimezone=UTC`). `Instant.from(LocalDateTime)` lanzaría `DateTimeException`.
- **Sin `@Async`**: el broadcast es en proceso y rápido; correr bajo el mismo hilo AFTER_COMMIT evita perder el contexto de suscripción y simplifica el test.
- **try/catch con `log.error`**: no rompe el flujo transaccional principal (regla "cero excepciones silenciadas", auditado en log).
- **Nota:** la dependencia `spring-boot-starter-websocket` ya está en `pom.xml`.

### Test — `WebSocketEventListenerTest`

**Ubicación:** `src/test/java/com/monteastur/envios/listener/WebSocketEventListenerTest.java`

Unit (Mockito):

1. `convertAndSend` recibe el topic `/topic/envios` y un `EnvioEstadoWsMessage` con `envioId`, `tracking`, `estado` y `timestamp` mapeados correctamente desde el evento.
2. No lanza excepción al público: si `convertAndSend` falla, se captura y loguea (no propaga).

## 4. Frontend — dependencias

Instalar en `frontend-react/`:

```bash
npm i sockjs-client stompjs
```

- Proyecto `.js`/`.jsx` (sin TypeScript): no se instalan `@types/*`.
- `stompjs` (client STOMP clásico) + `sockjs-client`: API `Stomp.over(new SockJS(url))`.

## 5. Frontend — hook `useRealTimeEnvios`

**Ubicación:** `frontend-react/src/hooks/useRealTimeEnvios.js`

**API:** `useRealTimeEnvios({ onMessage, enabled = true })` → `{ connected }`

- `connected`: booleano que refleja si la conexión STOMP está activa (para el indicador de estado si se requiere).
- En mount, si `enabled && navigator.onLine`:
  - `Stomp.over(new SockJS('/ws'))`.
  - `connect({}, onConnect, onError)`.
  - En `onConnect`: `subscribe('/topic/envios', frame => onMessage(JSON.parse(frame.body)))` y `setConnected(true)`.
  - Reconexión: al cerrarse o fallar, `setTimeout` de reintento (constante, p. ej. `3000 ms`) **solo mientras el componente siga montado** (guard con ref).
- Cleanup del `useEffect` (obligatorio): `unsubscribe()` + `disconnect()` y limpieza de timers en unmount.
- `onMessage` y `enabled` guardados en refs para evitar cierres obsoletos (el callback no entra en las dependencias del efecto).
- Guard de `navigator.onLine` al inicio; si la app está offline, no intenta conectar (coherente con el diseño offline-first existente: `useOnlineStatus`, `OfflineBanner`).

## 6. Frontend — proxy de Vite (dev)

En `frontend-react/vite.config.js`, añadir al bloque `server.proxy`:

```js
'/ws': {
  target: 'http://localhost:8895',
  ws: true,
  changeOrigin: true,
  secure: false
}
```

- Sin CORS cross-origin en dev: misma origin que `/api`, mismas cookies de sesión.
- No afecta al build de producción.

## 7. Frontend — integración en `AdminDashboard.jsx`

- Importar `useRealTimeEnvios` y usarlo:

```js
const { connected } = useRealTimeEnvios({ onMessage });
```

- `onMessage` (guardado en ref/useCallback estable): al recibir `{ tracking, estado }`:
  - Si algún envío visible en la lista actual (`envios`) tiene `codigoUnico === tracking` → `refreshFn()` (recarga la lista con los filtros/página actuales) + `showInfo('Envío ' + tracking + ' actualizado a ' + etiqueta)`.
  - Si no está visible (filtro/página distintos), **no** refresca ni notifica (evita churn sobre el polling de 15 s).
- El dedup del `ToastProvider` (`type:message` con ventana de 2 s) evita toasts duplicados.
- Reutiliza la infraestructura existente: `showInfo` de `useToast`, `refreshFn` de `usePolling`.

## 8. Tests frontend

### `useRealTimeEnvios.test.js`

`vi.mock('sockjs-client')` y `vi.mock('stompjs')` (fakes manuales):

1. Conecta y se suscribe a `/topic/envios` cuando `enabled` y `online`.
2. Entrega `onMessage` con el body del frame parseado (JSON).
3. No conecta cuando `enabled = false` u offline.
4. `disconnect()` y cleanup en unmount.

### Ajuste de tests existentes

- `AdminDashboard.test.jsx`: mockear `useRealTimeEnvios` (o el módulo `sockjs-client`) para que los tests existentes sigan verdes sin red.

## 9. Verificación final

- Frontend (`frontend-react/`):
  - `npm run lint`
  - `npm run test`
  - `npm run build`
- Backend (raíz, **JDK 25 obligatorio**):
  - `$env:JAVA_HOME = "C:\Users\astur\.jdks\openjdk-25.0.2"; C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd clean test`
  - Esperado: suite previa de 409 tests + `WebSocketEventListenerTest` verdes, `BUILD SUCCESS`.

## 10. Fuera de alcance

- No se integra el hook en `ShipmentDetailPage`/`UpdateEstadoPanel` (solo `AdminDashboard`, decisión del usuario).
- No se autentica el handshake WebSocket (el endpoint `/ws` cae en `.anyRequest().permitAll()`; se mantiene el estado actual).
- No se añade indicador visual permanente de conexión (solo se expone `connected` por si se usa después).
