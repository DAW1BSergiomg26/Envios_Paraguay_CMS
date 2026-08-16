import { vi, describe, it, expect, beforeEach } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import useWebSocket from './useWebSocket'

const mockUnsubscribe = vi.fn()
const mockSubscribe = vi.fn(() => ({ unsubscribe: mockUnsubscribe }))
const mockActivate = vi.fn()
const mockDeactivate = vi.fn()
const mockClientInstance = {
  subscribe: mockSubscribe,
  activate: mockActivate,
  deactivate: mockDeactivate,
}

vi.mock('sockjs-client', () => ({
  __esModule: true,
  default: vi.fn(() => ({})),
}))

vi.mock('@stomp/stompjs', () => ({
  __esModule: true,
  Client: vi.fn(() => mockClientInstance),
}))

function getClientConfig() {
  const calls = Client.mock.calls
  return calls[calls.length - 1][0]
}

describe('useWebSocket', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('conecta y se suscribe a /topic/envios con reconnectDelay 5000 por defecto', () => {
    renderHook(() => useWebSocket({ onMessage: vi.fn() }))

    const config = getClientConfig()
    expect(config.reconnectDelay).toBe(5000)
    expect(mockActivate).toHaveBeenCalled()

    act(() => config.onConnect())
    expect(mockSubscribe).toHaveBeenCalledWith('/topic/envios', expect.any(Function))
  })

  it('con accessToken añade ?access_token a la URL de SockJS y Authorization al frame CONNECT', () => {
    renderHook(() => useWebSocket({ onMessage: vi.fn(), accessToken: 'jwt-abc' }))

    const config = getClientConfig()
    config.webSocketFactory()
    expect(SockJS).toHaveBeenCalledWith('/ws?access_token=jwt-abc')
    expect(config.connectHeaders).toEqual({ Authorization: 'Bearer jwt-abc' })
  })

  it('sin accessToken no añade query param ni cabecera de autorización', () => {
    renderHook(() => useWebSocket({ onMessage: vi.fn() }))

    const config = getClientConfig()
    config.webSocketFactory()
    expect(SockJS).toHaveBeenCalledWith('/ws')
    expect(config.connectHeaders).toEqual({})
  })

  it('no crea el cliente cuando está deshabilitado', () => {
    renderHook(() => useWebSocket({ onMessage: vi.fn(), enabled: false }))

    expect(Client).not.toHaveBeenCalled()
    expect(mockActivate).not.toHaveBeenCalled()
  })

  it('conecta cuando se habilita tras estar deshabilitado', () => {
    const { rerender } = renderHook((props) => useWebSocket(props), {
      initialProps: { onMessage: vi.fn(), enabled: false },
    })

    expect(Client).not.toHaveBeenCalled()

    rerender({ onMessage: vi.fn(), enabled: true })
    expect(Client).toHaveBeenCalled()
    expect(mockActivate).toHaveBeenCalled()
  })

  it('entrega el mensaje parseado al onMessage y descarta JSON inválido', () => {
    const onMessage = vi.fn()
    renderHook(() => useWebSocket({ onMessage }))

    const config = getClientConfig()
    act(() => config.onConnect())
    const subscribeCallback = mockSubscribe.mock.calls[0][1]

    act(() => subscribeCallback({ body: JSON.stringify({ envioId: 1, tracking: 'MT-1', estado: 'EN_REPARTO' }) }))
    expect(onMessage).toHaveBeenCalledWith({ envioId: 1, tracking: 'MT-1', estado: 'EN_REPARTO' })

    act(() => subscribeCallback({ body: 'no-json' }))
    expect(onMessage).toHaveBeenCalledTimes(1)
  })

  it('usa endpoint y tópico personalizados', () => {
    renderHook(() => useWebSocket({ onMessage: vi.fn(), endpoint: '/socket', topic: '/topic/custom', accessToken: 'tok' }))

    const config = getClientConfig()
    config.webSocketFactory()
    expect(SockJS).toHaveBeenCalledWith('/socket?access_token=tok')

    act(() => config.onConnect())
    expect(mockSubscribe).toHaveBeenCalledWith('/topic/custom', expect.any(Function))
  })

  it('marca desconectado y estado RECONNECTING al cerrarse el socket', () => {
    const { result } = renderHook(() => useWebSocket({ onMessage: vi.fn() }))

    const config = getClientConfig()
    act(() => config.onConnect())
    expect(result.current.connected).toBe(true)
    expect(result.current.connectionState).toBe('CONNECTED')

    act(() => config.onWebSocketClose())
    expect(result.current.connected).toBe(false)
    expect(result.current.connectionState).toBe('RECONNECTING')
  })

  it('desconecta, cancela la suscripción y limpia al desmontar', () => {
    const { unmount } = renderHook(() => useWebSocket({ onMessage: vi.fn() }))

    const config = getClientConfig()
    act(() => config.onConnect())

    unmount()
    expect(mockUnsubscribe).toHaveBeenCalled()
    expect(mockDeactivate).toHaveBeenCalled()
  })

  it('expone un error ante un frame de error STOMP', () => {
    const { result } = renderHook(() => useWebSocket({ onMessage: vi.fn() }))

    const config = getClientConfig()
    act(() => config.onStompError({ headers: {}, body: 'Acceso denegado' }))
    expect(result.current.error).toBeTruthy()
  })
})
