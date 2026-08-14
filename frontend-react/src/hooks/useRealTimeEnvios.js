import { useEffect, useRef, useState } from 'react'
import SockJS from 'sockjs-client'
import { Stomp } from 'stompjs'

const TOPIC = '/topic/envios'
const RECONNECT_DELAY = 3000

export default function useRealTimeEnvios({ onMessage, enabled = true }) {
  const [connected, setConnected] = useState(false)
  const onMessageRef = useRef(onMessage)
  const enabledRef = useRef(enabled)
  const clientRef = useRef(null)
  const subscriptionRef = useRef(null)
  const reconnectTimerRef = useRef(null)
  const disposedRef = useRef(false)

  useEffect(() => {
    onMessageRef.current = onMessage
  }, [onMessage])

  useEffect(() => {
    enabledRef.current = enabled
  }, [enabled])

  useEffect(() => {
    if (!enabled || typeof navigator !== 'undefined' && !navigator.onLine) {
      return undefined
    }

    disposedRef.current = false

    const connect = () => {
      if (disposedRef.current) return
      const client = Stomp.over(new SockJS('/ws'))
      clientRef.current = client
      client.connect({}, () => {
        if (disposedRef.current) return
        subscriptionRef.current = client.subscribe(TOPIC, (frame) => {
          let message
          try {
            message = JSON.parse(frame.body)
          } catch {
            return
          }
          if (onMessageRef.current) onMessageRef.current(message)
        })
        setConnected(true)
      }, () => {
        setConnected(false)
        if (!disposedRef.current) {
          reconnectTimerRef.current = setTimeout(connect, RECONNECT_DELAY)
        }
      })
    }

    connect()

    return () => {
      disposedRef.current = true
      setConnected(false)
      if (reconnectTimerRef.current) {
        clearTimeout(reconnectTimerRef.current)
        reconnectTimerRef.current = null
      }
      if (subscriptionRef.current) {
        try { subscriptionRef.current.unsubscribe() } catch { /* ya cerrado */ }
        subscriptionRef.current = null
      }
      if (clientRef.current) {
        try { clientRef.current.disconnect() } catch { /* ya cerrado */ }
        clientRef.current = null
      }
    }
  }, [enabled])

  return { connected }
}
