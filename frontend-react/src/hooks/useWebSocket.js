import { useCallback, useEffect, useRef, useState } from 'react'
import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'

const CONNECTION_STATES = {
  IDLE: 'IDLE',
  CONNECTING: 'CONNECTING',
  CONNECTED: 'CONNECTED',
  RECONNECTING: 'RECONNECTING',
  CLOSED: 'CLOSED',
}

const HEARTBEAT_MS = 10000

/**
 * Hook de conexión STOMP segura vía SockJS.
 * - Token-ready: si se provee `accessToken`, se envía como `?access_token` en la
 *   URL de SockJS (SockJS no permite cabeceras en el handshake HTTP) y como
 *   `Authorization: Bearer` en el frame STOMP CONNECT.
 * - La reconexión automática la gestiona @stomp/stompjs con `reconnectDelay`.
 */
export default function useWebSocket({
  onMessage,
  enabled = true,
  accessToken = null,
  topic = '/topic/envios',
  endpoint = '/ws',
  reconnectDelay = 5000,
}) {
  const [connected, setConnected] = useState(false)
  const [connectionState, setConnectionState] = useState(CONNECTION_STATES.IDLE)
  const [error, setError] = useState(null)

  const onMessageRef = useRef(onMessage)
  const enabledRef = useRef(enabled)
  const clientRef = useRef(null)
  const subscriptionRef = useRef(null)
  const disposedRef = useRef(true)

  useEffect(() => { onMessageRef.current = onMessage }, [onMessage])
  useEffect(() => { enabledRef.current = enabled }, [enabled])

  const connect = useCallback(() => {
    if (disposedRef.current || !enabledRef.current || clientRef.current) return

    const url = endpoint + (accessToken ? `?access_token=${encodeURIComponent(accessToken)}` : '')
    const connectHeaders = accessToken ? { Authorization: `Bearer ${accessToken}` } : {}

    const client = new Client({
      webSocketFactory: () => new SockJS(url),
      connectHeaders,
      reconnectDelay,
      heartbeatIncoming: HEARTBEAT_MS,
      heartbeatOutgoing: HEARTBEAT_MS,
      onConnect: () => {
        if (disposedRef.current) return
        subscriptionRef.current = client.subscribe(topic, (frame) => {
          let message
          try {
            message = JSON.parse(frame.body)
          } catch {
            return
          }
          if (onMessageRef.current) onMessageRef.current(message)
        })
        setError(null)
        setConnected(true)
        setConnectionState(CONNECTION_STATES.CONNECTED)
      },
      onStompError: (frame) => {
        setError(frame?.body || 'Error STOMP')
      },
      onWebSocketClose: () => {
        setConnected(false)
        if (!disposedRef.current) setConnectionState(CONNECTION_STATES.RECONNECTING)
      },
      onWebSocketError: (event) => {
        setConnected(false)
        setError(event?.message || 'Error de conexión WebSocket')
        if (!disposedRef.current) setConnectionState(CONNECTION_STATES.RECONNECTING)
      },
    })

    clientRef.current = client
    setConnected(false)
    setConnectionState(CONNECTION_STATES.CONNECTING)
    client.activate()
  }, [accessToken, topic, endpoint, reconnectDelay])

  useEffect(() => {
    if (!enabled) return undefined

    disposedRef.current = false

    const isOffline = typeof navigator !== 'undefined' && !navigator.onLine
    if (!isOffline) connect()

    const handleOnline = () => connect()
    window.addEventListener('online', handleOnline)

    return () => {
      disposedRef.current = true
      window.removeEventListener('online', handleOnline)
      setConnected(false)
      setConnectionState(CONNECTION_STATES.CLOSED)
      if (subscriptionRef.current) {
        try { subscriptionRef.current.unsubscribe() } catch { /* ya cerrado */ }
        subscriptionRef.current = null
      }
      if (clientRef.current) {
        try { clientRef.current.deactivate() } catch { /* ya cerrado */ }
        clientRef.current = null
      }
    }
  }, [enabled, connect])

  return { connected, connectionState, error }
}
